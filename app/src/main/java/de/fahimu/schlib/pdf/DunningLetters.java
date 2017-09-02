/*
 * DunningLetters.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import android.support.annotation.MainThread;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;


import java.util.List;

import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;
import de.fahimu.schlib.app.R;
import de.fahimu.schlib.db.Book;
import de.fahimu.schlib.db.Lending;
import de.fahimu.schlib.db.User;
import de.fahimu.schlib.db.User.Role;

import static de.fahimu.schlib.pdf.Text.Align.CENTER;
import static de.fahimu.schlib.pdf.Text.Align.LEFT;
import static de.fahimu.schlib.pdf.Text.Align.RIGHT;

/**
 * A {@code DunningLetters} object represents a DIN A4 (210 x 297 mm) PDF document with dunning letters.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.07.2017
 * @since SchoolLibrary 1.06
 */
public final class DunningLetters extends TextDocument {

   private final List<Long> oids;

   private final String date;

   /**
    * Creates a new {@code DunningLetters} PDF document.
    */
   @MainThread
   public DunningLetters(List<Long> oids) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         this.oids = oids;
         this.date = App.formatDate(R.string.app_date, false, App.posixTime());
         String title = App.getStr(R.string.pdf_dunning_letters_title, date);
         String subject = App.getStr(R.string.pdf_dunning_letters_subject);
         open(title, subject);
      }
   }

   /** {@inheritDoc} */
   @Override
   @WorkerThread
   final boolean isEmpty() { return oids.isEmpty(); }

   /** {@inheritDoc} */
   @Override
   @WorkerThread
   void addLines() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         List<Lending> lendings = Lending.getByOidsWithDelay(oids);
         for (int page = 0; page < lendings.size(); page++) {
            Lending lending = lendings.get(page);

            Book book = lending.getBook();
            User user = lending.getUser();
            boolean isPupil = user.getRole() == Role.PUPIL;
            boolean delayed = lending.isDelayed();

            String text = App.getStr(delayed ?
                                     R.string.pdf_dunning_letters_headline_delayed :
                                     R.string.pdf_dunning_letters_headline_no_term, date);
            add(new SingleLine(0.0, 20, 22 + 44, CENTER, text));

            if (isPupil) {
               text = App.getStr(R.string.pdf_dunning_letters_salutation_pupil);
               SingleLine salutation = new SingleLine(0.0, 12, 14, LEFT, text);
               text = App.getStr(R.string.pdf_dunning_letters_annotation_pupil, user.getDisplay());
               add(salutation.appendAnnotatedUnderline(0.5, text, true, 10));
            } else {
               text = App.getStr(R.string.pdf_dunning_letters_salutation_tutor, user.getDisplayName());
               add(new SingleLine(0.0, 12, 14, LEFT, text));
            }
            add(new EmptyLine(14));
            addText(lending, 8, 10, 12, isPupil ?
                                        R.string.pdf_dunning_letters_text_1_pupil :
                                        R.string.pdf_dunning_letters_text_1_tutor);
            add(new EmptyLine(8));

            String colon = ":   ";
            add(new TableRow(0.0, 10, 14, App.getStr(R.string.book_title) + colon, book.getTitle()));
            add(new TableRow(0.0, 10, 14, App.getStr(R.string.book_author) + colon, book.getAuthor()));
            add(new TableRow(0.0, 10, 14, App.getStr(R.string.book_publisher) + colon, book.getPublisher()));
            add(new TableRow(0.0, 10, 14, App.getStr(R.string.book_shelf) + colon, book.getDisplayShelfNumber()));
            finalizeTable(1.0, 0.5, 8, 0, LEFT, LEFT);

            if (delayed) {
               if (isPupil) {
                  addText(lending, 8, 10, 12, R.string.pdf_dunning_letters_text_2_delayed_pupil);
                  add(new EmptyLine(36));

                  text = App.getStr(R.string.pdf_dunning_letters_salutation_parents);
                  add(new SingleLine(0.0, 12, 14 + 14, LEFT, text));
                  addText(lending, 8, 10, 12, R.string.pdf_dunning_letters_text_3_delayed_pupil);
                  add(new EmptyLine(24));

                  SingleLine cuttingLine = new SingleLine(1.0, 12, 14, LEFT, "");
                  text = App.getStr(R.string.pdf_dunning_letters_receipt_annotation_line);
                  cuttingLine.appendAnnotatedUnderline(0.0, text, false, 0);
                  add(cuttingLine).add(new EmptyLine(18));

                  addText(lending, 8, 10, 12, R.string.pdf_dunning_letters_receipt_text);
                  add(new EmptyLine(24));

                  SingleLine signatureLine = new SingleLine(1.0, 12, 14, LEFT, "");
                  text = App.getStr(R.string.pdf_dunning_letters_receipt_annotation_date);
                  signatureLine.appendAnnotatedUnderline(0.0, text, true, pt(40), LEFT);
                  text = App.getStr(R.string.pdf_dunning_letters_receipt_annotation_sign);
                  signatureLine.appendAnnotatedUnderline(0.0, text, true, pt(80), RIGHT);
                  add(signatureLine).add(new EmptyLine(18));

                  text = App.getStr(R.string.pdf_dunning_letters_receipt_bottom_line,
                        user.getDisplay(), user.getDisplayIdcard());
                  add(new SingleLine(0.75, 8, 10, CENTER, text));
               } else {
                  addText(lending, 8, 10, 12, R.string.pdf_dunning_letters_text_2_delayed_tutor);
               }
            } else {
               if (isPupil) {
                  addText(lending, 8, 10, 12, R.string.pdf_dunning_letters_text_2_no_term_pupil);
               } else {
                  addText(lending, 8, 10, 12, R.string.pdf_dunning_letters_text_2_no_term_tutor);
               }
            }
            if (page < lendings.size() - 1) {
               add(new PageBreak());
            }
         }
      }
   }

   @WorkerThread
   private void addText(Lending lending, int emptyHeight, int size, int height, @StringRes int resId) {
      Book book = lending.getBook();
      String text = replaceUserVariables(App.getStr(resId),
            "DATE", date, "BOOK", book.getDisplay(), "PERIOD", Integer.toString(book.getPeriod()),
            "ISSUE-DATE", lending.getIssueDate(), "ISSUE-TIME", lending.getIssueTime(),
            "TERM", lending.isDelayed() ? lending.getTermDate() : "",
            "DELAY", Integer.toString(lending.getDelay()));
      for (String line : text.split("\n")) {
         add(line.isEmpty() ? new EmptyLine(emptyHeight) : new MultiLine(0.0, size, height, true, line));
      }
   }

}