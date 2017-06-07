/*
 * BackupDatabase.java
 *
 * Copyright 2015 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.db;

import android.os.AsyncTask;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;
import de.fahimu.android.app.TaskRegistry;
import de.fahimu.android.share.ExternalFile;
import de.fahimu.android.share.ExternalOutputStream;

/**
 * An AsyncTask that backups the database file.
 */
public final class BackupDatabase extends AsyncTask<Void,Void,Void> {

   private final TaskRegistry taskRegistry = new TaskRegistry();

   private final ExternalFile.Type type;

   public BackupDatabase(ExternalFile.Type type) { this.type = type; }

   /**
    * Execute this task.
    */
   public final void execute() { taskRegistry.add(this); }

   @Override
   protected Void doInBackground(Void... voids) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         String backupFilename = App.formatDate("'database.'yyyyMMddHHmmss'.sqlite3.gzip'", true, App.posixTime());
         ExternalFile file = new ExternalFile(type, backupFilename);
         try (InputStream is = new FileInputStream(App.getDb().getPath());
              OutputStream os = new GZIPOutputStream(ExternalOutputStream.newInstance(file))) {
            byte[] buffer = new byte[8192];
            for (int length; (length = is.read(buffer)) > 0; ) {
               os.write(buffer, 0, length);
            }
         } catch (IOException ioe) {
            scope.d("****** " + ioe.getMessage());
         }
         taskRegistry.remove(this);
         return null;
      }
   }

}