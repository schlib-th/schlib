/*
 * Idcards85x54.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;


import de.fahimu.schlib.app.R;
import de.fahimu.schlib.db.Idcard;

import static de.fahimu.schlib.pdf.Text.Align.BASELINE;
import static de.fahimu.schlib.pdf.Text.Align.CENTER;

/**
 * An {@code Idcards85x54} object represents a DIN A4 (210 x 297 mm) PDF document with up to 10 ID cards on each page.
 * Each card consists of a Code 128 Barcode and a textual representation of this barcode. When printed with a 100%
 * scaling factor, this document will match a sheet of 10 business cards, each 85 x 54 mm, two in a row, with margins
 * of 13.5 mm at the upper and lower edging, 15 mm at the left and right edging, and a 10 mm separator between the two
 * rows.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class Idcards85x54 extends BarcodeDocument {

   @MainThread
   public Idcards85x54() {
      super(Idcard.getPrinted(), R.string.pdf_idcards_title, R.string.pdf_idcards_subject);
   }

   @Override
   @WorkerThread
   void writeSerial(int count, String code128, String serialDisplay) {
      double x = 57.5 + 95.0 * (count % 2), y = 236.5 - 54.0 * (count / 2);      // in mm
      writeElement(new Barcode128C(code128, pt(68.0), pt(30.0)), pt(x - 34.0), pt(y + 8.5));
      writeElement(new Text(serialDisplay, 154, 11, 40, 14, CENTER, BASELINE), pt(x), pt(y));
   }

   @Override
   @WorkerThread
   void finishPage(String pagePrefix, int page) {
      writeElementUpright(new Text(pagePrefix + page, 0, 114, 73, 9, CENTER, CENTER), PAGE_WIDTH / 2, pt(148.5));
   }

}