/*
 * MainActivity.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.view.Menu;


import de.fahimu.android.app.Log;
import de.fahimu.android.app.TaskRegistry;
import de.fahimu.schlib.db.Preference;

/**
 * The first visible activity after starting the app.
 * Initialize database in AsyncTask and decide with which Activity to continue.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class MainActivity extends SchlibActivity {

   private final class InitDatabaseAndReadFirstRun extends AsyncTask<Void,Void,Integer> {
      @Override
      @NonNull
      protected Integer doInBackground(Void... voids) {
         try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
            long start = System.currentTimeMillis();
            Preference firstRun = Preference.getNullable(Preference.FIRST_RUN);

            // The activity should be visible for at least one second before we continue
            long sleep = start + 1000 - System.currentTimeMillis();
            Log.d("sleep=" + sleep);
            if (sleep > 0) { try { Thread.sleep(sleep); } catch (InterruptedException e) { /* IGNORE */ } }
            return (firstRun == null) ? 0 : Integer.parseInt(firstRun.getValue());
         }
      }

      @Override
      protected void onPostExecute(@NonNull Integer firstRun) {
         Class<? extends SchlibActivity> schlibActivity;
         switch (firstRun) {
         case 1: schlibActivity = FirstRun1Activity.class; break;
         case 2: schlibActivity = RegisterPrintsActivity.class; break;
         case 3: schlibActivity = FirstRun3Activity.class; break;
         default: schlibActivity = LoginActivity.class;
         }
         startActivity(new Intent(MainActivity.this, schlibActivity));
         MainActivity.this.finish();
      }
   }

   /* ============================================================================================================== */

   private final TaskRegistry taskRegistry = new TaskRegistry();

   @Override
   protected int getContentViewId() { return R.layout.main; }

   @Override
   boolean isHomeShownAsUp() { return false; }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) { return false; }

   @Override
   protected void onPermissionGranted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         taskRegistry.add(new InitDatabaseAndReadFirstRun());
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

}