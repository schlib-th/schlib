/*
 * SearchString.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;


import java.util.HashMap;
import java.util.Map;

/**
 * A normalized string for case- and diacritic-insensitive searching in a list.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class SearchString {

   private static final Map<Character,Character> normalized = new HashMap<>(128);

   private static final String DIACRITIC = "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ";
   private static final String NORMALIZE = "aaaaäaaceeeeiiiidnooooöouuuüytßaaaaäaaceeeeiiiidnooooöouuuüyty";

   static {
      // add the 10 arabic digits unmodified
      for (char c = '0'; c <= '9'; c++) { normalized.put(c, c); }
      // add the 26 latin lower case letters unmodified
      for (char c = 'a'; c <= 'z'; c++) { normalized.put(c, c); }
      // add the 26 latin upper case letters normalized to lower case
      for (char c = 'A'; c <= 'Z'; c++) { normalized.put(c, Character.toLowerCase(c)); }

      // add the diacritic letters from ISO-8859-15 normalized to non-diacritic lower case
      for (int i = 0; i < DIACRITIC.length(); i++) {
         normalized.put(DIACRITIC.charAt(i), NORMALIZE.charAt(i));
      }
   }

   @ColorInt
   private static int spanColor = 0;

   @NonNull
   private static ForegroundColorSpan createColorSpan() {
      if (spanColor == 0) {
         spanColor = App.getColorFromRes(android.R.color.holo_red_dark);
      }
      return new ForegroundColorSpan(spanColor);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private final String tokens;
   private final int[]  offset;     // the start index of the n-th field in tokens

   private SearchString(String tokens, int[] offset) {
      this.tokens = tokens;
      this.offset = offset;
   }

   static class Builder {
      private final StringBuilder tokens;
      private final int[]         offset;

      private int field = 0;

      Builder(int fields) {
         this.tokens = new StringBuilder(64);
         this.offset = new int[fields + 1];
      }

      Builder add(String string) {
         offset[field++] = tokens.length();
         for (int i = 0; i < string.length(); i++) {
            Character c = normalized.get(string.charAt(i));
            tokens.append(c == null ? ' ' : c);
         }
         tokens.append(' ');
         return this;
      }

      SearchString createSearchString() {
         offset[field] = tokens.length();       // add additional element with tokens.length()
         return new SearchString(tokens.toString(), offset);
      }

   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private int[] tokensIndex;

   /**
    * Returns {@code true} if this search string contains the specified {@code normalizedQueries}.
    *
    * @param normalizedQueries
    *       the array of normalized queries as restricted by {@link QueryTextFilter}.
    * @return {@code true} if this search string contains the specified {@code normalizedQueries}.
    */
   @WorkerThread
   public boolean contains(String[] normalizedQueries) {
      final int len = normalizedQueries.length;
      if (len == 0) {
         tokensIndex = null;
         return true;
      } else {
         int index = -1, j = 0;
         tokensIndex = new int[len + len];      // an array to store the query indices and query lengths
         for (String normalizedQuery : normalizedQueries) {
            if ((index = tokens.indexOf(normalizedQuery, index + 1)) >= 0) {
               tokensIndex[j++] = index;
               tokensIndex[j++] = normalizedQuery.length();
            } else {
               tokensIndex = null;
               return false;
            }
         }
         return true;
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void setText(int field, TextView textView, String string) {
      if (tokensIndex == null) {
         textView.setText(string);
      } else {
         SpannableString text = new SpannableString(string);
         int tokensStart = offset[field], tokensEnd = offset[field + 1];
         for (int i = 0; i < tokensIndex.length; ) {
            int index = tokensIndex[i++], length = tokensIndex[i++];
            if (index >= tokensEnd) { break; }
            if (index >= tokensStart) {
               text.setSpan(createColorSpan(), index - tokensStart, index - tokensStart + length, 0);
            }
         }
         textView.setText(text);
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public static final class QueryTextFilter implements InputFilter {
      public CharSequence filter(CharSequence src, int start, int end, Spanned dst, int dstStart, int dstEnd) {
         String queryText = getNormalized(src, start, end, dst, dstStart, dstEnd);
         Log.d("queryText={" + new StringBuilder(dst).replace(dstStart, dstEnd, queryText) + "}");
         if (src instanceof Spanned) {
            SpannableString spannableString = new SpannableString(queryText);
            TextUtils.copySpansFrom((Spanned) src, start, end, null, spannableString, 0);
            return spannableString;
         } else {
            return queryText;
         }
      }

      private String getNormalized(CharSequence src, int start, int end, Spanned dst, int dstStart, int dstEnd) {
         StringBuilder b = new StringBuilder(end - start);
         for (int i = start; i < end; i++) {
            char c = src.charAt(i);
            if (c == ' ') {
               if (!isBlank(b, b.length() - 1)) { b.append(c); }
            } else {
               Character norm = normalized.get(c);
               if (norm != null) { b.append(norm); }
            }
         }
         if (isBlank(b, 0) && (dstStart == 0 || isBlank(dst, dstStart - 1))) { b.deleteCharAt(0); }
         if (isBlank(b, b.length() - 1) && isBlank(dst, dstEnd)) { b.deleteCharAt(b.length() - 1); }
         return b.toString();
      }

      private boolean isBlank(CharSequence cs, int index) {
         return index >= 0 && index < cs.length() && cs.charAt(index) == ' ';
      }
   }

}