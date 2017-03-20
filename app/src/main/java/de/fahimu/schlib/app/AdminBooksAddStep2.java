/*
 * AdminBooksAddStep2.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;


import de.fahimu.android.app.Log;

/**
 * Step 2 of adding books to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminBooksAddStep2 extends StepFragment {

   @Override
   StepFragment getNext() { return nextFragment; }

   private final AdminBooksAddStep3 nextFragment = new AdminBooksAddStep3();

   @Override
   int getTabNameId() { return R.string.admin_books_add_step_2_label; }

   /* ============================================================================================================== */

   @Override
   int getContentViewId() { return R.layout.admin_books_add_step_2; }

   private TextView explanation;

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);

         explanation = findView(TextView.class, R.id.admin_books_add_step_2_explanation);

         activity = (AdminBooksAddActivity) stepperActivity;
      }
   }

   private AdminBooksAddActivity activity;

   /* -------------------------------------------------------------------------------------------------------------- */

   /* ============================================================================================================== */

   @Override
   void clearInput() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         activity.refreshGUI();
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