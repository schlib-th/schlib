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


import de.fahimu.android.app.App;
import de.fahimu.android.db.Row;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Values;

import static de.fahimu.android.db.SQLite.MIN_TSTAMP;

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

   /* -------------------------------------------------------------------------------------------------------------- */

   static void create(SQLiteDatabase db) {
      createTableUses(db);
   }

   static void upgrade(SQLiteDatabase db, int oldVersion) {
      if (oldVersion < 2) {
         upgradeTableUsesV2(db);
      }
   }

   private static void createTableUses(SQLiteDatabase db) {
      Table tab = new Table(TAB, 6, true);
      tab.addReferences(UID, true).addIndex();           // essential to group rows by uid
      tab.addTimeColumn(LOGIN, true).addCheckPosixTime(MIN_TSTAMP).addDefaultPosixTime();
      tab.addTimeColumn(LOGOUT, false).addCheckPosixTime(MIN_TSTAMP);
      tab.addConstraint().addCheck(LOGOUT + ">=" + LOGIN);
      tab.create(db);
   }

   private static void upgradeTableUsesV2(SQLiteDatabase db) {
      Table.dropIndex(db, TAB, UID);
      SQLite.alterTablePrepare(db, TAB);
      createTableUses(db);
      SQLite.alterTableExecute(db, TAB, OID, UID, SQLite.datetimeToPosix(LOGIN), SQLite.datetimeToPosix(LOGOUT));
   }

   /* ============================================================================================================== */

   public static void login(@NonNull User user) {
      SQLite.insert(null, TAB, new Values().addLong(UID, user.getUid()));
   }

   /**
    * Returns the most recently inserted {@code Use} where {@code logout ISNULL}.
    *
    * @return the most recently inserted {@code Use} where {@code logout ISNULL}.
    */
   @Nullable
   public static Use getLoggedInNullable() {
      Use use = getLoggedIn();
      return use.values.notNull(OID) ? use : null;
   }

   /**
    * Returns the most recently inserted {@code Use} where {@code logout ISNULL}
    * or throws an Exception if there is no such row in the table.
    *
    * @return the most recently inserted {@code Use} where {@code logout ISNULL}.
    *
    * @throws RuntimeException
    *       if there is no such row in the table.
    */
   @NonNull
   public static Use getLoggedInNonNull() {
      Use use = getLoggedIn();
      if (use.values.notNull(OID)) {
         return use;
      } else {
         throw new RuntimeException("no user logged in");
      }
   }

   private static Use getLoggedIn() {
      Values columns = new Values(App.format("MAX(%1$s) AS %1$s", OID), UID, LOGIN, LOGOUT);
      return SQLite.get(Use.class, TAB, columns, null, null, LOGOUT + " ISNULL").get(0);
   }

   /* ============================================================================================================== */

   @NonNull
   @Override
   protected String getTable() { return TAB; }

   @NonNull
   public User getUser() {
      return User.getNonNull(values.getLong(UID));
   }

   /**
    * Updates the value of column {@code logout} to the current
    * <a href="https://en.wikipedia.org/wiki/Unix_time">POSIX time</a>.
    */
   public void logout() {
      setLong(LOGOUT, App.posixTime()).update();
   }

}