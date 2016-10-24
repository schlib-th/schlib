/*
 * Use.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import java.util.List;

import de.fahimu.android.db.Row;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Values;

/**
 * A in-memory representation of one row of table {@code uses}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class Use extends Row {

   private static final String TAB = "uses";

   private static final String OID    = BaseColumns._ID;
   private static final String UID    = "uid";
   private static final String LOGIN  = "login";
   private static final String LOGOUT = "logout";

   private static final Values TAB_COLUMNS = new Values().add(new String[] {
         SQLite.alias(TAB, OID, OID), UID, LOGIN, LOGOUT
   });

   static void create(SQLiteDatabase db) {
      Table tab = new Table(TAB, 6, OID, true);
      tab.addRefCol(UID, true).addIndex();
      tab.addColumn(LOGIN, Table.TYPE_TIME, true).addDefault("CURRENT_TIMESTAMP");
      tab.addColumn(LOGOUT, Table.TYPE_TIME, false);
      tab.addConstraint("logout_after_login").addCheck("IFNULL(" + LOGOUT + ">=" + LOGIN + ", 1)");
      tab.create(db);
   }

   /* ============================================================================================================== */

   public static void login(@NonNull User user) {
      SQLite.insert(TAB, new Values().add(UID, user.getUid()));
   }

   /**
    * Returns the most recently inserted {@code Use} where {@code logout ISNULL}.
    *
    * @return the most recently inserted {@code Use} where {@code logout ISNULL}.
    */
   @Nullable
   public static Use getLoggedInNullable() {
      List<Use> list = SQLite.get(Use.class, TAB, TAB_COLUMNS, null, LOGIN + " DESC", LOGOUT + " ISNULL");
      return (list.size() == 0) ? null : list.get(0);
   }

   @NonNull
   public static Use getLoggedInNonNull() {
      Use use = getLoggedInNullable();
      if (use == null) { throw new RuntimeException("no user logged in"); }
      return use;
   }

   /* ============================================================================================================== */

   /**
    * Creates a new {@code Use} that initially contains the column values from the specified cursor {@code c}.
    *
    * @param cursor
    *       the cursor.
    */
   @SuppressWarnings ("unused")
   public Use(Cursor cursor) { super(cursor); }

   @Override
   protected String getTable() { return TAB; }

   /* ============================================================================================================== */

   public User getUser() {
      return User.getNonNull(values.getLong(UID));
   }

   public Use setLogoutToNow() {
      return (Use) setNonNull(LOGOUT, SQLite.getDatetimeNow());
   }

}