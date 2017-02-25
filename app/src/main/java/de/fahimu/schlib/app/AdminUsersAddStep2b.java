/*
 * AdminUsersAddStep2b.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.Nullable;


import de.fahimu.android.app.Log;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.db.Idcard;

/**
 * Step 2b of adding users to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersAddStep2b extends StepFragment {

   @Override
   StepFragment getNext() { return null; }

   @Override
   int getTabNameId() { return R.string.admin_users_add_step_2_label; }

   /* ============================================================================================================== */

   @Override
   protected int getContentViewId() { return R.layout.admin_users_add_step_2b; }

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);
         activity = (AdminUsersAddActivity) stepperActivity;
      }
   }

   private AdminUsersAddActivity activity;

   /* ============================================================================================================== */

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
   void onBarcode(String barcode) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         int number = SerialNumber.parseCode128(barcode);
         if (Idcard.getNullable(number) != null) {       // it's a serial from the expected type
            scope.d("idcard=" + new SerialNumber(number).getDecimal());
         } else {
            stepperActivity.showErrorSnackbar(R.string.snackbar_error_not_a_idcard);
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   boolean isDoneEnabled() { return true; }

   @Override
   boolean onDoneClicked() { return true; }

}