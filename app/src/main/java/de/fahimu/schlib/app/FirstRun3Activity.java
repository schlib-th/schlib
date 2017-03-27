/*
 * FirstRun3Activity.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.content.Intent;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import de.fahimu.android.app.Log;
import de.fahimu.android.app.TaskRegistry;
import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.android.app.scanner.NoFocusDialog.ButtonListener;
import de.fahimu.android.db.BackupDatabase;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.share.ExternalFile;
import de.fahimu.android.share.ExternalInputStream;
import de.fahimu.android.share.ExternalOutputStream;
import de.fahimu.schlib.anw.CSVParser;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.anw.StringType;
import de.fahimu.schlib.db.Book;
import de.fahimu.schlib.db.Preference;
import de.fahimu.schlib.db.Use;
import de.fahimu.schlib.share.FileType;

import static de.fahimu.schlib.anw.StringType.AUTHOR;
import static de.fahimu.schlib.anw.StringType.KEYWORDS;
import static de.fahimu.schlib.anw.StringType.NUMBER;
import static de.fahimu.schlib.anw.StringType.PUBLISHER;
import static de.fahimu.schlib.anw.StringType.SHELF;
import static de.fahimu.schlib.anw.StringType.STOCKED;
import static de.fahimu.schlib.anw.StringType.TITLE;

/**
 * Assign Idcard to first user and optionally import CSV files into table {@code books}.
 * Continue with FirstRun4Activity if books where imported, else with LoginActivity.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class FirstRun3Activity extends SchlibActivity {

   private Button   importCSV;
   private Button   gotoLogin;
   private TextView explanation;

   private final TaskRegistry taskRegistry = new TaskRegistry();

   @Override
   protected int getContentViewId() { return R.layout.first_run_3; }

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      importCSV = findView(Button.class, R.id.first_run_3_importCSV);
      gotoLogin = findView(Button.class, R.id.first_run_3_gotoLogin);
      explanation = findView(TextView.class, R.id.first_run_3_explanation);
   }

   @Override
   boolean isHomeShownAsUp() { return false; }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) { return false; }

   @Override
   protected void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();

         int idcard = Use.getLoggedInNonNull().getUser().getIdcard();
         String serialNumber = new SerialNumber(idcard).getDisplay();
         explanation.setText(getString(R.string.first_run_3_explanation, serialNumber, ""));
      }
   }

   @Override
   protected void onPermissionGranted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         new BackupDatabase(FileType.BACKUP).execute();
         ExternalFile importDir = createImportDir();

         int idcard = Use.getLoggedInNonNull().getUser().getIdcard();
         String serialNumber = new SerialNumber(idcard).getDisplay();
         explanation.setText(getString(R.string.first_run_3_explanation, serialNumber, importDir.getName()));

         importCSV.setEnabled(true);
         gotoLogin.setEnabled(allBooksHaveScanId() && importDir.listNames("csv").length == 0);
      }
   }

   @Override
   protected void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
         taskRegistry.cancel();
      }
   }

   @Override
   public void onBackPressed() { /* IGNORE back button*/ }

   public void onImportCSVClicked(View view) {
      gotoLogin.setEnabled(false);
      importCSV.setEnabled(false);
      taskRegistry.add(new ImportCSVFiles());
   }

   public void onGotoLoginClicked(View view) {
      NoFocusDialog dialog = new NoFocusDialog(this);
      dialog.setMessage(R.string.dialog_message_first_run_3_continue_with_login);
      dialog.setButton0(R.string.app_no, null);
      dialog.setButton1(R.string.app_yes, new ButtonListener() {
         @Override
         public void onClick() { gotoLogin(); }
      }).show();
   }

   private void gotoLogin() {
      createImportDir().delete();
      Use.getLoggedInNonNull().setLogoutToNow().update();
      Preference.getNonNull(Preference.FIRST_RUN).delete();     // first run is done
      new BackupDatabase(FileType.BACKUP).execute();
      startActivity(new Intent(this, LoginActivity.class));
      finish();
   }

   /**
    * Creates the IMPORT directory and a file therein with the name 'Please copy csv files herein'.
    *
    * @return the IMPORT directory.
    */
   private ExternalFile createImportDir() {
      String copyCSVFiles = getString(R.string.first_run_3_copy_csv_files) + ".txt";
      ExternalOutputStream.newInstance(new ExternalFile(FileType.IMPORT, copyCSVFiles)).close();
      return new ExternalFile(FileType.IMPORT, null);
   }

   /**
    * Returns true if at least one book in the database has no assigned ISBN or label.
    *
    * @return true if at least one book in the database has no assigned ISBN or label.
    */
   private boolean allBooksHaveScanId() {
      return Book.countNoScanId() == 0;
   }

   /* ============================================================================================================== */

   /**
    * AsyncTask that imports the .csv files and inserts the books.
    */
   private final class ImportCSVFiles extends AsyncTask<Void,Void,Integer> {

      @Override
      protected Integer doInBackground(Void... voids) {
         try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
            String[] csvFiles = createImportDir().listNames("csv");
            for (String file : csvFiles) {
               parseCSVFile(file);
               if (isCancelled()) { break; }
            }
            taskRegistry.remove(this);
            return csvFiles.length;
         }
      }

      @Override
      protected void onCancelled(Integer nCSVFiles) { onEnd(nCSVFiles); }

      @Override
      protected void onPostExecute(Integer nCSVFiles) { onEnd(nCSVFiles); }

      private void onEnd(int nCSVFiles) {
         if (!allBooksHaveScanId()) {
            startActivity(new Intent(FirstRun3Activity.this, FirstRun4Activity.class));
            gotoLogin.setEnabled(false);
         } else if (nCSVFiles == 0) {
            NoFocusDialog dialog = new NoFocusDialog(FirstRun3Activity.this);
            dialog.setMessage(R.string.dialog_message_first_run_3_no_csv_files, createImportDir().getName());
            dialog.show(R.raw.horn);
            gotoLogin.setEnabled(true);
         } else {
            String[] csvFiles = createImportDir().listNames("csv");
            if (csvFiles.length > 0) {
               NoFocusDialog dialog = new NoFocusDialog(FirstRun3Activity.this);
               dialog.setMessage(R.string.dialog_message_first_run_3_file_corrupted,
                     createImportDir().getName(), csvFiles[0], csvFiles[0].replace(".csv", ".txt"));
               dialog.show(R.raw.horn);
            }
            gotoLogin.setEnabled(csvFiles.length == 0);
         }
         importCSV.setEnabled(true);
      }

   }

   private static final StringType[] TYPES = { SHELF, NUMBER, TITLE, PUBLISHER, AUTHOR, KEYWORDS, STOCKED };

   /**
    * Parse the specified CSV file in the IMPORT subdirectory and insert the books into the database.
    * If anything goes wrong, rollback.
    * Long running task! Must be called from an {@link android.os.AsyncTask AsyncTask}.
    *
    * @param file
    *       the name of the CSV file to parse.
    */
   private void parseCSVFile(String file) {
      Log.d("processing file " + file);

      ExternalFile csv = new ExternalFile(FileType.IMPORT, file);
      ExternalFile err = new ExternalFile(FileType.IMPORT, file.replace(".csv", ".txt"));
      ExternalInputStream is = ExternalInputStream.newInstance(csv);
      ExternalOutputStream os = ExternalOutputStream.newInstance(err);
      try (SQLite.Transaction transaction = new SQLite.Transaction()) {
         CSVParser parser = new CSVParser(this, is, os, TYPES);
         for (String[] line; (line = parser.readLine()) != null; ) {
            if (line.length == TYPES.length) { insertLine(parser, line); }
         }
         if (parser.isSuccessful()) {
            csv.delete();
            err.delete();
            transaction.setSuccessful();
         }
      }
   }

   private void insertLine(CSVParser parser, String[] line) {
      try {
         Book book = new Book();
         book.setTitle(line[2]).setPublisher(line[3]).setAuthor(line[4]).setKeywords(line[5]);

         String date = line[6].substring(6) + "-" + line[6].substring(3, 5) + "-" + line[6].substring(0, 2);
         book.setStocked(SQLite.getFromRawQuery("SELECT DATETIME(?,?)", date, "+8 hours"));  // set time to 8 AM UTC

         book.setShelf(line[0]).setNumber(Integer.parseInt(line[1]));
         book.setPeriod(14);

         book.insert();
      } catch (SQLException e) { parser.writeThrowable(e); }
   }

}