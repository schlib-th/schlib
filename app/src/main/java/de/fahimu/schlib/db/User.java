/*
 * User.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import java.util.ArrayList;
import java.util.List;

import de.fahimu.android.db.Row;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.db.Table;
import de.fahimu.android.db.Trigger;
import de.fahimu.android.db.Values;
import de.fahimu.schlib.app.App;
import de.fahimu.schlib.app.R;

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

   private static final String IDS  = "uids";
   static final         String TAB  = "users";
   private static final String PREV = "prev_users";

   private static final String OID    = BaseColumns._ID;
   static final         String UID    = "uid";
   private static final String NAME1  = "name1";
   private static final String NAME2  = "name2";
   private static final String SERIAL = "serial";
   private static final String ROLE   = "role";
   private static final String NBOOKS = "nbooks";
   static final         String IDCARD = "idcard";
   private static final String TSTAMP = "tstamp";

   private static final String ADMIN = "admin";
   private static final String TUTOR = "tutor";
   private static final String PUPIL = "pupil";

   private static final Values TAB_COLUMNS = new Values().add(new String[] {
         SQLite.alias(TAB, OID, OID),
         UID, NAME1, NAME2, SERIAL, ROLE, NBOOKS, IDCARD
   });

   private static final String SORT    = "sort";
   private static final String IDCARDS = "idcards";

   private static final Values EXT_COLUMNS = TAB_COLUMNS
         .add(App.format("group_concat(%s) AS %s", IDCARD, IDCARDS))
         .add(App.format("(CASE %s WHEN '%s' THEN 0 WHEN '%s' THEN 1 ELSE 2 END) AS %s", ROLE, ADMIN, TUTOR, SORT));

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
      ADMIN(User.ADMIN), TUTOR(User.TUTOR), PUPIL(User.PUPIL);

      @NonNull
      private final String value;

      Role(@NonNull String value) { this.value = value; }

      @NonNull
      static Role getEnum(@NonNull String value) {
         if (PUPIL.value.equals(value)) { return PUPIL; }
         if (TUTOR.value.equals(value)) { return TUTOR; }
         if (ADMIN.value.equals(value)) { return ADMIN; }
         throw new IllegalArgumentException("value=" + value);
      }
   }

   /* ============================================================================================================== */

   public static User insertAdmin(String name1, String name2, Integer idcard) {
      return insert(name1, name2, 0, Role.ADMIN, 5, idcard);
   }

   public static User insertTutor(String name1, String name2, Integer idcard) {
      return insert(name1, name2, 0, Role.TUTOR, 5, idcard);
   }

   public static synchronized User insertPupil(String name1, String name2, Integer idcard) {
      // Must be static synchronized to ensure that the calculation of MAX(serial) works properly.
      String column = App.format("MAX(%s)", SERIAL);
      String where = App.format("%s=? AND %s=? AND %s=?", NAME1, NAME2, ROLE);
      String maxSerial = SQLite.getFromQuery(TAB, column, "0", where, name1, name2, PUPIL);
      return insert(name1, name2, 1 + Integer.parseInt(maxSerial), Role.PUPIL, 1, idcard);
   }

   private static User insert(String name1, String name2, int serial, Role role, int nbooks, Integer idcard) {
      User user = new User().setName1(name1).setName2(name2).setSerial(serial);
      return user.setRole(role).setNbooks(nbooks).setIdcard(idcard).insert();
   }

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

   @Nullable
   public static User getNullable(long uid) {
      return User.get(App.format("%s=?", UID), uid);
   }

   @NonNull
   public static User getNonNull(long uid) {
      User user = User.getNullable(uid);
      if (user == null) { throw new RuntimeException("no user with uid " + uid); }
      return user;
   }

   /**
    * Returns a list of all users, grouped and ordered by {@code sort}, {@code name2} and {@code name1}.
    * The columns returned are defined by {@link #EXT_COLUMNS}, including {@code sort} and {@code idcards}.
    *
    * @return a list of all users, grouped and ordered by {@code sort}, {@code name2} and {@code name1}.
    */
   @NonNull
   public static ArrayList<User> getGrouped() {
      // SELECT <EXT_COLUMNS> FROM users GROUP BY sort, name2, name1 ORDER BY sort, name2, name1 ;
      String order = App.format("%s, %s, %s", SORT, NAME2, NAME1);
      return SQLite.get(User.class, TAB, EXT_COLUMNS, order, order, null);
   }

   /* ============================================================================================================== */

   private User() { super(); }

   /**
    * Creates a new {@code User} that initially contains the column values from the specified cursor {@code c}.
    *
    * @param cursor
    *       the cursor.
    */
   @SuppressWarnings ("unused")
   public User(Cursor cursor) { super(cursor); }

   @Override
   protected String getTable() { return TAB; }

   /* ============================================================================================================== */

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
   private User setName2(@NonNull String name2) {
      return (User) setNonNull(NAME2, name2);
   }

   public int getSerial() {
      return values.getInt(SERIAL);
   }

   /**
    * Attribute {@code serial} cannot be changed after creation for security reasons.
    */
   private User setSerial(int serial) {
      return (User) setNonNull(SERIAL, serial);
   }

   @NonNull
   public Role getRole() {
      return Role.getEnum(values.getNonNull(ROLE));
   }

   public User setRole(@NonNull Role role) {
      return (User) setNonNull(ROLE, role.value);
   }

   public int getNbooks() {
      return values.getInt(NBOOKS);
   }

   public User setNbooks(int nbooks) {
      return (User) setNonNull(NBOOKS, nbooks);
   }

   public boolean hasIdcard() {
      return values.getNullable(IDCARD) != null;
   }

   /**
    * Returns the number of the idcard that is assigned to this {@link User}.
    * <p> Precondition: {@link #hasIdcard()} must be {@code true}. </p>
    *
    * @return the number of the idcard that is assigned to this {@link User}.
    */
   public int getIdcard() {
      return values.getInt(IDCARD);
   }

   public User setIdcard(@Nullable Integer idcard) {
      return (User) setNullable(IDCARD, idcard);
   }

   /**
    * Returns the value of column {@link #IDCARDS} as a list of integers.
    *
    * @return the value of column {@link #IDCARDS} as a list of integers.
    *
    * @throws RuntimeException
    *       if there is no column {@link #IDCARDS}.
    */
   @NonNull
   public List<Integer> getIdcards() {
      String[] idcards = values.getNonNull(IDCARDS).split(",");
      List<Integer> list = new ArrayList<>(idcards.length);
      for (String idcard : idcards) {
         list.add(Integer.parseInt(idcard));
      }
      return list;
   }

   /* ============================================================================================================== */

   @NonNull
   public String getDisplayRoleName() {
      if (getRole() == Role.PUPIL) {
         return App.getStr(R.string.user_display_class, getName1(), getName2());
      } else {
         return getDisplay();
      }
   }

   @NonNull
   public String getDisplay() {
      if (getRole() == Role.PUPIL) {
         return App.getStr(R.string.user_display_pupil, getSerial(), getName1());
      } else if (getRole() == Role.TUTOR) {
         return App.getStr(R.string.user_display_tutor, getName1(), getName2());
      } else {
         return App.getStr(R.string.user_display_admin, getName1(), getName2());
      }
   }

}