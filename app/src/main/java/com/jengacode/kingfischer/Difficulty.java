// SPDX-License-Identifier: GPL-3.0-or-later
package com.jengacode.kingfischer;
/** Approximate rating-labelled difficulty, not a calibrated human Elo estimate. */
public final class Difficulty {
 public static final int MIN=400,MAX=3000;
 public static int check(int rating){if(rating<MIN||rating>MAX)throw new IllegalArgumentException("Choose a rating from 400 to 3000.");return rating;}
 private static double fraction(int rating){return (check(rating)-MIN)/(double)(MAX-MIN);}
 public static int depth(int rating){return 2+(int)Math.round(14*fraction(rating));}
 public static long millis(int rating){double x=fraction(rating);return 200+(long)(3800*x*x);}
 public static long nodes(int rating){double x=fraction(rating);return 256+(long)(749744*x*x*x);}
 public static double alternateProbability(int rating){double x=fraction(rating);return .7*(1-x)*(1-x);}
}
