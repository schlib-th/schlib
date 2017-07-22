/*
 * PupilList.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;


import java.util.ArrayList;
import java.util.TimeZone;

import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;
import de.fahimu.schlib.app.R;
import de.fahimu.schlib.db.User;

import static de.fahimu.schlib.pdf.Text.Align.CENTER;

/**
 * A {@code PupilList} object represents a DIN A4 (210 x 297 mm) PDF document with a list of pupils on it.
 * Each time new pupils are added to a class, such a document must be generated, printed and given to the teacher
 * to fill in the pupils names.
 * A {@code PupilList} document contains all pupils that were added to a class on a specific calendar day.
 * The first (and in many cases only) {@code PupilList} document will be generated when the school year begins and
 * contains the pupils of a class at this time.
 * If more pupils join in during the school year, a special version of this document will be created that contains
 * only the recently added pupils and a special headline.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class PupilList extends TextDocument {

   private final boolean firstList;

   private final ArrayList<User> users;

   private final String name2, name1, date;

   /**
    * Creates a new {@code PupilList} PDF document for the specified class and date.
    *
    * @param name2
    *       the school year.
    * @param name1
    *       the class name.
    * @param localDate
    *       number of days since 1.1.1970 in the {@link TimeZone#getDefault() default} timezone.
    */
   @MainThread
   public PupilList(String name2, String name1, long localDate) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         ArrayList<User> events = User.getInsertPupilsEvents(name2, name1);
         this.firstList = !events.isEmpty() && events.get(0).getLocalDate() == localDate;
         this.users = User.getPupilList(name2, name1, localDate);
         this.name2 = name2;
         this.name1 = name1;
         this.date = App.formatDate(R.string.app_date, true, localDate * 86400);
         String title = App.getStr(R.string.pdf_pupil_list_title, name1, name2, this.date);
         String subject = App.getStr(R.string.pdf_pupil_list_subject);
         open(title, subject);
      }
   }

   /** {@inheritDoc} */
   @Override
   @WorkerThread
   final boolean isEmpty() { return users.isEmpty(); }

   /** {@inheritDoc} */
   @Override
   @WorkerThread
   void addLines() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         String headline1 = App.getStr(R.string.pdf_pupil_list_headline_1);
         String headline2 = App.getStr(R.string.pdf_pupil_list_headline_2, name1, name2);
         String headline3 = App.getStr(R.string.pdf_pupil_list_headline_3, date, users.size());
         add(new SingleLine(0.0, 20, 22 + 11, CENTER, headline1));
         if (firstList) {
            add(new SingleLine(0.0, 20, 22 + 33, CENTER, headline2));
         } else {
            add(new SingleLine(0.0, 20, 22 + 11, CENTER, headline2));
            add(new SingleLine(0.0, 10, 12 + 33, CENTER, headline3));
         }

         String info = getInfoText();
         info = replaceUserVariables(info, "DATE", date, "SCHOOL-YEAR", name2, "CLASS-NAME", name1);
         for (String line : info.split("\n")) {
            add(line.isEmpty() ? new EmptyLine(8) : new MultiLine(0.0, 10, 12, true, line));
         }
         add(new EmptyLine(24));

         String column1 = App.getStr(R.string.pdf_pupil_list_column_1);
         String column2 = App.getStr(R.string.pdf_pupil_list_column_2);
         String column3 = App.getStr(R.string.pdf_pupil_list_column_3);
         add(new TableRow(0.0, 10, 24, column1, column2, column3));

         for (int row = 0; row < users.size(); row++) {
            User pupil = users.get(row);
            TableRow line = new TableRow(0.0, 10, 24, pupil.getDisplaySerial(), pupil.getDisplayIdcard());
            add(line.setSticky(row == 0 || row >= users.size() - 2));
         }
         finalizeTable(0.75, 0.5, 0, 10, CENTER, CENTER, CENTER);
      }
   }

   @WorkerThread
   private String getInfoText() {
      return App.getStr(R.string.pdf_pupil_list_text_1) +
            App.getStr(firstList ? R.string.pdf_pupil_list_text_2a : R.string.pdf_pupil_list_text_2b) +
            App.getStr(R.string.pdf_pupil_list_text_3);
   }

}