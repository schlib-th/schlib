/*
 * Values.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.db;

import android.content.ContentValues;
import android.database.Cursor;
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
    * Contains only {@link String} or {@code null} cv, no {@link Boolean} or {@link Number}.
    */
   @NonNull
   final ContentValues cv;

   /**
    * Creates a new empty {@code Values} object.
    */
   public Values() {
      cv = new ContentValues(15);      // 15 columns should be enough
   }

   /**
    * Creates a new {@code Values} object from the specified one.
    *
    * @param other
    *       the cv to copy
    */
   public Values(@NonNull Values other) {
      cv = new ContentValues(other.cv);
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
   public Values add(@NonNull String key, String value) {
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
   public Values add(@NonNull String key, Integer value) {
      cv.put(key, (value == null) ? null : value.toString());
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
   public Values add(@NonNull String key, Long value) {
      cv.put(key, (value == null) ? null : value.toString());
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
   public Values add(@NonNull String key) {
      cv.putNull(key);
      return this;
   }

   /**
    * Adds an entry with a string key and value null for each string in the specified array.
    *
    * @param keys
    *       the key array
    * @return this {@code Values} object.
    */
   @NonNull
   public Values add(@NonNull String[] keys) {
      for (String key : keys) {
         if (key != null) {
            cv.putNull(key);     // assure that no {@code null} keys are stored
         }
      }
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
         if (c.getType(i) != Cursor.FIELD_TYPE_NULL) {
            cv.put(key, c.getString(i));
         } else if (!cv.containsKey(key)) {
            cv.putNull(key);
         }
      }
      return this;
   }

   /* ============================================================================================================== */

   @Nullable
   private static String getNullable(@NonNull ContentValues values, @NonNull String key) {
      if (!values.containsKey(key)) {
         throw new RuntimeException(key + ": no such entry");
      }
      return (String) values.get(key);
   }

   @NonNull
   private static String getNonNull(@NonNull ContentValues values, @NonNull String key) {
      String value = getNullable(values, key);
      if (value == null) {
         throw new RuntimeException(key + ": value is null");
      }
      return value;
   }

   /**
    * Returns the value of the specified {@code key} as a possibly {@code null} {@link String}.
    *
    * @param key
    *       the key of the requested entry.
    * @return the value of the specified {@code key} as a possibly {@code null} {@link String}.
    *
    * @throws RuntimeException
    *       if there is no entry with the specified {@code key}.
    */
   @Nullable
   public String getNullable(@NonNull String key) {
      return getNullable(cv, key);
   }

   /**
    * Returns the value of the specified {@code key} as {@code NonNull} {@link String}.
    *
    * @param key
    *       the key of the requested entry.
    * @return the value of the specified {@code key} as {@code NonNull} {@link String}
    *
    * @throws RuntimeException
    *       if there is no entry with the specified {@code key} or the value is {@code null}.
    */
   @NonNull
   public String getNonNull(@NonNull String key) {
      return getNonNull(cv, key);
   }

   /**
    * Returns the value of the specified {@code key} as a possibly {@code null} {@link Integer}.
    *
    * @param key
    *       the key of the requested entry.
    * @return the value of the specified {@code key} as a possibly {@code null} {@link Integer}.
    *
    * @throws RuntimeException
    *       if there is no entry with the specified {@code key} or the value can't be converted.
    */
   @Nullable
   public Integer getNullableInt(@NonNull String key) {
      String value = getNullable(cv, key);
      if (value == null) { return null; }
      try { return Integer.parseInt(value); } catch (NumberFormatException e) {
         throw new RuntimeException(key, e);
      }
   }

   /**
    * Returns the value of the specified {@code key} as an {@code int}.
    *
    * @param key
    *       the key of the requested entry.
    * @return the value of the specified {@code key} as an {@code int}.
    *
    * @throws RuntimeException
    *       if there is no entry with the specified {@code key}, the value is {@code null} or can't be converted.
    */
   public int getInt(@NonNull String key) {
      String value = getNonNull(cv, key);
      try { return Integer.parseInt(value); } catch (NumberFormatException e) {
         throw new RuntimeException(key, e);
      }
   }

   /**
    * Returns the value of the specified {@code key} as a possibly {@code null} {@link Long}.
    *
    * @param key
    *       the key of the requested entry.
    * @return the value of the specified {@code key} as a possibly {@code null} {@link Long}.
    *
    * @throws RuntimeException
    *       if there is no entry with the specified {@code key} or the value can't be converted.
    */
   @Nullable
   public Long getNullableLong(@NonNull String key) {
      String value = getNullable(cv, key);
      if (value == null) { return null; }
      try { return Long.parseLong(value); } catch (NumberFormatException e) {
         throw new RuntimeException(key, e);
      }
   }

   /**
    * Returns the value of the specified {@code key} as a {@code long}.
    *
    * @param key
    *       the key of the requested entry.
    * @return the value of the specified {@code key} as a {@code long}.
    *
    * @throws RuntimeException
    *       if there is no entry with the specified {@code key}, the value is {@code null} or can't be converted.
    */
   public long getLong(@NonNull String key) {
      String value = getNonNull(cv, key);
      try { return Long.parseLong(value); } catch (NumberFormatException e) {
         throw new RuntimeException(key, e);
      }
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
    *
    * @return this {@code Values} object.
    */
   @NonNull
   Values clear() {
      cv.clear();
      return this;
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