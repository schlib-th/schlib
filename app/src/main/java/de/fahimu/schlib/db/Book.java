/*
 * Book.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fahimu.android.db.Row;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Trigger;
import de.fahimu.android.db.Values;
import de.fahimu.schlib.anw.ISBN;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.app.App;

/**
 * A in-memory representation of one row of table {@code books}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class Book extends Row {

   private static final String IDS  = "bids";
   static final         String TAB  = "books";
   private static final String PREV = "prev_books";

   private static final String OID       = BaseColumns._ID;
   static final         String BID       = "bid";
   private static final String TITLE     = "title";
   private static final String PUBLISHER = "publisher";
   private static final String AUTHOR    = "author";
   private static final String KEYWORDS  = "keywords";
   private static final String STOCKED   = "stocked";
   private static final String SHELF     = "shelf";
   private static final String NUMBER    = "number";
   private static final String PERIOD    = "period";
   private static final String ISBN      = "isbn";
   static final         String LABEL     = "label";
   private static final String TSTAMP    = "tstamp";

   private static final Values TAB_COLUMNS = new Values().add(new String[] {
         SQLite.alias(TAB, OID, OID),
         BID, TITLE, PUBLISHER, AUTHOR, KEYWORDS, STOCKED, SHELF, NUMBER, PERIOD, ISBN, LABEL
   });

   /**
    * Creates new tables {@code bids}, {@code books} and {@code prev_books} in the specified database.
    * Also three triggers are created that will copy any changed row from {@code books} to the history table
    * {@code prev_books} after inserting, updating or deleting.
    *
    * @param db
    *       the database where the tables are created.
    */
   static void create(SQLiteDatabase db) {
      // CREATE TABLE bids
      Table ids = new Table(IDS, 3, OID, true);
      ids.create(db);
      // CREATE TABLE books
      Table tab = new Table(TAB, 9, OID, false);
      tab.addRefCol(BID, true).addUnique();
      tab.addColumn(TITLE, Table.TYPE_TEXT, true).addIndex().addCheckLength(">=", 1);
      tab.addColumn(PUBLISHER, Table.TYPE_TEXT, true).addIndex();
      tab.addColumn(AUTHOR, Table.TYPE_TEXT, true).addIndex();
      tab.addColumn(KEYWORDS, Table.TYPE_TEXT, true).addIndex();
      tab.addColumn(STOCKED, Table.TYPE_TIME, true).addIndex().addDefault("CURRENT_TIMESTAMP");
      tab.addColumn(SHELF, Table.TYPE_TEXT, true).addIndex().addCheckLength(">=", 1);
      tab.addColumn(NUMBER, Table.TYPE_INTE, true).addIndex().addCheckBetween(1, 999);
      tab.addColumn(PERIOD, Table.TYPE_INTE, true).addCheckBetween(1, 90);
      tab.addColumn(ISBN, Table.TYPE_INTE, false).addIndex().addCheckLength("=", 13);
      tab.addRefCol(LABEL, false).addUnique();
      tab.addConstraint("location_unique").addUnique(SHELF, NUMBER);
      tab.create(db);
      // CREATE TABLE prev_books
      Table prev = new Table(PREV, 9, OID, true);
      prev.addRefCol(BID, true).addIndex();
      prev.addColumn(TITLE, Table.TYPE_TEXT, true).addCheckLength(">=", 1);
      prev.addColumn(PUBLISHER, Table.TYPE_TEXT, true);
      prev.addColumn(AUTHOR, Table.TYPE_TEXT, true);
      prev.addColumn(KEYWORDS, Table.TYPE_TEXT, true);
      prev.addColumn(STOCKED, Table.TYPE_TIME, true);
      prev.addColumn(SHELF, Table.TYPE_TEXT, true).addCheckLength(">=", 1);
      prev.addColumn(NUMBER, Table.TYPE_INTE, true).addCheckBetween(1, 999);
      prev.addColumn(PERIOD, Table.TYPE_INTE, true).addCheckBetween(1, 90);
      prev.addColumn(ISBN, Table.TYPE_INTE, false).addCheckLength("=", 13);
      prev.addRefCol(LABEL, false);
      prev.addColumn(TSTAMP, Table.TYPE_TIME, true).addDefault("CURRENT_TIMESTAMP");
      prev.create(db);

      // CREATE TRIGGER AI_books
      Trigger i = new Trigger(TAB, Trigger.Type.AFTER_INSERT);
      i.addInsert(PREV, BID, TITLE, PUBLISHER, AUTHOR, KEYWORDS, STOCKED, SHELF, NUMBER, PERIOD, ISBN, LABEL);
      i.addValues("NEW." + BID, "NEW." + TITLE, "NEW." + PUBLISHER, "NEW." + AUTHOR, "NEW." + KEYWORDS,
            "NEW." + STOCKED, "NEW." + SHELF, "NEW." + NUMBER, "NEW." + PERIOD, "NEW." + ISBN, "NEW." + LABEL);
      i.create(db);
      // CREATE TRIGGER AU_books
      Trigger u = new Trigger(TAB, Trigger.Type.AFTER_UPDATE);
      u.addInsert(PREV, BID, TITLE, PUBLISHER, AUTHOR, KEYWORDS, STOCKED, SHELF, NUMBER, PERIOD, ISBN, LABEL);
      u.addValues("NEW." + BID, "NEW." + TITLE, "NEW." + PUBLISHER, "NEW." + AUTHOR, "NEW." + KEYWORDS,
            "NEW." + STOCKED, "NEW." + SHELF, "NEW." + NUMBER, "NEW." + PERIOD, "NEW." + ISBN, "NEW." + LABEL);
      u.create(db);
      // CREATE TRIGGER AD_books
      Trigger d = new Trigger(TAB, Trigger.Type.AFTER_DELETE);
      d.addInsert(PREV, BID, TITLE, PUBLISHER, AUTHOR, KEYWORDS, STOCKED, SHELF, NUMBER, PERIOD, ISBN, LABEL);
      d.addValues("OLD." + BID, "OLD." + TITLE, "OLD." + PUBLISHER, "OLD." + AUTHOR, "OLD." + KEYWORDS,
            "OLD." + STOCKED, "OLD." + SHELF, "OLD." + NUMBER, "OLD." + PERIOD, "OLD." + ISBN, "OLD." + LABEL);
      d.create(db);
   }

   /* ============================================================================================================== */

   @NonNull
   public static Book create(String shelf, int number, String title) {
      return new Book().setShelf(shelf).setNumber(number).setTitle(title).
            setPublisher("").setAuthor("").setKeywords("").setPeriod(14);
   }

   @NonNull
   public Book insert() {
      try (SQLite.Transaction transaction = new SQLite.Transaction()) {
         setNonNull(BID, SQLite.insert(IDS, new Values().add(OID)));
         super.insert();
         transaction.setSuccessful();
         return this;
      }
   }

   /* ============================================================================================================== */

   /**
    * Returns the {@code Book} specified by {@code where} and {@code args}
    * or {@code null} if there is no such {@code Book}.
    *
    * @param where
    *       a filter declaring which rows to return.
    * @param args
    *       the string values, which will replace the {@code '?'} characters in {@code where}.
    * @return the {@code Book} specified by {@code where} and {@code args} or {@code null}.
    */
   @Nullable
   private static Book get(String where, Object... args) {
      List<Book> list = SQLite.get(Book.class, TAB, TAB_COLUMNS, null, null, where, args);
      return (list.size() == 0) ? null : list.get(0);
   }

   @Nullable
   public static Book getNullable(long bid) {
      return Book.get(App.format("%s=?", BID), bid);
   }

   @NonNull
   public static Book getNonNull(long bid) {
      Book book = Book.getNullable(bid);
      if (book == null) { throw new RuntimeException("no book with bid " + bid); }
      return book;
   }

   @Nullable
   public static Book get(String shelf, int number) {
      return Book.get(App.format("%s=? AND %s=?", SHELF, NUMBER), shelf, number);
   }

   @Nullable
   public static Book getIdentifiedByISBN(long isbn) {
      return Book.get(App.format("%s=? AND %s ISNULL", ISBN, LABEL), isbn);
   }

   /**
    * Returns a {@link Map} that associates each shelf name from table {@code prev_books} with a unique id.
    * When ordered by id, the shelf names are ordered alphabetically ascending.
    *
    * @return a {@link Map} that associates each shelf name from table {@code prev_books} with a unique id.
    */
   @NonNull
   public static Map<String,Integer> getShelfMap() {
      // SELECT shelf FROM prev_books GROUP BY shelf ORDER BY shelf ;
      List<Book> shelfList = SQLite.get(Book.class, PREV, new Values().add(SHELF), SHELF, SHELF, null);
      int group = 0;
      Map<String,Integer> shelfMap = new HashMap<>(shelfList.size());
      for (Book book : shelfList) { shelfMap.put(book.getShelf(), group++); }
      return shelfMap;
   }

   public static int countNoScanId() {
      // SELECT COUNT(*) FROM books WHERE isbn ISNULL AND label ISNULL ;
      String where = App.format("%s ISNULL AND %s ISNULL", ISBN, LABEL);
      return Integer.parseInt(SQLite.getFromQuery(TAB, "COUNT(*)", "0", where));
   }

   @NonNull
   public static ArrayList<Book> getNoScanId() {
      // SELECT <TAB_COLUMNS> FROM books WHERE isbn ISNULL AND label ISNULL ORDER BY shelf, number ;
      String order = App.format("%s, %s", SHELF, NUMBER);
      String where = App.format("%s ISNULL AND %s ISNULL", ISBN, LABEL);
      return SQLite.get(Book.class, TAB, TAB_COLUMNS, null, order, where);
   }

   /**
    * Returns the books currently lent to the specified {@code user}.
    *
    * @param user
    *       the user.
    * @return the books currently lent to the specified {@code user}.
    */
   @NonNull
   public static ArrayList<Book> getLentTo(User user) {
      String table = App.format("%s JOIN %s USING (%s)", TAB, Lending.TAB, BID);
      String where = App.format("%s=? AND %s ISNULL", Lending.UID, Lending.RETURN);
      return SQLite.get(Book.class, table, TAB_COLUMNS, null, null, where, user.getUid());
   }

   /**
    * Returns a list of all books, ordered by {@code shelf} and {@code number}.
    *
    * @return a list of all books, ordered by {@code shelf} and {@code number}.
    */
   @NonNull
   public static ArrayList<Book> get() {
      // SELECT <TAB_COLUMNS> FROM books ORDER BY shelf, number ;
      String order = App.format("%s, %s", SHELF, NUMBER);
      return SQLite.get(Book.class, TAB, TAB_COLUMNS, null, order, null);
   }

   /* ============================================================================================================== */

   private Book() { super(); }

   /**
    * Creates a new {@code Book} that initially contains the column values from the specified {@code cursor}.
    *
    * @param cursor
    *       the cursor.
    */
   @SuppressWarnings ("unused")
   public Book(Cursor cursor) { super(cursor); }

   @Override
   protected String getTable() { return TAB; }

   /* ============================================================================================================== */

   public long getBid() {
      return values.getLong(BID);
   }

   @NonNull
   public String getShelf() {
      return values.getNonNull(SHELF);
   }

   public Book setShelf(@NonNull String shelf) {
      return (Book) setNonNull(SHELF, shelf);
   }

   public int getNumber() {
      return values.getInt(NUMBER);
   }

   public Book setNumber(int number) {
      return (Book) setNonNull(NUMBER, number);
   }

   @NonNull
   public String getTitle() {
      return values.getNonNull(TITLE);
   }

   public Book setTitle(@NonNull String title) {
      return (Book) setNonNull(TITLE, title);
   }

   @NonNull
   public String getPublisher() {
      return values.getNonNull(PUBLISHER);
   }

   public Book setPublisher(@NonNull String publisher) {
      return (Book) setNonNull(PUBLISHER, publisher);
   }

   @NonNull
   public String getAuthor() {
      return values.getNonNull(AUTHOR);
   }

   public Book setAuthor(@NonNull String author) {
      return (Book) setNonNull(AUTHOR, author);
   }

   @NonNull
   public String getKeywords() {
      return values.getNonNull(KEYWORDS);
   }

   public Book setKeywords(@NonNull String keywords) {
      return (Book) setNonNull(KEYWORDS, keywords);
   }

   @NonNull
   public String getStocked() {
      return values.getNonNull(STOCKED);
   }

   public Book setStocked(@NonNull String stocked) {
      return (Book) setNonNull(STOCKED, stocked);
   }

   public int getPeriod() {
      return values.getInt(PERIOD);
   }

   public Book setPeriod(int period) {
      return (Book) setNonNull(PERIOD, period);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public boolean hasISBN() {
      return values.getNullable(ISBN) != null;
   }

   public boolean hasLabel() {
      return values.getNullable(LABEL) != null;
   }

   public boolean hasNoScanId() {
      return !hasISBN() && !hasLabel();
   }

   /**
    * Returns the ISBN of this {@code Book}.
    * <p> Precondition: {@link #hasISBN()} must be {@code true}. </p>
    *
    * @return the ISBN of this {@code Book}.
    */
   @NonNull
   public ISBN getISBN() {
      return new ISBN(values.getLong(ISBN));
   }

   public Book setISBN(@Nullable ISBN isbn) {
      return (Book) setNullable(ISBN, (isbn == null) ? null : isbn.getValue());
   }

   /**
    * Returns the {@code Label} number of this {@code Book}.
    * <p> Precondition: {@link #hasLabel()} must be {@code true}. </p>
    *
    * @return the {@code Label} number of this {@code Book}.
    */
   public int getLabel() {
      return values.getInt(LABEL);
   }

   public Book setLabel(@Nullable Integer label) {
      return (Book) setNullable(LABEL, label);
   }

   public Book setLabel(@Nullable Label label) {
      return (Book) setNullable(LABEL, (label == null) ? null : label.getId());
   }

   /* ============================================================================================================== */

   public String getDisplayNumber() {
      return App.format("%03d", getNumber());
   }

   public String getDisplayShelfNumber() {
      return App.format("%s %03d", getShelf(), getNumber());
   }

   public String getDisplayISBN() {
      return !hasISBN() ? "" : getISBN().getDisplay();
   }

   public String getDisplayLabel() {
      return !hasLabel() ? "" : new SerialNumber(getLabel()).getDisplay();
   }

   public String getMultilineISBNLabel() {
      String isbn = getDisplayISBN(), label = getDisplayLabel();
      return isbn.isEmpty() ? label : label.isEmpty() ? isbn : isbn + '\n' + label;
   }

   public String getDisplay() {
      return App.format("\"%s\" (%s)", getTitle(), getDisplayShelfNumber());
   }

}