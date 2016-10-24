/*
 * Element.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

/**
 * An {@code Element} can be written to a PDF Document.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
abstract class Element {

   /**
    * Writes the drawing instructions to the specified PDF Document.
    *
    * @param document
    *       the PDF Document where the instructions should be written to.
    */
   abstract Document write(Document document);

   /* q: save graphics state on stack
      Q: load graphics state from stack
      a b c d e f cm: modify CTM to [a b c d e f]; [1 0 0 1 tx ty] means translation
      lineWidth w: set line width
      x y m: begin a new subpath, move current point (CP) to (x,y) and set starting point (SP) to CP
      x y l: append a straight line from CP to x y, set CP to (x,y)
      h: append a straight line from CP to SP and close subpath
      x y w h re: append a rectangle (x,y,x+w,y+h) as a complete subpath to the current path
      S: stroke the path
      f: fill the path
      r g b RG: set stroking color space to DeviceRGB and stroking color to red, green and blue (0.0 to 1.0)
      r g b rg: set non-stroking color space to DeviceRGB and non-stroking color to red, green and blue (0.0 to 1.0)
   */
   /* BT: begin a text object, initialize text matrix to identity
      ET: end a text object, discard the text matrix
      cs Tc: set the character spacing to cs (unscaled text space units); default 0
      ws Tw: set the word spacing to ws (unscaled text space units); default 0
      font size Tf: font is the name of a Font resource, size is in pt; NO DEFAULT
      render Tr: set rendering mode (0 fill, 1 stroke, 2 fill then stroke); default 0
      a b c d e f Tm:
      x y Td: the same as 1 0 0 1 x y Tm
      string Tj: show a text string
      array TJ: show one or more text strings, allowing kerning: [(V)80(AA)]
   */

}
