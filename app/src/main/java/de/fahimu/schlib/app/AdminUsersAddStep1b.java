/*
 * AdminUsersAddStep1b.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.Nullable;


import de.fahimu.android.app.Log;

/**
 * Step 1b of adding users to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersAddStep1b extends StepFragment<AdminUsersAddActivity> {

   @Override
   protected int getContentViewId() {
      return R.layout.admin_users_add_step_1b;
   }

   @Override
   int getTabNameId() {
      return R.string.admin_users_add_step_1b_label;
   }

   @Nullable
   @Override
   StepFragment getNext() {
      return nextFragment;
   }

   final AdminUsersAddStep2b nextFragment = new AdminUsersAddStep2b();

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);
      }
   }

   @Override
   public void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         // clear model
         super.onResume();
      }
   }

   @Override
   public void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   boolean isDoneEnabled() { return true; }

   @Override
   boolean onDoneClicked() { return true; }

   @Override
   void updateModel() {
      stepperActivity.refreshGUI();
   }

}