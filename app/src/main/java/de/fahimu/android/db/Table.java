/*
 * Table.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.db;

import android.database.sqlite.SQLiteDatabase;


import java.util.ArrayList;
import java.util.List;

import de.fahimu.android.app.Log;

/**
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class Table {

   public static final String TYPE_TEXT = "TEXT    ";
   public static final String TYPE_INTE = "INTEGER ";
   public static final String TYPE_TIME = "DATETIME";

   private final int    align;
   private final String table;

   private final StringBuilder sql     = new StringBuilder(200);
   private final List<String>  indices = new ArrayList<>(10);

   private String  column;
   private boolean notNull;

   private StringBuilder newColumn(String separator, String column, String type) {
      this.column = column;
      sql.append(separator).append(column);
      for (int i = column.length(); i < align; i++) { sql.append(' '); }
      return sql.append(' ').append(type);
   }

   public Table(String table, int align, String oid, boolean autoIncrement) {
      this.align = align;
      this.table = table;
      sql.append("CREATE TABLE ").append(table);
      newColumn(" (\n", oid, TYPE_INTE).append(" PRIMARY KEY");
      if (autoIncrement) { sql.append(" AUTOINCREMENT"); }
   }

   public Table addColumn(String column, String type, boolean notNull) {
      this.notNull = notNull;
      newColumn(",\n", column, type).append(notNull ? " NOT NULL   " : "            ");
      return this;
   }

   public Table addRefCol(String column, boolean notNull) {
      addColumn(column, TYPE_INTE, notNull);
      sql.append(" REFERENCES ").append(column).append('s');
      return this;
   }

   public Table addConstraint(String constraint) {
      sql.append(",\nCONSTRAINT ").append(constraint);
      return this;
   }

   public Table addDefault(String sql) {
      this.sql.append(" DEFAULT ").append(sql);
      return this;
   }

   public Table addCheck(String sql) {
      this.sql.append(" CHECK (").append(sql).append(")");
      return this;
   }

   public Table addCheckBetween(int min, int max) {
      sql.append(" CHECK (").append(column);
      sql.append(" BETWEEN ").append(min).append(" AND ").append(max).append(")");
      return this;
   }

   public Table addCheckLength(String operator, int len) {
      if (notNull) {
         sql.append(" CHECK (LENGTH(").append(column);
      } else {
         sql.append(" CHECK (IFNULL(LENGTH(").append(column).append("), ").append(len);
      }
      sql.append(")").append(operator).append(len).append(")");
      return this;
   }

   public Table addCheckIn(String... values) {
      sql.append(" CHECK (").append(column).append(" IN ('").append(SQLite.catToString("', '", values)).append("'))");
      return this;
   }

   public Table addUnique(String... columns) {
      sql.append(" UNIQUE");
      if (columns.length > 0) { sql.append(" (").append(SQLite.catToString(", ", columns)).append(")"); }
      return this;
   }

   public Table addIndex() {
      indices.add("CREATE INDEX " + table + "_" + column + " ON " + table + " (" + column + ");");
      return this;
   }

   public void create(SQLiteDatabase db) {
      String createTable = this.sql.append("\n);").toString();
      Log.d(createTable);
      db.execSQL(createTable);
      for (String createIndex : indices) {
         Log.d(createIndex);
         db.execSQL(createIndex);
      }
   }

}