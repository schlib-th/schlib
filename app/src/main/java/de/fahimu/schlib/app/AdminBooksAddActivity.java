/*
 * AdminBooksAddActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.support.annotation.NonNull;


import de.fahimu.android.app.Log;
import de.fahimu.android.db.SQLite;
import de.fahimu.schlib.anw.ISBN;
import de.fahimu.schlib.db.Book;
import de.fahimu.schlib.db.Label;

/**
 * An activity for adding books to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminBooksAddActivity extends StepperActivity {

   ISBN    isbn   = null;
   Label   label  = null;
   String  shelf  = null;
   Integer number = null;

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   protected int getContentViewId() {
      return R.layout.admin_books_add;
   }

   @NonNull
   @Override
   StepFragment getFirstFragment() {
      return firstFragment;
   }

   private final AdminBooksAddStep0 firstFragment = new AdminBooksAddStep0();

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   protected void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
      }
   }

   @Override
   protected void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   void finishActivity() {
      Book book = new Book();
      book.setISBN(isbn).setLabel(label);
      // TODO book.setShelf(shelf).setNumber(number);

      // TODO book.setTitle() and so on

      book.setStocked(SQLite.getDatetimeNow());  // set time to 8 AM UTC

      // TODO book.insert();
      finish();
   }

}