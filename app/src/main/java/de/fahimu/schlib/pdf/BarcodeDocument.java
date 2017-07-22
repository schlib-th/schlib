/*
 * BarcodeDocument.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;


import java.util.List;

import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.app.App;
import de.fahimu.schlib.app.R;
import de.fahimu.schlib.db.Serial;

/**
 * A DIN A4 (210 x 297 mm) PDF document with barcodes on it, arranged in a grid.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
abstract class BarcodeDocument extends Document {

   @NonNull
   private final List<? extends Serial> serials;

   @NonNull
   private final String pagePrefix;

   @MainThread
   BarcodeDocument(@NonNull List<? extends Serial> serials, @StringRes int titleId, @StringRes int subject) {
      this.serials = serials;
      String title = App.getStr(titleId);
      pagePrefix = App.format("%s\u00a0-\u00a0%s\u00a0", title, App.getStr(R.string.pdf_page));
      open(title, App.getStr(subject));
   }

   @WorkerThread
   abstract void writeSerial(int count, String code128, String serialDisplay);

   @WorkerThread
   abstract void finishPage(String pagePrefix, int page);

   /** {@inheritDoc} */
   @Override
   @WorkerThread
   final boolean isEmpty() { return serials.isEmpty(); }

   /** {@inheritDoc} */
   @Override
   @WorkerThread
   final void writeAsync(AsyncDocumentWriter asyncDocumentWriter) {
      int count = 0, currentPage = serials.get(0).getPage();
      for (Serial serial : serials) {
         if (asyncDocumentWriter.isCancelled()) { return; }
         if (serial.getPage() != currentPage) {
            finishPage(pagePrefix, currentPage);
            asyncDocumentWriter.onPageWrite();

            writeNewPage();
            count = 0; currentPage = serial.getPage();
         }
         writeSerial(count, SerialNumber.getCode128(serial.getId()), serial.getDisplayId());
         count += 1;
      }
      finishPage(pagePrefix, currentPage);
      asyncDocumentWriter.onPageWrite();
   }

}