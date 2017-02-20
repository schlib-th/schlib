/*
 * AdminUsersAddStep1a.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.widget.TextView;


import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.ScannerAwareEditText;
import de.fahimu.android.app.scanner.ScannerAwareEditText.AbstractTextWatcher;
import de.fahimu.schlib.anw.StringType;
import de.fahimu.schlib.db.User;

/**
 * Step 1a of adding users to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersAddStep1a extends StepFragment {

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
         name1 = findView(ScannerAwareEditText.class, R.id.admin_users_add_step_1a_name1);
         name2 = findView(ScannerAwareEditText.class, R.id.admin_users_add_step_1a_name2);

         activity = (AdminUsersAddActivity) stepperActivity;

         name1.setScannerActivity(activity).addTextChangedListener(new TextChangedListener());
         name2.setScannerActivity(activity).addTextChangedListener(new TextChangedListener());
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
         AdminUsersAddStep1a.this.updateModel();
      }
   }

   /* ============================================================================================================== */

   @Override
   public void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         String role = activity.role.getDisplay();
         explanation.setText(App.getStr(R.string.admin_users_add_step_1a_explanation, role));
         name1.setText(""); name1.setError(null);
         name2.setText(""); name2.setError(null);
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
      return !activity.name1.isEmpty() && !activity.name2.isEmpty();
   }

   private boolean matches(StringType stringType, String text, TextView name) {
      int failPosition = stringType.matches(text);
      if (failPosition >= 0) {
         String message = (failPosition >= text.length()) ?
                          App.getStr(R.string.admin_users_add_step_1a_error_1) :
                          App.getStr(R.string.admin_users_add_step_1a_error_2, text.charAt(failPosition));
         name.requestFocus(); name.setError(message);
      }
      return failPosition < 0;
   }

   @Override
   boolean onDoneClicked() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         if (!matches(StringType.NAME1, activity.name1, name1)) { return false; }
         if (!matches(StringType.NAME2, activity.name2, name2)) { return false; }
         User user = User.get(activity.name1, activity.name2, 0);
         if (user != null) {
            activity.showErrorSnackbar(R.string.admin_users_add_step_1a_error_snackbar, user.getRole().getDisplay());
         }
         return user == null;
      }
   }

   @Override
   void updateModel() {
      activity.name1 = name1.getText().toString().trim();
      activity.name2 = name2.getText().toString().trim();
      activity.refreshGUI();
   }

}