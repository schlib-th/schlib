/*
 * Serial.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.db;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;
import de.fahimu.android.db.Row;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Values;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.app.R;

import static de.fahimu.android.db.SQLite.MIN_TSTAMP;

/**
 * A serial can have one of four states: Printed, Stocked, Used or Lost.
 * Printed: page NOTNULL AND lost ISNULL
 * Stocked: page ISNULL AND lost ISNULL, no book/user references the serial.
 * Used:    page ISNULL AND lost ISNULL, some book/user references the serial.
 * Lost:    page ISNULL AND lost NOTNULL
 * A serial will never be deleted after insertion.
 * If a serial is lost, it will be reused after a certain time when new serials must be printed.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public abstract class Serial extends Row {

   static final String OID  = BaseColumns._ID;
   static final String PAGE = "page";
   static final String LOST = "lost";

   static void create(SQLiteDatabase db, @NonNull String table, int min, int max) {
      createTable(db, table, min, max);
      addFirstRow(db, table, min);
   }

   static void upgrade(SQLiteDatabase db, @NonNull String table, int min, int max, int oldVersion) {
      if (oldVersion < 2) {
         upgradeTableV2(db, table, min, max);
      }
   }

   private static void createTable(SQLiteDatabase db, @NonNull String table, int min, int max) {
      Table tab = new Table(table, 4, false).addCheckBetween(min, max);
      tab.addLongColumn(PAGE, false).addCheckBetween(1, 999);
      tab.addTimeColumn(LOST, false).addCheckPosixTime(MIN_TSTAMP);
      tab.addConstraint().addCheck(PAGE + " ISNULL OR " + LOST + " ISNULL");
      tab.create(db);
   }

   private static void addFirstRow(SQLiteDatabase db, @NonNull String table, int min) {
      // assure the first serial is set to min
      SQLite.insert(db, table, new Values().addLong(OID, min).addLong(LOST, MIN_TSTAMP));
   }

   private static void upgradeTableV2(SQLiteDatabase db, @NonNull String table, int min, int max) {
      SQLite.alterTablePrepare(db, table);
      createTable(db, table, min, max);
      SQLite.alterTableExecute(db, table, OID, PAGE, SQLite.datetimeToPosix(LOST));
   }

   /* ============================================================================================================== */

   /**
    * Returns the current POSIX time minus the specified amount of years.
    *
    * @param years
    *       the number of years to subtract.
    * @return the current POSIX time minus the specified amount of years.
    */
   private static long nowMinusYears(int years) {
      Calendar calendar = new GregorianCalendar();
      calendar.add(Calendar.YEAR, -years);
      return calendar.getTimeInMillis() / 1000;
   }

   /**
    * Returns how often {@link #createOnePage(Class, String, int)} can be called without an exception thrown.
    * <p> Called before creating new labels or idcards. </p>
    *
    * @param table
    *       the table to query.
    * @param serialsPerPage
    *       the number of labels that can be placed on a single page.
    * @param max
    *       the maximum allowed value for column {@code _id}.
    */
   static int canCallCreateOnePage(String table, int serialsPerPage, int max) {
      // SELECT (free+lost)/$serialsPerPage FROM
      //    (SELECT $max-MAX(_id) AS free FROM $table),
      //    (SELECT COUNT(*) AS lost FROM $table WHERE lost<nowMinusYears(5)) ;
      String column = App.format("(free+lost)/%d", serialsPerPage);
      String query1 = App.format("SELECT %d-MAX(%s) AS free FROM %s", max, OID, table);
      String query2 = App.format("SELECT COUNT(*) AS lost FROM %s WHERE %s<%d", table, LOST, nowMinusYears(5));
      String tables = App.format("(%s), (%s)", query1, query2);
      return SQLite.getIntFromQuery(tables, column, null);
   }

   /**
    * Sets the state of {@code serialsPerPage} rows in {@code table} to 'Printed'.
    * First get the maximum page number currently assigned to a serial in the specified {@code table}.
    * Next update rows where the value of {@code lost} is less than the current date minus five years, meaning that
    * this serial is lost for more than five years. If more serials are needed, new rows will then be inserted.
    * <p> Called to create new labels or idcards. </p>
    *
    * @param cls
    *       either {@link Idcard} or {@link Label}.
    * @param table
    *       the table with the serials to create.
    * @param serialsPerPage
    *       the number of labels that can be placed on a single page.
    */
   static void createOnePage(Class<? extends Serial> cls, String table, int serialsPerPage) {
      try (SQLite.Transaction transaction = new SQLite.Transaction()) {
         int nextPage = 1 + SQLite.getIntFromQuery(table, "MAX(" + PAGE + ")", null);
         // SELECT _id FROM $table WHERE lost<nowMinusYears(5) ORDER BY _id ;
         String where = LOST + "<" + nowMinusYears(5);
         Iterator<? extends Serial> lost = SQLite.get(cls, table, new Values(OID), null, OID, where).iterator();
         for (int i = 0; i < serialsPerPage; i++) {
            if (lost.hasNext()) {
               lost.next().setLost(false).setLong(PAGE, nextPage).update();
            } else {
               SQLite.insert(null, table, new Values().addLong(PAGE, nextPage));
            }
         }
         transaction.setSuccessful();
      }
   }

   /**
    * Sets the state of the serials on the most recently printed page to 'Lost'.
    * <p> Called to delete labels or idcards. </p>
    *
    * @param table
    *       the table with the serials to update.
    * @return the number of updated rows.
    */
   static int deleteOnePage(String table) {
      // UPDATE $table SET page=NULL, lost=1000000000 WHERE page=(SELECT MAX(page) FROM $table) ;
      String where = App.format("%1$s=(SELECT MAX(%1$s) FROM %2$s)", PAGE, table);
      return SQLite.update(table, new Values(PAGE).addLong(LOST, MIN_TSTAMP), where);
   }

   /**
    * Returns the count of all 'Printed' serials in the specified {@code table}.
    *
    * @param table
    *       the table to query.
    * @return the count of all 'Printed' serials in the specified {@code table}.
    */
   static int countPrinted(String table) {
      // SELECT COUNT(*) FROM $table WHERE page NOTNULL ;
      return SQLite.getIntFromQuery(table, "COUNT(*)", PAGE + " NOTNULL");
   }

   /**
    * Returns a list of all 'Printed' serials in the specified {@code table}.
    * A serial has state 'Printed' if and only if {@code page NOTNULL}.
    * The list is ordered first by {@code page} and second by {@code _id}.
    * <p> Called when writing the 'Printed' marked idcards or labels to a PDF document. </p>
    *
    * @param cls
    *       either {@link Idcard} or {@link Label}.
    * @param table
    *       the table name (left join).
    * @param columns
    *       the column names.
    * @return a list of all 'Printed' serials in the specified {@code table}.
    */
   @NonNull
   static <S extends Serial> List<S> getPrinted(Class<S> cls, String table, Values columns) {
      // SELECT $columns FROM $table WHERE page NOTNULL ORDER BY page, _id ;
      return SQLite.get(cls, table, columns, null, PAGE + ", " + OID, PAGE + " NOTNULL");
   }

   /**
    * Returns the {@code Serial} with the specified {@code barcode}
    * or {@code null} if there is no such {@code Serial}.
    *
    * @param cls
    *       either {@link Idcard} or {@link Label}.
    * @param table
    *       the table name (left join).
    * @param columns
    *       the column names.
    * @param barcode
    *       the barcode of the requested {@code Serial}.
    * @return the {@code Serial} with the specified {@code barcode} or {@code null}.
    */
   @Nullable
   static <S extends Serial> S parse(Class<S> cls, String table, Values columns, String tab, String barcode) {
      int number = SerialNumber.parseCode128(barcode);
      return (number == 0) ? null : getNullable(cls, table, columns, tab, number);
   }

   /**
    * Returns the {@code Serial} with the specified {@code number}.
    *
    * @param cls
    *       either {@link Idcard} or {@link Label}.
    * @param table
    *       the table name (left join).
    * @param columns
    *       the column names.
    * @param number
    *       the number of the requested {@code Serial}.
    * @return the {@code Serial} with the specified {@code number}.
    *
    * @throws IllegalStateException
    *       if there is no such {@code Serial}.
    */
   @NonNull
   static <S extends Serial> S getNonNull(Class<S> cls, String table, Values columns, String tab, int number) {
      S serial = getNullable(cls, table, columns, tab, number);
      if (serial == null) {
         throw new RuntimeException("no " + cls.getSimpleName() + " with number " + number);
      }
      return serial;
   }

   @Nullable
   private static <S extends Serial> S getNullable(Class<S> cls, String table, Values columns, String tab, int number) {
      // SELECT $columns FROM $table WHERE $tab._id=$number ;
      String where = App.format("%s.%s=?", tab, OID);
      List<S> list = SQLite.get(cls, table, columns, null, null, where, number);
      return (list.size() == 0) ? null : list.get(0);
   }

   /**
    * Returns a list of all serials in the specified {@code table}, ordered by {@code _id}.
    * Rows where {@code lost} is less than the current date minus four years, meaning that this serial is lost
    * for at least four years, are excluded, because we assume that these serials are finally lost.
    *
    * @param cls
    *       either {@link Idcard} or {@link Label}.
    * @param table
    *       the table name (left join).
    * @param columns
    *       the column names.
    * @return a list of all serials in the specified {@code table}, ordered by {@code _id}.
    */
   @NonNull
   static <S extends Serial> ArrayList<S> get(Class<S> cls, String table, Values columns) {
      // SELECT $columns FROM $table WHERE lost ISNULL OR lost>nowMinusYears(4) ORDER BY _id ;
      return SQLite.get(cls, table, columns, null, OID, LOST + " ISNULL OR " + LOST + ">" + nowMinusYears(4));
   }

   /**
    * Returns a list of all stocked serials in the specified {@code table}, ordered by {@code _id}.
    *
    * @param cls
    *       either {@link Idcard} or {@link Label}.
    * @param table
    *       the table name (left join).
    * @param columns
    *       the column names.
    * @param uidOrBid
    *       either {@link User#UID} or {@link Book#BID}.
    * @return a list of all stocked serials in the specified {@code table}, ordered by {@code _id}.
    */
   @NonNull
   static <S extends Serial> ArrayList<S> getStocked(Class<S> cls, String table, Values columns, String uidOrBid) {
      // SELECT $columns FROM $table WHERE page ISNULL AND lost ISNULL AND $uidOrBid ISNULL ORDER BY _id ;
      String where = App.format("%s ISNULL AND %s ISNULL AND %s ISNULL", PAGE, LOST, uidOrBid);
      return SQLite.get(cls, table, columns, null, OID, where);
   }

   /**
    * Returns a ascending ordered list of all page numbers in the specified {@code table}.
    * <p> Called before registering 'Printed' PDF documents ({@link de.fahimu.schlib.app.RegisterPrintsActivity}). </p>
    *
    * @param cls
    *       either {@link Idcard} or {@link Label}.
    * @param table
    *       the table to query.
    * @return a ascending ordered list of all page numbers in the specified {@code table}.
    */
   @NonNull
   static List<Integer> getPageNumbers(Class<? extends Serial> cls, String table) {
      // SELECT page FROM $table WHERE page NOTNULL GROUP BY page ORDER BY page ;
      List<? extends Serial> serials = SQLite.get(cls, table, new Values(PAGE), PAGE, PAGE, PAGE + " NOTNULL");

      List<Integer> numbers = new ArrayList<>(serials.size());
      for (Serial serial : serials) {
         numbers.add(serial.getPage());
      }
      Log.d("numbers=" + numbers);
      return numbers;
   }

   /**
    * Promotes all serials from 'Printed' to 'Stocked'
    * which are printed on the same page as the specified {@code serial}.
    * <p> Precondition: {@code serial.isPrinted()} must be {@code true}. </p>
    * <p> Called after registering succeeds. </p>
    *
    * @param serial
    *       the serial that represents the page to register.
    */
   public static int setStocked(Serial serial) {
      // UPDATE $serial.getTable() SET page=NULL WHERE page=$serial.getPage() ;
      return SQLite.update(serial.getTable(), new Values(PAGE), PAGE + "=" + serial.getPage());
   }

   /* ============================================================================================================== */

   /**
    * Returns the serial number.
    *
    * @return the serial number.
    */
   public final int getId() {
      return values.getInt(OID);
   }

   /**
    * Returns {@code true} if the serial is printed, otherwise {@code false}.
    *
    * @return {@code true} if the serial is printed.
    */
   public final boolean isPrinted() {
      return values.notNull(PAGE);
   }

   /**
    * Returns the page number where the serial is printed.
    * <p> Precondition: {@link #isPrinted()} must be {@code true}. </p>
    *
    * @return the page number where the serial is printed.
    */
   public final int getPage() {
      return values.getInt(PAGE);
   }

   /**
    * Returns {@code true} if the serial is stocked, otherwise {@code false}.
    *
    * @return {@code true} if the serial is stocked.
    */
   public final boolean isStocked() {
      return !isPrinted() && !isLost() && !isUsed();
   }

   /**
    * Returns {@code true} if this serial is used, otherwise {@code false}.
    *
    * @return {@code true} if this serial is ued.
    */
   public abstract boolean isUsed();

   /**
    * Returns {@code true} if the serial is lost, otherwise {@code false}.
    *
    * @return {@code true} if the serial is lost.
    */
   public final boolean isLost() {
      return values.notNull(LOST);
   }

   /**
    * Returns the date when the serial was lost as a string, formatted {@code DD.MM.YYYY} in the default timezone.
    * <p> Precondition: {@link #isLost()} must be {@code true}. </p>
    *
    * @return the date when the serial was lost.
    */
   @NonNull
   private String getLostDate() {
      return App.formatDate(R.string.app_date, false, values.getLong(LOST));
   }

   @NonNull
   public final Serial setLost(boolean lost) {
      return (Serial) (lost ? setLong(LOST, App.posixTime()) : setNull(LOST));
   }

   @NonNull
   public final String getDisplayId() {
      return SerialNumber.getDisplay(getId());
   }

   @NonNull
   public final String getDisplay() {
      if (isPrinted()) {
         return App.getStr(R.string.serial_display_printed, getPage());
      } else if (isLost()) {
         return App.getStr(R.string.serial_display_lost, getLostDate());
      } else if (isUsed()) {
         return getDisplayUsed();
      } else {                // isStocked()
         return App.getStr(R.string.serial_display_stocked);
      }
   }

   @NonNull
   abstract String getDisplayUsed();

}