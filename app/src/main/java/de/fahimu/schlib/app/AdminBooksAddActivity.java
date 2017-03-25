/*
 * AdminBooksAddActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.support.annotation.NonNull;


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

   ISBN    isbn;
   Label   label;
   String  shelf;
   Integer number;

   String title, publisher, author, keywords;

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   protected int getContentViewId() {
      return R.layout.admin_books_add;
   }

   @NonNull
   @Override
   StepFragment createFirstFragment() {
      return new AdminBooksAddStep0().setActivity(this);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   void finishActivity() {
      Book book = new Book();
      book.setISBN(isbn).setLabel(label);
      book.setShelf(shelf).setNumber(number);
      book.setTitle(title).setPublisher(publisher).setAuthor(author).setKeywords(keywords);
      book.setPeriod(14).setStocked(SQLite.getDatetimeNow());
      book.insert();
      finish();
   }

}