/*
 * AdminBooksActivity.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import de.fahimu.android.app.ListView.Adapter;
import de.fahimu.android.app.ListView.Filter;
import de.fahimu.android.app.ListView.SearchableItem;
import de.fahimu.android.app.ListView.ViewHolder;
import de.fahimu.android.app.Log;
import de.fahimu.android.app.SearchString;
import de.fahimu.android.app.scanner.ScannerAwareSearchView;
import de.fahimu.schlib.anw.ISBN;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.db.Book;
import de.fahimu.schlib.db.Label;

/**
 * An activity for administrating books.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class AdminBooksActivity extends SchlibActivity {

   private final class BookItem extends SearchableItem<Book> {
      BookItem(@NonNull Book book) {
         super(book,
               book.getShelf(), book.getDisplayNumber(),
               book.getTitle(), book.getAuthor(), book.getKeywords(), book.getPublisher(),
               book.getDisplayMultilineISBNLabel());
      }
   }

   private final class BookViewHolder extends ViewHolder<BookItem> {
      private final TextView shelf, number, title, author, keywords, publisher, isbnLabel;

      BookViewHolder(LayoutInflater inflater, ViewGroup parent) {
         super(inflater, parent, R.layout.row_book);
         shelf = App.findView(itemView, TextView.class, R.id.row_book_shelf);
         number = App.findView(itemView, TextView.class, R.id.row_book_number);
         title = App.findView(itemView, TextView.class, R.id.row_book_title);
         author = App.findView(itemView, TextView.class, R.id.row_book_author);
         keywords = App.findView(itemView, TextView.class, R.id.row_book_keywords);
         publisher = App.findView(itemView, TextView.class, R.id.row_book_publisher);
         isbnLabel = App.findView(itemView, TextView.class, R.id.row_book_isbn_label);

         final ImageButton action = App.findView(itemView, ImageButton.class, R.id.row_book_action);
         action.setImageResource(R.drawable.ic_info_black_24dp);
         action.setContentDescription(App.getStr(R.string.row_book_action_info));
      }

      protected void bind(BookItem item) {
         item.setText(0, shelf);
         item.setText(1, number);
         item.setText(2, title);
         item.setText(3, author);
         item.setText(4, keywords);
         item.setText(5, publisher);
         item.setText(6, isbnLabel);
      }
   }

   private final class BooksAdapter extends Adapter<Book,BookItem,BookViewHolder> {

      BooksAdapter() {
         super(AdminBooksActivity.this, R.id.admin_books_list, R.string.admin_books_empty);
      }

      @Override
      protected BookViewHolder createViewHolder(LayoutInflater inflater, ViewGroup parent) {
         return new BookViewHolder(inflater, parent);
      }

      @Override
      protected ArrayList<Book> loadData() { return Book.get(); }

      @Override
      protected BookItem createItem(Book book) { return new BookItem(book); }

      @Override
      protected void onUpdated(int flags, List<BookItem> data) {
         updateMenuItems();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private void updateMenuItems() {
      if (searchView != null) {          // safety net if updateMenuItems is called before onCreateOptionsMenu
         boolean searching = searchView.hasFocus() || !searchView.getQuery().isEmpty();
         create.setVisible(!searching);
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Filters the book list depending on the currently displayed queryText.
    */
   private final class BookItemFilter implements Filter<BookItem> {
      @NonNull
      private final String[] normalizedQueries = SearchString.getNormalizedQueries(searchView);

      @Override
      public boolean matches(BookItem item) {
         return item.contains(normalizedQueries);
      }
   }

   /* ============================================================================================================== */

   private BooksAdapter booksAdapter;

   private MenuItem create;

   private ScannerAwareSearchView searchView;

   @Override
   protected int getContentViewId() { return R.layout.admin_books; }

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      booksAdapter = new BooksAdapter();
   }

   @Override
   protected void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
      }
   }

   @Override
   protected void onPermissionGranted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         booksAdapter.updateAsync(Adapter.RELOAD_DATA, new BookItemFilter());
      }
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onCreateOptionsMenu(menu);
         getMenuInflater().inflate(R.menu.admin_books, menu);

         create = menu.findItem(R.id.action_create);
         searchView = new ScannerAwareSearchView(menu.findItem(R.id.action_search),
               this, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
               new InputFilter.LengthFilter(20), new SearchString.QueryTextFilter());

         booksAdapter.updateAsync(0, new BookItemFilter());
         return true;
      }
   }

   @Override
   protected void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
         if (searchView != null) { searchView.collapse(); }
      }
   }

   @Override
   protected void onBarcode(String barcode) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         if (searchView != null) {
            if (barcode.length() == ISBN.LENGTH) {
               ISBN isbn = ISBN.parse(barcode);
               if (isbn == null) {
                  showErrorSnackbar(R.string.snackbar_error_not_a_isbn);
               } else {
                  searchView.expand(isbn.getDisplay().replace('-', ' '));
               }
            } else {
               Label label = Label.parse(barcode);
               if (label == null) {
                  showErrorSnackbar(R.string.snackbar_error_not_a_label);
               } else {
                  searchView.expand(SerialNumber.getDecimal(label.getId()));
               }
            }
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */
   /*  Override the methods called from {@link ScannerAwareSearchView}                                               */
   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   public void onFocusChange(View v, boolean hasFocus) {
      updateMenuItems();
   }

   @Override
   public void onQueryTextChange(String newText) {
      booksAdapter.updateAsync(0, new BookItemFilter());
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onCreateClicked(MenuItem item) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         startActivity(new Intent(this, AdminBooksAddActivity.class));
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onListItemClicked(@NonNull View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         long bid = booksAdapter.getItemByView(view).row.getBid();
         startActivity(new Intent(this, AdminBooksEditActivity.class).putExtra("bid", bid));
      }
   }

}