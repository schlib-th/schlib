/*
 * AdminBooksEditActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.Nullable;


import de.fahimu.android.app.Log;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.db.Book;
import de.fahimu.schlib.db.Label;

/**
 * An activity for editing books.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminBooksEditActivity extends SchlibActivity {

   @Override
   protected int getContentViewId() { return R.layout.admin_books_edit; }

   private Book book;

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      book = Book.getNonNull(getIntent().getLongExtra("bid", -1L));
   }

   @Override
   protected void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
         scope.d("book=" + book.getDisplay());
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
         Label label = Label.parse(barcode);
         if (label == null) {
            showErrorSnackbar(R.string.snackbar_error_not_a_label);
         } else {
            scope.d("label=" + SerialNumber.getDecimal(label.getId()));
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

}