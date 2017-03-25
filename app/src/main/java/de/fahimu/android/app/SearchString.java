/*
 * SearchString.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fahimu.android.app.scanner.ScannerAwareSearchView;

/**
 * A normalized string for case- and diacritic-insensitive searching in a list.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class SearchString implements Comparable<SearchString> {

   private static final Map<Character,Character> normalized = new HashMap<>();

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

   public static final class Builder {
      private final StringBuilder tokens;
      private final int[]         offset;

      private int field = 0;

      public Builder(int fields) {
         tokens = new StringBuilder(64);
         offset = new int[fields + 1];
      }

      public Builder add(String string) {
         offset[field++] = tokens.length();
         for (int i = 0; i < string.length(); i++) {
            final Character c = normalized.get(string.charAt(i));
            tokens.append(c == null ? ' ' : c);
         }
         tokens.append(' ');
         return this;
      }

      public SearchString buildSearchString() {
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
      final int arraySize = 2 * normalizedQueries.length;
      if (arraySize == 0) {
         tokensIndex = null;
         return true;
      } else {
         int index = -1, j = 0;
         tokensIndex = new int[arraySize];      // an array to store the query indices and query lengths
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

   /**
    * This method will only be used for {@link Collections#sort(List)},
    * so there's no need to override {@link #equals(Object)} and {@link #hashCode()}.
    * Precondition: if tokensIndex == null then another.tokensIndex == null;
    * if tokensIndex != null then another.tokensIndex != null && tokensIndex.length == another.tokensIndex.length.
    */
   @Override
   public int compareTo(@NonNull SearchString another) {
      if (tokensIndex == null) { return 0; }
      final int[] ti1 = tokensIndex, ti2 = another.tokensIndex;
      for (int i = 0; i < ti1.length; i += 2) {
         final int cmp = ti1[i] - ti2[i];
         if (cmp != 0) { return cmp; }
      }
      return 0;
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void setText(int field, TextView textView, String string) {
      if (tokensIndex == null) {
         textView.setText(string);
      } else {
         SpannableString text = new SpannableString(string);
         final int tokensStart = offset[field], tokensEnd = offset[field + 1];
         for (int i = 0; i < tokensIndex.length; ) {
            final int index = tokensIndex[i++], length = tokensIndex[i++];
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
         final String queryText = getNormalized(src, start, end, dst, dstStart, dstEnd);
         Log.d("queryText={" + new StringBuilder(dst).replace(dstStart, dstEnd, queryText) + "}");
         if (src instanceof Spanned) {
            final SpannableString spannableString = new SpannableString(queryText);
            TextUtils.copySpansFrom((Spanned) src, start, end, null, spannableString, 0);
            return spannableString;
         } else {
            return queryText;
         }
      }

      /**
       * Returns the normalized text from the range {@code start &hellip; end} of {@code src}.
       * First two or more {@code SPACE} characters will be replaced by one {@code SPACE}, latin upper case letters
       * will be replaced by their lower case version and diacritic characters by their non-diacritic version.
       * Latin lower case letters and arabic digits will be left unchanged, all other characters will be deleted.
       * Second the first or last character of the temporary string will be deleted if it is a {@code SPACE}
       * character and after replacement in {@code dst} between {@code dstStart} and {@code dstEnd}
       * the result string would contain leading or trailing {@code SPACE}s or sequences of {@code SPACE}s.
       */
      @NonNull
      private String getNormalized(CharSequence src, int start, int end, Spanned dst, int dstStart, int dstEnd) {
         final StringBuilder b = new StringBuilder(end - start);
         for (int i = start; i < end; i++) {
            final char c = src.charAt(i);
            if (c == ' ') {
               if (!isBlank(b, b.length() - 1)) { b.append(c); }
            } else {
               Character norm = normalized.get(c);
               if (norm != null) { b.append(norm); }
            }
         }
         if (isBlank(b, 0) && (dstStart == 0 || isBlank(dst, dstStart - 1))) {
            b.deleteCharAt(0);
         }
         if (isBlank(b, b.length() - 1) && isBlank(dst, dstEnd)) {
            b.deleteCharAt(b.length() - 1);
         }
         return b.toString();
      }

      /**
       * Returns true if the character at the specified {@code index} in {@code cs} is Unicode SPACE.
       * If {@code index} is {@link IndexOutOfBoundsException out of bounds}, false will be returned.
       */
      private static boolean isBlank(CharSequence cs, int index) {
         return index >= 0 && index < cs.length() && cs.charAt(index) == ' ';
      }
   }

   /**
    * Returns an array of non-empty normalized strings build from the specified {@link CharSequence} {@code cs}.
    * Latin lower case letters, arabic digits and German lower case diacritic characters (ä, ö, ü, ß) will be
    * left unchanged, Latin upper case letters and German upper case diacritic characters (Ä, Ö, Ü) will be
    * replaced by their lower case version and other diacritic characters by their lower case non-diacritic version.
    * All other characters are interpreted as limiters between the normalized strings. Non-empty normalized
    * strings will be added to the resulting array and returned.
    *
    * @return an array of non-empty normalized strings build from the specified {@link CharSequence} {@code cs}.
    */
   @NonNull
   public static String[] getNormalizedQueries(CharSequence cs) {
      final ArrayList<String> list = new ArrayList<>();
      final StringBuilder b = new StringBuilder();
      for (int i = 0; i < cs.length(); i++) {
         final Character norm = normalized.get(cs.charAt(i));
         if (norm != null) {
            b.append(norm);
         } else if (b.length() > 0) {
            list.add(b.toString());
            b.setLength(0);
         }
      }
      if (b.length() > 0) {
         list.add(b.toString());
      }
      return list.toArray(new String[0]);
   }

   public static String[] getNormalizedQueries(@Nullable ScannerAwareSearchView searchView) {
      return getNormalizedQueries(searchView == null ? "" : searchView.getQuery());
   }

}