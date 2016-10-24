/*
 * FirstRun1Activity.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.View;


import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.android.app.scanner.NoFocusDialog.ButtonListener;
import de.fahimu.android.app.scanner.ScannerAwareEditText;
import de.fahimu.android.db.SQLite;
import de.fahimu.android.share.ExternalFile;
import de.fahimu.schlib.anw.IntCipher;
import de.fahimu.schlib.anw.StringType;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.Label;
import de.fahimu.schlib.db.Preference;
import de.fahimu.schlib.db.Use;
import de.fahimu.schlib.db.User;

/**
 * Ask for first and last name, create admin user and login user.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class FirstRun1Activity extends SchlibActivity {

   private ScannerAwareEditText name1, name2;

   @Override
   protected int getContentViewId() { return R.layout.first_run_1; }

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onCreate(savedInstanceState);
         setDisplayHomeAsUpEnabled(false);
         name1 = findView(ScannerAwareEditText.class, R.id.first_run_1_name1);
         name1.setScannerActivity(this);
         name2 = findView(ScannerAwareEditText.class, R.id.first_run_1_name2);
         name2.setScannerActivity(this);
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) { return false; }

   @Override
   protected void onPermissionGranted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         ExternalFile.deleteRoot();
      }
   }

   @Override
   public void onBackPressed() { /* ignore back button */ }

   private boolean noInput = true;

   private String checkName(StringType stringType, String name) {
      int failPosition = stringType.matches(name);
      if (failPosition >= name.length()) {
         return getString(R.string.first_run_1_help_2);
      } else {
         return (failPosition < 0) ? null : getString(R.string.first_run_1_help_3, name.charAt(failPosition));
      }
   }

   public void onDoneClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         final String name1 = this.name1.getText().toString().trim();
         final String name2 = this.name2.getText().toString().trim();
         String message;

         if (name1.isEmpty() && name2.isEmpty() && noInput) {
            this.name1.requestFocus();
            this.name1.setError(getString(R.string.first_run_1_help_1));
         } else if ((message = checkName(StringType.NAME1, name1)) != null) {
            noInput = false;
            this.name1.requestFocus();
            this.name1.setError(message);
         } else if ((message = checkName(StringType.NAME2, name2)) != null) {
            noInput = false;
            this.name2.requestFocus();
            this.name2.setError(message);
         } else {
            NoFocusDialog dialog = new NoFocusDialog(this, NoFocusDialog.IGNORE_CANCEL);
            dialog.setTitle(R.string.first_run_1_dialog_title);
            dialog.setMessage(R.string.first_run_1_dialog_message);
            dialog.setPositiveButton(R.string.app_continue, new ButtonListener() {
               @Override
               public void onClick() { prepareDatabaseAndContinue(name1, name2); }
            }).show();

         }
      }
   }

   private void prepareDatabaseAndContinue(String name1, String name2) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         try (SQLite.Transaction transaction = new SQLite.Transaction()) {
            Idcard.createOnePage();
            for (int i = 0; i < 3; i++) {
               Label.createOnePage();
            }
            User user = User.insertAdmin(name1, name2, Idcard.FIRST_FREE_IDCARD);
            Use.login(user);
            Preference.insert(Preference.CIPHER_KEY, IntCipher.createKey());
            Preference.getNonNull(Preference.FIRST_RUN).setValue("2").update();
            transaction.setSuccessful();
         }
         startActivity(new Intent(this, RegisterPrintsActivity.class));
         finish();
      }
   }

}