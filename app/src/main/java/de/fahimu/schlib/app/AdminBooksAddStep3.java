/*
 * AdminBooksAddStep3.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.widget.TextView;


import java.util.ArrayList;

import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.ScannerAwareEditText;
import de.fahimu.android.app.scanner.ScannerAwareEditText.AbstractTextWatcher;
import de.fahimu.android.app.scanner.ScannerAwareEditText.ColumnAdapter;
import de.fahimu.android.app.scanner.ScannerAwareEditText.ColumnItem;
import de.fahimu.schlib.anw.StringType;
import de.fahimu.schlib.db.Book;

/**
 * Step 3 (last step) of adding books to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminBooksAddStep3 extends StepFragment<AdminBooksAddActivity> {

   @Override
   void passActivityToNextFragments() { /* last fragment */ }

   @Override
   StepFragment getNext() { return null; }

   @Override
   int getTabNameId() { return R.string.admin_books_add_step_3_label; }

   /* ============================================================================================================== */

   @Override
   int getContentViewId() { return R.layout.admin_books_add_step_3; }

   private TextView             explanation;
   private ScannerAwareEditText title, author, publisher, keywords;

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);
         explanation = findView(TextView.class, R.id.admin_books_add_step_3_explanation);
         title = findView(ScannerAwareEditText.class, R.id.admin_books_add_step_3_title);
         author = findView(ScannerAwareEditText.class, R.id.admin_books_add_step_3_author);
         publisher = findView(ScannerAwareEditText.class, R.id.admin_books_add_step_3_publisher);
         keywords = findView(ScannerAwareEditText.class, R.id.admin_books_add_step_3_keywords);

         title.setColumnAdapter(new BookColumnAdapter(Book.TITLE, new BookColumnGetter() {
            public String getValue(Book book) { return book.getTitle(); }
         }));
         title.addTextChangedListener(new TextChangedListener(title, new ActivityValueSetter() {
            public void setValue(String value) { activity.title = value; }
         }));

         author.setColumnAdapter(new BookColumnAdapter(Book.AUTHOR, new BookColumnGetter() {
            public String getValue(Book book) { return book.getAuthor(); }
         }));
         author.addTextChangedListener(new TextChangedListener(author, new ActivityValueSetter() {
            public void setValue(String value) { activity.author = value; }
         }));

         publisher.setColumnAdapter(new BookColumnAdapter(Book.PUBLISHER, new BookColumnGetter() {
            public String getValue(Book book) { return book.getPublisher(); }
         }));
         publisher.addTextChangedListener(new TextChangedListener(publisher, new ActivityValueSetter() {
            public void setValue(String value) { activity.publisher = value; }
         }));

         keywords.setColumnAdapter(new BookColumnAdapter(Book.KEYWORDS, new BookColumnGetter() {
            public String getValue(Book book) { return book.getKeywords(); }
         }));
         keywords.addTextChangedListener(new TextChangedListener(keywords, new ActivityValueSetter() {
            public void setValue(String value) { activity.keywords = value; }
         }));
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private static final class BookColumnItem extends ColumnItem {
      BookColumnItem(@NonNull Book book, @NonNull String column) {
         super(book, column);
      }
   }

   private interface BookColumnGetter {
      String getValue(Book book);
   }

   private static final class BookColumnAdapter extends ColumnAdapter<BookColumnItem> {
      private final String           column;
      private final BookColumnGetter getter;

      BookColumnAdapter(String column, BookColumnGetter getter) {
         this.column = column; this.getter = getter;
      }

      @Override
      protected void loadData(ArrayList<BookColumnItem> data) {
         for (Book book : Book.getColumnValues(column)) {
            data.add(new BookColumnItem(book, getter.getValue(book)));
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private interface ActivityValueSetter {
      void setValue(String value);
   }

   private final class TextChangedListener extends AbstractTextWatcher {
      private final ScannerAwareEditText editText;
      private final ActivityValueSetter  setter;

      TextChangedListener(ScannerAwareEditText editText, ActivityValueSetter setter) {
         this.editText = editText; this.setter = setter;
      }

      @Override
      public void afterTextChanged(Editable editable) {
         String text = editable.toString();
         // prevent entering of leading blanks (just because it doesn't make sense)
         if (!text.isEmpty() && text.trim().isEmpty()) { editable.clear(); }
         setter.setValue(editText.getText().toString().trim());
         activity.refreshGUI();
      }
   }

   /* ============================================================================================================== */

   @Override
   void clearInput() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         Book book = (activity.isbn == null) ? null : Book.getByISBN(activity.isbn);
         explanation.setText(book == null ? R.string.admin_books_add_step_3_explanation_1 :
                             R.string.admin_books_add_step_3_explanation_2);
         title.setError(null);
         title.setText(book == null ? "" : book.getTitle());
         author.setError(null);
         author.setText(book == null ? "" : book.getAuthor());
         publisher.setError(null);
         publisher.setText(book == null ? "" : book.getPublisher());
         keywords.setError(null);
         keywords.setText(book == null ? "" : book.getKeywords());
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   boolean isDoneEnabled() {
      return !activity.title.isEmpty();
   }

   @Override
   boolean isDone() {
      return StringType.TITLE.matches(title, activity.title) &&
            StringType.AUTHOR.matches(author, activity.author) &&
            StringType.PUBLISHER.matches(publisher, activity.publisher) &&
            StringType.KEYWORDS.matches(keywords, activity.keywords);
   }

}