/*
 * Text.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import de.fahimu.android.app.App;
import de.fahimu.schlib.app.R;

/**
 * A {@code Text} represents a PDF text object (Section 5).
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
final class Text extends Element {

   private static volatile FontMetric HELVETICA;

   private static FontMetric getFont() {
      if (HELVETICA == null) {
         synchronized (Text.class) {
            if (HELVETICA == null) {
               HELVETICA = new FontMetric(R.raw.helvetica).init();
            }
         }
      }
      return HELVETICA;
   }

   enum Align {LEFT, CENTER, RIGHT, TOP, BASELINE, BOTTOM}

   private final String text;
   private final double red, green, blue;
   private final double size, width, x, y;

   /**
    * Creates a new Text with the specified gray level, size and alignment.
    *
    * @param text
    *       the text string.
    * @param gray
    *       the gray level (0.0 to 1.0).
    * @param size
    *       the size of the glyphs (in pt).
    * @param horizontal
    *       the horizontal alignment of the text.
    * @param vertical
    *       the vertical alignment of the text.
    */
   Text(String text, double gray, double size, Align horizontal, Align vertical) {
      this(text, gray, gray, gray, size, horizontal, vertical);
   }

   /**
    * Creates a new Text with the specified color, size and alignment.
    *
    * @param text
    *       the text string.
    * @param red
    *       the red component (0 to 255) of the text color.
    * @param green
    *       the green component (0 to 255) of the text color.
    * @param blue
    *       the blue component (0 to 255) of the text color.
    * @param size
    *       the size of the glyphs (in pt).
    * @param horizontal
    *       the horizontal alignment of the text.
    * @param vertical
    *       the vertical alignment of the text.
    */
   Text(String text, int red, int green, int blue, double size, Align horizontal, Align vertical) {
      this(text, red / 255.0, green / 255.0, blue / 255.0, size, horizontal, vertical);
   }

   private Text(String text, double red, double green, double blue, double size, Align horizontal, Align vertical) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.size = size;

      double w = 0.0;
      StringBuilder b = new StringBuilder();
      if (!text.isEmpty()) {
         char left = toPrintable(text.charAt(0));
         for (int i = 1; i < text.length(); i++) {
            final char right = toPrintable(text.charAt(i));
            final int adj = getFont().getAdjustment(left, right);

            append(b, left);
            if (adj != 0) {
               // )adj(: End a string, adjust the text position by adj, begin a string.
               b.append(')').append(adj).append('(');
            }
            w += getFont().getWidth(left) - adj;
            left = right;
         }
         append(b, left);
         w += getFont().getWidth(left);
      }
      this.text = b.toString();
      this.width = size * w / 1000.0;

      switch (horizontal) {
      case LEFT: x = 0.0; break;
      case CENTER: x = -width / 2.0; break;
      case RIGHT: x = -width; break;
      default: throw new IllegalArgumentException("horizontal=" + horizontal.name());
      }

      switch (vertical) {
      case TOP: y = size * -getFont().getFontBBoxURY() / 1000.0; break;
      case CENTER: y = size * -(getFont().getFontBBoxURY() + getFont().getFontBBoxLLY()) / 2000.0; break;
      case BASELINE: y = 0.0; break;
      case BOTTOM: y = size * -getFont().getFontBBoxLLY() / 1000.0; break;
      default: throw new IllegalArgumentException("vertical=" + vertical.name());
      }
   }

   /**
    * Returns a {@code SPACE} if {@code c} is a {@code NO-BREAK SPACE}, otherwise {@code c}.
    */
   private static char toPrintable(final char c) {
      return (c == '\u00a0') ? ' ' : c;
   }

   /**
    * {@link StringBuilder#append(char) Appends} the specified character to the StringBuilder.
    * If {@code c} is {@code '\'}, {@code '('} or {@code ')'}, an additional {@code '\'} will be prefixed.
    */
   private static void append(StringBuilder b, char c) {
      switch (c) {
      case '\\': case '(': case ')': b.append('\\');
      default: b.append(c);
      }
   }

   /**
    * Returns the width of this Text in pt.
    *
    * @return the width of this Text in pt.
    */
   double getWidth() {
      return width;
   }

   /**
    * Writes the drawing instructions to the specified PDF Document.
    *
    * @param document
    *       the PDF Document where the instructions should be written to.
    */
   @Override
   Document write(Document document) {
      // q: Push the current graphics state on the graphics state stack (Section 4.3.3)
      // r g b rg: Set non-stroking color space to DeviceRGB and color to r, g and b (0.0 to 1.0) (Section 4.5.7)
      // 0 Tr: Set the text rendering mode to 0 (fill) (Section 5.2 and 5.2.5)
      // 1 0 0 1 x y cm: Modify CTM to [1 0 0 1 x y], meaning translation by x, y (Section 4.3.3 and 4.2.2)
      document.write("q %.3f %.3f %.3f rg 0 Tr 1 0 0 1 %.8f %.8f cm\n", red, green, blue, x, y);

      // BT: Begin a text object and initialize the text matrix to the identity (Section 5.3)
      // /F1 size Tf: Set the text font to font resource /F1 and the text font size to size (Section 5.2)
      // [(: Begin the array and begin a string (Section 5.3.2)
      // )]: End a string and end the array (Section 5.3.2)
      // TJ: Show one or more text strings with individual glyph positioning (Section 5.3.2)
      // ET: End a text object, discarding the text matrix (Section 5.3)
      // Q: Pop the graphics state stack (Section 4.3.3)
      return document.write("BT /F1 %.8f Tf [(%s)] TJ ET Q\n", size, text);
   }

   @Override
   public String toString() {
      return App.format("x=%.8f, y=%.8f, size=%.8f, width=%.8f, text=[(%s)]", x, y, size, width, text);
   }

}