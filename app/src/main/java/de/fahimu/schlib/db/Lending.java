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

import de.fahimu.android.app.App;
import de.fahimu.android.db.Row;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Values;

/**
 * A in-memory representation of one row of table {@code lendings}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class Lending extends Row {

   static final private String TAB    = "lendings";
   static final private String OID    = BaseColumns._ID;
   static final private String BID    = "bid";
   static final private String UID    = "uid";
   static final private String ISSUE  = "issue";
   static final private String RETURN = "return";

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
    * Called if the specified {@code book} is issued to the specified {@code user}.
    * A new row with the {@code bid} of the book and the {@code uid} of the user is inserted
    * into the {@code lendings} table and the value of column {@code issue} is set to the current time.
    *
    * @param book
    *       the book.
    * @param user
    *       the user.
    */
   public static void issueBook(Book book, User user) {
      SQLite.insert(TAB, new Values().add(BID, book.getBid()).add(UID, user.getUid()));
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
    * Returns the book of this issue.
    *
    * @return the book of this issue.
    */
   @NonNull
   public Book getBook() {
      return Book.getNonNull(values.getLong(BID));
   }

   /**
    * Returns the user of this issue.
    *
    * @return the user of this issue.
    */
   @NonNull
   public User getUser() {
      return User.getNonNull(values.getLong(UID));
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
    * returns the number of days the book was returned belated as an {@code int}.
    *
    * @param period
    *       maximum number of days the user is permitted to lend this book.
    * @return the number of days the book was returned belated as an {@code int}.
    */
   public int returnBook(int period) {
      setNonNull(RETURN, SQLite.getDatetimeNow()).update();

      // SELECT JULIANDAY(DATE(<RETURN>)) - JULIANDAY(IFNULL(DATE(MIN(action)), DATE(<ISSUE>, '+<PERIOD> days')))
      //    FROM (
      //       SELECT MIN(issue)  AS action FROM lendings WHERE issue  >= DATE(<ISSUE>, '+<PERIOD> days')
      //    UNION ALL
      //       SELECT MIN(return) AS action FROM lendings WHERE return >= DATE(<ISSUE>, '+<PERIOD> days')
      //    ) ;
      String issuePlusPeriod = App.format("DATE(?, '+%d days')", period);
      String subquery = "SELECT MIN(%2$s) AS action FROM %1$s WHERE %2$s >= %3$s";
      String subquery1 = App.format(subquery, TAB, ISSUE, issuePlusPeriod);
      String subquery2 = App.format(subquery, TAB, RETURN, issuePlusPeriod);
      String query = App.format("SELECT JULIANDAY(DATE(?)) - JULIANDAY(IFNULL(DATE(MIN(action)), %s))" +
            " FROM (%s UNION ALL %s)", issuePlusPeriod, subquery1, subquery2);
      String belated = SQLite.getFromRawQuery(query, getReturn(), getIssue(), getIssue(), getIssue());
      return (int) Double.parseDouble(belated);     // belated is formatted as a double from SQLITE
   }

}