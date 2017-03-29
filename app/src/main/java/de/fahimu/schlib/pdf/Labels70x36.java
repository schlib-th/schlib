/*
 * Labels70x36.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;


import de.fahimu.schlib.app.R;
import de.fahimu.schlib.db.Label;

import static de.fahimu.schlib.pdf.Text.Align.BASELINE;
import static de.fahimu.schlib.pdf.Text.Align.CENTER;

/**
 * A {@code Labels70x36} object represents a DIN A4 (210 x 297 mm) PDF document with up to 21 book labels on each page.
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
   void writeSerial(int count, String code128, String serialDisplay) {
      double x = 35.0 + 70.0 * (count % 3), y = 262.0 - 36.0 * (count / 3);      // in mm
      writeElement(new Barcode128C(code128, pt(55.0), pt(20.0)), pt(x - 27.5), pt(y + 5.5));
      writeElement(new Text(serialDisplay, 0.75, 11, CENTER, BASELINE), pt(x), pt(y));
   }

   @Override
   @WorkerThread
   void finishPage(String pagePrefix, int page) {
      writeElement(new Text(pagePrefix + page, 0.5, 9, CENTER, BASELINE), PAGE_WIDTH / 2, pt(33.0));
   }

}