/*
 * Row.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.db;

import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A in-memory representation of one row in a table.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public abstract class Row {

   /** The values of this row. */
   protected final Values values;

   /** The changed values of this row. Must be cleared after an {@link #update()} or {@link #insert()}. */
   private final Values change = new Values();

   /**
    * Creates a new empty row.
    */
   protected Row() { values = new Values(); }

   /**
    * Creates a new row that initially contains the column values from the specified row {@code other}.
    *
    * @param other
    *       the row from where the column values are copied.
    */
   protected Row(Row other) { values = new Values(other.values); }

   /**
    * Creates a new row that initially contains the column values from the specified cursor {@code c}.
    *
    * @param cursor
    *       the cursor.
    */
   protected Row(Cursor cursor) { values = new Values().add(cursor); }

   /* ============================================================================================================== */

   protected abstract String getTable();

   public final long getOid() {
      return values.getLong(BaseColumns._ID);
   }

   /* ============================================================================================================== */

   protected final Row setNonNull(@NonNull String key, @NonNull String value) {
      return setNullable(key, value);
   }

   protected final Row setNullable(@NonNull String key, @Nullable String value) {
      change.add(key, value); values.add(key, value);
      return this;
   }

   protected final Row setNonNull(@NonNull String key, int value) {
      return setNullable(key, value);
   }

   protected final Row setNullable(@NonNull String key, @Nullable Integer value) {
      change.add(key, value); values.add(key, value);
      return this;
   }

   protected final Row setNonNull(@NonNull String key, long value) {
      return setNullable(key, value);
   }

   protected final Row setNullable(@NonNull String key, @Nullable Long value) {
      change.add(key, value); values.add(key, value);
      return this;
   }

   /* ============================================================================================================== */

   protected Row insert() {
      long oid = SQLite.insert(getTable(), change);
      values.add(BaseColumns._ID, oid);
      change.clear();
      return this;
   }

   public void delete() {
      SQLite.delete(getTable(), BaseColumns._ID + "=?", values.getLong(BaseColumns._ID));
   }

   public final Row update() {
      SQLite.update(getTable(), change, BaseColumns._ID + "=?", values.getLong(BaseColumns._ID));
      change.clear();
      return this;
   }

   /* ============================================================================================================== */

   @Override
   public boolean equals(Object that) {
      return (that instanceof Row) && values.cv.equals(((Row) that).values.cv);
   }

   @Override
   public int hashCode() {
      return values.cv.hashCode();
   }

   @Override
   public String toString() {
      return (change.cv.size() == 0) ? values.toString() : values + " # " + change;
   }

}