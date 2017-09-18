/*
 * AdminBooksAddStep3.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.Editable;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.TextView;


import java.util.ArrayList;

import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.android.app.scanner.NoFocusDialog.ButtonListener;
import de.fahimu.android.app.scanner.ScannerAwareEditText;
import de.fahimu.android.app.scanner.ScannerAwareEditText.AbstractTextWatcher;
import de.fahimu.android.app.scanner.ScannerAwareEditText.ColumnAdapter;
import de.fahimu.android.app.scanner.ScannerAwareEditText.ColumnItem;
import de.fahimu.schlib.anw.ISBN;
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

   private String titleText, authorText, publisherText, keywordsText;

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);
         explanation = findView(TextView.class, R.id.admin_books_add_step_3_explanation);
         title = findView(ScannerAwareEditText.class, R.id.admin_books_add_step_3_title);
         author = findView(ScannerAwareEditText.class, R.id.admin_books_add_step_3_author);
         publisher = findView(ScannerAwareEditText.class, R.id.admin_books_add_step_3_publisher);
         keywords = findView(ScannerAwareEditText.class, R.id.admin_books_add_step_3_keywords);

         // Initialize the text strings for clearInput() and set existingISBN to
         // the scanned ISBN if there is already a book in the database with this ISBN, otherwise null.
         existingISBN = null;
         titleText = authorText = publisherText = keywordsText = "";
         if (activity.isbn != null) {
            ArrayList<Book> books = Book.getByISBNIncludeDeleted(activity.isbn);
            for (Book book : books) {
               titleText = getIfLonger(titleText, book.getTitle());
               authorText = getIfEmpty(authorText, book.getAuthor());
               publisherText = getIfEmpty(publisherText, book.getPublisher());
               keywordsText = getIfLonger(keywordsText, book.getKeywords());
            }
            existingISBN = books.isEmpty() ? null : activity.isbn;
         }

         title.setColumnAdapter(new BookColumnAdapter(Book.TITLE, new BookColumnGetter() {
            public String getValue(Book book) { return book.getTitle(); }
         }));
         author.setColumnAdapter(new BookColumnAdapter(Book.AUTHOR, new BookColumnGetter() {
            public String getValue(Book book) { return book.getAuthor(); }
         }));
         publisher.setColumnAdapter(new BookColumnAdapter(Book.PUBLISHER, new BookColumnGetter() {
            public String getValue(Book book) { return book.getPublisher(); }
         }));
         keywords.setColumnAdapter(new BookColumnAdapter(Book.KEYWORDS, new BookColumnGetter() {
            public String getValue(Book book) { return book.getKeywords(); }
         }));

         title.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
               if (!hasFocus) {     // if leaving the EditText
                  if (existingISBN == null) {
                     ArrayList<Book> books = Book.getByTitleIncludeDeleted(activity.title);
                     if (!books.isEmpty()) {
                        // Take the values from the book with the best rating
                        Book bestBook = books.get(0);
                        for (Book book : books) {
                           if (rating(book) > rating(bestBook)) { bestBook = book; }
                        }
                        author.setText(bestBook.getAuthor());
                        publisher.setText(bestBook.getPublisher());
                        keywords.setText(bestBook.getKeywords());
                     }
                  }
               }
            }
         });

         title.addTextChangedListener(new TextChangedListener(title, new ActivityValueSetter() {
            public void setValue(String value) { activity.title = value; }
         }));
         author.addTextChangedListener(new TextChangedListener(author, new ActivityValueSetter() {
            public void setValue(String value) { activity.author = value; }
         }));
         publisher.addTextChangedListener(new TextChangedListener(publisher, new ActivityValueSetter() {
            public void setValue(String value) { activity.publisher = value; }
         }));
         keywords.addTextChangedListener(new TextChangedListener(keywords, new ActivityValueSetter() {
            public void setValue(String value) { activity.keywords = value; }
         }));
      }
   }

   private String getIfLonger(String text, String column) {
      return (column.length() > text.length()) ? column : text;
   }

   private String getIfEmpty(String text, String column) {
      return text.isEmpty() ? column : text;
   }

   private int rating(Book book) {
      int rating = 0;
      if (!book.getAuthor().isEmpty()) { rating += 0x100000; }
      if (!book.getPublisher().isEmpty()) { rating += 0x10000; }
      return rating + book.getKeywords().length();
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

   private ISBN existingISBN;

   private final class BookColumnAdapter extends ColumnAdapter<BookColumnItem> {
      private final String           column;
      private final BookColumnGetter getter;

      BookColumnAdapter(String column, BookColumnGetter getter) {
         this.column = column; this.getter = getter;
      }

      @Override
      @WorkerThread
      protected void loadData(ArrayList<BookColumnItem> data) {
         for (Book book : Book.getColumnValues(column, existingISBN)) {
            String value = getter.getValue(book);
            if (!value.isEmpty()) {
               data.add(new BookColumnItem(book, value));
            }
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
         confirmedTitle = null;
         explanation.setText(titleText.isEmpty() ?
                             R.string.admin_books_add_step_3_explanation_enter :
                             R.string.admin_books_add_step_3_explanation_check);
         title.setError(null);
         title.setText(titleText);
         author.setError(null);
         author.setText(authorText);
         publisher.setError(null);
         publisher.setText(publisherText);
         keywords.setError(null);
         keywords.setText(keywordsText);
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   boolean isDoneEnabled() {
      return !activity.title.isEmpty();
   }

   private String confirmedTitle;

   private boolean noTitleIsbnInconsistency() {
      ISBN inconsistentISBN = null;
      ArrayList<Book> books = Book.getByTitleIncludeDeleted(activity.title);
      for (Book book : books) {
         if (book.hasISBN() && !book.getISBN().equals(activity.isbn)) {
            inconsistentISBN = book.getISBN(); break;
         }
      }
      if (inconsistentISBN == null || activity.title.equals(confirmedTitle)) {
         return true;
      } else {
         NoFocusDialog dialog = new NoFocusDialog(activity);
         ButtonListener buttonListener = new ButtonListener() {
            @Override
            public void onClick(int id) { confirmedTitle = activity.title; }
         };
         if (activity.isbn == null) {
            dialog.setMessage(R.string.dialog_message_admin_books_add_step_3_no_isbn,
                  inconsistentISBN.getDisplay());
            dialog.setButton0(R.string.dialog_button0_admin_books_add_step_3_no_isbn, null);
            dialog.setButton1(R.string.dialog_button1_admin_books_add_step_3_no_isbn, buttonListener);
         } else {
            dialog.setMessage(R.string.dialog_message_admin_books_add_step_3_inconsistent_isbn,
                  inconsistentISBN.getDisplay(), activity.isbn.getDisplay());
            dialog.setButton0(R.string.dialog_button0_admin_books_add_step_3_inconsistent_isbn, null);
            dialog.setButton1(R.string.dialog_button1_admin_books_add_step_3_inconsistent_isbn, buttonListener);
         }
         dialog.show(R.raw.horn);
         return false;
      }
   }

   @Override
   boolean isDone() {
      return noTitleIsbnInconsistency() &&
            StringType.TITLE.matches(title, activity.title) &&
            StringType.AUTHOR.matches(author, activity.author) &&
            StringType.PUBLISHER.matches(publisher, activity.publisher) &&
            StringType.KEYWORDS.matches(keywords, activity.keywords);
   }

}