// SPDX-License-Identifier: GPL-3.0-or-later
package com.jengacode.kingfischer;
import org.junit.Test;
import static org.junit.Assert.*;
import com.kingfischer.engine.chess.*;
import com.kingfischer.engine.gui.*;
import com.kingfischer.engine.util.Perft;
import com.kingfischer.engine.nnue.*;
import com.kingfischer.engine.search.*;
import com.kingfischer.engine.history.HistoryTables;
import com.kingfischer.engine.tt.TranspositionTable;
import java.nio.file.*;

public class ChessIntegrationTest {
 @Test public void startingMoveGeneration(){assertEquals(197281,new Perft().count(new Position(),4));}
 @Test public void pgnRoundTrip(){GameModel g=GameIO.importGame("[Event \"Test\"]\n1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 *");assertEquals(6,g.size());assertEquals(GameIO.moves(g),GameIO.moves(GameIO.importGame(GameIO.pgn(g,"White","Black","*"))));}
 @Test public void variationAndComments(){GameModel g=GameIO.importGame("1. e4 {hello} (1. d4 d5) e5 2. Nf3 $1 Nc6 *");assertEquals("e2e4 e7e5 g1f3 b8c6",GameIO.moves(g));}
 @Test public void illegalMovesRejected(){assertThrows(IllegalArgumentException.class,()->GameIO.importGame("1. e5"));assertThrows(IllegalArgumentException.class,()->GameIO.fromMoves("e2e5"));}
 @Test public void historySnapshotDoesNotChangeCurrentGame(){GameModel g=GameIO.fromMoves("e2e4 e7e5 g1f3");String fen=g.position().fen();assertEquals(Position.START,g.at(0).fen());assertEquals(fen,g.position().fen());g.undoTurn(0);assertEquals("e2e4 e7e5",GameIO.moves(g));}
 @Test public void checkmateAndRepetition(){GameModel mate=GameIO.fromMoves("f2f3 e7e5 g2g4 d8h4");assertEquals("0-1",mate.resultCode());GameModel draw=GameIO.fromMoves("g1f3 g8f6 f3g1 f6g8 g1f3 g8f6 f3g1 f6g8");assertEquals("Draw by threefold repetition",draw.result());}
 @Test public void castlingAndPromotionNotation(){GameModel g=GameIO.importGame("1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. O-O Nf6 *");assertTrue((g.move(6)&Move.CASTLE)!=0);Position p=new Position("7k/P7/8/8/8/8/8/7K w - - 0 1");int m=p.parseMove("a7a8q");assertEquals(5,Move.promotion(m));assertTrue(MoveNotation.format(p,m,false).startsWith("a8=Q"));}
 @Test public void bundledNetworkSearchReturnsLegalMove()throws Exception {Path path=Paths.get("src/main/assets/nnue.nnue");if(!Files.exists(path))path=Paths.get("app/src/main/assets/nnue.nnue");Network net=Network.fromStream(Files.newInputStream(path));Position p=new Position();SearchLimits l=new SearchLimits();l.depth=2;l.moveTime=1000;SearchResult r=new Search(p.copy(),net,new TranspositionTable(4),new HistoryTables(),l,1,20,false,line->{}).run();assertTrue(r.depth()>0);assertTrue(r.nodes()>0);assertEquals(r.bestMove(),p.parseMove(Move.uci(r.bestMove())));assertEquals(Position.START,p.fen());}
}
