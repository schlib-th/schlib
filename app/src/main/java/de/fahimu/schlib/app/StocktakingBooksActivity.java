/*
 * StocktakingBooksActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.fahimu.android.app.ListView.Adapter;
import de.fahimu.android.app.ListView.Filter;
import de.fahimu.android.app.ListView.Item;
import de.fahimu.android.app.ListView.ViewHolder;
import de.fahimu.android.app.Log;
import de.fahimu.schlib.anw.ISBN;
import de.fahimu.schlib.db.Book;
import de.fahimu.schlib.db.Label;

import static de.fahimu.android.app.ListView.Adapter.RELOAD_DATA;
import static de.fahimu.android.app.ListView.Adapter.SHOW_DELAYED;

/**
 * Activity for stocktaking {@link Book}s.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.08.2017
 * @since SchoolLibrary 1.08
 */
public final class StocktakingBooksActivity extends SchlibActivity implements OnItemSelectedListener {

   private final class BookItem extends Item<Book> {
      BookItem(@NonNull Book book) {
         super(book, book.getDisplayNumber(), book.getTitle());
      }
   }

   private final class BookItemViewHolder extends ViewHolder<BookItem> {
      private final TextView number, title;

      BookItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
         super(inflater, parent, R.layout.row_book_stocktaking);
         number = App.findView(itemView, TextView.class, R.id.row_book_stocktaking_number);
         title = App.findView(itemView, TextView.class, R.id.row_book_stocktaking_title);
      }

      protected void bind(BookItem item) {
         number.setText(item.getText(0));
         title.setText(item.getText(1));
      }
   }

   private final class BooksAdapter extends Adapter<Book,BookItem,BookItemViewHolder> {

      BooksAdapter() {
         super(StocktakingBooksActivity.this, R.id.stocktaking_books_list, R.string.stocktaking_books_empty);
      }

      @Override
      protected BookItemViewHolder createViewHolder(LayoutInflater inflater, ViewGroup parent) {
         return new BookItemViewHolder(inflater, parent);
      }

      @Override
      protected ArrayList<Book> loadData() {
         return Book.getNonVanishedNonIssued();
      }

      @Override
      protected BookItem createItem(Book book) {
         return new BookItem(book);
      }

      @Override
      protected void onUpdated(int flags, List<BookItem> data) { }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Shows only books from the {@link #selectedShelf} that are not an element of the {@link #scannedBooks} set.
    */
   private final class BookItemFilter implements Filter<BookItem> {
      /** Clone the {@link #scannedBooks} set to prevent {@link ConcurrentModificationException}s */
      private final String    shelf = selectedShelf;
      private final Set<Long> books = new HashSet<>(scannedBooks);

      @Override
      public boolean matches(BookItem item) {
         return item.row.getShelf().equals(shelf) && !books.contains(item.rid);
      }
   }

   private String selectedShelf;

   private final Set<Long> scannedBooks = new HashSet<>();     // OID of scanned books

   /* ============================================================================================================== */

   private BooksAdapter booksAdapter;
   private Spinner      shelfSpinner;

   @Override
   @LayoutRes
   protected int getContentViewId() { return R.layout.stocktaking_books; }

   @Override
   protected final void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      booksAdapter = new BooksAdapter();

      shelfSpinner = findView(Spinner.class, R.id.stocktaking_books_spinner);
      shelfSpinner.setOnItemSelectedListener(this);
   }

   @Override
   protected final void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
         List<String> shelfs = new ArrayList<>();
         for (Book book : Book.getShelfValues()) {
            shelfs.add(book.getShelf());
         }
         ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, shelfs);
         adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
         shelfSpinner.setAdapter(adapter);
      }
   }

   @Override
   protected final void onPermissionGranted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         selectedShelf = "";
         scannedBooks.clear();
         booksAdapter.updateAsync(RELOAD_DATA);
      }
   }

   @Override
   protected final void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
      }
   }

   @Override
   protected void onBarcode(String barcode) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         if (barcode.length() == ISBN.LENGTH) {
            ISBN isbn = ISBN.parse(barcode);
            if (isbn == null) {
               showErrorSnackbar(R.string.snackbar_error_not_a_isbn);
            } else {
               onBookScanned(Book.getIdentifiedByISBN(isbn));
            }
         } else {
            Label label = Label.parse(barcode);
            if (label == null) {
               showErrorSnackbar(R.string.snackbar_error_not_a_label);
            } else {
               onBookScanned(label.isUsed() ? Book.getByLabel(label) : null);
            }
         }
      }
   }

   private void onBookScanned(@Nullable Book book) {
      if (book == null) {
         showErrorSnackbar(R.string.stocktaking_books_snackbar_error_not_found);
      } else if (!book.getShelf().equals(selectedShelf)) {
         showErrorSnackbar(R.string.stocktaking_books_snackbar_error_wrong_shelf, book.getShelf());
      } else {
         boolean added = scannedBooks.add(book.getOid());
         @StringRes int resId = added ? R.string.stocktaking_books_snackbar_info_scanned :
                                R.string.stocktaking_books_snackbar_info_already_scanned;
         showInfoSnackbar(resId, book.getDisplayShelfNumber());
         if (book.isVanished()) {
            book.setVanished(null).update();    // book re-emerged magically after being set to vanished
            booksAdapter.updateAsync(RELOAD_DATA, new BookItemFilter());
         } else {
            booksAdapter.setSelection(book.getOid());
            booksAdapter.updateAsync(SHOW_DELAYED, new BookItemFilter());
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
      selectedShelf = (String) parent.getItemAtPosition(position);
      booksAdapter.updateAsync(0, new BookItemFilter());
   }

   @Override
   public void onNothingSelected(AdapterView<?> parent) {
      selectedShelf = "";
      booksAdapter.updateAsync(0, new BookItemFilter());
   }

   public void onListItemClicked(@NonNull View view) {
      final Book book = booksAdapter.getRowByView(view);
      book.setVanished(App.posixTime()).update();
      booksAdapter.updateAsync(RELOAD_DATA, new BookItemFilter());
      showUndoSnackbar(App.getStr(R.string.snackbar_undo_action), new OnClickListener() {
         @Override
         public void onClick(View v) { restore(book); }
      }, R.string.stocktaking_books_snackbar_undo_book_vanished, book.getDisplayShelfNumber());
   }

   private void restore(Book book) {
      book.setVanished(null).update();
      booksAdapter.updateAsync(RELOAD_DATA, new BookItemFilter());
      showInfoSnackbar(R.string.stocktaking_books_snackbar_info_book_restored, book.getDisplayShelfNumber());
   }

}