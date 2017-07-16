/*
 * AdminActivity.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import de.fahimu.android.app.Log;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.Label;

/**
 * Activity to choose between administrating books, users and doing other things.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class AdminActivity extends SchlibActivity {

   private Button   registerPrints;
   private TextView registerPrintsMessage;

   /* ============================================================================================================== */

   @Override
   protected int getContentViewId() { return R.layout.admin; }

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      registerPrints = findView(Button.class, R.id.admin_register_prints);
      registerPrintsMessage = findView(TextView.class, R.id.admin_register_prints_message);
   }

   @Override
   protected void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
         int pagesToRegister = Label.getPageNumbers().size() + Idcard.getPageNumbers().size();
         if (pagesToRegister == 0) {
            registerPrints.setEnabled(false);
            registerPrintsMessage.setText(App.getStr(R.string.admin_message_no_pages_to_register));
            registerPrintsMessage.setTextColor(App.getColorFromRes(android.R.color.darker_gray));
         } else {
            registerPrints.setEnabled(true);
            if (pagesToRegister == 1) {
               registerPrintsMessage.setText(App.getStr(R.string.admin_message_one_page_to_register));
            } else {
               registerPrintsMessage.setText(App.getStr(R.string.admin_message_pages_to_register, pagesToRegister));
            }
            registerPrintsMessage.setTextColor(App.getColorFromRes(android.R.color.holo_red_dark));
         }
      }
   }

   @Override
   protected void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
      }
   }

   /* ============================================================================================================== */

   public void onRegisterPrintsClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         startActivity(new Intent(this, RegisterPrintsActivity.class));
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onAdminIdcardsClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         startActivity(new Intent(this, AdminIdcardsActivity.class));
      }
   }

   public void onAdminLabelsClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         startActivity(new Intent(this, AdminLabelsActivity.class));
      }
   }

   public void onAdminUsersClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         startActivity(new Intent(this, AdminUsersActivity.class));
      }
   }

   public void onAdminBooksClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         startActivity(new Intent(this, AdminBooksActivity.class));
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onStocktakingIdcardsClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         startActivity(new Intent(this, StocktakingIdcardsActivity.class));
      }
   }

   public void onStocktakingLabelsClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         startActivity(new Intent(this, StocktakingLabelsActivity.class));
      }
   }

   public void onStocktakingUsersClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         startActivity(new Intent(this, StocktakingUsersActivity.class));
      }
   }

   public void onStocktakingBooksClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         startActivity(new Intent(this, StocktakingBooksActivity.class));
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onAdminLendingsClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         startActivity(new Intent(this, AdminLendingsActivity.class));
      }
   }

   public void onReprintPupilListClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         scope.d("clicked");
         // TODO startActivity(new Intent(this, ReprintPupilListActivity.class));
      }
   }

}