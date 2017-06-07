/*
 * Trigger.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.db;

import android.database.sqlite.SQLiteDatabase;

/**
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class Trigger {

   public enum Type {
      AFTER_INSERT("AFTER INSERT", "AI", "NEW."),
      AFTER_UPDATE("AFTER UPDATE", "AU", "NEW."),
      AFTER_DELETE("AFTER DELETE", "AD", "OLD.");

      private final String when, prefix, ref;

      Type(String when, String prefix, String ref) {
         this.when = when;
         this.prefix = prefix;
         this.ref = ref;
      }
   }

   public static void create(SQLiteDatabase db, String table, Type type, String dstTable, String... columns) {
      Trigger trigger = new Trigger(table, type);
      trigger.addInsert(dstTable, columns);
      trigger.create(db);
   }

   public static void drop(SQLiteDatabase db, String table, Type type) {
      SQLite.drop(db, "TRIGGER", type.prefix, table);
   }

   /* ============================================================================================================== */

   private final StringBuilder sql = new StringBuilder(200);

   private final String ref;

   public Trigger(String table, Type type) {
      sql.append("CREATE TRIGGER ").append(type.prefix).append('_').append(table);
      sql.append(" ").append(type.when).append(" ON ").append(table).append(" BEGIN");
      ref = type.ref;
   }

   private Trigger addInsert(String dstTable, String... columns) {
      sql.append("\nINSERT INTO ").append(dstTable);
      sql.append(" (").append(SQLite.catToString(", ", columns)).append(")");
      sql.append("\nVALUES (").append(ref).append(SQLite.catToString(", " + ref, columns)).append(");");
      return this;
   }

   public Trigger addInsertOrIgnoreSelected(String dstTable, String columns, String table, String where) {
      sql.append("\nINSERT OR IGNORE INTO ").append(dstTable);
      sql.append("\nSELECT ").append(columns).append(" FROM ").append(table);
      sql.append("\n   WHERE ").append(where).append(";");
      return this;
   }

   public void create(SQLiteDatabase db) {
      String createTrigger = sql.append("\nEND;").toString();
      SQLite.execSQL(db, createTrigger);
   }

}