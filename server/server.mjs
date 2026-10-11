// SPDX-License-Identifier: GPL-3.0-or-later
import http from 'node:http';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {execFile} from 'node:child_process';
import {promisify} from 'node:util';
const exec=promisify(execFile), root=path.dirname(fileURLToPath(import.meta.url));
const file=process.env.ROOM_DATA||path.join(root,'data','rooms.json');
const ttl=72*3600000, random=n=>crypto.randomBytes(n).toString('hex');
const rooms=new Map(), rates=new Map(), queues=new Map();
let validations=0;
fs.mkdirSync(path.dirname(file),{recursive:true});
if(fs.existsSync(file)){const saved=JSON.parse(fs.readFileSync(file,'utf8'));for(const r of saved)if(Date.now()-r.updated<ttl)rooms.set(r.id,r);}
function persist(){const temp=file+'.tmp';fs.writeFileSync(temp,JSON.stringify([...rooms.values()]),{mode:0o600});fs.renameSync(temp,file);}
function failure(status,message){const e=new Error(message);e.status=status;throw e;}
function equal(a,b){if(typeof a!=='string'||typeof b!=='string'||a.length!==b.length)return false;return crypto.timingSafeEqual(Buffer.from(a),Buffer.from(b));}
function auth(req,r){const t=(req.headers.authorization||'').replace(/^Bearer /,'');if(equal(t,r.white))return 0;if(r.black&&equal(t,r.black))return 1;failure(401,'Player access is invalid.');}
function state(r){return {id:r.id,moves:r.moves,joined:!!r.black,result:r.result||null,resultCode:r.resultCode||'*',updated:r.updated};}
function send(res,status,body){res.writeHead(status,{'Content-Type':'application/json','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end(JSON.stringify(body));}
async function body(req){const chunks=[];let length=0;for await(const b of req){length+=b.length;if(length>16384)failure(413,'Request too large');chunks.push(b);}try{return JSON.parse(Buffer.concat(chunks).toString('utf8')||'{}');}catch{failure(400,'Invalid JSON');}}
async function validate(moves){if(validations>=4)failure(503,'Server is busy; please try again.');validations++;try{const {stdout}=await exec(process.env.JAVA||'java',['-Xmx64m','-cp',path.join(root,'rules.jar'),'com.jengacode.rooms.RulesCheck',moves.join(' ')],{timeout:10000,maxBuffer:32768});return JSON.parse(stdout);}catch{failure(400,'Illegal move or finished game.');}finally{validations--;}}
function serial(id,fn){const prior=queues.get(id)||Promise.resolve();const next=prior.catch(()=>{}).then(fn);queues.set(id,next);return next.finally(()=>{if(queues.get(id)===next)queues.delete(id);});}
function expire(){let changed=false;for(const [id,r]of rooms)if(Date.now()-r.updated>ttl){rooms.delete(id);changed=true;}if(changed)persist();}
const escape=s=>s.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;').replaceAll("'",'&#39;');
const server=http.createServer(async(req,res)=>{try{
 const direct=req.socket.remoteAddress||'unknown';const forwarded=String(req.headers['x-forwarded-for']||'').split(',')[0].trim();const ip=process.env.TRUST_PROXY==='1'&&forwarded?forwarded:direct,now=Date.now();let rate=rates.get(ip);if(!rate||now-rate.since>60000){rate={since:now,count:0};rates.set(ip,rate);}if(++rate.count>180)failure(429,'Too many requests. Wait a moment.');
 const url=new URL(req.url,'http://localhost');
 if(req.method==='GET'&&url.pathname==='/health'){send(res,200,{status:'ok'});return;}
 if(req.method==='GET'&&url.pathname==='/invite'){
  const room=url.searchParams.get('room'),invite=url.searchParams.get('invite');if(!/^[a-f0-9]{24}$/.test(room||'')||!/^[a-f0-9]{48}$/.test(invite||''))failure(400,'Invalid invitation');
  const base=process.env.PUBLIC_URL||'http://'+req.headers.host;const u=new URL(base);if(!['http:','https:'].includes(u.protocol)||u.username||u.password)failure(500,'Invalid public URL');
  const deep='kingfischer://join?server='+encodeURIComponent(u.origin)+'&room='+room+'&invite='+invite;
  res.writeHead(200,{'Content-Type':'text/html; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer','Content-Security-Policy':"default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'"});
  res.end(`<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Join a KingFischer game</title><style>body{font:17px system-ui;background:#f5f1e8;color:#203b32;max-width:500px;margin:10vh auto;padding:24px}h1{font-size:36px}a{display:inline-block;background:#315d50;color:white;padding:16px 24px;border-radius:10px;text-decoration:none}p{line-height:1.6}</style><h1>Your move.</h1><p>You’ve been invited to a private KingFischer chess game.</p><a href="${escape(deep)}">Open KingFischer</a><p>If the app is already installed, tap the button. Otherwise, install it from the project’s APK, then open <b>Play friend → Join invitation</b> and paste this page’s address.</p><p>This invitation gives access to one player seat. Share it privately.</p></html>`);return;
 }
 if(req.method==='POST'&&url.pathname==='/api/rooms'){
  expire();if(rooms.size>=1000)failure(503,'Room limit reached. Try again later.');await body(req);
  const r={id:random(12),white:random(24),black:null,invite:random(24),moves:[],result:null,resultCode:'*',updated:now};rooms.set(r.id,r);persist();send(res,201,{id:r.id,token:r.white,invite:r.invite,color:0});return;
 }
 const match=url.pathname.match(/^\/api\/rooms\/([a-f0-9]{24})(?:\/(join|move|resign))?$/);if(!match)failure(404,'Route not found');
 const id=match[1],action=match[2];const r=rooms.get(id);if(!r||now-r.updated>ttl)failure(404,'Room expired or not found');
 if(req.method==='GET'&&!action){auth(req,r);send(res,200,state(r));return;}
 if(req.method!=='POST')failure(405,'Method not allowed');const payload=await body(req);
 await serial(id,async()=>{
  if(action==='join'){if(!equal(payload.invite,r.invite))failure(403,'Invitation is invalid');if(!/^[a-f0-9]{48}$/.test(payload.joinKey||''))failure(400,'Invalid join key');if(r.black&&!equal(payload.joinKey,r.joinKey))failure(409,'Your friend has already joined this room');if(!r.black){r.black=random(24);r.joinKey=payload.joinKey;r.updated=Date.now();persist();}send(res,200,{id:r.id,token:r.black,color:1});return;}
  const color=auth(req,r);
  if(action==='move'){
   if(!r.black)failure(409,'Wait for your friend to join.');if(typeof payload.ply!=='number'||!Number.isInteger(payload.ply)||!/^([a-h][1-8]){2}[qrbn]?$/.test(payload.move||''))failure(400,'Invalid move request');
   if(payload.ply===r.moves.length-1&&r.moves[payload.ply]===payload.move&&(payload.ply&1)===color){send(res,200,state(r));return;}
   if(r.result)failure(409,'Game has ended');if(payload.ply!==r.moves.length)failure(409,'The board has changed. Sync and retry.');if((r.moves.length&1)!==color)failure(403,'It is your friend’s turn');
   const verdict=await validate([...r.moves,payload.move]);r.moves.push(payload.move);r.result=verdict.result;r.resultCode=verdict.resultCode;
  }else if(action==='resign'){
   if(!r.result){r.result=(color===0?'Black':'White')+' wins by resignation';r.resultCode=color===0?'0-1':'1-0';}
  }else failure(404,'Route not found');
  r.updated=Date.now();persist();send(res,200,state(r));
 });
 }catch(e){if(!res.headersSent)send(res,e.status||500,{error:e.status?e.message:'Server error'});else res.end();}});
const sweep=setInterval(()=>{expire();for(const[ip,r]of rates)if(Date.now()-r.since>120000)rates.delete(ip);},60000);sweep.unref();
server.requestTimeout=15000;server.headersTimeout=10000;
server.listen(Number(process.env.PORT||8080),process.env.HOST||'0.0.0.0',()=>console.log('KingFischer rooms listening on port '+server.address().port));
