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

}
