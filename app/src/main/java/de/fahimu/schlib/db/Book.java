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
import java.util.GregorianCalendar;
import java.util.List;

import de.fahimu.android.app.App;
import de.fahimu.android.app.NumberPicker;
import de.fahimu.android.db.Row;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Trigger;
import de.fahimu.android.db.Trigger.Type;
import de.fahimu.android.db.Values;
import de.fahimu.android.db.View;
import de.fahimu.schlib.anw.ISBN;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.app.AdminUsersAddStep2;
import de.fahimu.schlib.app.FirstRun3Activity;

import static de.fahimu.android.db.SQLite.MIN_TSTAMP;
import static de.fahimu.schlib.anw.ISBN.MAX;
import static de.fahimu.schlib.anw.ISBN.MIN;
import static de.fahimu.schlib.db.Preference.FIRST_RUN;
import static de.fahimu.schlib.db.Preference.KEY;

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
   static final private String VIEW = "prev_books_newest";

   static final private String OID       = BaseColumns._ID;
   static final         String BID       = "bid";
   static final public  String TITLE     = "title";
   static final public  String PUBLISHER = "publisher";
   static final public  String AUTHOR    = "author";
   static final public  String KEYWORDS  = "keywords";
   static final private String STOCKED   = "stocked";
   static final private String SHELF     = "shelf";
   static final private String NUMBER    = "number";
   static final         String PERIOD    = "period";
   static final private String ISBN      = "isbn";
   static final         String LABEL     = "label";
   static final private String VANISHED  = "vanished";
   static final private String TSTAMP    = "tstamp";

   static final private Values TAB_COLUMNS = new Values(SQLite.alias(TAB, OID),
         BID, TITLE, PUBLISHER, AUTHOR, KEYWORDS, STOCKED, SHELF, NUMBER, PERIOD, ISBN, LABEL, VANISHED);

   /* -------------------------------------------------------------------------------------------------------------- */

   static void create(SQLiteDatabase db) {
      createTableBids(db);
      createTableBooks(db);
      createTablePrevBooks(db);

      createTrigger(db, Type.AFTER_INSERT);
      createTrigger(db, Type.AFTER_UPDATE);
      createTrigger(db, Type.AFTER_DELETE);

      createViewPrevBooksNewest(db);
   }

   static void upgrade(SQLiteDatabase db, int oldVersion) {
      if (oldVersion < 2) {
         Trigger.drop(db, TAB, Type.AFTER_INSERT);
         Trigger.drop(db, TAB, Type.AFTER_UPDATE);
         Trigger.drop(db, TAB, Type.AFTER_DELETE);

         upgradeTableBooksV2(db);
         deleteTemporaryRowsFromPrevBooks(db);
         upgradeTablePrevBooksV2(db);

         createTrigger(db, Type.AFTER_INSERT);
         createTrigger(db, Type.AFTER_UPDATE);
         createTrigger(db, Type.AFTER_DELETE);

         createViewPrevBooksNewest(db);
      }
   }

   private static void createTableBids(SQLiteDatabase db) {
      new Table(IDS, 3, true).create(db);
   }

   private static void createTableBooks(SQLiteDatabase db) {
      Table tab = new Table(TAB, 9, false);
      tab.addReferences(BID, true).addUnique();
      tab.addTextColumn(TITLE, true).addCheckLength(">=1");
      tab.addTextColumn(PUBLISHER, true);
      tab.addTextColumn(AUTHOR, true);
      tab.addTextColumn(KEYWORDS, true);
      tab.addTimeColumn(STOCKED, true).addCheckPosixTime(0).addDefaultPosixTime();
      tab.addTextColumn(SHELF, true).addCheckLength(">=1");
      tab.addLongColumn(NUMBER, true).addCheckBetween(1, 999);
      tab.addLongColumn(PERIOD, true).addCheckBetween(1, 90);
      tab.addLongColumn(ISBN, false).addIndex().addCheckBetween(MIN, MAX);
      tab.addReferences(LABEL, false).addUnique();
      tab.addTimeColumn(VANISHED, false).addCheckPosixTime(MIN_TSTAMP);
      tab.addConstraint().addUnique(SHELF, NUMBER);
      tab.create(db);
   }

   private static void createTablePrevBooks(SQLiteDatabase db) {
      Table prev = new Table(PREV, 9, true);
      prev.addReferences(BID, true).addIndex();          // index essential to group rows by bid
      prev.addTextColumn(TITLE, true).addCheckLength(">=1");
      prev.addTextColumn(PUBLISHER, true);
      prev.addTextColumn(AUTHOR, true);
      prev.addTextColumn(KEYWORDS, true);
      prev.addTimeColumn(STOCKED, true).addCheckPosixTime(0);
      prev.addTextColumn(SHELF, true).addCheckLength(">=1");
      prev.addLongColumn(NUMBER, true).addCheckBetween(1, 999);
      prev.addLongColumn(PERIOD, true).addCheckBetween(1, 90);
      prev.addLongColumn(ISBN, false).addCheckBetween(MIN, MAX);
      prev.addReferences(LABEL, false);
      prev.addTimeColumn(VANISHED, false).addCheckPosixTime(MIN_TSTAMP);
      prev.addTimeColumn(TSTAMP, true).addCheckPosixTime(MIN_TSTAMP).addDefaultPosixTime();
      prev.create(db);
   }

   private static void createTrigger(SQLiteDatabase db, Trigger.Type type) {
      Trigger.create(db, TAB, type, PREV,
            BID, TITLE, PUBLISHER, AUTHOR, KEYWORDS, STOCKED, SHELF, NUMBER, PERIOD, ISBN, LABEL, VANISHED);
   }

   /**
    * Select the newest row of every book in prev_books (therefore including deleted books).
    * <p>
    * <pre> {@code
    * CREATE VIEW prev_books_newest AS
    *    SELECT MAX(_id) AS _id, title, publisher, author, keywords, isbn FROM prev_books GROUP BY bid ;
    * }
    * </pre>
    */
   private static void createViewPrevBooksNewest(SQLiteDatabase db) {
      View view = new View(VIEW);
      String oid = App.format("MAX(%1$s) AS %1$s", OID);
      view.addSelect(PREV, new Values(oid, TITLE, PUBLISHER, AUTHOR, KEYWORDS, ISBN), BID, null, null);
      view.create(db);
   }

   private static void upgradeTableBooksV2(SQLiteDatabase db) {
      Table.dropIndex(db, TAB, TITLE, PUBLISHER, AUTHOR, KEYWORDS, STOCKED, SHELF, NUMBER, ISBN);
      SQLite.alterTablePrepare(db, TAB);
      createTableBooks(db);
      String stocked = SQLite.datetimeToPosix(STOCKED);
      SQLite.alterTableExecute(db, TAB,
            OID, BID, TITLE, PUBLISHER, AUTHOR, KEYWORDS, stocked, SHELF, NUMBER, PERIOD, ISBN, LABEL, "NULL");
   }

   /**
    * Deletes all rows from table {@code prev_books} where {@code isbn ISNULL AND label ISNULL}.
    * Since database version 2, this is done after the entry 'first_run' is deleted from table preferences.
    * <p>
    * <pre> {@code
    * DELETE FROM prev_books WHERE _id IN (
    *    SELECT prev_books._id
    *    FROM prev_books LEFT JOIN preferences ON key='first_run'
    *    WHERE key ISNULL AND isbn ISNULL AND label ISNULL
    * );
    * }
    * </pre>
    */
   public static void deleteTemporaryRowsFromPrevBooks(@Nullable SQLiteDatabase db) {
      String table = App.format("%s LEFT JOIN %s ON %s='%s'", PREV, Preference.TAB, KEY, FIRST_RUN);
      String where = App.format("%s ISNULL AND %s ISNULL AND %s ISNULL", KEY, ISBN, LABEL);
      String query = App.format("(SELECT %s.%s FROM %s WHERE %s)", PREV, OID, table, where);
      SQLite.delete(db, PREV, OID + " IN " + query);
   }

   private static void upgradeTablePrevBooksV2(SQLiteDatabase db) {
      Table.dropIndex(db, PREV, BID);
      SQLite.alterTablePrepare(db, PREV);
      createTablePrevBooks(db);
      String stocked = SQLite.datetimeToPosix(STOCKED);
      String tstamp = SQLite.datetimeToPosix(TSTAMP);
      SQLite.alterTableExecute(db, PREV,
            OID, BID, TITLE, PUBLISHER, AUTHOR, KEYWORDS, stocked, SHELF, NUMBER, PERIOD, ISBN, LABEL, "NULL", tstamp);
   }

   /* ============================================================================================================== */

   @NonNull
   @Override
   public Book insert() {
      try (SQLite.Transaction transaction = new SQLite.Transaction()) {
         setLong(BID, SQLite.insert(null, IDS, new Values(OID)));
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

   @NonNull
   public static Book getNonNull(long bid) {
      Book book = Book.get(BID + "=?", bid);
      if (book == null) { throw new RuntimeException("no book with bid " + bid); }
      return book;
   }

   @Nullable
   public static Book getIdentifiedByISBN(@NonNull ISBN isbn) {
      return Book.get(ISBN + "=? AND " + LABEL + " ISNULL", isbn.getValue());
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Returns the number of books that lack a identifying barcode, i. e. no isbn and no label.
    * This method will only be called from {@link FirstRun3Activity}.
    *
    * @return the number of books that lack a identifying barcode.
    */
   public static int countNoScanId() {
      // SELECT COUNT(*) FROM books WHERE isbn ISNULL AND label ISNULL ;
      String where = ISBN + " ISNULL AND " + LABEL + " ISNULL";
      return SQLite.getIntFromQuery(TAB, "COUNT(*)", where);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @NonNull
   private static ArrayList<Book> getIncludeDeleted(String where, Object... args) {
      Values columns = new Values(OID, TITLE, PUBLISHER, AUTHOR, KEYWORDS, ISBN);
      return SQLite.get(Book.class, VIEW, columns, null, OID + " DESC", where, args);
   }

   @NonNull
   public static ArrayList<Book> getByISBNIncludeDeleted(@NonNull ISBN isbn) {
      return getIncludeDeleted(ISBN + "=?", isbn.getValue());
   }

   @NonNull
   public static ArrayList<Book> getByTitleIncludeDeleted(@NonNull String title) {
      return getIncludeDeleted(TITLE + "=?", title);
   }

   /**
    * Returns a list of books from view {@code prev_books_newest} grouped and ordered by {@code column},
    * where values are only assigned for columns {@code _id} and the specified {@code column}.
    * This method will be called e. g. to populate lists in {@link AutoCompleteTextView}s.
    *
    * @param column
    *       the requested column.
    * @param isbn
    *       if not {@code null}, select only rows with the specified isbn.
    * @return a list of books from view {@code prev_books_newest} grouped and ordered by {@code column}.
    *
    * @throws IllegalArgumentException
    *       if the specified {@code column} is not one of
    *       {@link #TITLE}, {@link #PUBLISHER}, {@link #AUTHOR} or {@link #KEYWORDS}.
    */
   @NonNull
   public static ArrayList<Book> getColumnValues(@NonNull String column, @Nullable ISBN isbn) {
      if (!acceptedColumns.contains(column)) {
         throw new IllegalArgumentException(column + " not allowed");
      }
      Values columns = new Values(OID, column);
      if (isbn == null) {
         return SQLite.get(Book.class, VIEW, columns, column, column, null);
      } else {
         return SQLite.get(Book.class, VIEW, columns, column, column, ISBN + "=?", isbn.getValue());
      }
   }

   private static final List<String> acceptedColumns = Arrays.asList(TITLE, PUBLISHER, AUTHOR, KEYWORDS);

   /**
    * Returns a list of books from table {@code books} grouped and ordered by column {@code SHELF},
    * where values are only assigned for columns {@code _id} and column {@code SHELF}.
    * This method will be called e. g. to populate lists in {@link AutoCompleteTextView}s.
    *
    * @return a list of books from table {@code books} grouped and ordered by column {@code SHELF}.
    */
   @NonNull
   public static ArrayList<Book> getShelfValues() {
      return SQLite.get(Book.class, TAB, new Values(OID, SHELF), SHELF, SHELF, null);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

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
      ArrayList<Book> books = SQLite.get(Book.class, TAB, new Values(NUMBER), null, NUMBER, SHELF + "=?", shelf);

      ArrayList<Integer> numbers = new ArrayList<>(books.size());
      for (Book book : books) {
         numbers.add(book.getNumber());
      }
      return numbers;
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Returns a list of all books, ordered by {@code shelf} and {@code number}.
    *
    * @return a list of all books, ordered by {@code shelf} and {@code number}.
    */
   @NonNull
   public static ArrayList<Book> getAll() {
      // SELECT * FROM books ORDER BY shelf, number ;
      return SQLite.get(Book.class, TAB, TAB_COLUMNS, null, SHELF + ", " + NUMBER, null);
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
      return values.getText(SHELF);
   }

   @NonNull
   public Book setShelf(@NonNull String shelf) {
      return (Book) setText(SHELF, shelf);
   }

   public int getNumber() {
      return values.getInt(NUMBER);
   }

   @NonNull
   public Book setNumber(int number) {
      return (Book) setLong(NUMBER, number);
   }

   @NonNull
   public String getTitle() {
      return values.getText(TITLE);
   }

   @NonNull
   public Book setTitle(@NonNull String title) {
      return (Book) setText(TITLE, title);
   }

   @NonNull
   public String getPublisher() {
      return values.getText(PUBLISHER);
   }

   @NonNull
   public Book setPublisher(@NonNull String publisher) {
      return (Book) setText(PUBLISHER, publisher);
   }

   @NonNull
   public String getAuthor() {
      return values.getText(AUTHOR);
   }

   @NonNull
   public Book setAuthor(@NonNull String author) {
      return (Book) setText(AUTHOR, author);
   }

   @NonNull
   public String getKeywords() {
      return values.getText(KEYWORDS);
   }

   @NonNull
   public Book setKeywords(@NonNull String keywords) {
      return (Book) setText(KEYWORDS, keywords);
   }

   /**
    * Returns the date when the book was stocked as a string, formatted {@code DD.MM.YYYY} in the default timezone.
    *
    * @return the date when the book was stocked.
    */
   @NonNull
   public String getStockedDate() {
      return App.formatDate("dd'.'MM'.'yyyy", false, values.getLong(STOCKED));
   }

   /**
    * Called only by {@link FirstRun3Activity} to manually set {@code stocked} values read from CSV files.
    */
   @NonNull
   public Book setStocked(String dd, String mm, String yyyy) {
      // precondition: yyyy matches 2[0-9]{3}
      // set stocked to the specified date 'yyyy-mm-dd 10:00:00' localtime
      int d = Integer.parseInt(dd);
      int m = Integer.parseInt(mm);
      int y = Integer.parseInt(yyyy);
      return (Book) setLong(STOCKED, new GregorianCalendar(y, m - 1, d, 10, 0).getTimeInMillis() / 1000);
   }

   public int getPeriod() {
      return values.getInt(PERIOD);
   }

   @NonNull
   public Book setPeriod(int period) {
      return (Book) setLong(PERIOD, period);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public boolean hasISBN() {
      return values.notNull(ISBN);
   }

   public boolean hasLabel() {
      return values.notNull(LABEL);
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
      return (Book) (isbn == null ? setNull(ISBN) : setLong(ISBN, isbn.getValue()));
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
      return (Book) (label == null ? setNull(LABEL) : setLong(LABEL, label.getId()));
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Returns {@code true} if the book has vanished, otherwise {@code false}.
    *
    * @return {@code true} if the book has vanished.
    */
   public final boolean hasVanished() {
      return values.notNull(VANISHED);
   }

   /**
    * Returns the date when the book has vanished as a string, formatted {@code DD.MM.YYYY} in the default timezone.
    * <p> Precondition: {@link #hasVanished()} must be {@code true}. </p>
    *
    * @return the date when the book has vanished.
    */
   @NonNull
   public String getVanished() {
      return App.formatDate("dd'.'MM'.'yyyy", false, values.getLong(VANISHED));
   }

   @NonNull
   public final Book setVanished(boolean vanished) {
      return (Book) (vanished ? setLong(VANISHED, App.posixTime()) : setNull(VANISHED));
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
      return !hasLabel() ? "" : SerialNumber.getDisplay(values.getInt(LABEL));
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