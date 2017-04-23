/*
 * SQLite.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.db;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import java.util.ArrayList;

import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;

/**
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class SQLite {

   /**
    * A logging wrapper for {@link SQLiteDatabase#beginTransaction()},
    * {@link SQLiteDatabase#setTransactionSuccessful()} and {@link SQLiteDatabase#endTransaction()}.
    * The SQL statements {@code BEGIN TRANSACTION}, {@code COMMIT TRANSACTION} and {@code ROLLBACK TRANSACTION}
    * are logged with level verbose. Also, {@code Transaction} renames {@code endTransaction} to {@code close} and
    * implements {@link AutoCloseable}, so clients can make use of the new Java 7 {@code try}-with-resources statement.
    */
   public static final class Transaction implements AutoCloseable {
      private boolean closed = false, successful = false;

      /**
       * Begins a new Transaction.
       */
      public Transaction() {
         Log.d("BEGIN TRANSACTION");
         App.getDb().beginTransaction();
      }

      /**
       * Sets this Transaction to successful.
       */
      public void setSuccessful() {
         App.getDb().setTransactionSuccessful();
         successful = true;
      }

      /**
       * Ends this Transaction.
       */
      public void close() {
         if (!closed) {        // make close idempotent
            Log.d(successful ? "COMMIT TRANSACTION" : "ROLLBACK TRANSACTION");
            App.getDb().endTransaction();
            closed = true;
         }
      }
   }

   /* ============================================================================================================== */
   /*  Special queries when only one string should be returned.                                                      */
   /* ============================================================================================================== */

   /**
    * Returns the value of column 0 of the SQL statement {@code SELECT DATETIME('NOW')}.
    *
    * @return the value of column 0 of the SQL statement {@code SELECT DATETIME('NOW')}.
    */
   @NonNull
   public static String getDatetimeNow() {
      return getFromRawQuery("SELECT DATETIME('NOW')");
   }

   /**
    * Runs the provided SQL and returns the value of column 0 of the first row.
    * <p> The SQL SELECT statement and the result are logged with level verbose. </p>
    *
    * @param sql
    *       the SQL query to execute (must no be ; terminated).
    * @param args
    *       the values, which will replace the {@code '?'} characters in {@code sql}.
    * @return the value of column 0 of the first row.
    *
    * @throws IllegalStateException
    *       if no row is selected or the value of the column 0 is null.
    */
   @NonNull
   public static String getFromRawQuery(String sql, Object... args) {
      try (Cursor cursor = rawQuery(sql, args)) {
         String value = getValueOfFirstColumn(cursor, null);
         if (value == null) { throw new IllegalStateException("raw query return null"); }
         return value;
      }
   }

   /**
    * Queries the specified {@code table} with a SQL WHERE clause, specified by {@code where} and {@code args}, and
    * returns the value of the specified {@code column} or {@code defaultValue}, if no row is selected or the
    * value of the column is null.
    * <p> The SQL SELECT statement and the result are logged with level verbose. </p>
    *
    * @param table
    *       the table name.
    * @param column
    *       the column to return.
    * @param defaultValue
    *       the value to return if no row is selected or the value of the column is null.
    * @param where
    *       a filter declaring which rows to return.
    * @param args
    *       the values, which will replace the {@code '?'} characters in {@code where}.
    * @return the value of the specified {@code column} or {@code defaultValue}.
    */
   @Nullable
   public static String getFromQuery(String table, String column, String defaultValue, String where, Object... args) {
      try (Cursor cursor = query(table, new Values().add(column), null, null, where, args)) {
         return getValueOfFirstColumn(cursor, defaultValue);
      }
   }

   /**
    * Returns the value of column 0 of the specified cursor {@code c} or {@code defaultValue},
    * if no row is selected or the value of the column is null.
    *
    * @param c
    *       the cursor.
    * @param defaultValue
    *       the value to return if no row is selected or the value of the column is null.
    * @return the value of column 0 of the specified cursor.
    */
   @Nullable
   private static String getValueOfFirstColumn(Cursor c, @Nullable String defaultValue) {
      String value = (c.moveToFirst() && c.getType(0) != Cursor.FIELD_TYPE_NULL) ? c.getString(0) : defaultValue;
      Log.d("value=" + value);
      return value;
   }

   /* ============================================================================================================== */

   /**
    * {@code INSERT INTO} the specified {@code table} a new row with the specified {@code values}.
    * <p> The SQL INSERT statement and the result {@code id} are logged with level verbose. </p>
    *
    * @param db
    *       the database where to insert the row.
    * @param table
    *       the table to insert the row into.
    * @param values
    *       pairs of column names and column values of the new row.
    * @throws SQLException
    *       if an error occurred.
    */
   public static void insertFirstRowAfterCreate(SQLiteDatabase db, String table, Values values) throws SQLException {
      Log.d(App.format("INSERT INTO %s VALUES %s", table, values));
      long oid = db.insertWithOnConflict(table, null, values.cv, SQLiteDatabase.CONFLICT_NONE);
      if (oid == -1) { throw new SQLException("INSERT returned -1"); }
      Log.d("oid=" + oid);
   }

   /**
    * {@code INSERT OR ABORT INTO} the specified {@code table} a new row with the specified {@code values}.
    * <p> The SQL INSERT statement and the result {@code id} are logged with level verbose. </p>
    *
    * @param table
    *       the table to insert the row into.
    * @param values
    *       pairs of column names and column values of the new row.
    * @return the row ID of the newly inserted row.
    *
    * @throws SQLException
    *       if an error occurred.
    */
   public static long insert(String table, Values values) throws SQLException {
      Log.d(App.format("INSERT OR ABORT INTO %s VALUES %s", table, values));
      long oid = App.getDb().insertWithOnConflict(table, null, values.cv, SQLiteDatabase.CONFLICT_ABORT);
      if (oid == -1) { throw new SQLException("INSERT OR ABORT returned -1"); }
      Log.d("oid=" + oid);
      return oid;
   }

   /**
    * Queries the specified {@code table} with a SQL WHERE clause, specified by {@code where} and {@code args}, and
    * returns the result set as a list of {@link Row}s.
    * Each row consists of the specified {@code columns}, and the rows are sorted as specified by {@code order}.
    * If no row is selected, an empty list will be returned.
    * <p> The SQL SELECT statement and the result {@code List} object are logged with level verbose. </p>
    *
    * @param cls
    *       a subclass of {@link Row} that will be created for each row in the result set.
    * @param table
    *       the table name.
    * @param columns
    *       the name of the columns to return.
    * @param order
    *       a {@code String} specifying how to sort the result set or {@code null}.
    * @param where
    *       a filter declaring which rows to return.
    * @param args
    *       the values, which will replace the {@code '?'} characters in {@code where}.
    * @return the result set as a list of {@link Row}s.
    *
    * @throws SQLException
    *       if an error occurred.
    */
   @NonNull
   public static <R extends Row> ArrayList<R> get(Class<R> cls, String table, Values columns,
         String group, String order, String where, Object... args) throws SQLException {
      try (Cursor cursor = query(table, columns, group, order, where, args)) {
         ArrayList<R> list = new ArrayList<>(cursor.getCount());
         while (cursor.moveToNext()) {
            list.add(cls.newInstance().add(cls, cursor));
         }
         String prefix = "list<" + cls.getSimpleName() + ">(" + list.size() + ")=";
         if (list.size() <= 50) {
            Log.d(prefix + list);
         } else {
            Log.d(prefix + list.subList(0, 30) + " ... " + list.subList(list.size() - 20, list.size()));
         }
         return list;
      } catch (Exception exc) { throw new SQLException("bulk query failed", exc); }
   }

   @NonNull
   private static Cursor query(String table, Values columns, String group, String order, String where,
         Object... args) {
      Log.d(App.format("SELECT %s FROM %s WHERE %s GROUP BY %s ORDER BY %s",
            catToString(", ", columns.keys()), table, bind(where, args), group, order));
      return App.getDb().query(table, columns.keys(), where, toStringArray(args), group, null, order);
   }

   @NonNull
   private static Cursor rawQuery(String sql, Object... args) {
      Log.d(bind(sql, args));
      return App.getDb().rawQuery(sql, toStringArray(args));
   }

   /**
    * Updates in the specified {@code table} the rows specified by {@code where} with {@code values}.
    * <p> The SQL UPDATE statement and the number of updated rows are logged with level verbose. </p>
    *
    * @param table
    *       the table to update in.
    * @param values
    *       a map specifying the new values for some or all columns.
    * @param where
    *       a filter declaring which rows to update.
    * @param args
    *       the values, which will replace the {@code '?'} characters in {@code where}.
    * @return the number of updated rows.
    */
   public static int update(String table, Values values, String where, Object... args) {
      Log.d(App.format("UPDATE %s SET %s WHERE %s", table, values, bind(where, args)));
      int rows = App.getDb().update(table, values.cv, where, toStringArray(args));
      Log.d(rows + " rows were updated");
      return rows;
   }

   /**
    * Deletes in the specified {@code table} the rows specified by {@code where} and {@code args}.
    * <p> The SQL DELETE statement and the number of deleted rows are logged with level verbose. </p>
    *
    * @param table
    *       the table where to delete rows.
    * @param where
    *       a filter declaring which rows to delete.
    * @param args
    *       the values, which will replace the {@code '?'} characters in {@code where}.
    * @return the number of deleted rows.
    */
   public static int delete(String table, String where, Object... args) {
      Log.d(App.format("DELETE FROM %s WHERE %s", table, bind(where, args)));
      int rows = App.getDb().delete(table, where, toStringArray(args));
      Log.d(rows + " rows were deleted");
      return rows;
   }

   /* ============================================================================================================== */
   /*  Utility methods.                                                                                              */
   /* ============================================================================================================== */

   public static String alias(String table, String column, String alias) {
      return App.format("%s.%s AS %s", table, column, alias);
   }

   /**
    * Returns a new string that consists of the concatenated {@code values}, each converted to a string
    * and separated by the {@code separator} string. The array {@code values} must not be empty.
    *
    * @param separator
    *       the separator string.
    * @param values
    *       the array with the values.
    * @return a new string with the concatenated values.
    */
   @NonNull
   public static String catToString(String separator, String... values) {
      StringBuilder b = new StringBuilder(50).append(values[0]);
      for (int i = 1; i < values.length; i++) { b.append(separator).append(values[i]); }
      return b.toString();
   }

   /**
    * Replaces the question marks in the {@code where} clause by the specified {@code args} and
    * returns the final where clause as a string. Used for logging purposes.
    *
    * @param where
    *       the raw where clause.
    * @param args
    *       the arguments that replace the {@code '?'} characters.
    * @return the final where clause.
    */
   @NonNull
   private static String bind(String where, Object[] args) {
      if (where == null) { return ""; }
      StringBuilder b = new StringBuilder(where.length() + 20 * args.length);
      for (int j = 0, i = 0; i < where.length(); i++) {
         char c = where.charAt(i);
         if (c == '?') {
            b.append('\'').append(args[j++]).append('\'');
         } else {
            b.append(c);
         }
      }
      return b.toString();
   }

   /**
    * Returns a new string array containing the values from {@code args}, each converted to a string.
    *
    * @param args
    *       the objects that should be converted to a string.
    * @return a new string array with the converted values from {@code args}.
    */
   @NonNull
   private static String[] toStringArray(Object[] args) {
      String[] argsAsStrings = new String[args.length];
      for (int i = 0; i < args.length; i++) { argsAsStrings[i] = args[i].toString(); }
      return argsAsStrings;
   }

}