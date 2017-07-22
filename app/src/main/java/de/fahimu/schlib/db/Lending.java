/*
 * Lending.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.db;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;


import java.util.ArrayList;
import java.util.List;

import de.fahimu.android.app.App;
import de.fahimu.android.db.Row;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Trigger;
import de.fahimu.android.db.Trigger.Type;
import de.fahimu.android.db.Values;
import de.fahimu.android.db.View;
import de.fahimu.schlib.app.R;

import static de.fahimu.android.db.SQLite.MIN_TSTAMP;

/**
 * A in-memory representation of one row of table {@code lendings}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class Lending extends Row {

   static final         String TAB      = "lendings";
   static final private String TAB_OPD  = "opening_dates";
   static final private String VIEW_LOC = "lendings_loc";
   static final private String VIEW_DEL = "lendings_loc_delay";

   static final private String OID    = BaseColumns._ID;
   static final private String BID    = "bid";
   static final private String UID    = "uid";
   static final         String ISSUE  = "issue";
   static final private String DUN    = "dun";
   static final         String RETURN = "return";
   static final private String TERM   = "term";
   static final private String DELAY  = "delay";

   static final private int MIN_LENDING_TIME = 60;

   /* -------------------------------------------------------------------------------------------------------------- */

   static void create(SQLiteDatabase db) {
      createTableOpeningDates(db);
      createTableLendings(db);

      createViewLendingsLoc(db);
      createTrigger(db, Type.AFTER_INSERT, ISSUE);
      createTrigger(db, Type.AFTER_UPDATE, RETURN);
      createViewLendingsLocDelay(db);
   }

   static void upgrade(SQLiteDatabase db, int oldVersion) {
      if (oldVersion < 2) {
         createTableOpeningDates(db);     // new introduced with V2

         deleteShortTimeLendings(db);     // rows forbidden since V2 where return - issue < MIN_LENDING_TIME
         upgradeTableLendingsV2(db);

         createViewLendingsLoc(db);
         createTrigger(db, Type.AFTER_INSERT, ISSUE);
         createTrigger(db, Type.AFTER_UPDATE, RETURN);
         createViewLendingsLocDelay(db);
      }
   }

   /**
    * Table with local dates when the library was officially opened for pupils.
    */
   private static void createTableOpeningDates(SQLiteDatabase db) {
      new Table(TAB_OPD, 3, false).create(db);
   }

   private static void createTableLendings(SQLiteDatabase db) {
      Table tab = new Table(TAB, 6, true);
      tab.addReferences(BID, true).addIndex();           // essential to find all lendings of a given book
      tab.addReferences(UID, true).addIndex();           // essential to find all lendings of a given user
      tab.addTimeColumn(ISSUE, true).addCheckPosixTime(MIN_TSTAMP).addDefaultPosixTime();
      tab.addTimeColumn(DUN, false).addCheckPosixTime(MIN_TSTAMP);                    // new in version V2
      tab.addTimeColumn(RETURN, false).addCheckPosixTime(MIN_TSTAMP);
      tab.addConstraint().addCheck(DUN + " BETWEEN " + ISSUE + " AND " + RETURN);
      tab.addConstraint().addCheck(RETURN + "-" + ISSUE + ">=" + MIN_LENDING_TIME);
      tab.create(db);
   }

   /**
    * Select from {@code lendings} with values of
    * {@code issue}, {@code dun} and {@code return} converted to localtime.
    * <p>
    * <pre> {@code
    * CREATE VIEW lendings_loc AS
    * SELECT _id, bid, uid, CAST(STRFTIME('%s',issue ,'unixepoch','localtime') AS INTEGER) AS issue,
    *                       CAST(STRFTIME('%s',dun   ,'unixepoch','localtime') AS INTEGER) AS dun,
    *                       CAST(STRFTIME('%s',return,'unixepoch','localtime') AS INTEGER) AS return
    * FROM lendings ;
    * }
    * </pre>
    */
   private static void createViewLendingsLoc(SQLiteDatabase db) {
      View view = new View(VIEW_LOC);
      String issueLoc = SQLite.posixToLocal(ISSUE);
      String dunLoc = SQLite.posixToLocal(DUN);
      String returnLoc = SQLite.posixToLocal(RETURN);
      view.addSelect(TAB, new Values(OID, BID, UID, issueLoc, dunLoc, returnLoc), null, null, null);
      view.create(db);
   }

   /**
    * Creates a trigger of the specified type that inserts into table opening_dates after lendings changed.
    * <p>
    * <pre> {@code
    * INSERT OR IGNORE INTO opening_dates
    * SELECT column/86400 FROM lendings_loc
    *                     JOIN users USING (uid)
    *                     JOIN opened ON ((column/86400+4)%7=dw AND column%86400 BETWEEN s1 AND s2)
    * WHERE role='pupil' AND lendings_loc._id=NEW._id AND OLD.column ISNULL;
    * }
    * </pre>
    */
   private static void createTrigger(SQLiteDatabase db, Type type, String... columns) {
      Trigger trigger = new Trigger(TAB, type);
      for (String column : columns) {
         String table = App.format("%s JOIN %s USING (%s) %s", VIEW_LOC, User.TAB, UID, Preference.joinOpened(column));
         String where = App.format("%s='%s' AND %s.%s=NEW.%s", User.ROLE, User.PUPIL, VIEW_LOC, OID, OID);
         if (type == Type.AFTER_UPDATE) {
            where = where + " AND OLD." + column + " ISNULL";
         }
         trigger.addInsertOrIgnoreSelected(TAB_OPD, column + "/86400", table, where);
      }
      trigger.create(db);
   }

   /**
    * Select from {@code lendings_loc} and with extra columns {@code term} and {@code delay}.
    * <p>
    * <pre> {@code
    * CREATE VIEW lendings_loc_delay AS
    *    SELECT lendings_loc._id AS _id, bid, uid, issue, dun, return,
    *           MIN(opening_dates._id)*86400 AS term,
    *           IFNULL(return,CAST(STRFTIME('%s','now','localtime') AS INTEGER))/86400
    *         - IFNULL(MIN(opening_dates._id),issue/86400+period) AS delay
    *    FROM lendings_loc JOIN books USING (bid)
    *         LEFT JOIN opening_dates ON opening_dates._id>=issue/86400+period
    *    GROUP BY lendings_loc._id
    *    ORDER BY lendings_loc._id ;
    * }
    * </pre>
    */
   private static void createViewLendingsLocDelay(SQLiteDatabase db) {
      View view = new View(VIEW_DEL);
      String opdOid = TAB_OPD + "." + OID;
      String locOid = VIEW_LOC + "." + OID;

      String term = App.format("MIN(%s)*86400 AS %s", opdOid, TERM);
      String min = App.format("IFNULL(%s,CAST(STRFTIME('%%s','now','localtime') AS INTEGER))/86400", RETURN);
      String sub = App.format("IFNULL(MIN(%s),%s/86400+%s)", opdOid, ISSUE, Book.PERIOD);
      String delay = App.format("%s - %s AS %s", min, sub, DELAY);
      Values columns = new Values(SQLite.alias(VIEW_LOC, OID), BID, UID, ISSUE, DUN, RETURN, term, delay);

      String table = App.format("%s JOIN %s USING (%s) LEFT JOIN %s ON %s>=%s/86400+%s",
            VIEW_LOC, Book.TAB, BID, TAB_OPD, opdOid, ISSUE, Book.PERIOD);
      view.addSelect(table, columns, locOid, locOid, null);
      view.create(db);
   }

   private static void deleteShortTimeLendings(SQLiteDatabase db) {
      // DELETE FROM lendings
      //    WHERE CAST(STRFTIME('%s',return) AS INTEGER) - CAST(STRFTIME('%s',issue) AS INTEGER) < MIN_LENDING_TIME;
      String cast = "CAST(STRFTIME('%%s',%s) AS INTEGER)";
      SQLite.delete(db, TAB, App.format(cast, RETURN) + " - " + App.format(cast, ISSUE) + " < " + MIN_LENDING_TIME);
   }

   private static void upgradeTableLendingsV2(SQLiteDatabase db) {
      // Rebuild table lendings to contain integers for issue and return instead of text strings
      Table.dropIndex(db, TAB, BID, UID);
      SQLite.alterTablePrepare(db, TAB);
      createTableLendings(db);

      createViewLendingsLoc(db);
      createTrigger(db, Type.AFTER_INSERT, ISSUE, RETURN);

      SQLite.alterTableExecute(db, TAB, OID, BID, UID,
            SQLite.datetimeToPosix(ISSUE), "NULL", SQLite.datetimeToPosix(RETURN));

      Trigger.drop(db, TAB, Type.AFTER_INSERT);
      View.drop(db, VIEW_LOC);
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
      SQLite.insert(null, TAB, new Values().addLong(BID, book.getBid()).addLong(UID, user.getUid()));
   }

   private static String whereClause(String column, boolean issuedOnly) {
      return issuedOnly ? App.format("%s=? AND %s ISNULL", column, RETURN) : App.format("%s=?", column);
   }

   private static ArrayList<Lending> get(String where, Object... args) {
      Values columns = new Values(OID, BID, UID, ISSUE, DUN, RETURN);
      return SQLite.get(Lending.class, TAB, columns, null, OID, where, args);
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
   public static ArrayList<Lending> getByBook(Book book, boolean issuedOnly) {
      return get(whereClause(BID, issuedOnly), book.getBid());
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
   public static ArrayList<Lending> getByUser(User user, boolean issuedOnly) {
      return get(whereClause(UID, issuedOnly), user.getUid());
   }

   /**
    * Returns the {@link Lending}s from {@code lendings_loc_delay} as specified by {@code where} and {@code args}.
    * <p>
    * <pre> {@code
    * SELECT _id, bid, uid, issue, return, term, delay FROM lendings_loc_delay WHERE $where ORDER BY $order ;
    * }
    * </pre>
    *
    * @param order
    *       a {@code String} specifying how to sort the result set or {@code null}.
    * @param where
    *       a filter declaring which rows to return.
    * @param args
    *       the values, which will replace the {@code '?'} characters in {@code where}.
    * @return the {@link Lending}s
    */
   private static ArrayList<Lending> getLocalizedLendingsWithDelay(String order, String where, Object... args) {
      Values columns = new Values(OID, BID, UID, ISSUE, DUN, RETURN, TERM, DELAY);
      return SQLite.get(Lending.class, VIEW_DEL, columns, null, order, where, args);
   }

   public static ArrayList<Lending> getIssuedOnly() {
      return getLocalizedLendingsWithDelay(DELAY + " DESC, " + OID, RETURN + " ISNULL");
   }

   public static ArrayList<Lending> getByOids(List<Long> oids) {
      return getLocalizedLendingsWithDelay(DELAY + " DESC, " + OID, buildWhereClauseOidInList(VIEW_DEL, oids));
   }

   /**
    * Returns
    * <pre> {@code
    * WHERE $table._id IN (oid0,oid1, ... ,oidN)
    * }
    * </pre>
    */
   private static String buildWhereClauseOidInList(String table, List<Long> oids) {
      StringBuilder b = new StringBuilder(50);
      b.append(table).append('.').append(OID).append(" IN (").append(oids.get(0));
      for (int i = 1; i < oids.size(); i++) {
         b.append(',').append(oids.get(i));
      }
      return b.append(")").toString();
   }

   /**
    * <p>
    * <pre> {@code
    * UPDATE lendings SET dun=NULL WHERE dun>=$beginOfDay ;
    * }
    * </pre>
    */
   public static void resetDunned() {
      SQLite.update(TAB, new Values(DUN), DUN + ">=" + App.beginOfDay());
   }

   /**
    * <p>
    * <pre> {@code
    * UPDATE lendings SET dun=$posixTime WHERE lendings_loc_delay._id IN (oid0,oid1, ... ,oidN)
    * }
    * </pre>
    */
   public static void setDunned(List<Long> oids) {
      SQLite.update(TAB, new Values().addLong(DUN, App.posixTime()), buildWhereClauseOidInList(TAB, oids));
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
      return book != null ? book : (book = Book.getNonNull(values.getLong(BID)));
   }

   private Book book;

   /**
    * Returns the user of this issue.
    *
    * @return the user of this issue.
    */
   @NonNull
   public User getUser() {
      return user != null ? user : (user = User.getNonNull(values.getLong(UID)));
   }

   private User user;

   /**
    * Returns the POSIX time when the book was issued.
    *
    * @return the POSIX time when the book was issued.
    *
    * @see <a href="https://en.wikipedia.org/wiki/Unix_time">POSIX time</a>
    */
   private long getIssue() {
      return values.getLong(ISSUE);
   }

   public String getIssueDate() {
      return App.formatDate(R.string.app_date, true, getIssue());
   }

   public String getIssueTime() {
      return App.formatDate(R.string.app_time, true, getIssue());
   }

   private boolean isDunned() {
      return values.notNull(DUN);
   }

   /**
    * Returns the POSIX time when the book was dunned.
    * <p> Precondition: {@link #isDunned()} ()} must be {@code true}. </p>
    *
    * @return the POSIX time when the book was dunned.
    *
    * @see <a href="https://en.wikipedia.org/wiki/Unix_time">POSIX time</a>
    */
   private long getDun() {
      return values.getLong(DUN);
   }

   private String getDunDate() {
      return App.formatDate(R.string.app_date, true, getDun());
   }

   private boolean isReturned() {
      return values.notNull(RETURN);
   }

   /**
    * Returns the POSIX time when the book was returned.
    * <p> Precondition: {@link #isReturned()} must be {@code true}. </p>
    *
    * @return the POSIX time when the book was returned.
    *
    * @see <a href="https://en.wikipedia.org/wiki/Unix_time">POSIX time</a>
    */
   private long getReturn() {
      return values.getLong(RETURN);
   }

   @NonNull
   public Lending setCanceled(Long canceled) {
      return (Lending) (canceled == null ? setNull(RETURN) : setLong(RETURN, canceled));
   }

   public long getMinReturn() {
      return get(OID + "=?", getOid()).get(0).getIssue() + MIN_LENDING_TIME;
   }

   public boolean hasTerm() {
      return values.notNull(TERM);
   }

   /**
    * Returns the term of the lending.
    * <p>
    * Usually a book must be returned not later than the day of issue plus
    * the number of days the book can be lend at most (the period of the book).
    * The first day from this day when the library is official opened for pupils is called the term.
    * </p>
    * <p> Precondition: {@link #hasTerm()} must be {@code true}. </p>
    *
    * @return the term of the lending.
    */
   private long getTerm() {
      return values.getLong(TERM);
   }

   public String getTermDate() {
      return App.formatDate(R.string.app_date, true, getTerm());
   }

   /**
    * Returns the difference in calendar days between return of the book and the term.
    * <p>
    * Usually a book must be returned not later than the day of issue plus
    * the number of days the book can be lend at most (the period of the book).
    * The first day from this day when the library is official opened for pupils is called the term.
    * If there is no term, then the day of issue plus the book's period will be used for calculation instead.
    * </p><p>
    * If the book is not returned yet, the current day will be used for calculation instead.
    * </p>
    *
    * @return the difference in calendar days between return of the book and the term.
    */
   public int getDelay() {
      return values.getInt(DELAY);
   }

   /**
    * Updates the value of column {@code return} to the current posix time and
    * returns the number of days the book was returned delayed as an {@code int}.
    * <p>
    * If the lending time is less than MIN_LENDING_TIME seconds,
    * we assume this lending to be erroneous and we'll delete this row and return 0.
    * </p>
    *
    * @return the number of days the book was returned delayed as an {@code int}.
    *
    * @see <a href="https://en.wikipedia.org/wiki/Unix_time">POSIX time</a>
    */
   public int returnBook() {
      long now = App.posixTime();
      if (now - getIssue() < MIN_LENDING_TIME) {
         delete();
         return 0;
      } else {
         setLong(RETURN, now).update();
         return getLocalizedLendingsWithDelay(null, OID + "=?", getOid()).get(0).getDelay();
      }
   }

   /* ============================================================================================================== */

   @NonNull
   public String getDisplayMultilineIssueDelayDun() {
      StringBuilder b = new StringBuilder(App.getStr(R.string.lending_display_issue, getIssueDate()));
      if (hasTerm() && getDelay() >= 2) {
         b.append('\n').append(App.getStr(R.string.lending_display_delay_n, getDelay(), getTermDate()));
      } else if (isDunned()) {
         b.append('\n').append(App.getStr(R.string.lending_display_delay_0));
      }
      if (isDunned()) {
         b.append('\n').append(App.getStr(R.string.lending_display_dun, getDunDate()));
      }
      return b.toString();
   }

}