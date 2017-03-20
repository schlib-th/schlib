/*
 * AdminBooksAddStep0.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;


import de.fahimu.android.app.Log;
import de.fahimu.schlib.anw.ISBN;

/**
 * Step 0 of adding books to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminBooksAddStep0 extends StepFragment {

   @Override
   StepFragment getNext() { return nextFragment; }

   private final AdminBooksAddStep1 nextFragment = new AdminBooksAddStep1();

   @Override
   int getTabNameId() { return R.string.admin_books_add_step_0_label; }

   /* ============================================================================================================== */

   @Override
   int getContentViewId() { return R.layout.admin_books_add_step_0; }

   private RadioButton isbn;
   private RadioGroup  group;

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);
         isbn = findView(RadioButton.class, R.id.admin_books_add_step_0_isbn);
         group = findView(RadioGroup.class, R.id.admin_books_add_step_0_group);

         activity = (AdminBooksAddActivity) stepperActivity;

         group.setOnCheckedChangeListener(new RadioGroupListener());
      }
   }

   private AdminBooksAddActivity activity;

   /* -------------------------------------------------------------------------------------------------------------- */

   private final class RadioGroupListener implements OnCheckedChangeListener {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
         if (checkedId != R.id.admin_books_add_step_0_isbn) {
            activity.isbn = null;
         }
         displayIsbn();
      }
   }

   private void displayIsbn() {
      String isbnDisplay = (activity.isbn == null) ? "" : activity.isbn.getDisplay();
      isbn.setText(App.getStr(R.string.admin_books_add_step_0_isbn, isbnDisplay));
      activity.refreshGUI();
   }

   /* ============================================================================================================== */

   @Override
   void clearInput() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         group.clearCheck();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   void onBarcode(String barcode) {
      if ((activity.isbn = ISBN.parse(barcode)) == null) {
         group.clearCheck();
         activity.showErrorSnackbar(R.string.snackbar_error_not_a_isbn);
      } else if (group.getCheckedRadioButtonId() != R.id.admin_books_add_step_0_isbn) {
         group.check(R.id.admin_books_add_step_0_isbn);
      } else {
         displayIsbn();       // ISBN changed
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   boolean isDoneEnabled() {
      return activity.isbn != null || group.getCheckedRadioButtonId() == R.id.admin_books_add_step_0_no_isbn;
   }

   @Override
   boolean isDone() {
      return true;
   }

}