/*
 * AdminUsersAddStep2.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.widget.TextView;


import java.util.ArrayList;

import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.User;
import de.fahimu.schlib.db.User.Role;

/**
 * Step 2 (last step) of adding users to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersAddStep2 extends StepFragment<AdminUsersAddActivity> {

   @Override
   void passActivityToNextFragments() { /* last fragment */ }

   @Override
   StepFragment getNext() { return null; }

   @Override
   int getTabNameId() { return R.string.admin_users_add_step_2_label; }

   /* ============================================================================================================== */

   @Override
   int getContentViewId() { return R.layout.admin_users_add_step_2; }

   private TextView explanation, classListHint, textStatus, textIdcard;

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);
         explanation = findView(TextView.class, R.id.admin_users_add_step_2_explanation);
         classListHint = findView(TextView.class, R.id.admin_users_add_step_2_class_list_hint);
         textStatus = findView(TextView.class, R.id.admin_users_add_step_2_status);
         textIdcard = findView(TextView.class, R.id.admin_users_add_step_2_idcard);
      }
   }

   /* ============================================================================================================== */

   private Idcard lastScanned;

   @Override
   void clearInput() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         lastScanned = null;
         activity.scanned = new ArrayList<>();

         int resId = (activity.role != Role.PUPIL) ? R.string.admin_users_add_step_2_explanation :
                     (activity.count == 1) ? R.string.admin_users_add_step_2_explanation_pupil_1 :
                     R.string.admin_users_add_step_2_explanation_pupil_2;
         explanation.setText(App.getStr(resId,
               activity.count, activity.role.getDisplay(), activity.name1, activity.name2));
         classListHint.setText(activity.role != Role.PUPIL ? "" :
                               App.getStr(R.string.admin_users_add_step_2_pupil_list_hint));
         setStatusIdcard();
      }
   }

   private void setStatusIdcard() {
      @StringRes int resId = (activity.getRemaining() == 0) ? R.string.admin_users_add_step_2_status_0 :
                             (activity.getRemaining() == 1) ? R.string.admin_users_add_step_2_status_1 :
                             R.string.admin_users_add_step_2_status_n;
      textStatus.setText(App.getStr(resId, activity.getRemaining()));
      textIdcard.setText(lastScanned == null ? "" :
                         App.getStr(R.string.admin_users_add_step_2_idcard, lastScanned.getDisplayId()));
      activity.refreshGUI();
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   void onBarcode(String barcode) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         if (activity.getRemaining() == 0) {
            activity.showErrorSnackbar(R.string.admin_users_add_step_2_status_0);
         } else {
            Idcard idcard = Idcard.getNullable(SerialNumber.parseCode128(barcode));
            if (idcard == null) {
               activity.showErrorSnackbar(R.string.snackbar_error_not_a_idcard);
            } else if (idcard.isUsed()) {
               NoFocusDialog dialog = new NoFocusDialog(activity);
               dialog.setMessage(R.string.dialog_message_idcard_used, User.getNonNull(idcard.getUid()).getDisplay());
               dialog.show(R.raw.horn);
            } else if (activity.scanned.contains(idcard.getId())) {
               activity.showInfoSnackbar(R.string.admin_users_add_step_2_snackbar_info_scanned);
            } else {
               activity.scanned.add((lastScanned = idcard).getId());
               if (activity.getRemaining() == 0) {
                  App.playSound(R.raw.bell);
               }
               if (idcard.isLost()) {
                  idcard.setLost(false).update();     // Surprise! The serial isn't lost, set this serial to 'Stocked'.
                  activity.showInfoSnackbar(R.string.snackbar_info_idcard_was_lost);
               } else if (idcard.isPrinted()) {
                  // Promote all idcards from 'Printed' to 'Stocked' which are on the same page as the scanned idcard.
                  Idcard.setStocked(idcard);
                  activity.showInfoSnackbar(R.string.snackbar_info_idcard_registered);
               }
            }
            setStatusIdcard();
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   boolean isDoneEnabled() {
      return activity.getRemaining() == 0;
   }

   @Override
   boolean isDone() {
      return true;
   }

}