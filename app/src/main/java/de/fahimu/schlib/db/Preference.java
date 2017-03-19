/*
 * Preference.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.db;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import java.util.List;

import de.fahimu.android.db.Row;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Values;

/**
 * A in-memory representation of one row of table {@code preferences}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class Preference extends Row {

   static final public  String FIRST_RUN  = "first_run";
   static final public  String CIPHER_KEY = "cipher_key";
   static final private String TAB        = "preferences";
   static final private String OID        = BaseColumns._ID;
   static final private String KEY        = "key";
   static final private String VALUE      = "value";

   // SELECT preferences._id AS _id, key, value
   static final private Values TAB_COLUMNS = new Values().add(SQLite.alias(TAB, OID, OID), KEY, VALUE);

   static void create(SQLiteDatabase db) {
      Table tab = new Table(TAB, 5, OID, false);
      tab.addColumn(KEY, Table.TYPE_TEXT, true).addCheckLength(">=", 1).addUnique();
      tab.addColumn(VALUE, Table.TYPE_TEXT, true);
      tab.create(db);
      // after creation, we'll start with activity 'FirstRun1' (create admin account)
      SQLite.insertFirstRowAfterCreate(db, TAB, new Values().add(KEY, FIRST_RUN).add(VALUE, "1"));
   }

   /* ============================================================================================================== */

   @NonNull
   public static Preference insert(@NonNull String key, @NonNull String value) {
      return (Preference) new Preference().setKey(key).setValue(value).insert();
   }

   @Nullable
   public static Preference getNullable(@NonNull String key) {
      List<Preference> list = SQLite.get(Preference.class, TAB, TAB_COLUMNS, null, null, KEY + "=?", key);
      return (list.size() == 0) ? null : list.get(0);
   }

   @NonNull
   public static Preference getNonNull(@NonNull String key) {
      Preference preference = getNullable(key);
      if (preference == null) { throw new RuntimeException("no preference " + key); }
      return preference;
   }

   /* ============================================================================================================== */

   @NonNull
   @Override
   protected String getTable() { return TAB; }

   @NonNull
   private Preference setKey(@NonNull String key) {
      return (Preference) setNonNull(KEY, key);
   }

   @NonNull
   public String getValue() {
      return values.getNonNull(VALUE);
   }

   @NonNull
   public Preference setValue(@NonNull String value) {
      return (Preference) setNonNull(VALUE, value);
   }

}