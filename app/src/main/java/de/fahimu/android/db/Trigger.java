/*
 * Trigger.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.db;

import android.database.sqlite.SQLiteDatabase;


import de.fahimu.android.app.Log;

/**
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class Trigger {

   public enum Type {
      AFTER_INSERT("AFTER INSERT", "AI"),
      AFTER_UPDATE("AFTER UPDATE", "AU"),
      AFTER_DELETE("AFTER DELETE", "AD");

      private final String when, prefix;

      Type(String when, String prefix) {
         this.when = when;
         this.prefix = prefix;
      }
   }

   private final StringBuilder sql = new StringBuilder(200);

   public Trigger(String tabName, Type type) {
      sql.append("CREATE TRIGGER ").append(type.prefix).append('_').append(tabName);
      sql.append(" ").append(type.when).append(" ON ").append(tabName).append(" BEGIN");
   }

   public Trigger addInsert(String tabName, String... columns) {
      sql.append("\nINSERT INTO ").append(tabName);
      if (columns.length > 0) { sql.append(" (").append(SQLite.catToString(", ", columns)).append(")"); }
      return this;
   }

   public Trigger addValues(String... values) {
      sql.append("\nVALUES (").append(SQLite.catToString(", ", values)).append(");");
      return this;
   }

   public void create(SQLiteDatabase db) {
      String createTrigger = this.sql.append("\nEND;").toString();
      Log.d(createTrigger);
      db.execSQL(createTrigger);
   }

}