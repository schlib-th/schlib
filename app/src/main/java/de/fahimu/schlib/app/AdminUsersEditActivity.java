/*
 * AdminUsersEditActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.Nullable;


import de.fahimu.android.app.Log;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.User;

/**
 * An activity for editing users.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersEditActivity extends SchlibActivity {

   @Override
   protected int getContentViewId() { return R.layout.admin_users_edit; }

   private User user;

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
   }

   @Override
   protected void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
         user = User.getNonNull(getIntent().getLongExtra("uid", -1L));
         scope.d("user=" + user.getDisplay());
      }
   }

   @Override
   protected void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
      }
   }

   @Override
   protected void onBarcode(String barcode) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         Idcard idcard = Idcard.parse(barcode);
         if (idcard == null) {
            showErrorSnackbar(R.string.snackbar_error_not_a_idcard);
         } else {
            scope.d("idcard=" + SerialNumber.getDecimal(idcard.getId()));
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

}