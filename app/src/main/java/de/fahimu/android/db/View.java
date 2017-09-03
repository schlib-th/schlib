/*
 * View.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.db;

import android.database.sqlite.SQLiteDatabase;

/**
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.05.2017
 * @since SchoolLibrary 1.0
 */
public final class View {

   private final StringBuilder sql = new StringBuilder(200);

   public View(String name) {
      sql.append("CREATE VIEW ").append(name).append(" AS");
   }

   public View addSelect(String table, Values columns, String group, String order, String where, Object... args) {
      sql.append("\n   SELECT ").append(SQLite.catToString(", ", columns.keys()));
      sql.append("\n   FROM ").append(table);
      if (where != null) { sql.append("\n   WHERE ").append(SQLite.bind(where, args)); }
      if (group != null) { sql.append("\n   GROUP BY ").append(group); }
      if (order != null) { sql.append("\n   ORDER BY ").append(order); }
      return this;
   }

   public View addSQL(String sqlText) {
      sql.append("\n").append(sqlText);
      return this;
   }

   public void create(SQLiteDatabase db) {
      String createView = sql.append(';').toString();
      SQLite.execSQL(db, createView);
   }

   public static void drop(SQLiteDatabase db, String... names) {
      for (String name : names) {
         SQLite.drop(db, "VIEW", name);
      }
   }

}