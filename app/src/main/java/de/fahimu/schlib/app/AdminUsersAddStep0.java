/*
 * AdminUsersAddStep0.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;


import de.fahimu.android.app.Log;
import de.fahimu.schlib.db.User.Role;

/**
 * Step 0 of adding users to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersAddStep0 extends StepFragment {

   @Override
   StepFragment getNext() {
      AdminUsersAddActivity activity = (AdminUsersAddActivity) stepperActivity;
      return (activity == null || activity.role != Role.PUPIL) ? nextFragmentA : nextFragmentB;
   }

   private final AdminUsersAddStep1a nextFragmentA = new AdminUsersAddStep1a();
   private final AdminUsersAddStep1b nextFragmentB = new AdminUsersAddStep1b();

   @Override
   int getTabNameId() { return R.string.admin_users_add_step_0_label; }

   /* ============================================================================================================== */

   @Override
   int getContentViewId() { return R.layout.admin_users_add_step_0; }

   private RadioGroup group;

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);
         group = findView(RadioGroup.class, R.id.admin_users_add_step_0_group);

         activity = (AdminUsersAddActivity) stepperActivity;

         group.setOnCheckedChangeListener(new RadioGroupListener());
      }
   }

   private AdminUsersAddActivity activity;

   /* -------------------------------------------------------------------------------------------------------------- */

   final class RadioGroupListener implements OnCheckedChangeListener {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
         activity.role = null;
         switch (checkedId) {
         case R.id.admin_users_add_step_0_admin: activity.role = Role.ADMIN; break;
         case R.id.admin_users_add_step_0_tutor: activity.role = Role.TUTOR; break;
         case R.id.admin_users_add_step_0_pupil: activity.role = Role.PUPIL; break;
         }
         activity.refreshGUI();
      }
   }

   /* ============================================================================================================== */

   @Override
   public void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
         group.clearCheck();
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
      return activity.role != null;
   }

   @Override
   boolean isDone() {
      return true;
   }

}