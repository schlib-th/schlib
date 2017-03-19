/*
 * Use.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.db;

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

   static final private String TAB    = "uses";
   static final private String OID    = BaseColumns._ID;
   static final private String UID    = "uid";
   static final private String LOGIN  = "login";
   static final private String LOGOUT = "logout";

   // SELECT uses._id AS _id, uid, login, logout
   static final private Values TAB_COLUMNS = new Values().add(SQLite.alias(TAB, OID, OID), UID, LOGIN, LOGOUT);

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

   @NonNull
   @Override
   protected String getTable() { return TAB; }

   @NonNull
   public User getUser() {
      return User.getNonNull(values.getLong(UID));
   }

   @NonNull
   public Use setLogoutToNow() {
      return (Use) setNonNull(LOGOUT, SQLite.getDatetimeNow());
   }

}