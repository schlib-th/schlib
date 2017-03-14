/*
 * PupilList.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;


import java.util.ArrayList;

import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;
import de.fahimu.android.db.SQLite;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.app.R;
import de.fahimu.schlib.db.User;

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

   private final String name1, name2, date;

   /**
    * Creates a new {@code PupilList} PDF document for the specified class and the current calendar day.
    *
    * @param name1
    *       the class name.
    * @param name2
    *       the school year.
    */
   @MainThread
   public PupilList(String name1, String name2) {
      this(name1, name2, SQLite.getDatetimeNow().substring(0, 10));
   }

   /**
    * Creates a new {@code PupilList} PDF document for the specified class and calendar day.
    *
    * @param name1
    *       the class name.
    * @param name2
    *       the school year.
    * @param date
    *       the calendar day in the format {@code 'YYYY-MM-DD'}.
    */
   @MainThread
   public PupilList(String name1, String name2, String date) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         ArrayList<User> events = User.getInsertPupilsEvents(name1, name2);
         this.firstList = !events.isEmpty() && events.get(0).getTstamp().equals(date);
         this.users = User.getPupilList(name1, name2, date);
         this.name1 = name1;
         this.name2 = name2;
         this.date = App.format("%s.%s.%s", date.substring(8, 10), date.substring(5, 7), date.substring(0, 4));
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
         add(new SingleCenteredLine(headline1, 0.0, 18, 30));
         add(new SingleCenteredLine(headline2, 0.0, 18, 30));
         if (!firstList) {
            add(new SingleCenteredLine(headline3, 0.0, 10, 12));
         }
         add(new Line(36));

         String info = App.getStr(firstList ? R.string.pdf_pupil_list_text_1 : R.string.pdf_pupil_list_text_2);
         info = replaceUserVariables(info, "DATE", date, "CLASS-NAME", name1, "SCHOOL-YEAR", name2);
         for (String line : info.split("\n")) {
            add(line.isEmpty() ? new Line(8) : new MultiLine(line, 0.0, 10, 12, true));
         }
         add(new Line(36));

         String column1 = App.getStr(R.string.pdf_pupil_list_column_1);
         String column2 = App.getStr(R.string.pdf_pupil_list_column_2);
         String column3 = App.getStr(R.string.pdf_pupil_list_column_3);
         add(new TableRow(0.0, 10, 20, column1, column2, column3));

         for (User pupil : users) {
            String serial = App.format("%02d", pupil.getSerial());
            String idcard = new SerialNumber(pupil.getIdcard()).getDisplay();
            add(new TableRow(0.0, 10, 25, serial, idcard));    // TODO define height between 20 and 30
         }
         finalizeTable(3, 0.75, 0.5);
      }
   }

}