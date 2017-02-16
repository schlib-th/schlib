/*
 * AdminUsersAddStep1a.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;


import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.ScannerAwareEditText;

/**
 * Step 1a of adding users to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersAddStep1a extends StepFragment<AdminUsersAddActivity> {

   @Override
   int getContentViewId() {
      return R.layout.admin_users_add_step_1a;
   }

   @Override
   int getTabNameId() {
      return R.string.admin_users_add_step_1a_label;
   }

   @Nullable
   @Override
   StepFragment getNext() {
      return nextFragment;
   }

   final AdminUsersAddStep2a nextFragment = new AdminUsersAddStep2a();

   private TextView             explanation;
   private ScannerAwareEditText name1, name2;

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);
         explanation = findView(TextView.class, R.id.admin_users_add_step_1a_explanation);
         name1 = findView(ScannerAwareEditText.class, R.id.admin_users_add_step_1a_name1).
               setScannerActivity((AdminUsersAddActivity) getActivity());
         name2 = findView(ScannerAwareEditText.class, R.id.admin_users_add_step_1a_name2).
               setScannerActivity((AdminUsersAddActivity) getActivity());

         TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) { AdminUsersAddStep1a.this.updateModel(); }
         };
         name1.addTextChangedListener(textWatcher);
         name2.addTextChangedListener(textWatcher);
      }
   }

   @Override
   public void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         String role = getActivity(AdminUsersAddActivity.class).role.getDisplay();
         explanation.setText(App.getStr(R.string.admin_users_add_step_1a_explanation, role));
         name1.setText("");
         name2.setText("");
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
      return name1.getText().length() > 0 && name2.getText().length() > 0;
   }

   @Override
   boolean onDoneClicked() {
      return true; /* TODO check name1 und name2 */
   }

   @Override
   void updateModel() {
      AdminUsersAddActivity activity = getActivity(AdminUsersAddActivity.class);
      activity.name1 = name1.getText().toString();
      activity.name2 = name2.getText().toString();
      stepperActivity.refreshGUI();
   }

}