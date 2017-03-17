/*
 * OpenHelper.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;
import de.fahimu.android.share.ExternalFile;
import de.fahimu.android.share.ExternalInputStream;
import de.fahimu.schlib.share.FileType;

/**
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class OpenHelper extends SQLiteOpenHelper {

   public OpenHelper() {
      super(App.getInstance(), "database", null, 1);
      restoreDatabaseFile();
   }

   private static final String RESTORE_FILENAME = "restore.sqlite3.gzip";

   /**
    * If there is a file named "restore.sqlite3.gzip" in the backup directory,
    * unzip and copy it to our database directory as our restored database file.
    */
   private void restoreDatabaseFile() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         String[] backupFiles = new ExternalFile(FileType.BACKUP, null).listNames(null);
         scope.d("backupFiles=" + Arrays.toString(backupFiles));

         if (Arrays.asList(backupFiles).contains(RESTORE_FILENAME)) {
            try {
               File databasePath = App.getInstance().getDatabasePath(getDatabaseName()).getParentFile();
               for (String filename : databasePath.list()) {
                  if (!new File(databasePath, filename).delete()) {
                     throw new IOException("delete of file '" + filename + "' failed");
                  }
               }
               ExternalFile restoreFile = new ExternalFile(FileType.BACKUP, RESTORE_FILENAME);
               try (InputStream is = new GZIPInputStream(ExternalInputStream.newInstance(restoreFile));
                    OutputStream os = new FileOutputStream(new File(databasePath, getDatabaseName()))) {
                  byte[] buffer = new byte[8192];
                  for (int length; (length = is.read(buffer)) > 0; ) {
                     os.write(buffer, 0, length);
                  }
                  restoreFile.delete();
               }
            } catch (IOException ioe) {
               scope.d("****** " + ioe.getMessage());
            }
         }
      }
   }

   @Override
   public void onConfigure(SQLiteDatabase db) {
      Log.d(db.getPath() + (db.isReadOnly() ? " - read" : " - read/write"));
      db.setForeignKeyConstraintsEnabled(true);
   }

   @Override
   public void onCreate(SQLiteDatabase db) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         Idcard.create(db);
         Label.create(db);
         User.create(db);
         Book.create(db);
         Use.create(db);
         Lending.create(db);
         Preference.create(db);
      }
   }

   @Override
   public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.d("newVersion=" + newVersion);
      throw new IllegalArgumentException("newVersion=" + newVersion);
   }

   @Override
   public void onOpen(SQLiteDatabase db) {
      Log.d(db.getPath() + (db.isReadOnly() ? " - read" : " - read/write"));
   }

}