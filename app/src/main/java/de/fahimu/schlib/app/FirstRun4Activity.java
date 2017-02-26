/*
 * FirstRun4Activity.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;
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
import de.fahimu.schlib.anw.SerialNumber;
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

   final class BookItem extends Item<Book> {
      @WorkerThread
      BookItem(@NonNull Book book) { super(book); }
   }

   final class BookViewHolder extends ViewHolder<BookItem> {
      private final TextView shelf, number, title, isbn, label;
      private final ImageButton action;

      BookViewHolder(LayoutInflater inflater, ViewGroup parent) {
         super(inflater, parent, R.layout.first_run_4_row);
         shelf = App.findView(itemView, TextView.class, R.id.first_run_4_row_shelf);
         number = App.findView(itemView, TextView.class, R.id.first_run_4_row_number);
         title = App.findView(itemView, TextView.class, R.id.first_run_4_row_title);
         isbn = App.findView(itemView, TextView.class, R.id.first_run_4_row_isbn);
         label = App.findView(itemView, TextView.class, R.id.first_run_4_row_label);
         action = App.findView(itemView, ImageButton.class, R.id.first_run_4_row_action);
      }

      protected void bind(BookItem item) {
         shelf.setText(item.row.getShelf());
         number.setText(item.row.getDisplayNumber());
         title.setText(item.row.getTitle());
         isbn.setText(item.row.getDisplayISBN());
         label.setText(item.row.getDisplayLabel());
         if (item.row.hasNoScanId()) {
            action.setImageResource(R.drawable.ic_delete_black_24dp);
            action.setContentDescription(App.getStr(R.string.first_run_4_row_action_delete));
         } else {
            action.setImageResource(R.drawable.ic_undo_black_24dp);
            action.setContentDescription(App.getStr(R.string.first_run_4_row_action_undo));
         }
      }
   }

   final class BooksAdapter extends Adapter<Book,BookItem,BookViewHolder> {

      BooksAdapter() {
         super(FirstRun4Activity.this, R.id.first_run_4_list, R.string.first_run_4_empty);
      }

      @Override
      protected BookViewHolder createViewHolder(LayoutInflater inflater, ViewGroup parent) {
         return new BookViewHolder(inflater, parent);
      }

      @Override
      protected ArrayList<Book> loadData() { return Book.getNoScanId(); }

      @Override
      protected BookItem createItem(Book book) { return new BookItem(book); }

      @Override
      protected void onUpdated(int flags, List<BookItem> data) {
         BookItem selection = null;
         for (BookItem item : data) {
            if (item.row.hasNoScanId()) {
               selection = item; break;
            }
         }
         setSelection(selection);
         if (selection == null) {
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
         BookItem item = booksAdapter.getItemByView(view);
         if (item.row.hasNoScanId()) {
            showDialogDelete(item);
         } else {
            if (item.row.hasLabel()) {
               Label.getNonNull(item.row.getLabel()).setLost(true).update();
            }
            item.row.setISBN(null).setLabel((Integer) null).update();
            booksAdapter.setData(item);
            booksAdapter.updateAsync(0);
         }
      }
   }

   private void showDialogDelete(final BookItem item) {
      NoFocusDialog dialog = new NoFocusDialog(this, NoFocusDialog.IGNORE_CANCEL);
      dialog.setTitle(R.string.first_run_4_dialog_delete_title);
      dialog.setMessage(R.string.first_run_4_dialog_delete_message, item.row.getDisplay());
      dialog.setNegativeButton(R.string.first_run_4_dialog_delete_do_not_delete, NoFocusDialog.IGNORE_BUTTON);
      dialog.setPositiveButton(R.string.first_run_4_dialog_delete_please_delete, new ButtonListener() {
         @Override
         public void onClick() {
            item.row.delete();
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
         BookItem item = booksAdapter.getSelectedItem();
         if (item != null) {
            if (barcode.length() == ISBN.LENGTH) {
               processISBN(item, barcode);
            } else {
               processLabel(item, barcode);
            }
         }
      }
   }

   private void processISBN(@NonNull BookItem item, String barcode) {
      bufferedISBN = null;
      final ISBN isbn = ISBN.parse(barcode);
      if (isbn == null) {
         showErrorSnackbar(R.string.snackbar_error_not_a_isbn);
      } else {
         Book book = Book.getIdentifiedByISBN(isbn.getValue());
         if (book == null) {
            updateISBNLabel(item, isbn, null);
         } else {
            showDialogAssigned(R.string.first_run_4_dialog_assigned_message_isbn, book, new ButtonListener() {
               @Override
               public void onClick() { bufferISBN(isbn); }
            });
         }
      }
   }

   private void bufferISBN(ISBN isbn) {
      bufferedISBN = isbn;
      showUndoSnackbar(App.getStr(R.string.first_run_4_snackbar_isbn_action_discard), new OnClickListener() {
         @Override
         public void onClick(View v) { discardBufferedISBN(); }
      }, R.string.first_run_4_snackbar_isbn_buffered, isbn.getDisplay());
   }

   private void discardBufferedISBN() {
      bufferedISBN = null;
      showInfoSnackbar(R.string.first_run_4_snackbar_isbn_discarded);
   }

   private void processLabel(@NonNull BookItem item, String barcode) {
      Label label = Label.getNullable(SerialNumber.parseCode128(barcode));
      if (label == null) {
         showErrorSnackbar(R.string.snackbar_error_not_a_label);
      } else if (label.isUsed()) {
         Book book = Book.getNonNull(label.getBid());
         showDialogAssigned(R.string.first_run_4_dialog_assigned_message_label, book, NoFocusDialog.IGNORE_BUTTON);
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
            showDialogNoISBN(item, label);
         } else {
            updateISBNLabel(item, bufferedISBN, label);
         }
      }
      bufferedISBN = null;
   }

   private void showDialogAssigned(@StringRes int messageId, Book book, ButtonListener buttonListener) {
      NoFocusDialog dialog = new NoFocusDialog(this, NoFocusDialog.IGNORE_CANCEL);
      dialog.setTitle(R.string.first_run_4_dialog_assigned_title);
      dialog.setMessage(messageId, book.getDisplay());
      dialog.setPositiveButton(R.string.app_ok, buttonListener).show();
   }

   private void showDialogNoISBN(@NonNull final BookItem item, @NonNull final Label label) {
      NoFocusDialog dialog = new NoFocusDialog(this, NoFocusDialog.IGNORE_CANCEL);
      dialog.setTitle(R.string.first_run_4_dialog_no_isbn_title);
      dialog.setMessage(R.string.first_run_4_dialog_no_isbn_message);
      dialog.setNegativeButton(R.string.first_run_4_dialog_no_isbn_forgot_scan, NoFocusDialog.IGNORE_BUTTON);
      dialog.setPositiveButton(R.string.first_run_4_dialog_no_isbn_has_no_isbn, new ButtonListener() {
         @Override
         public void onClick() { updateISBNLabel(item, null, label); }
      }).show();
   }

   private void updateISBNLabel(@NonNull BookItem item, @Nullable ISBN isbn, @Nullable Label label) {
      item.row.setISBN(isbn).setLabel(label).update();
      booksAdapter.setData(item);
      booksAdapter.updateAsync(0);
   }

}