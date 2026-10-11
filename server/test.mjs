// SPDX-License-Identifier: GPL-3.0-or-later
import {spawn} from 'node:child_process';
import {mkdtempSync,rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import path from 'node:path';
import assert from 'node:assert/strict';
const dir=mkdtempSync(path.join(tmpdir(),'kingfischer-tests-')),port=18379,base='http://127.0.0.1:'+port;
let processHandle;
async function start(){processHandle=spawn(process.execPath,['server.mjs'],{cwd:path.dirname(new URL(import.meta.url).pathname),env:{...process.env,PORT:String(port),HOST:'127.0.0.1',ROOM_DATA:path.join(dir,'rooms.json')},stdio:['ignore','pipe','inherit']});await new Promise((ok,bad)=>{const timeout=setTimeout(()=>bad(new Error('Server did not start')),10000);processHandle.stdout.once('data',()=>{clearTimeout(timeout);ok();});processHandle.once('error',bad);});}
async function stop(){if(processHandle){const child=processHandle;processHandle=null;await new Promise(ok=>{child.once('exit',ok);child.kill();});}}
async function api(path,token,body){const response=await fetch(base+path,{method:body?'POST':'GET',headers:{...(token?{Authorization:'Bearer '+token}:{}),'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});return {status:response.status,body:await response.json()};}
try{
 await start();let response=await api('/api/rooms',null,{});assert.equal(response.status,201);const white=response.body,id=white.id,prefix='/api/rooms/'+id;
 assert.equal((await api(prefix,null)).status,401);
 assert.equal((await api(prefix+'/move',white.token,{ply:0,move:'e2e4'})).status,409);
 assert.equal((await api(prefix+'/join',null,{invite:'wrong',joinKey:'a'.repeat(48)})).status,403);
 const black=(await api(prefix+'/join',null,{invite:white.invite,joinKey:'a'.repeat(48)})).body;
 assert.equal((await api(prefix+'/join',null,{invite:white.invite,joinKey:'a'.repeat(48)})).body.token,black.token);
 assert.equal((await api(prefix+'/join',null,{invite:white.invite,joinKey:'b'.repeat(48)})).status,409);
 assert.equal((await api(prefix+'/move',black.token,{ply:0,move:'e2e4'})).status,403);
 assert.equal((await api(prefix+'/move',white.token,{ply:0,move:'e2e5'})).status,400);
 response=await api(prefix+'/move',white.token,{ply:0,move:'e2e4'});assert.equal(response.status,200);assert.deepEqual(response.body.moves,['e2e4']);
 response=await api(prefix+'/move',white.token,{ply:0,move:'e2e4'});assert.equal(response.status,200);assert.equal(response.body.moves.length,1);
 assert.equal((await api(prefix+'/move',black.token,{ply:0,move:'e7e5'})).status,409);
 const race=await Promise.all([api(prefix+'/move',black.token,{ply:1,move:'e7e5'}),api(prefix+'/move',black.token,{ply:1,move:'c7c5'})]);assert.equal(race.filter(x=>x.status===200).length,1);assert.equal(race.filter(x=>x.status===409).length,1);
 await stop();await start();response=await api(prefix,white.token);assert.equal(response.body.moves.length,2);assert.equal(response.body.joined,true);
 response=await api(prefix+'/resign',black.token,{});assert.equal(response.body.resultCode,'1-0');assert.equal((await api(prefix+'/move',white.token,{ply:2,move:'g1f3'})).status,409);
 const landing=await fetch(base+'/invite?room='+id+'&invite='+white.invite);assert.equal(landing.status,200);assert.match(await landing.text(),/kingfischer:\/\/join/);
 const fresh=(await api('/api/rooms',null,{})).body;const guest=(await api('/api/rooms/'+fresh.id+'/join',null,{invite:fresh.invite,joinKey:'c'.repeat(48)})).body;
 const fool=['f2f3','e7e5','g2g4','d8h4'];for(let ply=0;ply<fool.length;ply++){response=await api('/api/rooms/'+fresh.id+'/move',ply%2?guest.token:fresh.token,{ply,move:fool[ply]});assert.equal(response.status,200);}
 assert.equal(response.body.resultCode,'0-1');assert.match(response.body.result,/checkmate/);
 console.log('PASS: create/join, private access, turns, illegal moves, idempotency, concurrent moves, restart/reconnect, resignation, invite landing and checkmate.');
}finally{await stop();rmSync(dir,{recursive:true,force:true});}
