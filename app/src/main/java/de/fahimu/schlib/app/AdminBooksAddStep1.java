/*
 * AdminBooksAddStep1.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;


import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.db.Book;
import de.fahimu.schlib.db.Label;

/**
 * Step 1 of adding books to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminBooksAddStep1 extends StepFragment<AdminBooksAddActivity> {

   @Override
   void passActivityToNextFragments() {
      nextFragment.setActivity(activity);
   }

   @Override
   StepFragment getNext() { return nextFragment; }

   private final AdminBooksAddStep2 nextFragment = new AdminBooksAddStep2();

   @Override
   int getTabNameId() { return R.string.admin_books_add_step_1_label; }

   @Override
   boolean maybeOptional() { return true; }

   @Override
   boolean actualOptional() {
      return activity.isbn != null && Book.getIdentifiedByISBN(activity.isbn) == null;
   }

   /* ============================================================================================================== */

   @Override
   int getContentViewId() { return R.layout.admin_books_add_step_1; }

   private TextView explanation, displayLabel;

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);
         explanation = findView(TextView.class, R.id.admin_books_add_step_1_explanation);
         displayLabel = findView(TextView.class, R.id.admin_books_add_step_1_display_label);
      }
   }

   /* ============================================================================================================== */

   @Override
   void clearInput() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         activity.label = null;
         Book book = (activity.isbn == null) ? null : Book.getIdentifiedByISBN(activity.isbn);
         if (book == null) {
            explanation.setText(R.string.admin_books_add_step_1_explanation_1);
         } else {
            explanation.setText(App.getStr(R.string.admin_books_add_step_1_explanation_2, book.getDisplay()));
         }
         displayLabel();
      }
   }

   private void displayLabel() {
      String labelDisplay = (activity.label == null) ? "" : activity.label.getDisplayId();
      displayLabel.setText(App.getStr(R.string.admin_books_add_step_1_display_label, labelDisplay));
      activity.refreshGUI();
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   void onBarcode(String barcode) {
      Label label = Label.getNullable(SerialNumber.parseCode128(barcode));
      if (label == null) {
         activity.showErrorSnackbar(R.string.snackbar_error_not_a_label);
      } else if (label.isUsed()) {
         NoFocusDialog dialog = new NoFocusDialog(activity, NoFocusDialog.DEFAULT_CANCEL);
         dialog.setTitle(R.string.dialog_title_error);
         dialog.setMessage(R.string.dialog_message_label_used, Book.getNonNull(label.getBid()).getDisplay());
         dialog.show(R.raw.horn);
      } else {
         activity.label = label;
         if (label.isLost()) {
            label.setLost(false).update();       // Surprise! The label isn't lost, set this label to 'Stocked'.
            activity.showInfoSnackbar(R.string.snackbar_info_label_was_lost);
         } else if (label.isPrinted()) {
            // Promote all labels from 'Printed' to 'Stocked' which are on the same page as the scanned label.
            Label.setStocked(label);
            activity.showInfoSnackbar(R.string.snackbar_info_label_registered);
         }
      }
      displayLabel();
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   boolean isDoneEnabled() {
      return activity.label != null;
   }

   @Override
   boolean isDone() {
      return true;
   }

}