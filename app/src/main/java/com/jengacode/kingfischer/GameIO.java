// SPDX-License-Identifier: GPL-3.0-or-later
package com.jengacode.kingfischer;
import com.kingfischer.engine.chess.*;
import com.kingfischer.engine.gui.*;

/** Main-line standard-start PGN and UCI sequences. Variations and custom FEN are explicitly rejected. */
public final class GameIO {
 public static String moves(GameModel game){StringBuilder out=new StringBuilder();for(int i=0;i<game.size();i++){if(i>0)out.append(' ');out.append(Move.uci(game.move(i)));}return out.toString();}
 public static GameModel fromMoves(String text){GameModel g=new GameModel();if(text.trim().isEmpty())return g;for(String token:text.trim().split("\\s+")){if(g.size()>=1024)throw new IllegalArgumentException("Maximum 1,024 half-moves");int m=g.position().parseMove(token);if(m==0)throw new IllegalArgumentException("Illegal move: "+token);g.play(m);}return g;}
 public static GameModel importGame(String text){
  if(text.length()>100000)throw new IllegalArgumentException("Game text is too large");
  if(text.matches("(?s).*\\[\\s*(FEN|SetUp)\\s+.*"))throw new IllegalArgumentException("This version imports games from the standard starting position only.");
  String body=text.replaceAll("(?s)\\[[^\\]]*\\]", " ").replaceAll("(?s)\\{[^}]*\\}"," ").replaceAll("(?m);[^\\r\\n]*", " ");
  StringBuilder line=new StringBuilder();int nesting=0;for(char c:body.toCharArray()){if(c=='('){nesting++;line.append(' ');}else if(c==')'){if(--nesting<0)throw new IllegalArgumentException("Unbalanced PGN variation");line.append(' ');}else if(nesting==0)line.append(c);}if(nesting!=0)throw new IllegalArgumentException("Unbalanced PGN variation");
  body=line.toString().replaceAll("\\$\\d+"," ").replaceAll("\\d+\\.(\\.\\.)?", " ");GameModel g=new GameModel();
  for(String token:body.trim().split("\\s+")){if(token.isEmpty()||token.equals("*")||token.equals("1-0")||token.equals("0-1")||token.equals("1/2-1/2"))continue;
   if(g.size()>=1024)throw new IllegalArgumentException("Maximum 1,024 half-moves");String desired=normalize(token);int move=0;
   for(int candidate:g.legal())if(normalize(MoveNotation.format(g.position(),candidate,false)).equals(desired)||Move.uci(candidate).equals(token)){move=candidate;break;}
   if(move==0)throw new IllegalArgumentException("Cannot read move "+(g.size()/2+1)+": "+token);g.play(move);
  }return g;
 }
 private static String normalize(String s){return s.replace('0','O').replaceAll("[!?+#]","");}
 public static String pgn(GameModel g,String white,String black,String result){StringBuilder out=new StringBuilder("[Event \"KingFischer casual game\"]\n[White \""+white+"\"]\n[Black \""+black+"\"]\n[Result \""+result+"\"]\n\n");for(int i=0;i<g.size();i++){if((i&1)==0)out.append(i/2+1).append(". ");out.append(g.notation(i)).append(' ');}return out.append(result).append('\n').toString();}
}
