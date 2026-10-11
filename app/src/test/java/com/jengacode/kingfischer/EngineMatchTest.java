// SPDX-License-Identifier: GPL-3.0-or-later
package com.jengacode.kingfischer;
import org.junit.Test;
import static org.junit.Assert.*;
import com.kingfischer.engine.chess.*;
import com.kingfischer.engine.gui.GameModel;
import com.kingfischer.engine.nnue.Network;
import com.kingfischer.engine.search.*;
import com.kingfischer.engine.history.HistoryTables;
import com.kingfischer.engine.tt.TranspositionTable;
import java.nio.file.*;

public class EngineMatchTest {
 @Test public void ratingsHaveDistinctBoundedEffort(){assertEquals(1200,Difficulty.check(1200));assertThrows(IllegalArgumentException.class,()->Difficulty.check(0));assertThrows(IllegalArgumentException.class,()->Difficulty.check(3001));for(int rating=400;rating<3000;rating+=100){assertTrue(Difficulty.nodes(rating+100)>Difficulty.nodes(rating));assertTrue(Difficulty.millis(rating+100)>Difficulty.millis(rating));assertTrue(Difficulty.alternateProbability(rating+100)<Difficulty.alternateProbability(rating));}}
 @Test public void twoRatedEnginesCanPlayAndExportAReplayableGame()throws Exception{
  Path file=Paths.get("src/main/assets/nnue.nnue");if(!Files.exists(file))file=Paths.get("app/src/main/assets/nnue.nnue");Network net=Network.fromStream(Files.newInputStream(file));GameModel game=new GameModel();int[] ratings={800,1800};
  for(int ply=0;ply<8&&game.result()==null;ply++){int rating=ratings[game.position().side];SearchLimits l=new SearchLimits();l.depth=Difficulty.depth(rating);l.nodes=Difficulty.nodes(rating);l.moveTime=Difficulty.millis(rating);Position snapshot=game.position().copy();SearchResult result=new Search(snapshot,net,new TranspositionTable(4),new HistoryTables(),l,3,20,false,line->{}).run();assertNotEquals(0,result.bestMove());assertEquals(result.bestMove(),game.position().parseMove(Move.uci(result.bestMove())));game.play(result.bestMove());}
  assertEquals(8,game.size());assertEquals(GameIO.moves(game),GameIO.moves(GameIO.importGame(GameIO.pgn(game,"KingFischer 800","KingFischer 1800",game.resultCode()))));
 }
}
