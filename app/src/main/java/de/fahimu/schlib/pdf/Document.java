/*
 * Document.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import android.os.AsyncTask;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.fahimu.android.app.Log;
import de.fahimu.android.app.TaskRegistry;
import de.fahimu.android.share.ExternalFile;
import de.fahimu.android.share.ExternalOutputStream;
import de.fahimu.schlib.app.App;
import de.fahimu.schlib.db.Use;
import de.fahimu.schlib.db.User;
import de.fahimu.schlib.share.FileType;

/**
 * A {@code Document} represents a DIN A4 (210 x 297 mm) PDF document according to the PDF Specification Version 1.7.
 * Only a small subset of the available PDF features is supported to keep things as simple as possible.
 * All citations refer to the sixth edition of the PDF Reference from November 2006.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @see <a href="http://www.adobe.com/devnet/pdf/pdf_reference_archive.html">Adobe PDF Reference Archives</a>
 * @since SchoolLibrary 1.0
 */
public abstract class Document {

   /**
    * Returns the specified mm value in pt, the unit size of the default user space (Section 4.2.1 - User Space).
    * 1 inch = 25.4 mm = 72 pt => 1 mm = 72 pt / 25.4 = 2.8346 pt.
    *
    * @param mm
    *       the value in mm.
    * @return the specified mm value in pt.
    */
   static double pt(double mm) {
      return mm * 72.0 / 25.4;
   }

   /** The width of a DIN A4 document in pt. */
   static final double PAGE_WIDTH  = pt(210.0);
   /** The height of a DIN A4 document in pt. */
   static final double PAGE_HEIGHT = pt(297.0);

   private ExternalFile         externalFile;
   private ExternalOutputStream outputStream;

   /**
    * Opens an ExternalOutputStream and writes the PDF header and specified metadata to the document.
    *
    * @param title
    *       the title of the document.
    * @param subject
    *       the subject of the document.
    * @return this PDF Document.
    */
   final Document open(String title, String subject) {
      externalFile = new ExternalFile(FileType.PRINTS, title.replace('/', ':') + ".pdf");
      outputStream = ExternalOutputStream.newInstance(externalFile);

      // write the File Header (Section 3.4.1).
      write("%%PDF-1.6\n");                           // PDF 1.6 needed for ViewerPreferences/PrintScaling=None
      write("%%\u00E4\u00F6\u00FC\u00DF\n");          // comment line to mark the document as binary

      // start the File Body (Section 3.4.2).

      // Object 1: the Metadata dictionary
      User user = Use.getLoggedInNonNull().getUser();
      String author = user.getName1() + " " + user.getName2();
      begObj("<<").write("/Title(%s)/Author(%s)/Subject(%s)", title, author, subject);
      write("/Creator(%1$s)/Producer(%1$s)", "School-Library V1.0 (\u00A9 2017)");
      String d = new SimpleDateFormat("yyyyMMddHHmmssZ", Locale.US).format(new Date());
      write("/CreationDate(D:%1$s'%2$s')/ModDate(D:%1$s'%2$s')", d.substring(0, 17), d.substring(17)).endObj(">>");

      // Object 2: the Font dictionary for 'Helvetica'
      begObj("<<").write("/Type/Font/Subtype/Type1/Encoding/WinAnsiEncoding/BaseFont/Helvetica").endObj(">>");

      // Object 3: the MediaBox array
      begObj("[").write("0 0 %.8f %.8f", PAGE_WIDTH, PAGE_HEIGHT).endObj("]");

      return begPage();
   }

   /**
    * The current file offset.
    */
   private int offset;

   private final byte[] buffer = new byte[1024];

   /**
    * Writes the specified string Windows 1252 encoded to the PDF file.
    *
    * @param format
    *       the format string.
    * @param args
    *       the list of arguments passed to the formatter.
    * @return this document.
    */
   final Document write(String format, Object... args) {
      String s = App.format(format, args);
      int length = s.length();
      final byte[] buffer = (length <= this.buffer.length) ? this.buffer : new byte[length];

      for (int i = 0; i < length; i++) {
         char c = s.charAt(i);
         if ((c >= '\u0080' && c <= '\u009f') || c > '\u00ff') {
            throw new RuntimeException(App.format("char \\u%04x not WinAnsiEncoding compatible", (int) c));
         }
         buffer[i] = (byte) c;
      }
      outputStream.write(buffer, 0, length);
      offset += length;
      return this;
   }

   /**
    * The file offsets for creating the Cross-Reference Table.
    */
   private final List<Integer> objOffset = new ArrayList<>();

   private Document begObj(String beg) {
      objOffset.add(offset);                 // memorize the current file offset for the Cross-Reference Table
      write("%d 0 obj\n%s", objOffset.size(), beg);
      return this;
   }

   private Document endObj(String end) {
      write("%s\nendobj\n", end);
      return this;
   }

   /**
    * The file offset for calculating the stream length.
    */
   private int streamOffset;

   private Document begPage() {
      begObj("<<").write("/Length %d 0 R>>stream\n", objOffset.size() + 1);
      streamOffset = offset;                 // memorize the current file offset for the stream length
      return this;
   }

   private Document endPage() {
      int length = offset - streamOffset;
      endObj("\nendstream").begObj(Integer.toString(length)).endObj("");
      return this;
   }

   final Document writeNewPage() { return endPage().begPage(); }

   final Document writeElement(Element element, double x, double y) {
      write("q 1 0 0 1 %.8f %.8f cm\n", x, y);
      return element.write(this).write("Q\n");
   }

   final Document writeElementUpright(Element element, double x, double y) {
      write("q 0 1 -1 0 %.8f %.8f cm\n", x, y);
      return element.write(this).write("Q\n");
   }

   private void close() {
      try {
         // finish the File Body (Section 3.4.2).
         endPage();

         int count = (objOffset.size() - 3) / 2;      // page count
         int page = objOffset.size() + 1;             // object ID of first page object
         int pages = page + count;                    // object ID of pages object
         int catalog = pages + 1;                     // object ID of catalog object

         for (int i = 0; i < count; i++) {
            begObj("<<").write("/Type/Page/Parent %d 0 R/Contents %d 0 R", pages, 2 * i + 4).endObj(">>");
         }
         begObj("<<").write("/Type/Pages/Resources<</Font<</F1 2 0 R>>>>/MediaBox 3 0 R/Count %d/Kids[", count);
         for (int i = 0; i < count; i++) { write(" %d 0 R", page + i); }
         endObj("]>>");

         begObj("<<").write("/Type/Catalog/Pages %d 0 R/ViewerPreferences<</PrintScaling/None>>", pages).endObj(">>");

         // write the Cross-Reference Table (Section 3.4.3).
         int xrefOff = offset;                        // memorize the current file offset for the startxref
         write("xref\n");
         write("0 %d\n", 1 + objOffset.size());
         write("0000000000 65535 f \n");              // first entry (object 0) muss always be free
         for (int off : objOffset) {
            write("%010d 00000 n \n", off);
         }

         // write the File Trailer (Section 3.4.4).
         write("trailer\n");
         write("<</Info 1 0 R/Root %d 0 R/Size %d>>\n", catalog, 1 + objOffset.size());
         write("startxref\n");
         write("%d\n", xrefOff);
         write("%%%%EOF\n");
      } finally {
         outputStream.close();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public interface WriterListener {
      @MainThread
      void onPageWrite();

      @MainThread
      void onPostWrite();
   }

   @MainThread
   public static void writeAsync(TaskRegistry taskRegistry, final WriterListener listener, final Document... docs) {
      WriterListener multiListener = new WriterListener() {
         private int onPostWriteCount = docs.length;

         @Override
         @MainThread
         public void onPageWrite() { listener.onPageWrite(); }

         @Override
         @MainThread
         public void onPostWrite() { if (--onPostWriteCount == 0) { listener.onPostWrite(); } }
      };
      for (Document doc : docs) {
         doc.writeAsync(multiListener, taskRegistry);
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Returns {@code true} if the document is empty, i. e. it has no content. In this case,
    * the method {@link #writeAsync(AsyncDocumentWriter)} will not be called and the document will be deleted.
    *
    * @return {@code true} if the document is empty.
    */
   @WorkerThread
   abstract boolean isEmpty();

   /**
    * Writes the document with the specified {@code asyncTask} if not {@link #isEmpty()}.
    * The implementing method must periodically check if the task was {@link AsyncTask#isCancelled() cancelled}.
    *
    * @param asyncDocumentWriter
    *       the task which runs the method.
    */
   @WorkerThread
   abstract void writeAsync(AsyncDocumentWriter asyncDocumentWriter);

   @MainThread
   private void writeAsync(@NonNull WriterListener listener, @NonNull TaskRegistry taskRegistry) {
      taskRegistry.add(new AsyncDocumentWriter(listener, taskRegistry));
   }

   final class AsyncDocumentWriter extends AsyncTask<Void,Void,Void> {
      private final WriterListener listener;
      private final TaskRegistry   taskRegistry;

      @MainThread
      AsyncDocumentWriter(@NonNull WriterListener listener, @NonNull TaskRegistry taskRegistry) {
         this.listener = listener;
         this.taskRegistry = taskRegistry;
      }

      @Override
      @WorkerThread
      protected Void doInBackground(Void... voids) {
         try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
            if (isEmpty()) {
               close();
               externalFile.delete();
            } else {
               writeAsync(this);
               close();
            }
            taskRegistry.remove(this);
            return null;
         }
      }

      @WorkerThread
      final void onPageWrite() {
         publishProgress();
      }

      @Override
      @MainThread
      protected void onProgressUpdate(Void... values) { listener.onPageWrite(); }

      @Override
      @MainThread
      protected void onCancelled(Void v) { externalFile.delete(); }

      @Override
      @MainThread
      protected void onPostExecute(Void v) { listener.onPostWrite(); }
   }

}