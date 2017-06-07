/*
 * Table.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.db;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;


import java.util.ArrayList;
import java.util.List;

/**
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class Table {

   private static final String TYPE_TEXT  = "TEXT    ";
   private static final String TYPE_LONG  = "INTEGER ";
   private static final String TYPE_TIME  = "DATETIME";
   private static final String CONSTRAINT = "        ";

   private static final String EMPTY       = "           ";
   private static final String NOT_NULL    = "NOT NULL   ";
   private static final String PRIMARY_KEY = "PRIMARY KEY";

   private final int    align;
   private final String table;

   private final StringBuilder sql     = new StringBuilder(200);
   private final List<String>  indices = new ArrayList<>(10);

   private String  column;
   private boolean isNull;

   private Table append(String separator, String column, String type, String extra) {
      this.column = column;
      sql.append(separator).append(column);
      for (int i = column.length(); i < align; i++) { sql.append(' '); }
      sql.append(' ').append(type).append(' ').append(extra);
      return this;
   }

   public Table(String table, int align, boolean autoIncrement) {
      this.align = align;
      this.table = table;
      sql.append("CREATE TABLE ").append(table);
      append(" (\n", BaseColumns._ID, TYPE_LONG, PRIMARY_KEY);
      if (autoIncrement) { sql.append(" AUTOINCREMENT"); }
   }

   private Table addColumn(String column, String type, boolean notNull) {
      isNull = !notNull;
      return append(",\n", column, type, notNull ? NOT_NULL : EMPTY);
   }

   public Table addTextColumn(String column, boolean notNull) {
      return addColumn(column, TYPE_TEXT, notNull);
   }

   public Table addLongColumn(String column, boolean notNull) {
      return addColumn(column, TYPE_LONG, notNull);
   }

   public Table addTimeColumn(String column, boolean notNull) {
      return addColumn(column, TYPE_TIME, notNull);
   }

   public Table addReferences(String column, boolean notNull) {
      addLongColumn(column, notNull);
      sql.append(" REFERENCES ").append(column).append('s');
      return this;
   }

   public Table addConstraint() {
      return addColumn("", CONSTRAINT, false);
   }

   /* ============================================================================================================== */

   public Table addDefaultPosixTime() {
      sql.append(" DEFAULT (CAST(STRFTIME('%s','now') AS INTEGER))");
      return this;
   }

   private char constraint = 'a';

   private Table addConstrain(String type, String statement) {
      // CONSTRAINT a [CHECK|UNIQUE] (statement)
      sql.append(" CONSTRAINT ").append(constraint++);
      sql.append(" ").append(type).append(" (").append(statement).append(")");
      return this;
   }

   public Table addUnique(String... columns) {
      if (columns.length == 0) {
         sql.append(" UNIQUE");
         return this;
      } else {
         return addConstrain("UNIQUE", SQLite.catToString(", ", columns));
      }
   }

   public Table addCheck(String statement) {
      // CHECK ($statement)
      return addConstrain("CHECK", statement);
   }

   public Table addCheckBetween(long min, long max) {
      // CHECK ($column BETWEEN $min AND $max)
      return addCheck(column + " BETWEEN " + min + " AND " + max);
   }

   public Table addCheckLength(String cmp) {
      // CHECK (LENGTH($column) $cmp)
      return addCheck("LENGTH(" + column + ")" + cmp);
   }

   public Table addCheckPosixTime(long min) {
      // CHECK (TYPEOF($column)='integer' AND $column>=$min)
      // CHECK ($column ISNULL OR TYPEOF($column)='integer' AND $column>=$min)
      String checkNull = isNull ? column + " ISNULL OR " : "";
      return addCheck(checkNull + "TYPEOF(" + column + ")='integer' AND " + column + ">=" + min);
   }

   public Table addCheckIn(String... values) {
      // CHECK ($column IN ('$value1', '$value2'))
      return addCheck(column + " IN ('" + SQLite.catToString("', '", values) + "')");
   }

   public Table addIndex() {
      indices.add("CREATE INDEX " + table + "_" + column + " ON " + table + " (" + column + ");");
      return this;
   }

   public void create(SQLiteDatabase db) {
      String createTable = sql.append("\n);").toString();
      SQLite.execSQL(db, createTable);
      for (String createIndex : indices) {
         SQLite.execSQL(db, createIndex);
      }
   }

   /* ============================================================================================================== */

   public static void dropIndex(SQLiteDatabase db, String table, String... columns) {
      for (String column : columns) {
         SQLite.drop(db, "INDEX", table, column);
      }
   }

}