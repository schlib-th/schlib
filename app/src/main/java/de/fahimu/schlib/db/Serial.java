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
import java.util.Iterator;
import java.util.List;

import de.fahimu.android.app.Log;
import de.fahimu.android.db.Row;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Values;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.app.App;
import de.fahimu.schlib.app.R;

/**
 * A serial can have one of four states: Printed, Stocked, Used or Lost.
 * Printed: page NOTNULL AND lost ISNULL
 * Stocked: page ISNULL AND lost ISNULL, no book/user references the serial.
 * Used:    page ISNULL AND lost ISNULL, some book/user references the serial.
 * Lost:    page ISNULL AND lost NOTNULL
 * A serial will never be deleted after insertion. If a serial is lost, it will be reused after a couple of years
 * when new serials must be printed.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public abstract class Serial extends Row {

   static final String OID  = BaseColumns._ID;
   static final String PAGE = "page";
   static final String LOST = "lost";

   static void create(@NonNull SQLiteDatabase db, @NonNull String table, int min, int max) {
      Table tab = new Table(table, 4, OID, false).addCheckBetween(min, max);
      tab.addColumn(PAGE, Table.TYPE_INTE, false).addCheckBetween(1, 999);
      tab.addColumn(LOST, Table.TYPE_TIME, false).addCheckLength("=", 19);
      tab.addConstraint("undefined_state").addCheck(PAGE + " ISNULL OR " + LOST + " ISNULL");
      tab.create(db);
      // ensure that we'll start with min
      SQLite.insertFirstRowAfterCreate(db, table, new Values().add(OID, min).add(LOST, "1972-04-19 15:00:00"));
   }

   /* ============================================================================================================== */

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
   static int canCallCreateOnePage(@NonNull String table, int serialsPerPage, int max) {
      // SELECT (free+lost)/<serialsPerPage> FROM
      //    (SELECT <max>-MAX(_id) AS free FROM <table>),
      //    (SELECT COUNT(*) AS lost FROM <table> WHERE lost < (SELECT DATETIME('NOW', '-5 years'))) ;
      String column = App.format("(free+lost)/%d", serialsPerPage);
      String query1 = App.format("SELECT %d-MAX(%s) AS free FROM %s", max, OID, table);
      String query2 = App.format("SELECT DATETIME('NOW', '-5 years')");
      String query3 = App.format("SELECT COUNT(*) AS lost FROM %s WHERE %s<(%s)", table, LOST, query2);
      String tables = App.format("(%s), (%s)", query1, query3);
      return Integer.parseInt(SQLite.getFromQuery(tables, column, "0", null));
   }

   /**
    * Sets the state of {@code serialsPerPage} rows in {@code table} to 'Printed'.
    * First get the maximum page number currently assigned to a serial in the specified {@code table}.
    * Next update rows where the value of {@code lost} is less than the current date minus five years, meaning that
    * this serial was not used for the last five years. If more serials are needed, new rows will then be inserted.
    * <p> Called to create new labels or idcards. </p>
    *
    * @param cls
    *       either {@link Idcard} or {@link Label}.
    * @param table
    *       the table with the serials to create.
    * @param serialsPerPage
    *       the number of labels that can be placed on a single page.
    */
   static void createOnePage(@NonNull Class<? extends Serial> cls, @NonNull String table, int serialsPerPage) {
      try (SQLite.Transaction transaction = new SQLite.Transaction()) {
         int nextPage = 1 + Integer.parseInt(SQLite.getFromQuery(table, "MAX(" + PAGE + ")", "0", null));
         // SELECT _id FROM <table> WHERE lost < (SELECT DATETIME('NOW', '-5 years')) ORDER BY _id ;
         String where = App.format("%s < (SELECT DATETIME('NOW', '-5 years'))", LOST);
         Iterator<? extends Serial> lost = SQLite.get(cls, table, new Values().add(OID), null, OID, where).iterator();
         for (int i = 0; i < serialsPerPage; i++) {
            if (lost.hasNext()) {
               lost.next().setLost(false).setNonNull(PAGE, nextPage).update();
            } else {
               SQLite.insert(table, new Values().add(PAGE, nextPage));
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
   static int deleteOnePage(@NonNull String table) {
      // UPDATE <table> SET page=NULL, lost=(SELECT DATETIME('NOW', '-6 years'))
      //    WHERE page=(SELECT MAX(page) FROM <table>) ;
      String lost = SQLite.getFromRawQuery("SELECT DATETIME('NOW', '-6 years')");
      String where = App.format("%s=(SELECT MAX(%s) FROM %s)", PAGE, PAGE, table);
      return SQLite.update(table, new Values().add(PAGE).add(LOST, lost), where);
   }

   /**
    * Returns the count of all 'Printed' serials in the specified {@code table}.
    *
    * @param table
    *       the table to query.
    * @return the count of all 'Printed' serials in the specified {@code table}.
    */
   static int countPrinted(@NonNull String table) {
      // SELECT COUNT(*) FROM <table> WHERE page NOTNULL ;
      return Integer.parseInt(SQLite.getFromQuery(table, "COUNT(*)", "0", PAGE + " NOTNULL"));
   }

   /**
    * Returns a list of all 'Printed' serials in the specified {@code table}.
    * A serial has state 'Printed' if and only if {@code page NOTNULL}.
    * The list is ordered first by {@code page} and second by {@code _id}.
    * <p> Called when writing the 'Printed' marked idcards or labels to a PDF document. </p>
    *
    * @param cls
    *       either {@link Idcard} or {@link Label}.
    * @param joinedTable
    *       the table name (left join).
    * @param columns
    *       the column names.
    * @return a list of all 'Printed' serials in the specified {@code table}.
    */
   @NonNull
   static <S extends Serial> List<S> getPrinted(@NonNull Class<S> cls, @NonNull String joinedTable,
         @NonNull Values columns) {
      // SELECT <columns> FROM <table> WHERE page NOTNULL ORDER BY page, _id ;
      return SQLite.get(cls, joinedTable, columns, null, PAGE + ", " + OID, PAGE + " NOTNULL");
   }

   /**
    * Returns the {@code Serial} with the specified {@code barcode}
    * or {@code null} if there is no such {@code Serial}.
    *
    * @param cls
    *       either {@link Idcard} or {@link Label}.
    * @param joinedTable
    *       the table name (left join).
    * @param columns
    *       the column names.
    * @param table
    *       the table name for qualifying {@code _id} in the where clause.
    * @param barcode
    *       the barcode of the requested {@code Serial}.
    * @return the {@code Serial} with the specified {@code barcode} or {@code null}.
    */
   @Nullable
   static <S extends Serial> S parse(@NonNull Class<S> cls, @NonNull String joinedTable,
         @NonNull Values columns, @NonNull String table, String barcode) {
      int serial = SerialNumber.parseCode128(barcode);
      return (serial == 0) ? null : getNullable(cls, joinedTable, columns, table, serial);
   }

   /**
    * Returns the {@code Serial} with the specified {@code number}.
    *
    * @param cls
    *       either {@link Idcard} or {@link Label}.
    * @param joinedTable
    *       the table name (left join).
    * @param columns
    *       the column names.
    * @param table
    *       the table name for qualifying {@code _id} in the where clause.
    * @param number
    *       the number of the requested {@code Serial}.
    * @return the {@code Serial} with the specified {@code number}.
    *
    * @throws IllegalStateException
    *       if there is no such {@code Serial}.
    */
   @NonNull
   static <S extends Serial> S getNonNull(@NonNull Class<S> cls, @NonNull String joinedTable,
         @NonNull Values columns, @NonNull String table, int number) {
      S serial = getNullable(cls, joinedTable, columns, table, number);
      if (serial == null) { throw new RuntimeException("no " + cls.getSimpleName() + " with number " + number); }
      return serial;
   }

   @Nullable
   private static <S extends Serial> S getNullable(@NonNull Class<S> cls, @NonNull String joinedTable,
         @NonNull Values columns, @NonNull String table, int number) {
      // SELECT <columns> FROM <table> WHERE _id=<serial> ;
      String where = App.format("%s.%s=?", table, OID);
      List<S> list = SQLite.get(cls, joinedTable, columns, null, null, where, number);
      return (list.size() == 0) ? null : list.get(0);
   }

   /**
    * Returns a list of all serials in the specified {@code table}, ordered by {@code _id}.
    * Rows where {@code lost} is less than the current date minus four years, meaning that this serial was lost
    * for the last four years, are excluded, because these serials can't be set from lost to stocked anymore.
    *
    * @param cls
    *       either {@link Idcard} or {@link Label}.
    * @param joinedTable
    *       the table name (left join).
    * @param columns
    *       the column names.
    * @return a list of all serials in the specified {@code table}, ordered by {@code _id}.
    */
   @NonNull
   static <S extends Serial> ArrayList<S> get(@NonNull Class<S> cls, @NonNull String joinedTable,
         @NonNull Values columns) {
      // SELECT <columns> FROM <table> WHERE lost ISNULL OR lost > (SELECT DATETIME('NOW', '-4 years')) ORDER BY _id ;
      String where = App.format("%s ISNULL OR %s > (SELECT DATETIME('NOW', '-4 years'))", LOST, LOST);
      return SQLite.get(cls, joinedTable, columns, null, OID, where);
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
   static List<Integer> getPageNumbers(@NonNull Class<? extends Serial> cls, @NonNull String table) {
      // SELECT page FROM <table> WHERE page NOTNULL GROUP BY page ORDER BY page ;
      String where = App.format("%s NOTNULL", PAGE);
      List<? extends Serial> groupedSerials = SQLite.get(cls, table, new Values().add(PAGE), PAGE, PAGE, where);

      List<Integer> numbers = new ArrayList<>(groupedSerials.size());
      for (Serial serial : groupedSerials) {
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
   public static int setStocked(@NonNull Serial serial) {
      // UPDATE <serial.getTable()> SET page=NULL WHERE page=<serial.getPage()> ;
      return SQLite.update(serial.getTable(), new Values().add(PAGE), PAGE + "=?", serial.getPage());
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
      return values.getNullable(PAGE) != null;
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
      return values.getNullable(LOST) != null;
   }

   /**
    * Returns the date when the serial was lost as a string, formatted {@code DD.MM.YYYY}.
    * <p> Precondition: {@link #isLost()} must be {@code true}. </p>
    *
    * @return the date when the serial was lost.
    */
   @NonNull
   private String getLostDate() {
      String datetime = values.getNonNull(LOST);
      return App.format("%s.%s.%s", datetime.substring(8, 10), datetime.substring(5, 7), datetime.substring(0, 4));
   }

   @NonNull
   public final Serial setLost(boolean lost) {
      return (Serial) setNullable(LOST, lost ? SQLite.getDatetimeNow() : null);
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