/*
 * TextDocument.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;

import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;
import de.fahimu.schlib.app.R;
import de.fahimu.schlib.pdf.Text.Align;

import static de.fahimu.schlib.pdf.Text.Align.BOTTOM;
import static de.fahimu.schlib.pdf.Text.Align.CENTER;
import static de.fahimu.schlib.pdf.Text.Align.LEFT;
import static de.fahimu.schlib.pdf.Text.Align.RIGHT;
import static de.fahimu.schlib.pdf.Text.Align.TOP;

/**
 * A DIN A4 (210 x 297 mm) PDF document containing text and tables.
 */
abstract class TextDocument extends Document {

   private abstract class Line {
      private final int     height;
      private       boolean sticky;

      /**
       * Creates a new empty line with the specified height.
       *
       * @param height
       *       the height of the empty line (in pt).
       */
      @WorkerThread
      Line(int height) { this.height = height; }

      @WorkerThread
      int getHeight() { return height; }

      @WorkerThread
      Line setSticky(boolean sticky) {
         this.sticky = sticky; return this;
      }

      @WorkerThread
      abstract void write(double y);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private final class Block extends Line {
      private final List<Line> lines = new ArrayList<>();

      @WorkerThread
      Block(Line firstLine) {
         super(0); lines.add(firstLine);
      }

      @Override
      @WorkerThread
      int getHeight() {
         int height = 0;
         for (Line line : lines) {
            height += line.getHeight();
         }
         return height;
      }

      @Override
      @WorkerThread
      void write(double y) {
         for (Line line : lines) {
            line.write(y);
            y -= line.getHeight();
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   class EmptyLine extends Line {

      /**
       * Creates a new empty line with the specified height.
       *
       * @param height
       *       the height of the empty line (in pt).
       */
      @WorkerThread
      EmptyLine(int height) { super(height); }

      @WorkerThread
      void write(double y) { }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   final class PageBreak extends EmptyLine {

      /**
       * Creates a page break, a line that forces the layout processor to start a new page.
       */
      @WorkerThread
      PageBreak() { super((int) PAGE_HEIGHT); }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   final class SingleLine extends Line {
      private final Text   text;
      private final double textX;

      /**
       * Creates a new single line of text that will be placed centered on the page.
       *
       * @param gray
       *       the gray level (0.0 to 1.0).
       * @param size
       *       the size of the glyphs (in pt).
       * @param height
       *       the height of the line (in pt).
       * @param align
       *       the horizontal alignment of the text.
       * @param text
       *       the text string.
       */
      @WorkerThread
      SingleLine(double gray, int size, int height, Align align, String text) {
         super(height);
         this.text = new Text(text, gray, size, align, TOP);
         this.textX = (align == LEFT) ? PAGE_LEFT : (align == RIGHT) ? PAGE_RIGHT : (PAGE_LEFT + PAGE_RIGHT) / 2;
      }

      /**
       * Appends an annotated underline that will be drawn as far as the right edge of the page.
       * It starts at the right end of this line plus the specified padding.
       *
       * @param gray
       *       the gray level (0.0 to 1.0).
       * @param text
       *       the annotation text string.
       * @param dots
       *       if true a dotted line will be printed, else a dashed line.
       * @param padding
       *       the distance between the end of this line and the beginning of the underline (in pt).
       */
      @WorkerThread
      SingleLine appendAnnotatedUnderline(double gray, String text, boolean dots, int padding) {
         double width = PAGE_RIGHT - textX - this.text.getWidth() - padding;
         double lineX = PAGE_RIGHT - width;
         return appendAnnotatedUnderline(gray, text, dots, lineX, width);
      }

      /**
       * Appends an annotated underline that will be drawn with the specified width and page alignment.
       *
       * @param gray
       *       the gray level (0.0 to 1.0).
       * @param text
       *       the annotation text string.
       * @param dots
       *       if true a dotted line will be printed, else a dashed line.
       * @param width
       *       the width of the underline (in pt).
       * @param align
       *       the horizontal alignment of the underline on the page.
       */
      @WorkerThread
      SingleLine appendAnnotatedUnderline(double gray, String text, boolean dots, double width, Align align) {
         double lineX = (align == LEFT) ? PAGE_LEFT :
                        (align == RIGHT) ? PAGE_RIGHT - width : (PAGE_LEFT + PAGE_RIGHT - width) / 2;
         return appendAnnotatedUnderline(gray, text, dots, lineX, width);
      }

      private final class AnnotatedLine {
         private final Text   text;
         private final double textX;
         private final String drawLineOps;

         private AnnotatedLine(Text text, double textX, String drawLineOps) {
            this.text = text; this.textX = textX; this.drawLineOps = drawLineOps;
         }
      }

      private boolean isAnnotated;
      private final List<AnnotatedLine> annotatedLines = new ArrayList<>();

      @WorkerThread
      private SingleLine appendAnnotatedUnderline(double gray, String text, boolean dots, double lineX, double width) {
         Text annotation = new Text(text, gray, 8, CENTER, TOP);
         String dashPattern = dots ? "0 2" : "4 5";
         // q: Push the current graphics state on the graphics state stack (Section 4.3.3)
         // 0.75 G: Set stroking color space to DeviceGray and stroking gray level to 0.75 (Section 4.5.7)
         // lineWidth w: Set the line width (Section 4.3.3)
         // [lineWidth] 0 d: Set the line dash pattern to lineWidth on, lineWidth off (Section 4.3.3)
         // 1 J: Set the line cap style to round cap (Section 4.3.3)
         String op1 = App.format("q 0.67 G 1 w [%s] 0 d 1 J\n", dashPattern);
         // x %1$.8f m x+width %1$.8f l: Append to the current path as a complete subpath
         // the straight line from (x,y) to (x+width, y) (Section 4.4.1)
         // S: stroke the path (Section 4.4.2)
         // Q: Pop the graphics state stack (Section 4.3.3)
         String op2 = App.format("%.8f %%1$.8f m %.8f %%1$.8f l S Q\n", lineX, lineX + width);

         isAnnotated = true;
         annotatedLines.add(new AnnotatedLine(annotation, lineX + width / 2, op1 + op2));
         return this;
      }

      @Override
      @WorkerThread
      int getHeight() {
         return super.getHeight() + (isAnnotated ? 10 : 0);    // default height for annotation is 10
      }

      @Override
      @WorkerThread
      void write(double y) {
         writeElement(text, textX, y);
         for (AnnotatedLine annotatedLine : annotatedLines) {
            TextDocument.this.write(App.format(annotatedLine.drawLineOps, y - text.getOffsetTop()));
            writeElement(annotatedLine.text, annotatedLine.textX, y - super.getHeight());
         }
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
       * @param gray
       *       the gray level (0 to 255).
       * @param size
       *       the size of the glyphs (in pt).
       * @param height
       *       the height of one of the lines (in pt).
       * @param justified
       *       if true, the text will be aligned to the right bound of the document.
       * @param text
       *       the text string.
       */
      @WorkerThread
      MultiLine(double gray, int size, int height, boolean justified, String text) {
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
               addSyllableAndSeparator(text, start, i, gray, size, (c == ' ') ? space : hyphen);
               start = i + 1;
            }
         }
         addSyllableAndSeparator(text, start, text.length(), gray, size, space);
      }

      @WorkerThread
      private void addSyllableAndSeparator(String text, int start, int end, double gray, int size, Text separator) {
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
      int getHeight() {
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
      private final Text[]   texts;
      private final Align[]  aligns;
      private final String[] strings;
      private final double   gray;
      private final int      size;

      /**
       * Creates a new column row with the specified columns values.
       *
       * @param gray
       *       the gray level (0.0 to 1.0).
       * @param size
       *       the size of the glyphs (in pt).
       * @param height
       *       the height of the line (in pt).
       * @param strings
       *       the text strings of each column.
       */
      @WorkerThread
      TableRow(double gray, int size, int height, String... strings) {
         super(height);
         this.texts = new Text[strings.length];
         this.aligns = new Align[strings.length];
         this.strings = strings;
         this.gray = gray;
         this.size = size;
      }

      @WorkerThread
      private void setAlignsAndTexts(Align[] alignments) {
         for (int i = 0; i < texts.length; i++) {
            aligns[i] = alignments[i];
            texts[i] = new Text(strings[i], gray, size, aligns[i], CENTER);
         }
      }

      private double[] columnWidths;      // the width of the columns, except the last one
      private double   lineGray;
      private double   lineWidth;
      private int      padding;

      /**
       * Sets the color and line width of the edging lines, the cell padding and the width of the columns.
       *
       * @param lineGray
       *       the color of the lines that edge the cells.
       * @param lineWidth
       *       the width of the lines that edge the cells (in pt).
       * @param padding
       *       the left and right padding of the cell (in pt).
       * @param columnWidths
       *       the width of the columns (in pt).
       */
      @WorkerThread
      private void setCellValues(double lineGray, double lineWidth, int padding, double[] columnWidths) {
         this.lineGray = lineGray; this.lineWidth = lineWidth;
         this.padding = padding; this.columnWidths = columnWidths;
      }

      @Override
      @WorkerThread
      void write(double y) {
         double x = PAGE_LEFT, h = getHeight();
         // q: Push the current graphics state on the graphics state stack (Section 4.3.3)
         // lineGray G: Set stroking color space to DeviceGray and stroking gray level to lineGray (Section 4.5.7)
         // lineWidth w: Set the line width (Section 4.3.3)
         TextDocument.this.write("q %.3f G %.8f w\n", lineGray, lineWidth);

         for (int i = 0; i < columnWidths.length + 1; i++) {
            double width = (i < columnWidths.length) ? padding + columnWidths[i] + padding : PAGE_RIGHT - x;
            // x y-h width h re: Append a rectangle to the current path as a complete subpath,
            // with lower-left corner (x, y-h) and dimensions (width, h) (Section 4.4.1)
            TextDocument.this.write("%.8f %.8f %.8f %.8f re\n", x, y - h, width, h);
            if (i < texts.length) {
               Text text = (i < columnWidths.length) ? texts[i] : getTrimmed(i, width - 2 * padding);
               double offset = (aligns[i] == LEFT) ? padding :
                               (aligns[i] == RIGHT) ? width - padding : width / 2;
               writeElement(text, x + offset, y - h / 2);
            }
            x += width;
         }
         // S: stroke the path (Section 4.4.2)
         // Q: Pop the graphics state stack (Section 4.3.3)
         TextDocument.this.write("S Q\n");
      }

      private Text getTrimmed(int i, double maxWidth) {
         String string = strings[i];
         while (texts[i].getWidth() > maxWidth) {
            string = string.substring(0, string.lastIndexOf(' '));
            texts[i] = new Text(string + " ...", gray, size, aligns[i], CENTER);
         }
         return texts[i];
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
            add(new EmptyLine(0));  // if last line is a block, enforce refreshLayout by adding empty non sticky line
            writeDocument();
            try { Thread.sleep(250); } catch (InterruptedException e) { /* IGNORE */ }
            assertNotCancelled();
            scope.d("write document finished successfully");
         } catch (CancellationException exception) {
            scope.d("write document cancelled by user");
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

   private Block block;

   @WorkerThread
   final TextDocument add(@NonNull Line line) {
      assertNotCancelled();
      if (!line.sticky) {
         if (block != null) {
            lines.add(block);
            refreshLayout(block);
            block = null;
         }
         lines.add(line);
         refreshLayout(line);
      } else if (block == null) {
         block = new Block(line);
      } else {
         block.lines.add(line);
      }
      return this;
   }

   private double y = PAGE_TOP;

   @WorkerThread
   private void refreshLayout(Line line) {
      if (y - line.getHeight() < PAGE_BOTTOM) {
         startNewPage();
      }
      if (y == PAGE_TOP && line instanceof EmptyLine) {
         return;     // ignore empty line at top of page
      }
      y -= line.getHeight();
   }

   private int page;    // each page will be counted twice, once in addLines() and once again in writeDocument()

   @WorkerThread
   private void startNewPage() {
      // because we count each page twice, we'll report new pages only on odd page values
      if ((++page & 1) == 1) {
         asyncDocumentWriter.onPageWrite();
      }
      y = PAGE_TOP;
   }

   @WorkerThread
   private void writeDocument() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         startNewPage();
         final int pages = page;       // the total number of pages
         for (Line line : lines) {
            assertNotCancelled();
            if (y - line.getHeight() < PAGE_BOTTOM) {
               finishPage(pages);
               writeNewPage();
            }
            if (y == PAGE_TOP && line instanceof EmptyLine) {
               continue;   // ignore empty line at top of page
            }
            line.write(y);
            y -= line.getHeight();
         }
         finishPage(pages);
      }
   }

   @WorkerThread
   private void finishPage(int pages) {
      String text = App.getStr(R.string.pdf_page_of, page - pages + 1, pages);
      writeElement(new Text(text, 0.5, 9, CENTER, BOTTOM), PAGE_WIDTH / 2, MARGIN);
      startNewPage();
   }

   /**
    * Must be called by the concrete subclass after adding the last {@link TableRow} of a table.
    *
    * @param lineGray
    *       the color of the lines that edge the cells.
    * @param lineWidth
    *       the width of the lines that edge the cells (in pt).
    * @param height
    *       the height of the empty line finalizing the table (in pt).
    * @param padding
    *       the left and right padding of the cells (in pt).
    * @param alignments
    *       the horizontal text alignment for each column.
    */
   @WorkerThread
   final void finalizeTable(double lineGray, double lineWidth, int height, int padding, Align... alignments) {
      // Step 1: add new empty non sticky line with the specified height
      add(new EmptyLine(height));

      // Step 2: Build temporary LinkedList of all TableRows
      LinkedList<TableRow> tableRows = new LinkedList<>();
      buildTableRows(tableRows, lines, 1);

      // Step 3: Calculate the width of each column, except the last one
      final double[] widths = new double[alignments.length - 1];
      for (final TableRow tableRow : tableRows) {
         tableRow.setAlignsAndTexts(alignments);
         for (int j = 0, min = Math.min(alignments.length - 1, tableRow.texts.length); j < min; j++) {
            if (widths[j] < tableRow.texts[j].getWidth()) {
               widths[j] = tableRow.texts[j].getWidth();
            }
         }
      }
      // Step 4: Set the cell values for each row
      for (TableRow tableRow : tableRows) {
         tableRow.setCellValues(lineGray, lineWidth, padding, widths);
      }
   }

   @WorkerThread
   private boolean buildTableRows(LinkedList<TableRow> tableRows, List<Line> lines, int lastOffset) {
      for (int i = lines.size() - 1 - lastOffset; i >= 0; i--) {
         Line line = lines.get(i);
         if (line instanceof TableRow) {
            tableRows.addFirst((TableRow) line);
         } else if (line instanceof Block) {
            if (buildTableRows(tableRows, ((Block) line).lines, 0)) {
               return true;
            }
         } else {
            return true;
         }
      }
      return false;
   }

}