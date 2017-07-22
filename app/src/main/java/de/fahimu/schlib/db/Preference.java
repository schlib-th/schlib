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


import java.util.Arrays;
import java.util.List;

import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;
import de.fahimu.android.db.Row;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Values;
import de.fahimu.android.db.View;

/**
 * A in-memory representation of one row of table {@code preferences}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class Preference extends Row {

   static final private String OPENED     = "opened";
   static final public  String FIRST_RUN  = "first_run";
   static final public  String CIPHER_KEY = "cipher_key";
   static final         String TAB        = "preferences";
   static final private String VIEW       = OPENED;
   static final private String OID        = BaseColumns._ID;
   static final         String KEY        = "key";
   static final private String VALUE      = "value";

   /* -------------------------------------------------------------------------------------------------------------- */

   static void create(SQLiteDatabase db) {
      createTablePreferences(db);
      insertOpened(db);
      insertFirstRun(db);
      createViewOpened(db);
   }

   static void upgrade(SQLiteDatabase db, int oldVersion) {
      if (oldVersion < 2) {
         upgradeTablePreferencesV2(db);
         // Mandatory entry 'opened' and view 'opened' introduced with version 2
         insertOpened(db);
         createViewOpened(db);
      }
   }

   private static void createTablePreferences(SQLiteDatabase db) {
      Table tab = new Table(TAB, 5, false);
      tab.addTextColumn(KEY, true).addCheckLength(">=3").addUnique();
      tab.addTextColumn(VALUE, true);
      tab.create(db);
   }

   /**
    * If not specified otherwise by any admin, we assume the library
    * to be official opened for pupils from Monday to Friday, 07.00 - 15.00 localtime.
    */
   private static void insertOpened(SQLiteDatabase db) {
      String value = opened("1:07.00-15.00", "2:07.00-15.00", "3:07.00-15.00", "4:07.00-15.00", "5:07.00-15.00");
      SQLite.insert(db, TAB, new Values().addText(KEY, OPENED).addText(VALUE, value));
   }

   /**
    * After creation, we'll start with activity 'FirstRun1' (create admin account).
    */
   private static void insertFirstRun(SQLiteDatabase db) {
      SQLite.insert(db, TAB, new Values().addText(KEY, FIRST_RUN).addText(VALUE, "1"));
   }

   /**
    * Select the 3-digit-codes from entry {@code 'opened'}:
    * <p>
    * <pre> {@code
    * CREATE VIEW opened AS
    *    WITH RECURSIVE split(dw, s1, s2, value) AS (
    *       SELECT NULL, NULL, NULL, value FROM preferences WHERE key='opened' UNION ALL
    *       SELECT CAST(SUBSTR(value,1,1) AS INTEGER),
    *              CAST(SUBSTR(value,3,2) * 3600 + SUBSTR(value, 6,2) * 60 AS INTEGER),
    *              CAST(SUBSTR(value,9,2) * 3600 + SUBSTR(value,12,2) * 60 AS INTEGER),
    *              SUBSTR(value,15) FROM split WHERE value != ''
    *    ) SELECT dw, s1, s2 FROM split WHERE dw NOTNULL ;
    * }
    * </pre>
    */
   private static void createViewOpened(SQLiteDatabase db) {
      View view = new View(VIEW);
      view.addSQL(App.format("WITH RECURSIVE split(dw, s1, s2, %s) AS (", VALUE));
      view.addSQL(App.format("  SELECT NULL, NULL, NULL, %s FROM %s WHERE %s='%s' UNION ALL", VALUE, TAB, KEY, OPENED));
      view.addSQL(App.format("  SELECT CAST(SUBSTR(%1$s,1,1) AS INTEGER),", VALUE));
      view.addSQL(App.format("         CAST(SUBSTR(%1$s,3,2) * 3600 + SUBSTR(%1$s, 6,2) * 60 AS INTEGER),", VALUE));
      view.addSQL(App.format("         CAST(SUBSTR(%1$s,9,2) * 3600 + SUBSTR(%1$s,12,2) * 60 AS INTEGER),", VALUE));
      view.addSQL(App.format("         SUBSTR(%1$s,15) FROM split WHERE %1$s != ''", VALUE));
      view.addSQL(App.format(") SELECT dw, s1, s2 FROM split WHERE dw NOTNULL"));
      view.create(db);
   }

   private static void upgradeTablePreferencesV2(SQLiteDatabase db) {
      SQLite.alterTablePrepare(db, TAB);
      createTablePreferences(db);
      SQLite.alterTableExecute(db, TAB, OID, KEY, VALUE);
   }

   /* ============================================================================================================== */

   /**
    * Returns the {@code JOIN-ON} SQL statement
    * <pre> {@code JOIN opened ON ((column/86400+4)%7=dw AND issue %86400 BETWEEN s1 AND s2)} </pre>.
    *
    * @param column
    *       the column to compare with.
    * @return the {@code JOIN-ON} SQL statement specified above.
    */
   static String joinOpened(String column) {
      return "JOIN " + VIEW + " ON ((" + column + "/86400+4)%7=dw AND " + column + "%86400 BETWEEN s1 AND s2)";
   }

   /**
    * Returns a string that contains information when the library is official opened for pupils.
    * The {@code periods} are strings in the fix format {@code 'D:HH.MM-HH.MM'}. Each period starts with
    * {@code D} stating the day of week from Sunday ({@code 0}) to Saturday ({@code 6}), followed by the
    * beginning and ending of the period in hours (00-23) and minutes (00-59).
    * <p>
    * For example, the two strings {@code "1:07.40-08.10"} and {@code "4:07.35-08.15"} mean opened
    * on Monday from 07.40 until 08.10 and on Thursday from 07.35 until 08.15.
    * </p>
    *
    * @param periods
    *       the opening information as one or more strings.
    * @return a normalized string that contains information when the library is official opened for pupils.
    */
   @NonNull
   private static String opened(String... periods) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         scope.d(Arrays.toString(periods));
         StringBuilder opened = new StringBuilder();
         for (String period : periods) {
            String timeFormat = "(([0-1][0-9])|(2[0-3]))\\.[0-5][0-9]";
            if (period.matches("^[0-6]:" + timeFormat + "-" + timeFormat + "$")) {
               opened.append(period).append(',');
            }
         }
         scope.d(opened.toString());
         return opened.toString();
      }
   }

   @NonNull
   public static Preference insert(@NonNull String key, @NonNull String value) {
      return (Preference) new Preference().setKey(key).setValue(value).insert();
   }

   @Nullable
   public static Preference getNullable(@NonNull String key) {
      Values columns = new Values(OID, KEY, VALUE);
      List<Preference> list = SQLite.get(Preference.class, TAB, columns, null, null, KEY + "=?", key);
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
      return (Preference) setText(KEY, key);
   }

   @NonNull
   public String getValue() {
      return values.getText(VALUE);
   }

   @NonNull
   public Preference setValue(@NonNull String value) {
      return (Preference) setText(VALUE, value);
   }

}