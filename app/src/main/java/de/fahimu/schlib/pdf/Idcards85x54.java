/*
 * Idcards85x54.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;


import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.app.R;
import de.fahimu.schlib.db.Idcard;

/**
 * An {@code Idcards85x54} object represents an A4 (210 x 297 mm) PDF document with up to 10 ID cards on each page.
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
   void writeSerial(int count, SerialNumber sn) {
      double x = 57.5 + 95 * (count % 2), y = 236.5 - 54 * (count / 2);
      writeElement(new Barcode128C(sn.getCode128(), 68, 30), x - 34, y + 8.5);
      writeElement(new Text(sn.getDisplay(), 154, 11, 40, 5, Text.Alignment.CENTER), x, y);
   }

   @Override
   @WorkerThread
   void finishPage(String pagePrefix, int page) {
      writeElementUpright(new Text(pagePrefix + page, 0, 114, 73, 3, Text.Alignment.CENTER), 106, 148.5);
   }

}