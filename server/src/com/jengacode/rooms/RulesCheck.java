// SPDX-License-Identifier: GPL-3.0-or-later
package com.jengacode.rooms;
import com.kingfischer.engine.gui.GameModel;
import com.kingfischer.engine.chess.Move;

/** Stateless authoritative validation using exactly the same engine rules as Android. */
public final class RulesCheck {
 public static void main(String[] args){try{
  GameModel g=new GameModel();String text=args.length==0?"":args[0];if(text.length()>10000)throw new IllegalArgumentException("Move list too long");
  if(!text.isEmpty())for(String move:text.split(" ")){if(g.size()>=1024)throw new IllegalArgumentException("Game length limit reached");int m=g.position().parseMove(move);if(m==0)throw new IllegalArgumentException("Illegal move");g.play(m);}
  String r=g.result();System.out.print("{\"side\":"+g.position().side+",\"result\":"+(r==null?"null":"\""+r+"\"")+",\"resultCode\":\""+g.resultCode()+"\",\"fen\":\""+g.position().fen()+"\"}");
 }catch(Exception e){System.err.print(e.getMessage());System.exit(2);}}
}
