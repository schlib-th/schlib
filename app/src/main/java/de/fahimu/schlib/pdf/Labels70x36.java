/*
 * Labels70x36.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;


import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.app.R;
import de.fahimu.schlib.db.Label;

/**
 * A {@code Labels70x36} object represents a A4 (210 x 297 mm) PDF document with up to 21 book labels on each page.
 * Each label consists of a Code 128 Barcode and a textual representation of this barcode. When printed with a 100%
 * scaling factor, this document will match a sheet of 24 adhesive labels, each 70 x 36 mm, three in a row, with a
 * margin of 4.5 mm at the upper and lower edging. To account for printer limitations, the last row will be omitted.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class Labels70x36 extends BarcodeDocument {

   @MainThread
   public Labels70x36() {
      super(Label.getPrinted(), R.string.pdf_labels_title, R.string.pdf_labels_subject);
   }

   @Override
   @WorkerThread
   void writeSerial(int count, SerialNumber sn) {
      double x = 35 + 70 * (count % 3), y = 262 - 36 * (count / 3);
      writeElement(new Barcode128C(sn.getCode128(), 55, 20), x - 27.5, y + 5.5);
      writeElement(new Text(sn.getDisplay(), 200, 200, 200, 4, Text.Alignment.CENTER), x, y);
   }

   @Override
   @WorkerThread
   void finishPage(String pagePrefix, int page) {
      writeElement(new Text(pagePrefix + page, 120, 120, 120, 3, Text.Alignment.CENTER), 105, 33);
   }

}