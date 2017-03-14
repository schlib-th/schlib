/*
 * TextDocument.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import android.support.annotation.WorkerThread;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;

import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;
import de.fahimu.schlib.app.R;

import static de.fahimu.schlib.pdf.Text.Align.BOTTOM;
import static de.fahimu.schlib.pdf.Text.Align.CENTER;
import static de.fahimu.schlib.pdf.Text.Align.LEFT;
import static de.fahimu.schlib.pdf.Text.Align.TOP;

/**
 * A DIN A4 (210 x 297 mm) PDF document containing text and tables.
 */
abstract class TextDocument extends Document {

   class Line {
      private final double height;

      /**
       * Creates a new empty line with the specified height.
       *
       * @param height
       *       the height of the empty line (in pt).
       */
      @WorkerThread
      Line(int height) { this.height = height; }

      @WorkerThread
      double getHeight() { return height; }

      @WorkerThread
      void write(double y) { }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   final class SingleCenteredLine extends Line {
      private final Text text;

      /**
       * Creates a new single line of text that will be placed centered on the page.
       *
       * @param text
       *       the text string.
       * @param gray
       *       the gray level (0.0 to 1.0).
       * @param size
       *       the size of the glyphs (in pt).
       * @param height
       *       the height of the line (in pt).
       */
      @WorkerThread
      SingleCenteredLine(String text, double gray, int size, int height) {
         super(height);
         this.text = new Text(text, gray, size, CENTER, TOP);
      }

      @Override
      @WorkerThread
      void write(double y) {
         writeElement(text, PAGE_WIDTH / 2, y);
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   final class MultiLine extends Line {

      private final class LayoutInfo {
         private final double extraSpace;
         private final Text   firstSyllable;

         LayoutInfo(double extraSpace, Text firstSyllable) {
            this.extraSpace = extraSpace; this.firstSyllable = firstSyllable;
         }
      }

      private final boolean justified;
      private final Text    space, hyphen;
      private final List<Text>       syllables   = new ArrayList<>();
      private final List<Text>       separators  = new ArrayList<>();
      private final List<LayoutInfo> layoutInfos = new ArrayList<>();

      /**
       * Creates a new line of text that will be placed left aligned on the page and can span over multiple lines.
       *
       * @param text
       *       the text string.
       * @param gray
       *       the gray level (0 to 255).
       * @param size
       *       the size of the glyphs (in pt).
       * @param height
       *       the height of one of the lines (in pt).
       */
      @WorkerThread
      MultiLine(String text, double gray, int size, int height, boolean justified) {
         super(height);
         this.justified = justified;
         space = new Text(" ", gray, size, LEFT, TOP);
         hyphen = new Text("-", gray, size, LEFT, TOP);
         addSyllablesAndSeparators(text, gray, size);
         layoutSyllables();
      }

      @WorkerThread
      private void addSyllablesAndSeparators(String text, double gray, int size) {
         int start = 0;
         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ' || c == '|') {
               addSyllableAndSeparator(text, gray, size, start, i, (c == ' ') ? space : hyphen);
               start = i + 1;
            }
         }
         addSyllableAndSeparator(text, gray, size, start, text.length(), space);
      }

      @WorkerThread
      private void addSyllableAndSeparator(String text, double gray, int size, int start, int end, Text separator) {
         if (start < end) {
            syllables.add(new Text(text.substring(start, end), gray, size, LEFT, TOP));
            separators.add(separator);
         }
      }

      @WorkerThread
      private void layoutSyllables() {
         int spaceCount = 0;
         double x = PAGE_LEFT;
         for (int i = 0; i < syllables.size(); i++) {
            Text syllable = syllables.get(i), separator = separators.get(i);
            double separatorEOLWidth = (separator == hyphen) ? hyphen.getWidth() : 0.0;
            if (x + syllable.getWidth() + separatorEOLWidth > PAGE_RIGHT) {
               double extraSpace = calculateExtraSpace(spaceCount, x, separators.get(i - 1));
               layoutInfos.add(new LayoutInfo(extraSpace, syllable));
               spaceCount = 0; x = PAGE_LEFT;   // reset
            }
            x += syllable.getWidth();
            if (separator == space) {
               spaceCount += 1; x += space.getWidth();
            }
         }
         layoutInfos.add(new LayoutInfo(0.0, null));     // add LayoutInfo for last line
      }

      @WorkerThread
      private double calculateExtraSpace(int spaceCount, double x, Text lastSeparator) {
         if (!justified) {
            return 0.0;
         } else if (lastSeparator == hyphen) {
            // add hyphen width to x because we have to print it
            return (PAGE_RIGHT - (x + hyphen.getWidth())) / spaceCount;
         } else {
            // last space was erroneously taken into account - correct this!
            return (PAGE_RIGHT - (x - space.getWidth())) / (spaceCount - 1);
         }
      }

      @Override
      @WorkerThread
      double getHeight() {
         return super.getHeight() * layoutInfos.size();
      }

      @Override
      @WorkerThread
      void write(double y) {
         double x = PAGE_LEFT;
         Iterator<LayoutInfo> iterator = layoutInfos.iterator();
         LayoutInfo layoutInfo = iterator.next();

         for (int i = 0; i < syllables.size(); i++) {
            Text syllable = syllables.get(i), separator = separators.get(i);
            if (syllable == layoutInfo.firstSyllable) {
               if (separators.get(i - 1) == hyphen) {
                  writeElement(hyphen, x, y);
               }
               layoutInfo = iterator.next();
               x = PAGE_LEFT; y -= super.getHeight();       // set cursor to start of next line
            }
            writeElement(syllable, x, y);
            x += syllable.getWidth();
            if (separator == space) {
               x += space.getWidth() + layoutInfo.extraSpace;
            }
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   final class TableRow extends Line {
      private final Text[] columns;

      private double[] widths;
      private double   gray, lineWidth;

      /**
       * Creates a new column row with the specified columns values.
       *
       * @param gray
       *       the gray level (0.0 to 1.0).
       * @param size
       *       the size of the glyphs (in pt).
       * @param height
       *       the height of the line (in pt).
       * @param columns
       *       the text strings.
       */
      @WorkerThread
      TableRow(double gray, int size, int height, String... columns) {
         super(height);
         this.columns = new Text[columns.length];
         for (int i = 0; i < columns.length; i++) {
            this.columns[i] = new Text(columns[i], gray, size, CENTER, CENTER);
         }
      }

      /**
       * Sets the width of the width.length cells and the color and line width of the edging lines.
       *
       * @param widths
       *       the width of the cells.
       * @param gray
       *       the color of the lines that edge the cells.
       * @param lineWidth
       *       the width of the lines that edge the cells.
       */
      @WorkerThread
      private void setCellValues(double[] widths, double gray, double lineWidth) {
         this.widths = widths; this.gray = gray; this.lineWidth = lineWidth;
      }

      @Override
      @WorkerThread
      void write(double y) {
         double x = PAGE_LEFT, h = getHeight();
         // q: Push the current graphics state on the graphics state stack (Section 4.3.3)
         // gray G: Set stroking color space to DeviceGray and stroking gray level to gray (Section 4.5.7)
         // lineWidth w: Set the line width (Section 4.3.3)
         TextDocument.this.write("q %.3f G %.8f w\n", gray, lineWidth);

         for (int i = 0; i < widths.length; i++) {
            double width = (i == widths.length - 1) ? PAGE_RIGHT - x : 10.0 + widths[i] + 10.0;
            // x y-h width h re: Append a rectangle to the current path as a complete subpath,
            // with lower-left corner (x, y-h) and dimensions (width, h) (Section 4.4.1)
            TextDocument.this.write("%.8f %.8f %.8f %.8f re\n", x, y - h, width, h);
            if (i < columns.length) { writeElement(columns[i], x + width / 2, y - h / 2); }
            x += width;
         }
         // S: stroke the path (Section 4.4.2)
         // Q: Pop the graphics state stack (Section 4.3.3)
         TextDocument.this.write("S Q\n");
      }
   }

   /* ============================================================================================================== */

   /**
    * Returns a copy of the specified text after replacing any user variables with the specified substitution strings.
    * A user variable may only contain capital latin letters or a hyphen and must be enclosed by curly braces,
    * e. g. {@code DATE}, {@code CLASS-NAME} or {@code SCHOOL-YEAR}.
    *
    * @param text
    *       the text with zero or more user variables.
    * @return a copy of the specified text after replacing any user variables with the specified substitution strings.
    */
   @WorkerThread
   final String replaceUserVariables(String text, String... substitutions) {
      if (substitutions.length % 2 != 0) {
         throw new IllegalArgumentException(Arrays.toString(substitutions) + ".length not even");
      }
      StringBuilder b = new StringBuilder(text.length());
      for (int end, i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == '{' && (end = text.indexOf('}', i + 1)) >= 0) {
            for (int j = 0; j < substitutions.length; j += 2) {
               if (text.substring(i + 1, end).equals(substitutions[j])) {
                  b.append(substitutions[j + 1]);
                  i = end; c = 0; break;
               }
            }
         }
         if (c > 0) { b.append(c); }
      }
      return b.toString();
   }

   /* ============================================================================================================== */

   private static final double MARGIN      = pt(20.0);
   private static final double PAGE_FOOTER = pt(6.0);    // space for the page number footer
   private static final double PAGE_LEFT   = MARGIN;
   private static final double PAGE_RIGHT  = PAGE_WIDTH - MARGIN;
   private static final double PAGE_TOP    = PAGE_HEIGHT - MARGIN;
   private static final double PAGE_BOTTOM = MARGIN + PAGE_FOOTER;

   private final List<Line> lines = new ArrayList<>();

   private AsyncDocumentWriter asyncDocumentWriter;

   @WorkerThread
   private void assertNotCancelled() {
      if (asyncDocumentWriter.isCancelled()) { throw new CancellationException(); }
   }

   /** {@inheritDoc} */
   @Override
   @WorkerThread
   final void writeAsync(AsyncDocumentWriter asyncDocumentWriter) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         this.asyncDocumentWriter = asyncDocumentWriter;
         try {
            addLines();
            writeDocument(getNumberOfPages());
         } catch (CancellationException exception) {
            /* just return if cancelled */
         } finally {
            this.asyncDocumentWriter = null;
         }
      }
   }

   /**
    * Must be implemented by a concrete subclass and builds the content
    * of the document by {@link #add(Line) adding} one or more {@link Line}s to it.
    */
   @WorkerThread
   abstract void addLines();

   @WorkerThread
   final TextDocument add(Line line) {
      assertNotCancelled();
      lines.add(line); return this;
   }

   /**
    * Must be called by the concrete subclass after adding the last {@link TableRow} of a table.
    *
    * @param columns
    *       the number of columns of the table.
    * @param gray
    *       the color of the lines that edge the table's cells.
    * @param lineWidth
    *       the width of the lines that edge the table's cells.
    */
   @WorkerThread
   final void finalizeTable(int columns, double gray, double lineWidth) {
      // Step 1: Find first row of table.
      int firstRow = lines.size();
      do { --firstRow; } while (firstRow >= 0 && lines.get(firstRow) instanceof TableRow);
      firstRow++;

      // Step 2: Calculate the width of each column
      final double[] widths = new double[columns];
      for (int i = firstRow; i < lines.size(); i++) {
         TableRow row = (TableRow) lines.get(i);
         for (int j = 0, min = Math.min(columns, row.columns.length); j < min; j++) {
            if (widths[j] < row.columns[j].getWidth()) {
               widths[j] = row.columns[j].getWidth();
            }
         }
      }
      // Step 3: Set the cell values for each row
      for (int i = firstRow; i < lines.size(); i++) {
         ((TableRow) lines.get(i)).setCellValues(widths, gray, lineWidth);
      }
   }

   @WorkerThread
   private int getNumberOfPages() {
      int page = 1;
      double y = PAGE_TOP;
      for (Line line : lines) {
         assertNotCancelled();
         if (y - line.getHeight() < PAGE_BOTTOM) {
            page += 1; y = PAGE_TOP;
         }
         y -= line.getHeight();
      }
      return page;
   }

   @WorkerThread
   private void writeDocument(final int pages) {
      int page = 1;
      double y = PAGE_TOP;
      for (Line line : lines) {
         assertNotCancelled();
         if (y - line.getHeight() < PAGE_BOTTOM) {
            finishPage(page, pages);
            writeNewPage();
            page += 1; y = PAGE_TOP;
         }
         line.write(y);
         y -= line.getHeight();
      }
      finishPage(page, pages);
   }

   @WorkerThread
   private void finishPage(final int page, final int pages) {
      String text = App.getStr(R.string.pdf_page_of, page, pages);
      writeElement(new Text(text, 0.5, 9, CENTER, BOTTOM), PAGE_WIDTH / 2, MARGIN);
      asyncDocumentWriter.onPageWrite();
   }

}