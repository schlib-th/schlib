/*
 * FirstRun4Activity.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import de.fahimu.android.app.ListView.Adapter;
import de.fahimu.android.app.ListView.Item;
import de.fahimu.android.app.ListView.ViewHolder;
import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.android.app.scanner.NoFocusDialog.ButtonListener;
import de.fahimu.schlib.anw.ISBN;
import de.fahimu.schlib.db.Book;
import de.fahimu.schlib.db.Label;

/**
 * Assign a unique number to every imported book, either an ISBN or a Label.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class FirstRun4Activity extends SchlibActivity {

   private final class BookItem extends Item<Book> {
      BookItem(@NonNull Book book) {
         super(book,
               book.getShelf(), book.getDisplayNumber(),
               book.getTitle(), book.getDisplayISBN(), book.getDisplayLabel());
      }
   }

   private final class BookViewHolder extends ViewHolder<BookItem> {
      private final TextView shelf, number, title, isbn, label;
      private final ImageButton action;

      BookViewHolder(LayoutInflater inflater, ViewGroup parent) {
         super(inflater, parent, R.layout.row_book_first_run_4);
         shelf = App.findView(itemView, TextView.class, R.id.row_book_first_run_4_shelf);
         number = App.findView(itemView, TextView.class, R.id.row_book_first_run_4_number);
         title = App.findView(itemView, TextView.class, R.id.row_book_first_run_4_title);
         isbn = App.findView(itemView, TextView.class, R.id.row_book_first_run_4_isbn);
         label = App.findView(itemView, TextView.class, R.id.row_book_first_run_4_label);
         action = App.findView(itemView, ImageButton.class, R.id.row_book_first_run_4_action);
      }

      protected void bind(BookItem item) {
         shelf.setText(item.getText(0));
         number.setText(item.getText(1));
         title.setText(item.getText(2));
         isbn.setText(item.getText(3));
         label.setText(item.getText(4));
         if (item.row.hasNoScanId()) {
            action.setImageResource(R.drawable.ic_delete_black_24dp);
            action.setContentDescription(App.getStr(R.string.row_book_first_run_4_action_delete));
         } else {
            action.setImageResource(R.drawable.ic_undo_black_24dp);
            action.setContentDescription(App.getStr(R.string.row_book_first_run_4_action_undo));
         }
      }
   }

   private final class BooksAdapter extends Adapter<Book,BookItem,BookViewHolder> {

      BooksAdapter() {
         super(FirstRun4Activity.this, R.id.first_run_4_list, R.string.first_run_4_empty);
      }

      @Override
      protected BookViewHolder createViewHolder(LayoutInflater inflater, ViewGroup parent) {
         return new BookViewHolder(inflater, parent);
      }

      @Override
      protected ArrayList<Book> loadData() { return Book.getAll(); }

      @Override
      protected BookItem createItem(Book book) { return new BookItem(book); }

      @Override
      protected void onUpdated(int flags, List<BookItem> data) {
         long rid = -1;
         for (BookItem item : data) {
            if (item.row.hasNoScanId()) {
               rid = item.rid; break;
            }
         }
         setSelection(rid);
         if (rid == -1) {
            showInfoSnackbar(R.string.first_run_4_empty);
         }
      }
   }

   /* ============================================================================================================== */

   private BooksAdapter booksAdapter;
   private Button       deleteLabels, createLabels, registerPrints;

   private ISBN bufferedISBN;

   @Override
   protected int getContentViewId() { return R.layout.first_run_4; }

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      booksAdapter = new BooksAdapter();
      deleteLabels = findView(Button.class, R.id.first_run_4_delete_labels);
      createLabels = findView(Button.class, R.id.first_run_4_create_labels);
      registerPrints = findView(Button.class, R.id.first_run_4_register_prints);
   }

   @Override
   protected void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
         booksAdapter.updateAsync(Adapter.RELOAD_DATA);
         bufferedISBN = null;
         updateButtons();
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      // We don't want the help or logout menu items, but the home button.
      // So don't call super.onCreateOptionsMenu(menu), but return true to build the menu.
      return true;
   }

   @Override
   protected void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public final void onListItemClicked(@NonNull View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         bufferedISBN = null;
         Book book = booksAdapter.getRowByView(view);
         if (book.hasNoScanId()) {
            showDialogDelete(book);
         } else {
            if (book.hasLabel()) {
               book.getLabel().setLost(true).update();
            }
            book.setISBN(null).setLabel(null).update();
            booksAdapter.setData(book);
            booksAdapter.updateAsync(0);
         }
      }
   }

   private void showDialogDelete(final Book book) {
      NoFocusDialog dialog = new NoFocusDialog(this);
      dialog.setMessage(R.string.dialog_message_first_run_4_delete_book, book.getDisplay());
      dialog.setButton0(R.string.dialog_button0_first_run_4_delete_book, null);
      dialog.setButton1(R.string.dialog_button1_first_run_4_delete_book, new ButtonListener() {
         @Override
         public void onClick(int id) {
            book.delete();
            booksAdapter.updateAsync(Adapter.RELOAD_DATA);
         }
      }).show();
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private void updateButtons() {
      int printed = Label.countPrinted();
      deleteLabels.setEnabled(printed > 0);
      createLabels.setEnabled(Label.canCallCreateOnePage() > 0);
      registerPrints.setEnabled(printed > 0);
   }

   public void onDeleteLabelsClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         bufferedISBN = null;
         int before = Label.countPrinted();
         int after = before - Label.deleteOnePage();
         updateButtons();
         if (after == 0) {
            showInfoSnackbar(R.string.snackbar_info_label_deleted_last);
         } else {
            showInfoSnackbar(R.string.snackbar_info_label_deleted, before - after, after);
         }
      }
   }

   public void onCreateLabelsClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         bufferedISBN = null;
         int before = Label.countPrinted();
         int after = before + Label.createOnePage();
         updateButtons();
         if (before == 0) {
            showInfoSnackbar(R.string.snackbar_info_label_created_first, after - before);
         } else {
            showInfoSnackbar(R.string.snackbar_info_label_created, after - before, after);
         }
      }
   }

   public void onRegisterPrintsClicked(View view) {
      bufferedISBN = null;
      startActivity(new Intent(this, RegisterPrintsActivity.class));
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   public void onBarcode(String barcode) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         Book book = booksAdapter.getSelectedRow();
         if (book != null) {
            if (barcode.length() == ISBN.LENGTH) {
               processISBN(book, barcode);
            } else {
               processLabel(book, barcode);
            }
         }
      }
   }

   private void processISBN(@NonNull Book scannedBook, String barcode) {
      bufferedISBN = null;
      final ISBN isbn = ISBN.parse(barcode);
      if (isbn == null) {
         showErrorSnackbar(R.string.snackbar_error_not_a_isbn);
      } else {
         Book book = Book.getIdentifiedByISBN(isbn);
         if (book == null) {
            updateISBNLabel(scannedBook, isbn, null);
         } else {
            NoFocusDialog dialog = new NoFocusDialog(this);
            dialog.setMessage(R.string.dialog_message_first_run_4_isbn_used, book.getDisplay());
            dialog.setOnCancelListener(new OnCancelListener() {
               @Override
               public void onCancel(DialogInterface dialog) { bufferISBN(isbn); }
            }).show(R.raw.horn);
         }
      }
   }

   private void bufferISBN(ISBN isbn) {
      bufferedISBN = isbn;
      showUndoSnackbar(App.getStr(R.string.first_run_4_snackbar_undo_action_discard), new OnClickListener() {
         @Override
         public void onClick(View v) { discardBufferedISBN(); }
      }, R.string.first_run_4_snackbar_undo_isbn_buffered, isbn.getDisplay());
   }

   private void discardBufferedISBN() {
      bufferedISBN = null;
      showInfoSnackbar(R.string.first_run_4_snackbar_info_isbn_discarded);
   }

   private void processLabel(@NonNull Book scannedBook, String barcode) {
      Label label = Label.parse(barcode);
      if (label == null) {
         showErrorSnackbar(R.string.snackbar_error_not_a_label);
      } else if (label.isUsed()) {
         Book book = Book.getByLabel(label);
         NoFocusDialog dialog = new NoFocusDialog(this);
         dialog.setMessage(R.string.dialog_message_first_run_4_label_used, book.getDisplay()).show(R.raw.horn);
      } else {
         if (label.isLost()) {
            label.setLost(false).update();       // Surprise! The label isn't lost, set this label to 'Stocked'.
            showInfoSnackbar(R.string.snackbar_info_label_was_lost);
         } else if (label.isPrinted()) {
            // Promote all serials from 'Printed' to 'Stocked' which are on the same page as the scanned serial.
            Label.setStocked(label);
            showInfoSnackbar(R.string.snackbar_info_label_registered);
         }
         if (bufferedISBN == null) {
            showDialogNoISBN(scannedBook, label);
         } else {
            updateISBNLabel(scannedBook, bufferedISBN, label);
         }
      }
      bufferedISBN = null;
   }

   private void showDialogNoISBN(@NonNull final Book book, @NonNull final Label label) {
      NoFocusDialog dialog = new NoFocusDialog(this);
      dialog.setMessage(R.string.dialog_message_first_run_4_no_isbn);
      dialog.setButton0(R.string.dialog_button0_first_run_4_no_isbn, null);
      dialog.setButton1(R.string.dialog_button1_first_run_4_no_isbn, new ButtonListener() {
         @Override
         public void onClick(int id) { updateISBNLabel(book, null, label); }
      }).show(R.raw.horn);
   }

   private void updateISBNLabel(@NonNull Book book, @Nullable ISBN isbn, @Nullable Label label) {
      book.setISBN(isbn).setLabel(label).update();
      booksAdapter.setData(book);
      booksAdapter.updateAsync(0);
   }

}