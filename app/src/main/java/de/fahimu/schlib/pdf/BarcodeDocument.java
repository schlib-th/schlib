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
 * An A4 (210 x 297 mm) PDF document with {@code barcodesPerPage} barcodes on it, arranged in a grid.
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
      pagePrefix = App.format("%s - %s ", App.getStr(titleId), App.getStr(R.string.pdf_page));
      open(titleId, subject);
   }

   @WorkerThread
   abstract void writeSerial(int count, SerialNumber sn);

   @WorkerThread
   abstract void finishPage(String pagePrefix, int page);

   @WorkerThread
   final boolean isEmpty() { return serials.isEmpty(); }

   /**
    * Writes the document with the specified {@code asyncTask} if {@link #isEmpty()} is {@code false}.
    *
    * @param asyncDocumentWriter
    *       the task which runs the method.
    */
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
         writeSerial(count, new SerialNumber(serial.getId()));
         count += 1;
      }
      finishPage(pagePrefix, currentPage);
      asyncDocumentWriter.onPageWrite();
   }

}