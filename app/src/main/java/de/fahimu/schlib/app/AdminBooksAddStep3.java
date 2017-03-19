/*
 * AdminBooksAddStep3.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;


import de.fahimu.android.app.Log;

/**
 * Step 3 (last step) of adding books to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminBooksAddStep3 extends StepFragment {

   @Override
   StepFragment getNext() { return null; }

   @Override
   int getTabNameId() { return R.string.admin_books_add_step_3_label; }

   /* ============================================================================================================== */

   @Override
   int getContentViewId() { return R.layout.admin_books_add_step_3; }

   private TextView explanation;

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);

         explanation = findView(TextView.class, R.id.admin_books_add_step_3_explanation);

         activity = (AdminBooksAddActivity) stepperActivity;
      }
   }

   private AdminBooksAddActivity activity;

   /* -------------------------------------------------------------------------------------------------------------- */

   /* ============================================================================================================== */

   @Override
   public void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
         activity.refreshGUI();
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
      return true;
   }

   @Override
   boolean isDone() {
      return true;
   }

}