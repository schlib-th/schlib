/*
 * ReminderIdcards.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;
import de.fahimu.schlib.app.R;
import de.fahimu.schlib.db.User;

import static de.fahimu.schlib.pdf.Text.Align.CENTER;
import static de.fahimu.schlib.pdf.Text.Align.LEFT;

/**
 * A {@code ReminderIdcards} object represents a DIN A4 (210 x 297 mm) PDF document with a list of pupils on it.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 24.07.2017
 * @since SchoolLibrary 1.09
 */
public final class ReminderIdcards extends TextDocument {

   private final List<User> pupils;

   private final String date;

   /**
    * Creates a new {@code ReminderIdcards} PDF document.
    */
   @MainThread
   public ReminderIdcards(List<User> pupils) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         this.pupils = Collections.unmodifiableList(pupils);
         this.date = App.formatDate(R.string.app_date, false, App.posixTime());
         String title = App.getStr(R.string.pdf_reminder_idcards_title, date);
         String subject = App.getStr(R.string.pdf_reminder_idcards_subject);
         open(title, subject);
      }
   }

   /** {@inheritDoc} */
   @Override
   @WorkerThread
   final boolean isEmpty() { return pupils.isEmpty(); }

   /** {@inheritDoc} */
   @Override
   @WorkerThread
   void addLines() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         String headline = App.getStr(R.string.pdf_reminder_idcards_title, date);
         add(new SingleLine(0.0, 20, 22 + 33, CENTER, headline));

         String info = App.getStr(R.string.pdf_reminder_idcards_text);
         info = replaceUserVariables(info, "DATE", date);
         for (String line : info.split("\n")) {
            add(line.isEmpty() ? new EmptyLine(8) : new MultiLine(0.0, 10, 12, true, line));
         }
         add(new EmptyLine(36));

         TreeMap<String,List<User>> groups = groupPupils(pupils);

         for (Map.Entry<String,List<User>> group : groups.entrySet()) {
            List<User> pupils = group.getValue();

            User pupil = pupils.get(0);
            String subhead, column1, column2, column3;
            subhead = App.getStr(R.string.pdf_reminder_idcards_headline, pupil.getName1(), pupil.getName2());
            column1 = App.getStr(R.string.pdf_reminder_idcards_column_1);
            column2 = App.getStr(R.string.pdf_reminder_idcards_column_2);
            column3 = App.getStr(R.string.pdf_reminder_idcards_column_3);

            add(new SingleLine(0.0, 12, 14 + 8, LEFT, subhead).setSticky(true));
            add(new TableRow(0.0, 10, 20, column1, column2, column3).setSticky(true));

            for (int row = 0; row < pupils.size(); row++) {
               pupil = pupils.get(row);
               column1 = pupil.getDisplaySerial();
               column2 = pupil.getDisplayIdcard();
               TableRow line = new TableRow(0.0, 10, 20, column1, column2);
               add(line.setSticky(row == 0 || row >= pupils.size() - 2));
            }
            finalizeTable(0.75, 0.5, 20, 8, CENTER, CENTER, CENTER);
         }
      }
   }

   /**
    * Groups the pupils by their class (name2, name1) to define paragraphs in the final document.
    */
   @WorkerThread
   private TreeMap<String,List<User>> groupPupils(List<User> pupils) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         TreeMap<String,List<User>> groups = new TreeMap<>();
         for (User pupil : pupils) {
            String key = pupil.getName2() + pupil.getName1();
            if (!groups.containsKey(key)) {
               groups.put(key, new ArrayList<User>());
            }
            groups.get(key).add(pupil);
         }
         return groups;
      }
   }

}