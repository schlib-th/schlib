/*
 * User.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.db;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;


import java.util.ArrayList;
import java.util.List;

import de.fahimu.android.app.App;
import de.fahimu.android.app.scanner.ScannerAwareEditText;
import de.fahimu.android.db.Row;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Trigger;
import de.fahimu.android.db.Trigger.Type;
import de.fahimu.android.db.Values;
import de.fahimu.android.db.View;
import de.fahimu.schlib.app.R;
import de.fahimu.schlib.app.StocktakingUsersActivity;
import de.fahimu.schlib.pdf.PupilList;

import static de.fahimu.android.db.SQLite.MIN_TSTAMP;

/**
 * A in-memory representation of one row of table {@code users}.
 * <p/>
 * On the one hand, a user can be an admin or tutor, on the other hand it can be a pupil.
 * An admin or tutor is identified by his name (e.g. 'John', 'Doe'), the required serial is set to 0.
 * A pupil is identified by class and school year (e.g. '2b', '2014/15') and a class list serial greater than zero.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class User extends Row {

   static final private String IDS  = "uids";
   static final         String TAB  = "users";
   static final private String PREV = "prev_users";
   static final private String VIEW = "prev_users_oldest_pupils";

   static final private String OID    = BaseColumns._ID;
   static final         String UID    = "uid";
   static final         String ROLE   = "role";
   static final private String NAME2  = "name2";
   static final private String NAME1  = "name1";
   static final private String SERIAL = "serial";
   static final private String NBOOKS = "nbooks";
   static final         String IDCARD = "idcard";
   static final private String TSTAMP = "tstamp";

   static final private String MIN_SERIAL = "min_serial";
   static final private String MAX_SERIAL = "max_serial";
   static final private String LOCAL_DATE = "local_date";

   static final private String ADMIN = "admin";
   static final private String TUTOR = "tutor";
   static final         String PUPIL = "pupil";

   static final private Values TAB_COLUMNS = new Values(SQLite.alias(TAB, OID),
         UID, ROLE, NAME2, NAME1, SERIAL, NBOOKS, IDCARD);

   /* -------------------------------------------------------------------------------------------------------------- */

   static void create(SQLiteDatabase db) {
      createTableUids(db);
      createTableUsers(db);
      createTablePrevUsers(db);

      createTrigger(db, Type.AFTER_INSERT);
      createTrigger(db, Type.AFTER_UPDATE);
      createTrigger(db, Type.AFTER_DELETE);

      createViewPrevUsersOldestPupils(db);
   }

   static void upgrade(SQLiteDatabase db, int oldVersion) {
      if (oldVersion < 2) {
         Trigger.drop(db, TAB, Type.AFTER_INSERT);
         Trigger.drop(db, TAB, Type.AFTER_UPDATE);
         Trigger.drop(db, TAB, Type.AFTER_DELETE);

         upgradeTableUsersV2(db);
         SQLite.delete(db, PREV, IDCARD + " ISNULL");      // null forbidden since V2
         upgradeTablePrevUsersV2(db);

         createTrigger(db, Type.AFTER_INSERT);
         createTrigger(db, Type.AFTER_UPDATE);
         createTrigger(db, Type.AFTER_DELETE);

         createViewPrevUsersOldestPupils(db);
      }
   }

   private static void createTableUids(SQLiteDatabase db) {
      new Table(IDS, 3, true).create(db);
   }

   private static void createTableUsers(SQLiteDatabase db) {
      Table tab = new Table(TAB, 6, false);
      tab.addReferences(UID, true).addUnique();
      tab.addTextColumn(ROLE, true).addCheckIn(ADMIN, TUTOR, PUPIL);
      tab.addTextColumn(NAME2, true).addCheckLength(">=1");
      tab.addTextColumn(NAME1, true).addCheckLength(">=1");
      tab.addLongColumn(SERIAL, true).addCheckBetween(0, 99);
      tab.addLongColumn(NBOOKS, true).addCheckBetween(0, 99);
      tab.addReferences(IDCARD, true).addUnique();
      tab.addConstraint().addUnique(NAME2, NAME1, SERIAL);
      tab.create(db);
   }

   private static void createTablePrevUsers(SQLiteDatabase db) {
      Table prev = new Table(PREV, 6, true);
      prev.addReferences(UID, true).addIndex();          // index essential to group rows by uid
      prev.addTextColumn(ROLE, true).addCheckIn(ADMIN, TUTOR, PUPIL);
      prev.addTextColumn(NAME2, true).addCheckLength(">=1");
      prev.addTextColumn(NAME1, true).addCheckLength(">=1");
      prev.addLongColumn(SERIAL, true).addCheckBetween(0, 99);
      prev.addLongColumn(NBOOKS, true).addCheckBetween(0, 99);
      prev.addReferences(IDCARD, true);
      prev.addTimeColumn(TSTAMP, true).addCheckPosixTime(MIN_TSTAMP).addDefaultPosixTime();
      prev.create(db);
   }

   private static void createTrigger(SQLiteDatabase db, Trigger.Type type) {
      Trigger.create(db, TAB, type, PREV, UID, ROLE, NAME2, NAME1, SERIAL, NBOOKS, IDCARD);
   }

   /**
    * Select the oldest row of every pupil in prev_users (therefore including deleted pupils).
    * That is the row that was inserted in table prev_users when the pupil was inserted in table users.
    * <p>
    * <pre> {@code
    * CREATE VIEW prev_users_oldest_pupils AS
    *    SELECT MIN(_id) AS _id, name2, name1, serial, idcard,
    *           CAST(STRFTIME('%s',tstamp,'unixepoch','localtime') AS INTEGER) AS tstamp
    *    FROM prev_users WHERE role='pupil' GROUP BY uid ;
    * }
    * </pre>
    */
   private static void createViewPrevUsersOldestPupils(SQLiteDatabase db) {
      View view = new View(VIEW);
      String oid = App.format("MIN(%1$s) AS %1$s", OID);
      String tstamp = SQLite.posixToLocal(TSTAMP);
      view.addSelect(PREV, new Values(oid, NAME2, NAME1, SERIAL, IDCARD, tstamp), UID, null, ROLE + "=?", PUPIL);
      view.create(db);
   }

   private static void upgradeTableUsersV2(SQLiteDatabase db) {
      Table.dropIndex(db, TAB, NAME1, NAME2, SERIAL, ROLE);
      SQLite.alterTablePrepare(db, TAB);
      createTableUsers(db);
      SQLite.alterTableExecute(db, TAB, OID, UID, ROLE, NAME2, NAME1, SERIAL, NBOOKS, IDCARD);
   }

   private static void upgradeTablePrevUsersV2(SQLiteDatabase db) {
      Table.dropIndex(db, PREV, UID);
      SQLite.alterTablePrepare(db, PREV);
      createTablePrevUsers(db);
      String tstamp = SQLite.datetimeToPosix(TSTAMP);
      SQLite.alterTableExecute(db, PREV, OID, UID, ROLE, NAME2, NAME1, SERIAL, NBOOKS, IDCARD, tstamp);
   }

   /* ============================================================================================================== */

   public enum Role {
      ADMIN(User.ADMIN, R.string.user_admin),
      TUTOR(User.TUTOR, R.string.user_tutor),
      PUPIL(User.PUPIL, R.string.user_pupil);

      @NonNull
      private final String value, display;

      Role(@NonNull String value, @StringRes int displayId) {
         this.value = value; this.display = App.getStr(displayId);
      }

      @NonNull
      static Role getEnum(@NonNull String value) {
         if (PUPIL.value.equals(value)) { return PUPIL; }
         if (TUTOR.value.equals(value)) { return TUTOR; }
         if (ADMIN.value.equals(value)) { return ADMIN; }
         throw new IllegalArgumentException("value=" + value);
      }

      @NonNull
      public String getDisplay() {
         return display;
      }
   }

   /* ============================================================================================================== */

   @NonNull
   public static User insertAdminOrTutor(Role role, String name2, String name1, int idcard) {
      if (role == Role.PUPIL) {
         throw new IllegalArgumentException("role PUPIL not allowed");
      }
      return insert(role, name2, name1, 0, 5, idcard);
   }

   public static void insertPupils(String name2, String name1, @NonNull List<Integer> idcards) {
      try (SQLite.Transaction transaction = new SQLite.Transaction()) {
         int serial = getNextAvailableSerial(name2, name1);
         for (int idcard : idcards) {
            insert(Role.PUPIL, name2, name1, serial++, 1, idcard);
         }
         transaction.setSuccessful();
      }
   }

   @NonNull
   private static User insert(Role role, String name2, String name1, int serial, int nbooks, int idcard) {
      return new User().setRole(role).setName2(name2).setName1(name1).setSerial(serial)
            .setNbooks(nbooks).setIdcard(idcard).insert();
   }

   @NonNull
   @Override
   protected User insert() {
      try (SQLite.Transaction transaction = new SQLite.Transaction()) {
         setLong(UID, SQLite.insert(null, IDS, new Values(OID)));
         super.insert();
         transaction.setSuccessful();
         return this;
      }
   }

   /* ============================================================================================================== */

   /**
    * Returns the {@code User} specified by {@code where} and {@code args}
    * or {@code null} if there is no such {@code User}.
    *
    * @param where
    *       a filter declaring which rows to return.
    * @param args
    *       the string values, which will replace the {@code '?'} characters in {@code where}.
    * @return the {@code User} specified by {@code where} and {@code args} or {@code null}.
    */
   @Nullable
   private static User get(String where, Object... args) {
      ArrayList<User> list = SQLite.get(User.class, TAB, TAB_COLUMNS, null, null, where, args);
      return (list.size() == 0) ? null : list.get(0);
   }

   @NonNull
   public static User getNonNull(long uid) {
      User user = User.get(UID + "=?", uid);
      if (user == null) { throw new RuntimeException("no user with uid " + uid); }
      return user;
   }

   @Nullable
   public static User getAdminOrTutor(String name2, String name1) {
      return User.get(NAME2 + "=? AND " + NAME1 + "=? AND " + SERIAL + "=0", name2, name1);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Returns a list of all users, ordered by {@code role}, {@code name2}, {@code name1} and {@code serial}.
    * The ordering of the roles is 'admin', 'tutor' and 'pupil' (not alphabetically).
    *
    * @return a list of all users, ordered by {@code role}, {@code name2}, {@code name1} and {@code serial}.
    */
   @NonNull
   public static ArrayList<User> getAll() {
      String order = App.format("(CASE %s WHEN '%s' THEN 0 WHEN '%s' THEN 1 ELSE 2 END), %s, %s, %s",
            ROLE, ADMIN, TUTOR, NAME2, NAME1, SERIAL);
      return SQLite.get(User.class, TAB, TAB_COLUMNS, null, order, null);
   }

   /**
    * Returns a list of all pupils, ordered by {@code name2}, {@code name1} and {@code serial}.
    * <p> This method will be called to populate the list in {@link StocktakingUsersActivity}. </p>
    *
    * @return a list of all pupils, ordered by {@code name2}, {@code name1} and {@code serial}.
    */
   @NonNull
   public static ArrayList<User> getPupils() {
      // SELECT * FROM users WHERE role='pupil' ORDER BY name2, name1, serial ;
      String order = App.format("%s, %s, %s", NAME2, NAME1, SERIAL);
      return SQLite.get(User.class, TAB, TAB_COLUMNS, null, order, ROLE + "=?", PUPIL);
   }

   /**
    * Returns a list of pupils grouped and ordered by column {@code name1},
    * where values are only assigned for column {@code _id} and {@code name1}.
    * <p> This method will be called e. g. to populate lists in {@link ScannerAwareEditText}s. </p>
    *
    * @return a list of pupils grouped and ordered by column {@code name1}.
    */
   @NonNull
   public static ArrayList<User> getPupilsName1() {
      // SELECT _id, name1 FROM users WHERE role='pupil' GROUP BY name1 ORDER BY name1 ;
      Values columns = new Values(OID, NAME1);
      return SQLite.get(User.class, TAB, columns, NAME1, NAME1, ROLE + "=?", PUPIL);
   }

   /**
    * Returns the number of pupils in the specified school class.
    */
   public static int countPupils(String name2, String name1) {
      // SELECT COUNT(*) FROM users WHERE role='pupil' AND name2='$name2' AND name1='$name1' ;
      String where = App.format("%s=? AND %s=? AND %s=?", ROLE, NAME2, NAME1);
      return SQLite.getIntFromQuery(TAB, "COUNT(*)", where, PUPIL, name2, name1);
   }

   /**
    * Returns the next available serial for the specified school class.
    */
   public static int getNextAvailableSerial(String name2, String name1) {
      // If we would search for MAX(serial) in table 'users', and the last inserted pupil would have
      // been deleted, then its serial number would be reused, but that's erroneous! So we'll have
      // to search for MAX(serial) in the history table 'prev_users' where deleted pupils still exist.

      // SELECT MAX(serial) FROM prev_users WHERE role='pupil' AND name2='$name2' AND name1='$name1' ;
      String column = "MAX(" + SERIAL + ")";
      String where = App.format("%s=? AND %s=? AND %s=?", ROLE, NAME2, NAME1);
      return 1 + SQLite.getIntFromQuery(PREV, column, where, PUPIL, name2, name1);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Returns the pupils list that will be printed as a PDF document, sorted by {@code serial}.
    * Values are only assigned for columns {@code serial} and {@code idcard}.
    * This method will be called by {@link PupilList}.
    * <p>
    * <pre> {@code
    * SELECT serial, idcard
    * FROM prev_users_oldest_pupils
    * WHERE name2='$name2' AND name1='$name1' AND tstamp/86400=$localDate
    * ORDER BY serial ;
    * }
    * </pre>
    */
   @NonNull
   public static ArrayList<User> getPupilList(String name2, String name1, long localDate) {
      String where = App.format("%s=? AND %s=? AND %s/86400=%d", NAME2, NAME1, TSTAMP, localDate);
      return SQLite.get(User.class, VIEW, new Values(SERIAL, IDCARD), null, SERIAL, where, name2, name1);
   }

   @NonNull
   private static ArrayList<User> getEvents(Values columns, String group, String order, String where, Object... args) {
      String minSerial = App.format("MIN (%s) AS %s", SERIAL, MIN_SERIAL);
      String maxSerial = App.format("MAX (%s) AS %s", SERIAL, MAX_SERIAL);
      String localDate = App.format("%s/86400 AS %s", TSTAMP, LOCAL_DATE);
      columns.addNull(minSerial).addNull(maxSerial).addNull(localDate);
      return SQLite.get(User.class, VIEW, columns, group, order, where, args);
   }

   /**
    * Returns the insert-pupils-events for the specified school class.
    * Values are only assigned for columns
    * {@code min_serial}, {@code max_serial} and {@code tstamp}.
    * This method will be called by {@link PupilList} to determine which version of pupil list must be printed.
    * <p>
    * <pre> {@code
    * SELECT MIN(serial) AS min_serial, MAX(serial) AS max_serial, tstamp/86400 AS local_date,
    * FROM prev_users_oldest_pupils
    * WHERE name2='$name2' AND name1='$name1'
    * GROUP BY local_date
    * ORDER BY local_date ;
    * }
    * </pre>
    */
   @NonNull
   public static ArrayList<User> getInsertPupilsEvents(String name2, String name1) {
      String where = App.format("%s=? AND %s=?", NAME2, NAME1);
      return getEvents(new Values(), LOCAL_DATE, LOCAL_DATE, where, name2, name1);
   }

   /**
    * Returns all insert-pupils-events sorted by date (descending), school year and class name.
    * Values are only assigned for columns
    * {@code min_serial}, {@code max_serial}, {@code local_date}, {@code name2} and {@code name1}.
    * This method will be called to present all such events in a list when a pupil list should be reprinted.
    * <p>
    * <pre> {@code
    * SELECT MIN(serial) AS min_serial, MAX(serial) AS max_serial, tstamp/86400 AS local_date, name2, name1
    * FROM prev_users_oldest_pupils
    * GROUP BY local_date,      name2, name1
    * ORDER BY local_date DESC, name2, name1 ;
    * }
    * </pre>
    */
   @NonNull
   public static ArrayList<User> getInsertPupilsEvents() {
      String group = App.format("%s,      %s, %s", LOCAL_DATE, NAME2, NAME1);
      String order = App.format("%s DESC, %s, %s", LOCAL_DATE, NAME2, NAME1);
      return getEvents(new Values(NAME2, NAME1), group, order, null);
   }

   /* ============================================================================================================== */

   @NonNull
   @Override
   protected String getTable() { return TAB; }

   public long getUid() {
      return values.getLong(UID);
   }

   @NonNull
   public Role getRole() {
      return Role.getEnum(values.getText(ROLE));
   }

   @NonNull
   public User setRole(@NonNull Role role) {
      return (User) setText(ROLE, role.value);
   }

   @NonNull
   public String getName2() {
      return values.getText(NAME2);
   }

   /**
    * Attribute {@code name2} cannot be changed after creation for security reasons.
    */
   @NonNull
   private User setName2(@NonNull String name2) {
      return (User) setText(NAME2, name2);
   }

   @NonNull
   public String getName1() {
      return values.getText(NAME1);
   }

   /**
    * Attribute {@code name1} cannot be changed after creation for security reasons.
    */
   @NonNull
   private User setName1(@NonNull String name1) {
      return (User) setText(NAME1, name1);
   }

   public int getSerial() {
      return values.getInt(SERIAL);
   }

   /**
    * Attribute {@code serial} cannot be changed after creation for security reasons.
    */
   @NonNull
   private User setSerial(int serial) {
      return (User) setLong(SERIAL, serial);
   }

   public int getNbooks() {
      return values.getInt(NBOOKS);
   }

   @NonNull
   public User setNbooks(int nbooks) {
      return (User) setLong(NBOOKS, nbooks);
   }

   public int getIdcard() {
      return values.getInt(IDCARD);
   }

   @NonNull
   public User setIdcard(int idcard) {
      return (User) setLong(IDCARD, idcard);
   }

   public int getMinSerial() {
      return values.getInt(MIN_SERIAL);
   }

   public int getMaxSerial() {
      return values.getInt(MAX_SERIAL);
   }

   public long getLocalDate() {
      return values.getLong(LOCAL_DATE);
   }

   /* ============================================================================================================== */

   @NonNull
   public String getDisplay() {
      if (getRole() == Role.PUPIL) {
         return App.getStr(R.string.user_display_pupil, getSerial(), getName1(), getName2());
      } else if (getRole() == Role.TUTOR) {
         return App.getStr(R.string.user_display_tutor, getName1(), getName2());
      } else {
         return App.getStr(R.string.user_display_admin, getName1(), getName2());
      }
   }

}