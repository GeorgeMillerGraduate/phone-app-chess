// SPDX-License-Identifier: GPL-3.0-or-later
package com.jengacode.kingfischer;
import android.content.Context;
import android.os.*;
import com.kingfischer.engine.chess.Position;
import com.kingfischer.engine.nnue.Network;
import com.kingfischer.engine.search.*;
import com.kingfischer.engine.history.HistoryTables;
import com.kingfischer.engine.tt.TranspositionTable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.*;
import java.util.regex.*;
import com.kingfischer.engine.chess.Move;

/** Shared across Activity recreation; one network in memory, one search at a time. */
public final class EngineService {
 private static EngineService singleton;
 public static synchronized EngineService get(Context c){if(singleton==null)singleton=new EngineService(c.getApplicationContext());return singleton;}
 private final android.content.res.AssetManager assets; private final Handler ui=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private final AtomicLong generation=new AtomicLong();
 private volatile Search active;private Network network;
 private EngineService(Context c){assets=c.getAssets();}
 public void cancel(){generation.incrementAndGet();Search s=active;if(s!=null)s.stop();}
 public void search(Position p,long millis,int depth,Consumer<SearchResult> done,Consumer<String> info,Consumer<String> error){
  run(p,millis,depth,0,done,info,error);
 }
 public void play(Position p,int rating,Consumer<SearchResult> done,Consumer<String> info,Consumer<String> error){Difficulty.check(rating);run(p,Difficulty.millis(rating),Difficulty.depth(rating),rating,done,info,error);}
 private void run(Position p,long millis,int depth,int rating,Consumer<SearchResult> done,Consumer<String> info,Consumer<String> error){
  cancel();long ticket=generation.get();Position snapshot=p.copy();Position rootPosition=snapshot.copy();
  worker.submit(()->{try{
   if(ticket!=generation.get())return;
   if(network==null){ui.post(()->{if(ticket==generation.get())info.accept("Loading your engine’s neural network…");});network=Network.fromStream(assets.open("nnue.nnue"));}
   if(ticket!=generation.get())return;
   SearchLimits limits=new SearchLimits();limits.moveTime=millis;limits.depth=depth;if(rating>0)limits.nodes=Difficulty.nodes(rating);
   final Map<Integer,Integer> alternatives=new TreeMap<>();final int[] reportedDepth={0};
   Pattern candidate=Pattern.compile("info depth (\\d+).*?multipv (\\d+).*? pv ([a-h][1-8][a-h][1-8][qrbn]?)");
   Search s=new Search(snapshot,network,new TranspositionTable(8),new HistoryTables(),limits,rating>0&&rating<2400?3:1,20,false,line->{if(rating>0){Matcher match=candidate.matcher(line);if(match.find()){int d=Integer.parseInt(match.group(1));if(d>reportedDepth[0]){alternatives.clear();reportedDepth[0]=d;}if(d==reportedDepth[0]){try{int m=rootPosition.parseMove(match.group(3));if(m!=0)alternatives.put(Integer.parseInt(match.group(2)),m);}catch(IllegalArgumentException ignored){}}}}if(line.startsWith("info depth"))ui.post(()->{if(ticket==generation.get())info.accept(line);});});
   active=s;if(ticket!=generation.get()){s.stop();return;}SearchResult result=s.run();
   if(rating>0&&alternatives.size()>1&&ThreadLocalRandom.current().nextDouble()<Difficulty.alternateProbability(rating)){ArrayList<Integer> choices=new ArrayList<>(alternatives.values());choices.remove(Integer.valueOf(result.bestMove()));if(!choices.isEmpty())result=new SearchResult(choices.get(ThreadLocalRandom.current().nextInt(choices.size())),0,result.score(),result.depth(),result.nodes(),result.milliseconds());}
   final SearchResult completed=result;
   ui.post(()->{if(ticket==generation.get())done.accept(completed);});
  }catch(Exception|OutOfMemoryError e){ui.post(()->{if(ticket==generation.get())error.accept(e.getMessage()==null?e.toString():e.getMessage());});}
  finally {active=null;}});
 }
}
