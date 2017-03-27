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
public final class AdminUsersAddStep1a extends StepFragment<AdminUsersAddActivity> {

   @Override
   void passActivityToNextFragments() {
      nextFragment.setActivity(activity);
   }

   @Override
   StepFragment getNext() { return nextFragment; }

   private final AdminUsersAddStep2 nextFragment = new AdminUsersAddStep2();

   @Override
   int getTabNameId() { return R.string.admin_users_add_step_1a_label; }

   /* ============================================================================================================== */

   @Override
   int getContentViewId() { return R.layout.admin_users_add_step_1a; }

   private TextView             explanation;
   private ScannerAwareEditText name1, name2;

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);
         explanation = findView(TextView.class, R.id.admin_users_add_step_1a_explanation);
         name1 = findView(ScannerAwareEditText.class, R.id.admin_users_add_step_1a_name1);
         name2 = findView(ScannerAwareEditText.class, R.id.admin_users_add_step_1a_name2);
         name1.addTextChangedListener(new TextChangedListener());
         name2.addTextChangedListener(new TextChangedListener());
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private final class TextChangedListener extends AbstractTextWatcher {
      @Override
      public void afterTextChanged(Editable editable) {
         String text = editable.toString();
         // prevent entering of leading blanks (just because it doesn't make sense)
         if (!text.isEmpty() && text.trim().isEmpty()) { editable.clear(); }
         activity.count = 1;
         activity.name1 = name1.getText().toString().trim();
         activity.name2 = name2.getText().toString().trim();
         activity.refreshGUI();
      }
   }

   /* ============================================================================================================== */

   @Override
   void clearInput() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         String role = activity.role.getDisplay();
         explanation.setText(App.getStr(R.string.admin_users_add_step_1a_explanation, role));
         name1.setText(""); name1.setError(null);
         name2.setText(""); name2.setError(null);
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   boolean isDoneEnabled() {
      return !activity.name1.isEmpty() && !activity.name2.isEmpty();
   }

   @Override
   boolean isDone() {
      if (!StringType.NAME1.matches(name1, activity.name1)) { return false; }
      if (!StringType.NAME2.matches(name2, activity.name2)) { return false; }
      User user = User.get(activity.name1, activity.name2, 0);
      if (user != null) {
         activity.showErrorSnackbar(R.string.admin_users_add_step_1a_snackbar_error, user.getRole().getDisplay());
      }
      return user == null;
   }

}