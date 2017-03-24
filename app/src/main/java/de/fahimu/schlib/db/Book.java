/*
 * Book.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.db;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.AutoCompleteTextView;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import de.fahimu.android.app.NumberPicker;
import de.fahimu.android.db.Row;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Trigger;
import de.fahimu.android.db.Values;
import de.fahimu.schlib.anw.ISBN;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.app.AdminUsersAddStep2;
import de.fahimu.schlib.app.App;

/**
 * A in-memory representation of one row of table {@code books}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class Book extends Row {

   static final private String IDS  = "bids";
   static final         String TAB  = "books";
   static final private String PREV = "prev_books";

   static final private String OID       = BaseColumns._ID;
   static final         String BID       = "bid";
   static final public  String TITLE     = "title";
   static final public  String PUBLISHER = "publisher";
   static final public  String AUTHOR    = "author";
   static final public  String KEYWORDS  = "keywords";
   static final private String STOCKED   = "stocked";
   static final public  String SHELF     = "shelf";
   static final private String NUMBER    = "number";
   static final private String PERIOD    = "period";
   static final private String ISBN      = "isbn";
   static final         String LABEL     = "label";
   static final private String TSTAMP    = "tstamp";

   // SELECT books._id AS _id, bid, title, publisher, author, keywords, stocked, shelf, number, period, isbn, label
   static final private Values TAB_COLUMNS = new Values().add(SQLite.alias(TAB, OID, OID),
         BID, TITLE, PUBLISHER, AUTHOR, KEYWORDS, STOCKED, SHELF, NUMBER, PERIOD, ISBN, LABEL);

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
   @Override
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
      ArrayList<Book> list = SQLite.get(Book.class, TAB, TAB_COLUMNS, null, null, where, args);
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
   public static Book getIdentifiedByISBN(ISBN isbn) {
      return Book.get(App.format("%s=? AND %s ISNULL", ISBN, LABEL), isbn.getValue());
   }

   public static int countNoScanId() {
      // SELECT COUNT(*) FROM books WHERE isbn ISNULL AND label ISNULL ;
      String where = App.format("%s ISNULL AND %s ISNULL", ISBN, LABEL);
      return Integer.parseInt(SQLite.getFromQuery(TAB, "COUNT(*)", "0", where));
   }

   /**
    * Returns the books currently lent to the specified {@code user}.
    *
    * @param user
    *       the user.
    * @return the books currently lent to the specified {@code user}.
    */
   @NonNull
   public static ArrayList<Book> getLentTo(@NonNull User user) {
      String table = App.format("%s JOIN %s USING (%s)", TAB, Lending.TAB, BID);
      String where = App.format("%s=? AND %s ISNULL", Lending.UID, Lending.RETURN);
      return SQLite.get(Book.class, table, TAB_COLUMNS, null, null, where, user.getUid());
   }

   /**
    * Returns a list of books where values are only assigned for column {@code _id}
    * and the specified {@code column}, grouped and ordered by the specified {@code column}.
    * This method will be called e. g. to populate lists in {@link AutoCompleteTextView}s.
    *
    * @param column
    *       the requested column.
    * @param history
    *       if false search the main table {@code books}, otherwise the history table {@code prev_books}.
    * @return a list of books grouped and ordered by the specified {@code column}.
    *
    * @throws IllegalArgumentException
    *       if the specified {@code column} is not one of
    *       {@link #TITLE}, {@link #PUBLISHER}, {@link #AUTHOR}, {@link #KEYWORDS} or {@link #SHELF}.
    */
   @NonNull
   public static ArrayList<Book> getColumnValues(@NonNull String column, boolean history) {
      if (!acceptedColumns.contains(column)) {
         throw new IllegalArgumentException(column + " not allowed");
      }
      Values columns = new Values().add(OID).add(column);
      return SQLite.get(Book.class, history ? PREV : TAB, columns, column, column, null);
   }

   private static HashSet<String> acceptedColumns =
         new HashSet<>(Arrays.asList(TITLE, PUBLISHER, AUTHOR, KEYWORDS, SHELF));

   /**
    * Returns a ascending ordered list of all used numbers of books on the specified {@code shelf}.
    * This method will be called by {@link AdminUsersAddStep2} to populate the {@link NumberPicker}.
    *
    * @param shelf
    *       the shelf to search for.
    * @return a ascending ordered list of all used numbers of books on the specified {@code shelf}.
    */
   @NonNull
   public static ArrayList<Integer> getNumbers(@NonNull String shelf) {
      // SELECT number FROM books WHERE shelf='$shelf' ORDER BY number ;
      String where = App.format("%s=?", SHELF);
      ArrayList<Book> books = SQLite.get(Book.class, TAB, new Values().add(NUMBER), null, NUMBER, where, shelf);

      ArrayList<Integer> numbers = new ArrayList<>(books.size());
      for (Book book : books) {
         numbers.add(book.getNumber());
      }
      return numbers;
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

   @NonNull
   @Override
   protected String getTable() { return TAB; }

   public long getBid() {
      return values.getLong(BID);
   }

   @NonNull
   public String getShelf() {
      return values.getNonNull(SHELF);
   }

   @NonNull
   public Book setShelf(@NonNull String shelf) {
      return (Book) setNonNull(SHELF, shelf);
   }

   public int getNumber() {
      return values.getInt(NUMBER);
   }

   @NonNull
   public Book setNumber(int number) {
      return (Book) setNonNull(NUMBER, number);
   }

   @NonNull
   public String getTitle() {
      return values.getNonNull(TITLE);
   }

   @NonNull
   public Book setTitle(@NonNull String title) {
      return (Book) setNonNull(TITLE, title);
   }

   @NonNull
   public String getPublisher() {
      return values.getNonNull(PUBLISHER);
   }

   @NonNull
   public Book setPublisher(@NonNull String publisher) {
      return (Book) setNonNull(PUBLISHER, publisher);
   }

   @NonNull
   public String getAuthor() {
      return values.getNonNull(AUTHOR);
   }

   @NonNull
   public Book setAuthor(@NonNull String author) {
      return (Book) setNonNull(AUTHOR, author);
   }

   @NonNull
   public String getKeywords() {
      return values.getNonNull(KEYWORDS);
   }

   @NonNull
   public Book setKeywords(@NonNull String keywords) {
      return (Book) setNonNull(KEYWORDS, keywords);
   }

   @NonNull
   public String getStocked() {
      return values.getNonNull(STOCKED);
   }

   @NonNull
   public Book setStocked(@NonNull String stocked) {
      return (Book) setNonNull(STOCKED, stocked);
   }

   public int getPeriod() {
      return values.getInt(PERIOD);
   }

   @NonNull
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

   @NonNull
   public Book setISBN(@Nullable ISBN isbn) {
      return (Book) setNullable(ISBN, (isbn == null) ? null : isbn.getValue());
   }

   /**
    * Returns the {@code Label} number of this {@code Book}.
    * <p> Precondition: {@link #hasLabel()} must be {@code true}. </p>
    *
    * @return the {@code Label} number of this {@code Book}.
    */
   @NonNull
   public Label getLabel() {
      return Label.getNonNull(values.getInt(LABEL));
   }

   @NonNull
   public Book setLabel(@Nullable Label label) {
      return (Book) setNullable(LABEL, (label == null) ? null : label.getId());
   }

   /* ============================================================================================================== */

   @NonNull
   public String getDisplayNumber() {
      return App.format("%03d", getNumber());
   }

   @NonNull
   public String getDisplayISBN() {
      return !hasISBN() ? "" : getISBN().getDisplay();
   }

   @NonNull
   public String getDisplayLabel() {
      return !hasLabel() ? "" : new SerialNumber(values.getInt(LABEL)).getDisplay();
   }

   @NonNull
   public String getDisplayMultilineISBNLabel() {
      String isbn = getDisplayISBN(), label = getDisplayLabel();
      return isbn.isEmpty() ? label : label.isEmpty() ? isbn : isbn + '\n' + label;
   }

   @NonNull
   public String getDisplay() {
      return App.format("\"%s\" (%s %03d)", getTitle(), getShelf(), getNumber());
   }

}