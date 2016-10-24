/*
 * CSVParser.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.anw;

import android.content.Context;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.fahimu.schlib.app.R;

/**
 * A CSV file parser.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class CSVParser {

   private final Context context;

   private final StringType[] stringTypes;

   private final int[] positionMap;

   private final LineNumberReader reader;

   private final OutputStreamWriter writer;

   private boolean successful = true;

   public CSVParser(Context context, InputStream is, OutputStream os, StringType[] stringTypes) {
      this.context = context;
      this.stringTypes = stringTypes;
      this.reader = new LineNumberReader(new InputStreamReader(is, Charset.forName("UTF-8")));
      this.writer = new OutputStreamWriter(os, Charset.forName("UTF-8"));
      this.positionMap = parseHeadline();
   }

   private int[] parseHeadline() {
      String[] parsed = parseLine();
      int[] positionMap = new int[stringTypes.length];
      Arrays.fill(positionMap, -1);

      if (parsed == null) {
         writeError(R.string.csv_parser_missing_headline);
      } else {
         for (int j, i = 0; i < parsed.length; i++) {
            for (j = 0; j < stringTypes.length; j++) {
               if (parsed[i].equals(stringTypes[j].getName())) { break; }
            }
            if (j < stringTypes.length) {
               positionMap[j] = i;
            } else {
               writeError(R.string.csv_parser_unknown_column, parsed[i]);
            }
         }
      }
      return positionMap;
   }

   /* ============================================================================================================== */

   public String[] readLine() {
      String[] parsed = parseLine();
      if (parsed == null) { return null; }      /* EOF */

      boolean successful = true;
      String[] line = new String[stringTypes.length];
      for (int i = 0; i < line.length; i++) {
         if (positionMap[i] >= parsed.length) {
            successful = false;
            writeError(R.string.csv_parser_missing_column, stringTypes[i].getName());
         } else {
            String col = (positionMap[i] == -1) ? "" : parsed[positionMap[i]];
            int idx = stringTypes[i].matches(col);
            line[i] = (idx >= 0) ? "" : col;
            if (idx >= 0) {
               successful = false;
               if (col.isEmpty()) {
                  writeError(R.string.csv_parser_empty_column, stringTypes[i].getName());
               } else {
                  StringBuilder b = new StringBuilder(col.length() + 2).append(col.substring(0, idx)).append('|');
                  if (idx < col.length()) {
                     b.append(col.charAt(idx)).append('|').append(col.substring(idx + 1));
                  }
                  writeError(R.string.csv_parser_flawed_column, stringTypes[i].getName(), b.toString());
               }
            }
         }
      }
      return successful ? line : new String[0];
   }

   public void writeThrowable(Throwable throwable) {
      writeError(R.string.csv_parser_internal_error, throwable.getLocalizedMessage());
   }

   public boolean isSuccessful() {
      try {
         writer.close();
         reader.close();
      } catch (IOException e) { throw new RuntimeException(e); }
      return successful;
   }

   /* ============================================================================================================== */

   private void writeError(int resId, Object... args) {
      successful = false;
      try {
         String msg = context.getString(resId, args);
         writer.write(context.getString(R.string.csv_parser_line_template, reader.getLineNumber(), msg));
      } catch (IOException e) { throw new RuntimeException(e); }
   }

   private String[] parseLine() {
      try {
         String line = reader.readLine();
         if (line == null) { return null; }     /* EOF */

         boolean betweenQuotes = false;
         StringBuilder b = new StringBuilder(25);
         List<String> list = new ArrayList<>(stringTypes.length);

         for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (betweenQuotes) {
               if (c == '"') {
                  betweenQuotes = false;
               } else { b.append(c); }
            } else {
               if (c == '"') {
                  betweenQuotes = true;
                  if (i > 0 && line.charAt(i - 1) == '"') { b.append('"');    /* special case escape sequence "" */ }
               } else if (c == ',') {
                  list.add(b.toString().trim());
                  b.setLength(0);
               } else { b.append(c); }
            }
         }
         list.add(b.toString().trim());
         return list.toArray(new String[list.size()]);
      } catch (IOException e) { throw new RuntimeException(e); }
   }

}