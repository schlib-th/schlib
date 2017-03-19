/*
 * Label.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.db;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import java.util.ArrayList;
import java.util.List;

import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Values;
import de.fahimu.schlib.app.R;

/**
 * A in-memory representation of one row of table {@code labels}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class Label extends Serial {

   static final private String TAB      = "labels";
   static final private int    MIN      = 100000;
   static final private int    MAX      = 999999;
   static final private int    PER_PAGE = 21;

   static void create(@NonNull SQLiteDatabase db) { create(db, TAB, MIN, MAX); }

   // SELECT labels._id AS _id, page, lost, bid
   private static final Values COLUMNS_FOR_JOIN_QUERY =
         new Values().add(SQLite.alias(TAB, OID, OID), PAGE, LOST, Book.BID);

   // FROM labels LEFT JOIN books ON labels._id=books.label
   private static final String TABLE_NAME_FOR_JOIN_QUERY =
         App.format("%s LEFT JOIN %s ON %s.%s=%s.%s", TAB, Book.TAB, TAB, OID, Book.TAB, Book.LABEL);

   /* ============================================================================================================== */

   /**
    * Returns how often {@link #createOnePage()} can be called without an exception thrown.
    * <p> Called before creating new labels. </p>
    */
   public static int canCallCreateOnePage() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return canCallCreateOnePage(TAB, PER_PAGE, MAX);
      }
   }

   /**
    * Sets the state of {@link #PER_PAGE} rows in table {@code labels} to 'Printed'.
    * First get the maximum page number currently assigned to a label.
    * Next update rows where the value of {@code lost} is less than the current date minus five years, meaning that
    * this label was not used for the last five years. If more labels are needed, new rows will then be inserted.
    * <p> Called to create new labels. </p>
    */
   public static int createOnePage() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         createOnePage(Label.class, TAB, PER_PAGE); return PER_PAGE;
      }
   }

   /**
    * Sets the state of the labels on the most recently printed page to 'Lost'.
    * <p> Called to delete labels. </p>
    *
    * @return the number of changed rows.
    */
   public static int deleteOnePage() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return deleteOnePage(TAB);
      }
   }

   /**
    * Returns the count of all 'Printed' labels in the specified {@code table}.
    *
    * @return the count of all 'Printed' labels in the specified {@code table}.
    */
   public static int countPrinted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return countPrinted(TAB);
      }
   }

   /**
    * Returns a list of all 'Printed' labels.
    * The list is ordered first by {@code page} and second by {@code _id}.
    * <p> Called when writing the labels to a PDF document. </p>
    *
    * @return a list of all 'Printed' labels.
    */
   @NonNull
   public static List<Label> getPrinted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return getPrinted(Label.class, TABLE_NAME_FOR_JOIN_QUERY, COLUMNS_FOR_JOIN_QUERY);
      }
   }

   /**
    * Returns the {@code Label} with the specified {@code number} or
    * {@code null} if there is no such {@code Label}.
    *
    * @param number
    *       the number of the requested {@code Label}.
    * @return the {@code Label} with the specified {@code number} or {@code null}.
    */
   @Nullable
   public static Label getNullable(int number) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return getNullable(Label.class, TABLE_NAME_FOR_JOIN_QUERY, COLUMNS_FOR_JOIN_QUERY, TAB, number);
      }
   }

   /**
    * Returns the {@code Label} with the specified {@code number}.
    *
    * @param number
    *       the number of the requested {@code Label}.
    * @return the {@code Label} with the specified {@code number}.
    *
    * @throws IllegalStateException
    *       if there is no such {@code Label}.
    */
   @NonNull
   public static Label getNonNull(int number) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return getNonNull(Label.class, TABLE_NAME_FOR_JOIN_QUERY, COLUMNS_FOR_JOIN_QUERY, TAB, number);
      }
   }

   /**
    * Returns a list of all labels, ordered by {@code _id}.
    * Rows where {@code lost} is less than the current date minus four years, meaning that this label was lost
    * for the last four years, are excluded, because these labels can't be set from lost to stocked anymore.
    *
    * @return a list of all labels, ordered by {@code _id}.
    */
   @NonNull
   public static ArrayList<Label> get() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return get(Label.class, TABLE_NAME_FOR_JOIN_QUERY, COLUMNS_FOR_JOIN_QUERY);
      }
   }

   /**
    * Returns a ascending ordered list of all page numbers in table {@code labels}.
    * <p> Called before registering 'Printed' PDF documents ({@link de.fahimu.schlib.app.RegisterPrintsActivity}). </p>
    *
    * @return a ascending ordered list of all page numbers in table {@code labels}.
    */
   @NonNull
   public static List<Integer> getPageNumbers() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return getPageNumbers(Label.class, TAB);
      }
   }

   /* ============================================================================================================== */

   @NonNull
   @Override
   protected String getTable() { return TAB; }

   /**
    * Returns {@code true} if this Label is used, otherwise {@code false}.
    *
    * @return {@code true} if this Label is ued.
    */
   public boolean isUsed() {
      return values.getNullable(Book.BID) != null;
   }

   /**
    * Returns the {@code bid} of the {@link Book} that references this {@code Label}.
    * <p> Precondition: {@link #isUsed()} must be {@code true}. </p>
    *
    * @return the {@code bid} of the {@link Book} that references this {@code Label}.
    */
   public long getBid() {
      return values.getLong(Book.BID);
   }

   @NonNull
   @Override
   String getDisplayUsed() {
      return App.getStr(R.string.serial_display_used_label);
   }

}