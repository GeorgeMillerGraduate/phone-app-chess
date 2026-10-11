// SPDX-License-Identifier: GPL-3.0-or-later
package com.jengacode.kingfischer;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import com.kingfischer.engine.chess.*;
import com.kingfischer.engine.gui.GameModel;
import java.util.function.IntConsumer;

/** Numbered two-column figurine SAN, with the actual piece PNGs and standard SAN accessibility labels. */
public final class MoveList extends ScrollView {
 private final Bitmap[] images=new Bitmap[16];private final LinearLayout rows;
 private GameModel cached;private int count=-1,ply=-1;
 public MoveList(Context c){super(c);setBackgroundColor(0xFFECEBE7);rows=new LinearLayout(c);rows.setOrientation(LinearLayout.VERTICAL);addView(rows);String[] names={"","pawn","knight","bishop","rook","queen","king"};for(int color=0;color<2;color++)for(int type=2;type<=6;type++)try(java.io.InputStream in=c.getAssets().open("pieces/"+(color==0?"white_":"black_")+names[type]+".png")){images[color*8+type]=BitmapFactory.decodeStream(in);}catch(java.io.IOException e){throw new IllegalStateException(e);}}
 private int dp(int x){return (int)(getResources().getDisplayMetrics().density*x+.5f);}
 public void show(GameModel g,int selected,IntConsumer choose){if(cached==g&&count==g.size()&&ply==selected)return;cached=g;count=g.size();ply=selected;rows.removeAllViews();
  if(count==0){TextView empty=label("Moves will appear here.");empty.setPadding(dp(12),dp(16),dp(12),dp(16));rows.addView(empty);return;}
  Position p=new Position();LinearLayout row=null;
  for(int i=0;i<count;i++){if((i&1)==0){row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setBackgroundColor((i/2&1)==0?0xFFF5F4F0:0xFFECEBE7);TextView number=label(""+(i/2+1));number.setGravity(Gravity.CENTER);number.setTextColor(0xFF777B75);row.addView(number,new LinearLayout.LayoutParams(dp(42),dp(38)));rows.addView(row);}
   int move=g.move(i),piece=p.board[Move.from(move)],type=piece&7;String san=g.notation(i);LinearLayout cell=new LinearLayout(getContext());cell.setGravity(Gravity.CENTER_VERTICAL);cell.setPadding(dp(9),0,dp(6),0);cell.setContentDescription(san);if(selected==i+1)cell.setBackgroundColor(0xFFD1D6D0);
   if(type>=2&&type<=6&&!san.startsWith("O-O")){ImageView icon=new ImageView(getContext());icon.setImageBitmap(images[piece]);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);cell.addView(icon,new LinearLayout.LayoutParams(dp(23),dp(27)));san=san.substring(1);}
   TextView notation=label(san);notation.setTextSize(16);notation.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));cell.addView(notation);row.addView(cell,new LinearLayout.LayoutParams(0,dp(38),1));final int index=i+1;if(choose!=null)cell.setOnClickListener(v->choose.accept(index));p.make(move);
  }
  if((count&1)==1)row.addView(new TextView(getContext()),new LinearLayout.LayoutParams(0,dp(38),1));
  final int target=selected==count?Math.max(0,count/2*dp(38)):Math.max(0,(selected-1)/2*dp(38)-dp(38));post(()->smoothScrollTo(0,target));
 }
 private TextView label(String s){TextView t=new TextView(getContext());t.setText(s);t.setTextColor(0xFF303B34);t.setTextSize(14);return t;}
}
