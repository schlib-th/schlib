/*
 * AdminUsersAddStep2b.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.Nullable;


import de.fahimu.android.app.Log;

/**
 * Step 2b of adding users to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersAddStep2b extends StepFragment<AdminUsersAddActivity> {

   @Override
   protected int getContentViewId() {
      return R.layout.admin_users_add_step_2b;
   }

   @Override
   int getTabNameId() {
      return R.string.admin_users_add_step_2_label;
   }

   @Nullable
   @Override
   StepFragment getNext() {
      return null;
   }

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