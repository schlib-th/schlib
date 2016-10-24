/*
 * Lending.java
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

import de.fahimu.android.app.Log;
import de.fahimu.android.db.Row;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Values;
import de.fahimu.schlib.app.App;

/**
 * A in-memory representation of one row of table {@code lendings}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class Lending extends Row {

   static final String TAB = "lendings";

   private static final String OID    = BaseColumns._ID;
   private static final String BID    = "bid";
   static final         String UID    = "uid";
   private static final String ISSUE  = "issue";
   static final         String RETURN = "return";

   private static final Values TAB_COLUMNS = new Values().add(new String[] {
         SQLite.alias(TAB, OID, OID), BID, UID, ISSUE, RETURN
   });

   static void create(SQLiteDatabase db) {
      Table tab = new Table(TAB, 6, OID, true);
      tab.addRefCol(BID, true).addIndex();
      tab.addRefCol(UID, true).addIndex();
      tab.addColumn(ISSUE, Table.TYPE_TIME, true).addDefault("CURRENT_TIMESTAMP");
      tab.addColumn(RETURN, Table.TYPE_TIME, false);
      tab.addConstraint("return_after_issue").addCheck("IFNULL(" + RETURN + ">=" + ISSUE + ", 1)");
      tab.create(db);
   }

   /* ============================================================================================================== */

   /**
    * Called if the book with the specified {@code bid} is issued to the user with the specified {@code uid}.
    * A new row with the specified {@code bid} and {@code uid} is inserted into the {@code lendings} table
    * and the value of column {@code issue} is set to the current time.
    *
    * @param bid
    *       the book id.
    * @param uid
    *       the user id.
    */
   public static void issueBook(long bid, long uid) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         SQLite.insert(TAB, new Values().add(BID, bid).add(UID, uid));
      }
   }

   /**
    * Returns the pending {@code Lending} or {@code null} if the book is currently not issued.
    *
    * @param bid
    *       the book id.
    * @return the pending {@code Lending} or {@code null} if the book is currently not issued.
    */
   public static Lending getPendingLending(long bid) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         String where = App.format("%s=? AND %s ISNULL", BID, RETURN);
         List<Lending> list = SQLite.get(Lending.class, TAB, TAB_COLUMNS, null, null, where, bid);
         return (list.size() == 0) ? null : list.get(0);
      }
   }

   /* ============================================================================================================== */

   /**
    * Creates a new {@code Lending} that initially contains the column values from the specified {@code cursor}.
    *
    * @param cursor
    *       the cursor.
    */
   @SuppressWarnings ("unused")
   public Lending(@NonNull Cursor cursor) { super(cursor); }

   @Override
   protected String getTable() { return TAB; }

   /* ============================================================================================================== */

   /**
    * Returns the book ID of this issue.
    *
    * @return the book ID of this issue.
    */
   public long getBid() {
      return values.getLong(BID);
   }

   /**
    * Returns the user ID of this issue.
    *
    * @return the user ID of this issue.
    */
   public long getUid() {
      return values.getLong(UID);
   }

   /**
    * Returns the date when the book was issued.
    *
    * @return the date when the book was issued.
    */
   @NonNull
   public String getIssue() {
      return values.getNonNull(ISSUE);
   }

   /**
    * Returns the date when the book was returned or {@code null} if the book is still issued.
    *
    * @return the date when the book was returned or {@code null} if the book is still issued.
    */
   @Nullable
   public String getReturn() {
      return values.getNullable(RETURN);
   }

   /**
    * Updates the value of column {@code return} to {@code DATETIME('NOW')} and
    * returns the number of days between {@code issue} and {@code return} as an {@code int}.
    *
    * @return the number of days between {@code issue} and {@code return} as an {@code int}.
    */
   public int returnBook() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         setNonNull(RETURN, SQLite.getDatetimeNow()).update();
         String days = SQLite.getFromRawQuery("SELECT JULIANDAY(?) - JULIANDAY(?)", getReturn(), getIssue());
         return (int) Double.parseDouble(days);     // days is formatted as a double from SQLITE
      }
   }

}