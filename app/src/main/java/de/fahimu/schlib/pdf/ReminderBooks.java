/*
 * ReminderBooks.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;
import de.fahimu.schlib.app.R;
import de.fahimu.schlib.db.Lending;
import de.fahimu.schlib.db.User;
import de.fahimu.schlib.db.User.Role;

import static de.fahimu.schlib.pdf.Text.Align.CENTER;
import static de.fahimu.schlib.pdf.Text.Align.LEFT;

/**
 * A {@code ReminderBooks} object represents a DIN A4 (210 x 297 mm) PDF document with a list of lendings on it.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.07.2017
 * @since SchoolLibrary 1.06
 */
public final class ReminderBooks extends TextDocument {

   private final List<Long> oids;

   private final String date;

   /**
    * Creates a new {@code ReminderBooks} PDF document.
    */
   @MainThread
   public ReminderBooks(List<Long> oids) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         this.oids = oids;
         this.date = App.formatDate(R.string.app_date, false, App.posixTime());
         String title = App.getStr(R.string.pdf_reminder_books_title, date);
         String subject = App.getStr(R.string.pdf_reminder_books_subject);
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
         String headline = App.getStr(R.string.pdf_reminder_books_title, date);
         add(new SingleLine(0.0, 20, 22 + 33, CENTER, headline));

         String info = App.getStr(R.string.pdf_reminder_books_text);
         info = replaceUserVariables(info, "DATE", date);
         for (String line : info.split("\n")) {
            add(line.isEmpty() ? new EmptyLine(8) : new MultiLine(0.0, 10, 12, true, line));
         }
         add(new EmptyLine(36));

         TreeMap<String,List<Lending>> groups = groupLendings(Lending.getByOids(oids));

         for (Map.Entry<String,List<Lending>> group : groups.entrySet()) {
            List<Lending> lendings = group.getValue();

            User user = lendings.get(0).getUser();
            String subhead, column1, column2, column3, column4;
            if (user.getRole() == Role.PUPIL) {
               subhead = App.getStr(R.string.pdf_reminder_books_headline_pupil, user.getName1(), user.getName2());
               column1 = App.getStr(R.string.pdf_reminder_books_column_1_head_pupil);
            } else {
               subhead = App.getStr(R.string.pdf_reminder_books_headline_tutor);
               column1 = App.getStr(R.string.pdf_reminder_books_column_1_head_tutor);
            }
            column2 = App.getStr(R.string.pdf_reminder_books_column_2_head);
            column3 = App.getStr(R.string.pdf_reminder_books_column_3_head);
            column4 = App.getStr(R.string.pdf_reminder_books_column_4_head);

            add(new SingleLine(0.0, 10, 12 + 6, LEFT, subhead).setSticky(true));
            add(new TableRow(0.0, 9, 16, column1, column2, column3, column4).setSticky(true));

            for (int row = 0; row < lendings.size(); row++) {
               Lending lending = lendings.get(row);
               user = lending.getUser();
               if (user.getRole() == Role.PUPIL) {
                  column1 = user.getDisplaySerial();
               } else {
                  column1 = user.getDisplayName();
               }
               column2 = App.getStr(R.string.pdf_reminder_books_column_2_body, lending.getDelay());
               column3 = lending.getBook().getDisplayShelfNumber();
               column4 = lending.getBook().getTitle();
               TableRow line = new TableRow(0.0, 9, 16, column1, column2, column3, column4);
               add(line.setSticky(row == 0 || row >= lendings.size() - 2));
            }
            finalizeTable(0.75, 0.5, 16, 6, CENTER, CENTER, CENTER, LEFT);
         }
      }
   }

   /**
    * Groups the lendings. Admins and tutors are added to the empty group, pupils are added to groups
    * depending on their class (name2, name1). These groups define paragraphs in the final Reminder document.
    */
   @WorkerThread
   private TreeMap<String,List<Lending>> groupLendings(List<Lending> lendings) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         TreeMap<String,List<Lending>> groups = new TreeMap<>();
         for (Lending lending : lendings) {
            String key = "";     // default for ADMIN or TUTOR
            User user = lending.getUser();
            if (user.getRole() == Role.PUPIL) {
               key = user.getName2() + user.getName1();
            }
            if (!groups.containsKey(key)) {
               groups.put(key, new ArrayList<Lending>());
            }
            groups.get(key).add(lending);
         }
         return groups;
      }
   }

}