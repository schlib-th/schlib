/*
 * Row.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.db;

import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

/**
 * A in-memory representation of one row in a table.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public abstract class Row {

   /** The values of this row. */
   protected final Values values = new Values();

   /** The changed values of this row. Must be cleared after an {@link #update()} or {@link #insert()}. */
   private final Values change = new Values();

   final <R extends Row> R add(Class<R> cls, Cursor cursor) {
      values.add(cursor);
      return cls.cast(this);
   }

   /* ============================================================================================================== */

   @NonNull
   protected abstract String getTable();

   public long getOid() {
      return values.getLong(BaseColumns._ID);
   }

   /* ============================================================================================================== */

   @NonNull
   protected final Row setText(@NonNull String key, @NonNull String value) {
      change.addText(key, value); values.addText(key, value);
      return this;
   }

   @NonNull
   protected final Row setLong(@NonNull String key, long value) {
      change.addLong(key, value); values.addLong(key, value);
      return this;
   }

   @NonNull
   protected final Row setNull(@NonNull String key) {
      change.addNull(key); values.addNull(key);
      return this;
   }

   /* ============================================================================================================== */

   @NonNull
   protected Row insert() {
      long oid = SQLite.insert(null, getTable(), change);
      values.addLong(BaseColumns._ID, oid);
      change.clear();
      return this;
   }

   public void delete() {
      SQLite.delete(null, getTable(), BaseColumns._ID + "=?", values.getLong(BaseColumns._ID));
   }

   @NonNull
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