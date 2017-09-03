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
import de.fahimu.android.db.SQLite.Transaction;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Trigger;
import de.fahimu.android.db.Trigger.Type;
import de.fahimu.android.db.Values;
import de.fahimu.android.db.View;
import de.fahimu.schlib.app.R;

import static de.fahimu.android.db.SQLite.MIN_TSTAMP;
import static de.fahimu.android.db.Trigger.Type.AFTER_INSERT;
import static de.fahimu.android.db.Trigger.Type.AFTER_UPDATE;

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
   static final private String TAB_DUN  = "dunning_letters";
   static final private String VIEW_LOC = "lendings_loc";
   static final private String VIEW_DEL = "lendings_loc_delay";

   static final private String OID    = BaseColumns._ID;
   static final private String BID    = "bid";
   static final private String UID    = "uid";
   static final         String ISSUE  = "issue";
   static final         String RETURN = "return";

   static final private String LENDING = "lending";
   static final private String DUN     = "dun";

   static final private String COUNT = "count";
   static final private String TERM  = "term";
   static final private String DELAY = "delay";

   static final private int MIN_LENDING_TIME = 60;

   /* -------------------------------------------------------------------------------------------------------------- */

   static void create(SQLiteDatabase db) {
      createTableLendings(db);
      createTableOpeningDates(db);
      createTableDunningLetters(db);

      createViewLendingsLoc(db);
      createViewLendingsLocDelay(db);
      createTrigger(db, AFTER_INSERT, ISSUE);
      createTrigger(db, AFTER_UPDATE, RETURN);
   }

   static void upgrade(SQLiteDatabase db, int oldVersion) {
      Trigger.drop(db, TAB, AFTER_INSERT, AFTER_UPDATE);
      View.drop(db, VIEW_LOC, VIEW_DEL);

      if (oldVersion < 2) {
         createTableOpeningDates(db);     // new introduced with V2
         deleteShortTimeLendings(db);     // rows forbidden since V2 where return - issue < MIN_LENDING_TIME
      }
      if (oldVersion < 4) {
         createTableDunningLetters(db);   // new introduced with V4 as an extension of V2's column dun
         if (oldVersion >= 2) { migrateDunningLetters(db); }
         upgradeTableLendings(db, oldVersion);
      }
      createViewLendingsLoc(db);
      createViewLendingsLocDelay(db);
      createTrigger(db, AFTER_INSERT, ISSUE);
      createTrigger(db, AFTER_UPDATE, RETURN);
   }

   private static void createTableLendings(SQLiteDatabase db) {
      Table tab = new Table(TAB, 6, true);
      tab.addReferences(BID, true).addIndex();           // essential to find all lendings of a given book
      tab.addReferences(UID, true).addIndex();           // essential to find all lendings of a given user
      tab.addTimeColumn(ISSUE, true).addCheckPosixTime(MIN_TSTAMP).addDefaultPosixTime();
      tab.addTimeColumn(RETURN, false).addCheckPosixTime(MIN_TSTAMP);
      tab.addConstraint().addCheck(RETURN + "-" + ISSUE + ">=" + MIN_LENDING_TIME);
      tab.create(db);
   }

   /**
    * Table with local dates when the library was officially opened for pupils.
    */
   private static void createTableOpeningDates(SQLiteDatabase db) {
      new Table(TAB_OPD, 3, false).create(db);
   }

   private static void createTableDunningLetters(SQLiteDatabase db) {
      Table tab = new Table(TAB_DUN, 7, true);
      tab.addReferences(LENDING, true).addIndex();       // essential to find all dunning letters of a given lending
      tab.addTimeColumn(DUN, true).addCheckPosixTime(MIN_TSTAMP).addDefaultPosixTime();   // moved from lendings in V4
      tab.create(db);
   }

   /**
    * Select from {@code lendings} and {@code dunning_letters} with values of
    * {@code issue}, {@code return} and {@code dun} converted to localtime.
    * <p>
    * <pre> {@code
    * CREATE VIEW lendings_loc AS
    * SELECT _id, bid, uid, CAST(STRFTIME('%s',issue ,'unixepoch','localtime') AS INTEGER) AS issue,
    *                       CAST(STRFTIME('%s',return,'unixepoch','localtime') AS INTEGER) AS return,
    *                       CAST(STRFTIME('%s',dun   ,'unixepoch','localtime') AS INTEGER) AS dun, count
    * FROM lendings LEFT JOIN
    * (SELECT MAX(_id), COUNT(*) AS count, dun, lending AS _id FROM dunning_letters GROUP BY lending) USING(_id) ;
    * }
    * </pre>
    */
   private static void createViewLendingsLoc(SQLiteDatabase db) {
      View view = new View(VIEW_LOC);
      String issueLoc = SQLite.posixToLocal(ISSUE);
      String returnLoc = SQLite.posixToLocal(RETURN);
      String dunLoc = SQLite.posixToLocal(DUN);
      String query = App.format("SELECT MAX(%1$s), COUNT(*) AS %2$s, %3$s, %4$s AS %1$s FROM %5$s GROUP BY %4$s",
            OID, COUNT, DUN, LENDING, TAB_DUN);
      String table = App.format("%s LEFT JOIN (%s) USING (%s)", TAB, query, OID);
      view.addSelect(table, new Values(OID, BID, UID, issueLoc, returnLoc, dunLoc, COUNT), null, null, null);
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
         if (type == AFTER_UPDATE) {
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
    *    SELECT lendings_loc._id AS _id, bid, uid, issue, return, dun, count,
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
      Values columns = new Values(SQLite.alias(VIEW_LOC, OID), BID, UID, ISSUE, RETURN, DUN, COUNT, term, delay);

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

   private static void migrateDunningLetters(SQLiteDatabase db) {
      // INSERT INTO dunning_letters (lending, dun) SELECT _id, dun FROM lendings WHERE dun NOTNULL;
      SQLite.execSQL(db, App.format("INSERT INTO %1$s (%2$s, %3$s) SELECT %4$s, %3$s FROM %5$s WHERE %3$s NOTNULL;",
            TAB_DUN, LENDING, DUN, OID, TAB));
   }

   private static void upgradeTableLendings(SQLiteDatabase db, int oldVersion) {
      Table.dropIndex(db, TAB, BID, UID);
      SQLite.alterTablePrepare(db, TAB);
      createTableLendings(db);

      if (oldVersion < 2) {
         createViewLendingsLoc(db);
         createTrigger(db, AFTER_INSERT, ISSUE, RETURN);
      }
      String issueDate = oldVersion < 2 ? SQLite.datetimeToPosix(ISSUE) : ISSUE;
      String returnDate = oldVersion < 2 ? SQLite.datetimeToPosix(RETURN) : RETURN;
      SQLite.alterTableExecute(db, TAB, OID, BID, UID, issueDate, returnDate);

      if (oldVersion < 2) {
         Trigger.drop(db, TAB, AFTER_INSERT);
         View.drop(db, VIEW_LOC);
      }
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
      Values columns = new Values(OID, BID, UID, ISSUE, RETURN);
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
    * SELECT _id, bid, uid, issue, return, dun, count, term, delay
    * FROM lendings_loc_delay WHERE $where ORDER BY $order ;
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
      Values columns = new Values(OID, BID, UID, ISSUE, RETURN, DUN, COUNT, TERM, DELAY);
      return SQLite.get(Lending.class, VIEW_DEL, columns, null, order, where, args);
   }

   public static ArrayList<Lending> getByUserWithDelay(User user) {
      return getLocalizedLendingsWithDelay(OID + " DESC", UID + "=?", user.getUid());
   }

   public static ArrayList<Lending> getByBookWithDelay(Book book) {
      return getLocalizedLendingsWithDelay(OID + " DESC", BID + "=?", book.getBid());
   }

   public static ArrayList<Lending> getIssuedOnlyWithDelay() {
      return getLocalizedLendingsWithDelay(DELAY + " DESC, " + OID, RETURN + " ISNULL");
   }

   public static ArrayList<Lending> getByOidsWithDelay(List<Long> oids) {
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
    * DELETE FROM dunning_letters WHERE dun>=$beginOfDay ;
    * }
    * </pre>
    */
   public static void resetDunned() {
      SQLite.delete(null, TAB_DUN, DUN + ">=" + App.beginOfDay());
   }

   /**
    * For every {@code oid} in {@code oids} execute
    * <p>
    * <pre> {@code
    * INSERT INTO dunning_letters (lending, dun) VALUES ($oid, $posixTime) ;
    * }
    * </pre>
    */
   public static void setDunned(List<Long> oids) {
      long now = App.posixTime();
      try (Transaction transaction = new Transaction()) {
         for (long oid : oids) {
            SQLite.insert(null, TAB_DUN, new Values().addLong(LENDING, oid).addLong(DUN, now));
         }
         transaction.setSuccessful();
      }
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

   private long getIssue() {
      return values.getLong(ISSUE);
   }

   /**
    * Returns the date when the book was issued, formatted {@code DD.MM.YYYY} in the default timezone.
    *
    * @return the date when the book was issued, formatted {@code DD.MM.YYYY} in the default timezone.
    **/
   public String getIssueDate() {
      return App.formatDate(R.string.app_date, true, getIssue());
   }

   /**
    * Returns the time when the book was issued, formatted {@code HH:mm} in the default timezone.
    *
    * @return the time when the book was issued, formatted {@code HH:mm} in the default timezone.
    */
   public String getIssueTime() {
      return App.formatDate(R.string.app_time, true, getIssue());
   }

   private boolean isDunned() {
      return values.notNull(DUN);
   }

   /**
    * Returns the date when the book was dunned most recently, formatted {@code DD.MM.YYYY} in the default timezone.
    * <p> Precondition: {@link #isDunned()} ()} must be {@code true}. </p>
    *
    * @return the date when the book was dunned most recently, formatted {@code DD.MM.YYYY} in the default timezone.
    */
   private String getDunDate() {
      return App.formatDate(R.string.app_date, true, values.getLong(DUN));
   }

   /**
    * Returns how many times the book was dunned.
    * <p> Precondition: {@link #isDunned()} ()} must be {@code true}. </p>
    *
    * @return how many times the book was dunned.
    */
   public int getDunCount() {
      return values.getInt(COUNT);
   }

   private boolean isReturned() {
      return values.notNull(RETURN);
   }

   /**
    * Returns the date when the book was returned, formatted {@code DD.MM.YYYY} in the default timezone.
    * <p> Precondition: {@link #isReturned()} must be {@code true}. </p>
    *
    * @return the date when the book was returned, formatted {@code DD.MM.YYYY} in the default timezone.
    */
   private String getReturnDate() {
      return App.formatDate(R.string.app_date, true, values.getLong(RETURN));
   }

   @NonNull
   public Lending setCanceled(Long canceled) {
      return (Lending) (canceled == null ? setNull(RETURN) : setLong(RETURN, canceled));
   }

   public long getMinReturn() {
      return get(OID + "=?", getOid()).get(0).getIssue() + MIN_LENDING_TIME;
   }

   private boolean hasTerm() {
      return values.notNull(TERM);
   }

   /**
    * Returns the term of the lending, formatted {@code DD.MM.YYYY} in the default timezone.
    * <p>
    * Usually a book must be returned not later than the day of issue plus
    * the number of days the book can be lend at most (the period of the book).
    * The first day after this day when the library is official opened for pupils is called the term.
    * </p>
    * <p> Precondition: {@link #hasTerm()} must be {@code true}. </p>
    *
    * @return the term of the lending, formatted {@code DD.MM.YYYY} in the default timezone.
    */
   public String getTermDate() {
      return App.formatDate(R.string.app_date, true, values.getLong(TERM));
   }

   /**
    * Returns the difference in calendar days between return of the book and the term.
    * <p>
    * Usually a book must be returned not later than the day of issue plus
    * the number of days the book can be lend at most (the period of the book).
    * The first day after this day when the library is official opened for pupils is called the term.
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

   public boolean isDelayed() {
      return hasTerm() && getDelay() >= 2;
   }

   public boolean isDelayed(int minDelay) {
      if (minDelay < 2) {
         throw new IllegalArgumentException("minDelay=" + minDelay);
      }
      return hasTerm() && getDelay() >= minDelay;
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
   public String getDisplayPrevBook() {
      return Book.getByBidIncludeDeleted(values.getLong(BID)).getDisplay();
   }

   @NonNull
   public String getDisplayPrevUser() {
      return User.getByUidIncludeDeleted(values.getLong(UID)).getDisplay();
   }

   @NonNull
   public String getDisplayIssueReturnDelay() {
      if (isReturned()) {
         if (isDelayed()) {
            return App.getStr(R.string.lending_display_returned_delay, getDelay(), getIssueDate(), getReturnDate());
         } else {
            return App.getStr(R.string.lending_display_returned, getIssueDate(), getReturnDate());
         }
      } else {
         if (isDelayed()) {
            return App.getStr(R.string.lending_display_issued_delay, getDelay(), getTermDate(), getIssueDate());
         } else {
            return App.getStr(R.string.lending_display_issued, getIssueDate());
         }
      }
   }

   @NonNull
   public String getDisplayMultilineIssueDelayDun() {
      StringBuilder b = new StringBuilder(App.getStr(R.string.lending_display_issue, getIssueDate()));
      if (isDelayed()) {
         b.append('\n').append(App.getStr(R.string.lending_display_delay_n, getDelay(), getTermDate()));
         if (isDunned()) {
            b.append('\n').append(App.getStr(R.string.lending_display_dunning_letter, getDunCount(), getDunDate()));
         }
      } else if (isDunned()) {
         b.append('\n').append(App.getStr(R.string.lending_display_delay_0));
         b.append('\n').append(App.getStr(R.string.lending_display_reminder, getDunCount(), getDunDate()));
      }
      return b.toString();
   }

}