/*
 * Text.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import de.fahimu.schlib.app.R;

/**
 * A {@code Barcode128C} represents a <a href="https://en.wikipedia.org/wiki/Code_128">Code 128</a> with Code Set C.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
class Text extends Element {

   private static final FontMetric HELVETICA = new FontMetric(R.raw.helvetica).init();

   enum Alignment {LEFT, CENTER, RIGHT}

   private final String    text;
   private final double    red;
   private final double    green;
   private final double    blue;
   private final double    height;
   private final Alignment alignment;

   /**
    * Creates a new Text with the specified text string, gray-value, height (in mm) and alignment.
    *
    * @param text
    *       the text string.
    * @param red
    *       the red component (0 to 255) of the text color.
    * @param green
    *       the green component (0 to 255) of the text color.
    * @param blue
    *       the blue component (0 to 255) of the text color.
    * @param height
    *       the height of the glyphs (in mm).
    * @param alignment
    *       the alignment of the text.
    */
   Text(String text, int red, int green, int blue, double height, Alignment alignment) {
      this.text = text;
      this.red = red / 255.0;
      this.green = green / 255.0;
      this.blue = blue / 255.0;
      this.height = height;
      this.alignment = alignment;
   }

   private double calcTransX() {
      if (alignment == Alignment.LEFT || text.isEmpty()) {
         return 0;
      } else {
         double w = 0;
         char lft = text.charAt(0);
         for (int i = 1; i < text.length(); i++) {
            w += (HELVETICA.getWidth(lft) - HELVETICA.getAdjustment(lft, text.charAt(i)));
            lft = text.charAt(i);
         }
         w += HELVETICA.getWidth(lft);
         return height * Document.MM * ((alignment == Alignment.CENTER) ? -(w / 2) : -w) / 1000.0;
      }
   }

   /**
    * Writes the drawing instructions to the specified PDF Document.
    *
    * @param document
    *       the PDF Document where the instructions should be written to.
    */
   @Override
   Document write(Document document) {
      document.write("q %.3f %.3f %.3f rg 0 Tr 1 0 0 1 %.8f 0 cm\n", red, green, blue, calcTransX());
      document.write("BT /F1 %.8f Tf [(", height * Document.MM);
      if (!text.isEmpty()) {
         char lft = text.charAt(0);
         for (int i = 1; i < text.length(); i++) {
            boolean esc = (lft == '\\' || lft == '(' || lft == ')');
            int adj = HELVETICA.getAdjustment(lft, text.charAt(i));
            if (adj == 0) {
               document.write("%s%c", esc ? "\\" : "", lft);
            } else {
               document.write("%s%c)%d(", esc ? "\\" : "", lft, adj);
            }
            lft = text.charAt(i);
         }
         document.write("%c", lft);
      }
      return document.write(")] TJ ET Q\n");
   }

}