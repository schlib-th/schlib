/*
 * AdminUsersAddActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.support.annotation.NonNull;


import de.fahimu.android.app.Log;
import de.fahimu.schlib.db.User.Role;

/**
 * An activity for adding users to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersAddActivity extends StepperActivity {

   Role   role  = null;
   int    count = 0;
   String name1 = "", name2 = "";

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   protected int getContentViewId() {
      return R.layout.admin_users_add;
   }

   @NonNull
   @Override
   StepFragment getFirstFragment() {
      return firstFragment;
   }

   final AdminUsersAddStep0 firstFragment = new AdminUsersAddStep0();

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

}