/*
 * Values.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A convenience class for building {@link android.content.ContentValues ContentValues}.
 * In contrast to the android class, the {@code add} methods can be concatenated, because each {@code add} method
 * returns the reference on the {@code Values} wrapper object. Using this class results in more compact code.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class Values {

   /**
    * Contains only {@code null} or objects of type {@link String} or {@link Long}.
    */
   @NonNull
   final ContentValues cv;

   /**
    * Creates a new empty {@code Values} object.
    */
   public Values() {
      cv = new ContentValues(10);      // 10 columns should be enough
   }

   /**
    * Creates a new {@code Values} object from the specified one
    * and adds an entry with value {@code null} for each key in the specified array.
    *
    * @param other
    *       the values to copy.
    * @param keys
    *       the key array.
    */
   public Values(@NonNull Values other, @NonNull String... keys) {
      cv = new ContentValues(other.cv);
      addKeys(keys);
   }

   /**
    * Creates a new {@code Values} object
    * and adds an entry with value {@code null} for each key in the specified array.
    *
    * @param keys
    *       the key array.
    */
   public Values(@NonNull String... keys) {
      cv = new ContentValues(keys.length);
      addKeys(keys);
   }

   private void addKeys(@NonNull String... keys) {
      for (String key : keys) {
         if (key != null) {
            cv.putNull(key);     // assure that no {@code null} keys are stored
         }
      }
   }

   /* ============================================================================================================== */

   /**
    * Adds the specified entry and returns this {@code Values} object.
    *
    * @param key
    *       the key of the entry.
    * @param value
    *       the value of the entry.
    * @return this {@code Values} object.
    */
   @NonNull
   public Values addText(@NonNull String key, @NonNull String value) {
      cv.put(key, value);
      return this;
   }

   /**
    * Adds the specified entry and returns this {@code Values} object.
    *
    * @param key
    *       the key of the entry.
    * @param value
    *       the value of the entry.
    * @return this {@code Values} object.
    */
   @NonNull
   public Values addLong(@NonNull String key, long value) {
      cv.put(key, value);
      return this;
   }

   /**
    * Adds an entry with the specified key and value null and returns this {@code Values} object.
    *
    * @param key
    *       the key of the entry.
    * @return this {@code Values} object.
    */
   @NonNull
   public Values addNull(@NonNull String key) {
      cv.putNull(key);
      return this;
   }

   /**
    * Adds an entry with the column name and column value for each column of the specified cursor {@code c}.
    * If the column value is {@code null} and this object already contains an entry for the column name, the
    * value of this entry will not be changed, otherwise a new entry with value {@code null} will be added.
    *
    * @param c
    *       the cursor.
    * @return this {@code Values} object.
    */
   @NonNull
   public Values add(@NonNull Cursor c) {
      for (int i = 0; i < c.getColumnCount(); i++) {
         String key = c.getColumnName(i);
         switch (c.getType(i)) {
         case Cursor.FIELD_TYPE_INTEGER:
            cv.put(key, c.getLong(i)); break;
         case Cursor.FIELD_TYPE_STRING:
            cv.put(key, c.getString(i)); break;
         case Cursor.FIELD_TYPE_NULL:
            if (!cv.containsKey(key)) { cv.putNull(key); } break;
         case Cursor.FIELD_TYPE_FLOAT:
            throw new SQLException("Column type 'REAL' not supported");
         case Cursor.FIELD_TYPE_BLOB:
            throw new SQLException("Column type 'BLOB' not supported");
         }
      }
      return this;
   }

   /* ============================================================================================================== */

   @Nullable
   private <T> T getNullable(Class<T> type, @NonNull String key) {
      if (!cv.containsKey(key)) {
         throw new RuntimeException(key + ": no such entry");
      } else {
         try {
            return type.cast(cv.get(key));
         } catch (ClassCastException cce) {
            throw new RuntimeException(key + ": expected " + type.getCanonicalName()
                  + ", found " + cv.get(key).getClass().getCanonicalName());
         }
      }
   }

   @NonNull
   private <T> T getNonNull(Class<T> type, @NonNull String key) {
      T value = getNullable(type, key);
      if (value == null) {
         throw new RuntimeException(key + ": value is null");
      }
      return value;
   }

   /**
    * Returns {@code true} if the value of the specified {@code key} is not {@code null}.
    *
    * @param key
    *       the key of the requested entry.
    * @return {@code true} if the value of the specified {@code key} is not {@code null}.
    *
    * @throws RuntimeException
    *       if there is no entry with the specified {@code key}.
    */
   public boolean notNull(@NonNull String key) {
      return getNullable(Object.class, key) != null;
   }

   /**
    * Returns the value of the specified {@code key} as {@code NonNull} {@link String}.
    *
    * @param key
    *       the key of the requested entry.
    * @return the value of the specified {@code key} as {@code NonNull} {@link String}
    *
    * @throws RuntimeException
    *       if there is no entry with the specified {@code key} or the value is not an instance of {@code String}.
    */
   @NonNull
   public String getText(@NonNull String key) {
      return getNonNull(String.class, key);
   }

   /**
    * Returns the value of the specified {@code key} as an {@code int}.
    *
    * @param key
    *       the key of the requested entry.
    * @return the value of the specified {@code key} as an {@code int}.
    *
    * @throws RuntimeException
    *       if there is no entry with the specified {@code key} or the value is not an instance of {@code int}.
    */
   public int getInt(@NonNull String key) {
      Long value = getNonNull(Long.class, key);
      if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
         throw new RuntimeException(value + " out of int bounds");
      }
      return value.intValue();
   }

   /**
    * Returns the value of the specified {@code key} as a {@code long}.
    *
    * @param key
    *       the key of the requested entry.
    * @return the value of the specified {@code key} as a {@code long}.
    *
    * @throws RuntimeException
    *       if there is no entry with the specified {@code key} or the value is not an instance of {@code long}.
    */
   public long getLong(@NonNull String key) {
      return getNonNull(Long.class, key);
   }

   /* ============================================================================================================== */

   /**
    * Returns a new string array containing the key of every entry.
    *
    * @return a new string array containing the key of every entry.
    */
   @NonNull
   String[] keys() {
      int i = 0;
      String[] keys = new String[cv.size()];
      for (String s : cv.keySet()) { keys[i++] = s; }
      return keys;
   }

   /**
    * Removes all entries and returns this {@code Values} object.
    */
   void clear() {
      cv.clear();
   }

   /**
    * Returns a string representation of the wrapped {@code ContentValues} object.
    *
    * @return a string representation of the wrapped {@code ContentValues} object.
    */
   @Override
   public String toString() {
      return "(" + cv.toString() + ")";
   }

}