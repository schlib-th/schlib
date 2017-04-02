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
import de.fahimu.android.db.Values;
import de.fahimu.schlib.app.R;
import de.fahimu.schlib.pdf.PupilList;

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

   static final private String OID    = BaseColumns._ID;
   static final         String UID    = "uid";
   static final private String NAME1  = "name1";
   static final private String NAME2  = "name2";
   static final private String SERIAL = "serial";
   static final private String ROLE   = "role";
   static final private String NBOOKS = "nbooks";
   static final         String IDCARD = "idcard";
   static final private String TSTAMP = "tstamp";

   /** Part of the result set of {@link #getInsertPupilsEvents(Values, String, String, String, Object...)} */
   static final private String MIN_SERIAL = "minSerial";
   /** Part of the result set of {@link #getInsertPupilsEvents(Values, String, String, String, Object...)} */
   static final private String MAX_SERIAL = "maxSerial";

   static final private String ADMIN = "admin";
   static final private String TUTOR = "tutor";
   static final private String PUPIL = "pupil";

   // SELECT users._id AS _id, uid, name1, name2, serial, role, nbooks, idcard
   static final private Values TAB_COLUMNS =
         new Values().add(SQLite.alias(TAB, OID, OID), UID, NAME1, NAME2, SERIAL, ROLE, NBOOKS, IDCARD);

   /**
    * Creates new tables {@code uids}, {@code users} and {@code prev_users} in the specified database.
    * Also three triggers are created that will copy any changed row from {@code users} to the history table
    * {@code prev_users} after inserting, updating or deleting.
    *
    * @param db
    *       the database where the tables are created.
    */
   static void create(SQLiteDatabase db) {
      // CREATE TABLE uids
      Table ids = new Table(IDS, 3, OID, true);
      ids.create(db);
      // CREATE TABLE users
      Table tab = new Table(TAB, 6, OID, false);
      tab.addRefCol(UID, true).addUnique();
      tab.addColumn(NAME1, Table.TYPE_TEXT, true).addCheckLength(">=", 1).addIndex();
      tab.addColumn(NAME2, Table.TYPE_TEXT, true).addCheckLength(">=", 1).addIndex();
      tab.addColumn(SERIAL, Table.TYPE_INTE, true).addCheckBetween(0, 99).addIndex();
      tab.addColumn(ROLE, Table.TYPE_TEXT, true).addCheckIn(ADMIN, TUTOR, PUPIL).addIndex();
      tab.addColumn(NBOOKS, Table.TYPE_INTE, true).addCheckBetween(0, 99);
      tab.addRefCol(IDCARD, false).addUnique();
      tab.addConstraint("name_unique").addUnique(NAME1, NAME2, SERIAL);
      tab.create(db);
      // CREATE TABLE prev_users
      Table prev = new Table(PREV, 6, OID, true);
      prev.addRefCol(UID, true).addIndex();
      prev.addColumn(NAME1, Table.TYPE_TEXT, true).addCheckLength(">=", 1);
      prev.addColumn(NAME2, Table.TYPE_TEXT, true).addCheckLength(">=", 1);
      prev.addColumn(SERIAL, Table.TYPE_INTE, true).addCheckBetween(0, 99);
      prev.addColumn(ROLE, Table.TYPE_TEXT, true).addCheckIn(ADMIN, TUTOR, PUPIL);
      prev.addColumn(NBOOKS, Table.TYPE_INTE, true).addCheckBetween(0, 99);
      prev.addRefCol(IDCARD, false);
      prev.addColumn(TSTAMP, Table.TYPE_TIME, true).addDefault("CURRENT_TIMESTAMP");
      prev.create(db);

      // CREATE TRIGGER AI_users
      Trigger insert = new Trigger(TAB, Trigger.Type.AFTER_INSERT);
      insert.addInsert(PREV, UID, NAME1, NAME2, SERIAL, ROLE, NBOOKS, IDCARD);
      insert.addValues("NEW." + UID, "NEW." + NAME1, "NEW." + NAME2, "NEW." + SERIAL, "NEW." + ROLE, "NEW." + NBOOKS,
            "NEW." + IDCARD);
      insert.create(db);
      // CREATE TRIGGER AU_users
      Trigger update = new Trigger(TAB, Trigger.Type.AFTER_UPDATE);
      update.addInsert(PREV, UID, NAME1, NAME2, SERIAL, ROLE, NBOOKS, IDCARD);
      update.addValues("NEW." + UID, "NEW." + NAME1, "NEW." + NAME2, "NEW." + SERIAL, "NEW." + ROLE, "NEW." + NBOOKS,
            "NEW." + IDCARD);
      update.create(db);
      // CREATE TRIGGER AD_users
      Trigger delete = new Trigger(TAB, Trigger.Type.AFTER_DELETE);
      delete.addInsert(PREV, UID, NAME1, NAME2, SERIAL, ROLE, NBOOKS, IDCARD);
      delete.addValues("OLD." + UID, "OLD." + NAME1, "OLD." + NAME2, "OLD." + SERIAL, "OLD." + ROLE, "OLD." + NBOOKS,
            "OLD." + IDCARD);
      delete.create(db);
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
   public static User insert(Role role, String name1, String name2, int idcard) {
      if (role == Role.PUPIL) {
         throw new IllegalArgumentException("role PUPIL not allowed");
      }
      return insert(name1, name2, 0, role, 5, idcard);
   }

   @NonNull
   public static ArrayList<User> insertPupils(String name1, String name2, @NonNull List<Integer> idcards) {
      try (SQLite.Transaction transaction = new SQLite.Transaction()) {
         int serial = getNextAvailableSerial(name1, name2);
         ArrayList<User> users = new ArrayList<>(idcards.size());
         for (int idcard : idcards) {
            users.add(insert(name1, name2, serial++, Role.PUPIL, 1, idcard));
         }
         transaction.setSuccessful();
         return users;
      }
   }

   @NonNull
   private static User insert(String name1, String name2, int serial, Role role, int nbooks, int idcard) {
      User user = new User().setName1(name1).setName2(name2).setSerial(serial);
      return user.setRole(role).setNbooks(nbooks).setIdcard(idcard).insert();
   }

   @NonNull
   @Override
   protected User insert() {
      try (SQLite.Transaction transaction = new SQLite.Transaction()) {
         setNonNull(UID, SQLite.insert(IDS, new Values().add(OID)));
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
      List<User> list = SQLite.get(User.class, TAB, TAB_COLUMNS, null, null, where, args);
      return (list.size() == 0) ? null : list.get(0);
   }

   /**
    * Returns {@link #get(String, Object...) User.get("uid=?", uid)}.
    */
   @Nullable
   public static User getNullable(long uid) {
      return User.get(App.format("%s=?", UID), uid);
   }

   /**
    * Returns {@link #get(String, Object...) User.get("uid=?", uid)}
    * or throws a {@link RuntimeException} if {@link #get(String, Object...)} returns {@code null}.
    *
    * @throws RuntimeException
    *       if {@link #get(String, Object...)} returns {@code null}.
    */
   @NonNull
   public static User getNonNull(long uid) {
      User user = User.getNullable(uid);
      if (user == null) { throw new RuntimeException("no user with uid " + uid); }
      return user;
   }

   /**
    * Returns {@link #get(String, Object...) User.get("name1=? AND name2=? AND serial=?", name1, name2, serial)}.
    */
   @Nullable
   public static User get(String name1, String name2, int serial) {
      String where = App.format("%s=? AND %s=? AND %s=?", NAME1, NAME2, SERIAL);
      return User.get(where, name1, name2, serial);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Returns a list of all users, ordered by {@code role}, {@code name2}, {@code name1} and {@code serial}.
    * The ordering of the roles is 'admin', 'tutor' and 'pupil' (not alphabetically).
    *
    * @return a list of all users, ordered by {@code role}, {@code name2}, {@code name1} and {@code serial}.
    */
   @NonNull
   public static ArrayList<User> get() {
      String order = App.format("(CASE %s WHEN '%s' THEN 0 WHEN '%s' THEN 1 ELSE 2 END), %s, %s, %s",
            ROLE, ADMIN, TUTOR, NAME2, NAME1, SERIAL);
      return SQLite.get(User.class, TAB, TAB_COLUMNS, null, order, null);
   }

   /**
    * Returns a list of all pupils, ordered by {@code name2}, {@code name1} and {@code serial}.
    *
    * @return a list of all pupils, ordered by {@code name2}, {@code name1} and {@code serial}.
    */
   @NonNull
   public static ArrayList<User> getPupils() {
      // SELECT <TAB_COLUMNS> FROM users WHERE role='pupil' ORDER BY name2, name1, serial ;
      String order = App.format("%s, %s, %s", NAME2, NAME1, SERIAL);
      return SQLite.get(User.class, TAB, TAB_COLUMNS, null, order, App.format("%s=?", ROLE), PUPIL);
   }

   /**
    * Returns a list of pupils grouped and ordered by column {@code name1},
    * where values are only assigned for column {@code _id} and {@code name1}.
    * This method will be called e. g. to populate lists in {@link ScannerAwareEditText}s.
    *
    * @return a list of pupils grouped and ordered by column {@code name1}.
    */
   @NonNull
   public static ArrayList<User> getPupilsName1() {
      // SELECT _id, name1 FROM users WHERE role='pupil' GROUP BY name1 ORDER BY name1 ;
      Values columns = new Values().add(OID).add(NAME1);
      return SQLite.get(User.class, TAB, columns, NAME1, NAME1, App.format("%s=?", ROLE), PUPIL);
   }

   /**
    * Returns the number of pupils in the specified school class.
    */
   public static int countPupils(String name1, String name2) {
      // SELECT COUNT(*) FROM users WHERE name1='$name1' AND name2='$name2' AND role='pupil' ;
      String where = App.format("%s=? AND %s=? AND %s=?", NAME1, NAME2, ROLE);
      return Integer.parseInt(SQLite.getFromQuery(TAB, "COUNT(*)", "0", where, name1, name2, PUPIL));
   }

   /**
    * Returns the next available serial for the specified school class.
    */
   public static int getNextAvailableSerial(String name1, String name2) {
      // If we would search for MAX(serial) in table 'users', and the last inserted pupil would have
      // been deleted, then its serial number would be reused, but that's erroneous! So we'll have
      // to search for MAX(serial) in the history table 'prev_users' where deleted pupils still exist.

      // SELECT MAX(serial) FROM prev_users WHERE name1='$name1' AND name2='$name2' AND role='pupil' ;
      String column = App.format("MAX(%s)", SERIAL);
      String where = App.format("%s=? AND %s=? AND %s=?", NAME1, NAME2, ROLE);
      return 1 + Integer.parseInt(SQLite.getFromQuery(PREV, column, "0", where, name1, name2, PUPIL));
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Subquery that selects the oldest row of every pupil in prev_users.
    * That's the row that was inserted in table prev_users when the pupil was inserted in table users.
    * (SELECT name1, name2, serial, idcard, DATE(tstamp) AS tstamp, MIN(_id)
    * FROM prev_users WHERE role='pupil' GROUP BY uid)
    */
   private static final String OLDEST_PUPIL_ROWS = App.format(
         "(SELECT %1$s, %2$s, %3$s, %4$s, DATE(%5$s) AS %5$s, MIN(%6$s) FROM %7$s WHERE %8$s='%9$s' GROUP BY %10$s)",
         NAME1, NAME2, SERIAL, IDCARD, TSTAMP, OID, PREV, ROLE, PUPIL, UID);

   /**
    * Returns the pupils list that will be print as a PDF document, sorted by {@code serial}.
    * Only the methods {@link #getSerial()} and {@link #getIdcard()}
    * are permitted to be called for the returned {@code User} objects.
    * This method will be called e. g. by {@link PupilList}.
    */
   @NonNull
   public static ArrayList<User> getPupilList(String name1, String name2, String date) {
      // SELECT serial, idcard FROM (<OLDEST-PUPIL-ROWS>)
      //    WHERE name1='$name1' AND name2='$name2' AND tstamp='$date' ORDER BY serial ;
      String where = App.format("%s=? AND %s=? AND %s=?", NAME1, NAME2, TSTAMP);
      Values columns = new Values().add(SERIAL).add(IDCARD);
      return SQLite.get(User.class, OLDEST_PUPIL_ROWS, columns, null, SERIAL, where, name1, name2, date);
   }

   @NonNull
   private static ArrayList<User> getInsertPupilsEvents(
         Values columns, String group, String order, String where, Object... args) {
      String minSerial = App.format("MIN(%s) AS %s", SERIAL, MIN_SERIAL);
      String maxSerial = App.format("MAX(%s) AS %s", SERIAL, MAX_SERIAL);
      columns.add(minSerial).add(maxSerial).add(TSTAMP);
      return SQLite.get(User.class, OLDEST_PUPIL_ROWS, columns, group, order, where, args);
   }

   /**
    * Returns the insert-pupils-events for the specified school class.
    * Only the methods {@link #getMinSerial()}, {@link #getMaxSerial()}
    * and {@link #getTstamp()} are permitted to be called for the returned {@code User} objects.
    * This method will be called e. g. by {@link PupilList} to determine which version of pupil list must be printed.
    */
   @NonNull
   public static ArrayList<User> getInsertPupilsEvents(String name1, String name2) {
      // SELECT MIN(serial) AS minSerial, MAX(serial) AS maxSerial, tstamp FROM (<OLDEST-PUPIL-ROWS>)
      //    WHERE name1='$name1' AND name2='$name2' GROUP BY tstamp ORDER BY tstamp ;
      String where = App.format("%s=? AND %s=?", NAME1, NAME2);
      return getInsertPupilsEvents(new Values(), TSTAMP, TSTAMP, where, name1, name2);
   }

   /**
    * Returns all insert-pupils-events sorted by date (descending), school year and class name.
    * Only the methods {@link #getName1()}, {@link #getName2()}, {@link #getMinSerial()}, {@link #getMaxSerial()}
    * and {@link #getTstamp()} are permitted to be called for the returned {@code User} objects.
    * This method will be called to present all such events in a list when a pupil list should be reprinted.
    */
   @NonNull
   public static ArrayList<User> getInsertPupilsEvents() {
      // SELECT name1, name2, MIN(serial) AS minSerial, MAX(serial) AS maxSerial, tstamp FROM (<OLDEST-PUPIL-ROWS>)
      //    GROUP BY tstamp, name2, name1 ORDER BY tstamp DESC, name2, name1
      String group = App.format("%s, %s, %s", TSTAMP, NAME2, NAME1);
      String order = App.format("%s DESC, %s, %s", TSTAMP, NAME2, NAME1);
      return getInsertPupilsEvents(new Values().add(NAME1).add(NAME2), group, order, null);
   }

   /* ============================================================================================================== */

   @NonNull
   @Override
   protected String getTable() { return TAB; }

   public long getUid() {
      return values.getLong(UID);
   }

   @NonNull
   public String getName1() {
      return values.getNonNull(NAME1);
   }

   /**
    * Attribute {@code name1} cannot be changed after creation for security reasons.
    */
   @NonNull
   private User setName1(@NonNull String name1) {
      return (User) setNonNull(NAME1, name1);
   }

   @NonNull
   public String getName2() {
      return values.getNonNull(NAME2);
   }

   /**
    * Attribute {@code name2} cannot be changed after creation for security reasons.
    */
   @NonNull
   private User setName2(@NonNull String name2) {
      return (User) setNonNull(NAME2, name2);
   }

   public int getSerial() {
      return values.getInt(SERIAL);
   }

   /**
    * Attribute {@code serial} cannot be changed after creation for security reasons.
    */
   @NonNull
   private User setSerial(int serial) {
      return (User) setNonNull(SERIAL, serial);
   }

   @NonNull
   public Role getRole() {
      return Role.getEnum(values.getNonNull(ROLE));
   }

   @NonNull
   public User setRole(@NonNull Role role) {
      return (User) setNonNull(ROLE, role.value);
   }

   public int getNbooks() {
      return values.getInt(NBOOKS);
   }

   @NonNull
   public User setNbooks(int nbooks) {
      return (User) setNonNull(NBOOKS, nbooks);
   }

   public int getIdcard() {
      return values.getInt(IDCARD);
   }

   @NonNull
   public User setIdcard(int idcard) {
      return (User) setNonNull(IDCARD, idcard);
   }

   @NonNull
   public String getTstamp() {
      return values.getNonNull(TSTAMP);
   }

   public int getMinSerial() {
      return values.getInt(MIN_SERIAL);
   }

   public int getMaxSerial() {
      return values.getInt(MAX_SERIAL);
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