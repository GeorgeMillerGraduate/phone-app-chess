// SPDX-License-Identifier: GPL-3.0-or-later
package com.jengacode.kingfischer;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import com.kingfischer.engine.chess.*;
import com.kingfischer.engine.movegen.MoveGenerator;
import java.util.*;

/** Exact engine squares, a1=0. Never mutates the position supplied by its owner. */
public final class ChessBoard extends View {
 public interface Listener { void onMove(int[] candidates); }
 private Position position = new Position();
 private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Bitmap[] pieces = new Bitmap[16];
 private final RectF rect = new RectF();
 private final int[] drawLegal=new int[256];
 private int selected=-1, last=0; private boolean flipped, enabled; private Listener listener;
 public ChessBoard(Context context) { super(context); setContentDescription("Chess board. Tap a piece, then its destination.");
  String[] types={"","pawn","knight","bishop","rook","queen","king"};
  for(int c=0;c<2;c++) for(int t=1;t<=6;t++) try(java.io.InputStream in=context.getAssets().open("pieces/"+(c==0?"white_":"black_")+types[t]+".png")){pieces[c*8+t]=BitmapFactory.decodeStream(in);} catch(java.io.IOException e){throw new IllegalStateException(e);}
 }
 public void set(Position p, boolean flip, int lastMove, boolean interactive, Listener l){position=p;flipped=flip;last=lastMove;enabled=interactive;listener=l;selected=-1;invalidate();}
 @Override protected void onMeasure(int w,int h){int side=MeasureSpec.getSize(w);setMeasuredDimension(side,side);}
 private int square(int col,int row){return flipped?(7-col)+row*8:col+(7-row)*8;}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas); float cell=getWidth()/8f;
  int[] legal=drawLegal; int n=selected<0?0:MoveGenerator.legal(position,legal);
  for(int r=0;r<8;r++)for(int c=0;c<8;c++){int sq=square(c,r);float x=c*cell,y=r*cell;
   paint.setColor(((sq/8+sq%8)&1)==0?Color.rgb(111,143,124):Color.rgb(237,230,213));canvas.drawRect(x,y,x+cell,y+cell,paint);
   int pc=position.board[sq];if(pc!=0&&pieces[pc]!=null){rect.set(x+cell*.06f,y+cell*.04f,x+cell*.94f,y+cell*.96f);canvas.drawBitmap(pieces[pc],null,rect,null);}
   if(sq==selected || position.inCheck()&&sq==position.king(position.side)){paint.setColor(sq==selected?0xFF315D50:0xFFB6463A);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(cell*.045f);canvas.drawRect(x+2,y+2,x+cell-2,y+cell-2,paint);paint.setStyle(Paint.Style.FILL);}
   for(int i=0;i<n;i++)if(Move.from(legal[i])==selected&&Move.to(legal[i])==sq){paint.setColor(0x88334D3F);canvas.drawCircle(x+cell/2,y+cell/2,cell*.1f,paint);break;}
   paint.setTextSize(cell*.19f);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setColor(((sq/8+sq%8)&1)==0?0xFFEFE8D6:0xFF476354);
   if(c==0)canvas.drawText(""+(sq/8+1),x+3,y+cell*.2f,paint);
   if(r==7)canvas.drawText(""+(char)('a'+sq%8),x+cell*.8f,y+cell-3,paint);
  }
 }
 @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;performClick();if(!enabled||listener==null)return true;
  int col=(int)(e.getX()/(getWidth()/8f)),row=(int)(e.getY()/(getWidth()/8f));if(col<0||col>7||row<0||row>7)return true;
  int sq=square(col,row);int[] legal=new int[256];int n=MoveGenerator.legal(position,legal);ArrayList<Integer> found=new ArrayList<>();
  if(selected>=0)for(int i=0;i<n;i++)if(Move.from(legal[i])==selected&&Move.to(legal[i])==sq)found.add(legal[i]);
  if(!found.isEmpty()){selected=-1;listener.onMove(found.stream().mapToInt(Integer::intValue).toArray());}
  else {int pc=position.board[sq];selected=pc!=0&&(pc>>3)==position.side?sq:-1;invalidate();}return true;
 }
 @Override public boolean performClick(){return super.performClick();}
}
