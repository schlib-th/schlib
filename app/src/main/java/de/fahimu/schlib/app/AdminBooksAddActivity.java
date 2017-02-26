/*
 * AdminBooksAddActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.support.annotation.NonNull;


import de.fahimu.android.app.Log;

/**
 * An activity for adding books to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminBooksAddActivity extends StepperActivity {

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   protected int getContentViewId() {
      return R.layout.admin_books_add;
   }

   @NonNull
   @Override
   StepFragment getFirstFragment() {
      return firstFragment;
   }

   private final AdminUsersAddStep0 firstFragment = new AdminUsersAddStep0();

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   protected void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
      }
   }

   @Override
   protected void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   void finishActivity() { }

}