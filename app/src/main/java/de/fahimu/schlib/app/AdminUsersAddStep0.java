/*
 * AdminUsersAddStep0.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.RadioGroup;


import de.fahimu.android.app.Log;
import de.fahimu.schlib.db.User.Role;

/**
 * Step 0 of adding users to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersAddStep0 extends StepFragment<AdminUsersAddActivity> {

   @Override
   int getContentViewId() {
      return R.layout.admin_users_add_step_0;
   }

   @Override
   int getTabNameId() {
      return R.string.admin_users_add_step_0_label;
   }

   @Nullable
   @Override
   StepFragment getNext() {
      if (stepperActivity == null) {
         return nextFragmentA;
      } else if (getActivity(AdminUsersAddActivity.class).role != Role.PUPIL) {
         return nextFragmentA;
      } else {
         return nextFragmentB;
      }
   }

   final AdminUsersAddStep1a nextFragmentA = new AdminUsersAddStep1a();
   final AdminUsersAddStep1b nextFragmentB = new AdminUsersAddStep1b();

   private RadioGroup group;

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);
         group = findView(RadioGroup.class, R.id.admin_users_add_step_0_group);
      }
   }

   @Override
   public void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         group.clearCheck();
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
   boolean isDoneEnabled() {
      return getActivity(AdminUsersAddActivity.class).role != null;
   }

   @Override
   boolean onDoneClicked() {
      return getActivity(AdminUsersAddActivity.class).role != null;
   }

   @Override
   void updateModel() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         AdminUsersAddActivity activity = getActivity(AdminUsersAddActivity.class);
         activity.role = null;
         switch (group.getCheckedRadioButtonId()) {
         case R.id.admin_users_add_step_0_admin: activity.role = Role.ADMIN; break;
         case R.id.admin_users_add_step_0_tutor: activity.role = Role.TUTOR; break;
         case R.id.admin_users_add_step_0_pupil: activity.role = Role.PUPIL; break;
         }
         stepperActivity.refreshGUI();
      }
   }

}