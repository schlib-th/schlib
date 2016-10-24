/*
 * OpenHelper.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;

/**
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class OpenHelper extends SQLiteOpenHelper {

   public OpenHelper() {
      super(App.getInstance(), "database", null, 1);
   }

   @Override
   public void onConfigure(SQLiteDatabase db) {
      Log.d(db.getPath() + (db.isReadOnly() ? " - read" : " - read/write"));
      db.setForeignKeyConstraintsEnabled(true);
   }

   @Override
   public void onCreate(SQLiteDatabase db) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         Idcard.create(db);
         Label.create(db);
         User.create(db);
         Book.create(db);
         Use.create(db);
         Lending.create(db);
         Preference.create(db);
      }
   }

   @Override
   public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.d("newVersion=" + newVersion);
      throw new IllegalArgumentException("newVersion=" + newVersion);
   }

   @Override
   public void onOpen(SQLiteDatabase db) {
      Log.d(db.getPath() + (db.isReadOnly() ? " - read" : " - read/write"));
   }

}