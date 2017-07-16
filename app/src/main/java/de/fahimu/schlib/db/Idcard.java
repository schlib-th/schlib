/*
 * Idcard.java
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
 * A in-memory representation of one row of table {@code idcards}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class Idcard extends Serial {

   static final private String TAB      = "idcards";
   static final public  int    MIN      = 1000;
   static final private int    MAX      = 9999;
   static final private int    PER_PAGE = 10;

   static void create(SQLiteDatabase db) {
      create(db, TAB, MIN, MAX);
   }

   static void upgrade(SQLiteDatabase db, int oldVersion) {
      upgrade(db, TAB, MIN, MAX, oldVersion);
   }

   // SELECT idcards._id AS _id, page, lost, uid
   private static final Values JOIN_COLUMNS = new Values(SQLite.alias(TAB, OID), PAGE, LOST, User.UID);

   // FROM idcards LEFT JOIN users ON idcards._id=users.idcard
   private static final String JOINED_TABLE =
         App.format("%1$s LEFT JOIN %2$s ON %1$s.%3$s=%2$s.%4$s", TAB, User.TAB, OID, User.IDCARD);

   /* ============================================================================================================== */

   /**
    * Returns how often {@link #createOnePage()} can be called without an exception thrown.
    * <p> Called before creating new idcards. </p>
    */
   public static int canCallCreateOnePage() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return canCallCreateOnePage(TAB, PER_PAGE, MAX);
      }
   }

   /**
    * Sets the state of {@link #PER_PAGE} rows in table {@code idcards} to 'Printed'.
    * First get the maximum page number currently assigned to an idcard.
    * Next update rows where the value of {@code lost} is less than the current date minus five years, meaning that
    * this idcard was not used for the last five years. If more idcards are needed, new rows will then be inserted.
    * <p> Called to create new idcards. </p>
    */
   public static int createOnePage() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         createOnePage(Idcard.class, TAB, PER_PAGE); return PER_PAGE;
      }
   }

   /**
    * Sets the state of the idcards on the most recently printed page to 'Lost'.
    * <p> Called to delete idcards. </p>
    *
    * @return the number of changed rows.
    */
   public static int deleteOnePage() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return deleteOnePage(TAB);
      }
   }

   /**
    * Returns the count of all 'Printed' idcards in the specified {@code table}.
    *
    * @return the count of all 'Printed' idcards in the specified {@code table}.
    */
   public static int countPrinted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return countPrinted(TAB);
      }
   }

   /**
    * Returns a list of all 'Printed' idcards.
    * The list is ordered first by {@code page} and second by {@code _id}.
    * <p> Called when writing the idcards to a PDF document. </p>
    *
    * @return a list of all 'Printed' idcards.
    */
   @NonNull
   public static List<Idcard> getPrinted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return getPrinted(Idcard.class, JOINED_TABLE, JOIN_COLUMNS);
      }
   }

   /**
    * Returns the {@code Idcard} with the specified {@code barcode}
    * or {@code null} if there is no such {@code Idcard}.
    *
    * @param barcode
    *       the barcode of the requested {@code Idcard}.
    * @return the {@code Idcard} with the specified {@code barcode} or {@code null}.
    */
   @Nullable
   public static Idcard parse(String barcode) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return parse(Idcard.class, JOINED_TABLE, JOIN_COLUMNS, TAB, barcode);
      }
   }

   /**
    * Returns the {@code Idcard} with the specified {@code number}.
    *
    * @param number
    *       the number of the requested {@code Idcard}.
    * @return the {@code Idcard} with the specified {@code number}.
    *
    * @throws IllegalStateException
    *       if there is no such {@code Idcard}.
    */
   @NonNull
   public static Idcard getNonNull(int number) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return getNonNull(Idcard.class, JOINED_TABLE, JOIN_COLUMNS, TAB, number);
      }
   }

   /**
    * Returns a list of all idcards, ordered by {@code _id}.
    * Rows where {@code lost} is less than the current date minus four years, meaning that this idcard was lost
    * for the last four years, are excluded, because these idcards can't be set from lost to stocked anymore.
    *
    * @return a list of all idcards, ordered by {@code _id}.
    */
   @NonNull
   public static ArrayList<Idcard> get() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return get(Idcard.class, JOINED_TABLE, JOIN_COLUMNS);
      }
   }

   /**
    * Returns a list of all stocked idcards, ordered by {@code _id}.
    *
    * @return a list of all stocked idcards, ordered by {@code _id}.
    */
   @NonNull
   public static ArrayList<Idcard> getStocked() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return getStocked(Idcard.class, JOINED_TABLE, JOIN_COLUMNS, User.UID);
      }
   }

   /**
    * Returns a ascending ordered list of all page numbers in table {@code idcards}.
    * <p> Called before registering 'Printed' PDF documents ({@link de.fahimu.schlib.app.RegisterPrintsActivity}). </p>
    *
    * @return a ascending ordered list of all page numbers in table {@code idcards}.
    */
   @NonNull
   public static List<Integer> getPageNumbers() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return getPageNumbers(Idcard.class, TAB);
      }
   }

   /* ============================================================================================================== */

   @NonNull
   @Override
   protected String getTable() { return TAB; }

   /**
    * Returns {@code true} if this Idcard is used, otherwise {@code false}.
    *
    * @return {@code true} if this Idcard is used.
    */
   public boolean isUsed() {
      return values.notNull(User.UID);
   }

   /**
    * Returns the {@code uid} of the {@link User} that references this {@code Idcard}.
    * <p> Precondition: {@link #isUsed()} must be {@code true}. </p>
    *
    * @return the {@code uid} of the {@link User} that references this {@code Idcard}.
    */
   public long getUid() {
      return values.getLong(User.UID);
   }

   @NonNull
   @Override
   String getDisplayUsed() {
      return App.getStr(R.string.serial_display_used_idcard);
   }

}