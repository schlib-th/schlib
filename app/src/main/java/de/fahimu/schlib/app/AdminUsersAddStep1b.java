/*
 * AdminUsersAddStep1b.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;


import java.util.ArrayList;

import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.ScannerAwareEditText;
import de.fahimu.android.app.scanner.ScannerAwareEditText.AbstractTextWatcher;
import de.fahimu.android.app.scanner.ScannerAwareEditText.ColumnAdapter;
import de.fahimu.android.app.scanner.ScannerAwareEditText.ColumnItem;
import de.fahimu.schlib.db.User;

/**
 * Step 1b of adding users to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersAddStep1b extends StepFragment {

   @Override
   int getContentViewId() {
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

   private ScannerAwareEditText name1;

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);
         name1 = findView(ScannerAwareEditText.class, R.id.admin_users_add_step_1a_name1);   // TODO 1b

         activity = (AdminUsersAddActivity) stepperActivity;

         name1.setScannerActivity(activity).addTextChangedListener(new TextChangedListener());
         name1.setColumnAdapter(new PupilName1Adapter());
      }
   }

   private AdminUsersAddActivity activity;

   /* -------------------------------------------------------------------------------------------------------------- */

   final class TextChangedListener extends AbstractTextWatcher {
      @Override
      public void afterTextChanged(Editable editable) {
         String text = editable.toString();
         // prevent entering of leading blanks (just because it doesn't make sense)
         if (!text.isEmpty() && text.trim().isEmpty()) { editable.clear(); }
         AdminUsersAddStep1b.this.updateModel();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   final static class PupilName1Item extends ColumnItem {
      PupilName1Item(@NonNull User user) {
         super(user, user.getName1());
      }
   }

   final class PupilName1Adapter extends ColumnAdapter<PupilName1Item> {
      @Override
      protected void loadData(ArrayList<PupilName1Item> data) {
         ArrayList<User> users = User.getPupilsName1();
         for (User user : users) {
            data.add(new PupilName1Item(user));
         }
      }
   }

   /* ============================================================================================================== */

   @Override
   public void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         name1.setText(""); name1.setError(null);  // TODO name2
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
      return !activity.name1.isEmpty();
   }

   @Override
   boolean onDoneClicked() {
      // TODO check
      return true;
   }

   @Override
   void updateModel() {
      activity.name1 = name1.getText().toString().trim();
      activity.refreshGUI();
   }

}