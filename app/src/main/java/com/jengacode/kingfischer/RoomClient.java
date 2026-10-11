// SPDX-License-Identifier: GPL-3.0-or-later
package com.jengacode.kingfischer;
import android.net.Uri;
import android.os.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class RoomClient {
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler ui=new Handler(Looper.getMainLooper());
 public static String server(String address){try{URI u=new URI(address.trim());if(u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null||!("https".equals(u.getScheme())||BuildConfig.DEBUG&&"http".equals(u.getScheme())))throw new Exception();String path=u.getPath();if(path!=null&&!path.isEmpty()&&!path.equals("/"))throw new Exception();return u.getScheme()+"://"+u.getRawAuthority();}catch(Exception e){throw new IllegalArgumentException("Use a server address such as https://chess.example.com"+(BuildConfig.DEBUG?" or http://192.168.1.10:8080 for a local test.":"."));}}
 public static JSONObject invite(String text) throws Exception {Uri u=Uri.parse(text.trim());if(!(("kingfischer".equals(u.getScheme())&&"join".equals(u.getHost()))||(("https".equals(u.getScheme())||BuildConfig.DEBUG&&"http".equals(u.getScheme()))&&"/invite".equals(u.getPath()))))throw new IllegalArgumentException("Paste a KingFischer invitation link.");String base=u.getQueryParameter("server");if(base==null)base=u.getScheme()+"://"+u.getEncodedAuthority();String room=u.getQueryParameter("room"),key=u.getQueryParameter("invite");if(room==null||key==null||!room.matches("[a-f0-9]{24}")||!key.matches("[a-f0-9]{48}"))throw new IllegalArgumentException("The invitation is incomplete.");return new JSONObject().put("server",server(base)).put("room",room).put("invite",key);}
 public void call(String server,String path,String token,JSONObject body,Consumer<JSONObject> done,Consumer<String> error){worker.submit(()->{HttpURLConnection c=null;try{
  c=(HttpURLConnection)new URL(server+path).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setRequestProperty("Accept","application/json");if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
  if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
  int code=c.getResponseCode();InputStream stream=code<400?c.getInputStream():c.getErrorStream();if(stream==null)throw new IOException("Server returned "+code);
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=stream){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(bytes.size()+n>200000)throw new IOException("Response is too large");bytes.write(b,0,n);}}
  JSONObject response=new JSONObject(bytes.toString("UTF-8"));if(code>=400)throw new IOException(response.optString("error","Server returned "+code));ui.post(()->done.accept(response));
 }catch(Exception e){String message=e.getMessage()==null?"Unable to reach the room server":e.getMessage();ui.post(()->error.accept(message));}finally{if(c!=null)c.disconnect();}});}
 public void close(){worker.shutdownNow();}
}
