/*
 * Lending.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.db;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import java.util.ArrayList;

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

   static final         String TAB    = "lendings";
   static final private String OID    = BaseColumns._ID;
   static final private String BID    = "bid";
   static final         String UID    = "uid";
   static final private String ISSUE  = "issue";
   static final         String RETURN = "return";

   // SELECT lendings._id AS _id, bid, uid, issue, return
   static final private Values TAB_COLUMNS = new Values().add(SQLite.alias(TAB, OID, OID), BID, UID, ISSUE, RETURN);

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
      SQLite.insert(TAB, new Values().add(BID, bid).add(UID, uid));
   }

   private static String whereClause(String column, boolean issuedOnly) {
      return issuedOnly ? App.format("%s=? AND %s ISNULL", column, RETURN) : App.format("%s=?", column);
   }

   /**
    * Returns the {@link Lending}s of the specified book.
    *
    * @param book
    *       the book.
    * @param issuedOnly
    *       if {@code true}, the result list will either be empty (book is returned)
    *       or have exactly one element (book is currently issued).
    * @return the {@link Lending}s of the specified book.
    */
   public static ArrayList<Lending> getLendings(Book book, boolean issuedOnly) {
      return SQLite.get(Lending.class, TAB, TAB_COLUMNS, null, OID, whereClause(BID, issuedOnly), book.getBid());
   }

   /**
    * Returns the {@link Lending}s of the specified user.
    *
    * @param user
    *       the user.
    * @param issuedOnly
    *       if {@code true}, only lendings will be returned where the books are currently issued.
    * @return the {@link Lending}s of the specified user.
    */
   public static ArrayList<Lending> getLendings(User user, boolean issuedOnly) {
      return SQLite.get(Lending.class, TAB, TAB_COLUMNS, null, OID, whereClause(UID, issuedOnly), user.getUid());
   }

   /* ============================================================================================================== */

   @NonNull
   @Override
   protected String getTable() { return TAB; }

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
      setNonNull(RETURN, SQLite.getDatetimeNow()).update();
      String days = SQLite.getFromRawQuery("SELECT JULIANDAY(?) - JULIANDAY(?)", getReturn(), getIssue());
      return (int) Double.parseDouble(days);     // days is formatted as a double from SQLITE
   }

}