/*
 * AdminLendingsActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import de.fahimu.android.app.ListView.Adapter;
import de.fahimu.android.app.ListView.Filter;
import de.fahimu.android.app.ListView.SearchableItem;
import de.fahimu.android.app.ListView.ViewHolder;
import de.fahimu.android.app.Log;
import de.fahimu.android.app.SearchString;
import de.fahimu.android.app.TaskRegistry;
import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.android.app.scanner.NoFocusDialog.ButtonListener;
import de.fahimu.android.app.scanner.ScannerAwareSearchView;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.Lending;
import de.fahimu.schlib.pdf.Document;
import de.fahimu.schlib.pdf.Document.WriterListener;
import de.fahimu.schlib.pdf.DunningLetters;
import de.fahimu.schlib.pdf.ReminderBooks;

/**
 * Activity to administrate {@link Lending}s.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.07.2017
 * @since SchoolLibrary 1.06
 */
public final class AdminLendingsActivity extends SchlibActivity {

   private final class LendingItem extends SearchableItem<Lending> {
      LendingItem(@NonNull Lending lending) {
         super(lending,
               lending.getBook().getDisplayShelfNumber(),
               lending.getUser().getDisplayIdcard(),
               lending.getBook().getTitle(), lending.getUser().getDisplay(),
               lending.getDisplayMultilineIssueDelayDun());
      }
   }

   private final class LendingViewHolder extends ViewHolder<LendingItem> {
      private final TextView shelfNumber, idcard, book, user, issueDelayDun;

      LendingViewHolder(LayoutInflater inflater, ViewGroup parent) {
         super(inflater, parent, R.layout.row_lending);
         shelfNumber = App.findView(itemView, TextView.class, R.id.row_lending_shelf_number);
         idcard = App.findView(itemView, TextView.class, R.id.row_lending_idcard);
         book = App.findView(itemView, TextView.class, R.id.row_lending_book);
         user = App.findView(itemView, TextView.class, R.id.row_lending_user);
         issueDelayDun = App.findView(itemView, TextView.class, R.id.row_lending_issue_delay_dun);
      }

      protected void bind(LendingItem item) {
         item.setText(0, shelfNumber);
         item.setText(1, idcard);
         item.setText(2, book);
         item.setText(3, user);
         item.setText(4, issueDelayDun);
      }
   }

   private final class LendingsAdapter extends Adapter<Lending,LendingItem,LendingViewHolder> {

      LendingsAdapter() {
         super(AdminLendingsActivity.this, R.id.admin_lendings_list, R.string.admin_lendings_empty);
      }

      @Override
      protected LendingViewHolder createViewHolder(LayoutInflater inflater, ViewGroup parent) {
         return new LendingViewHolder(inflater, parent);
      }

      @Override
      protected ArrayList<Lending> loadData() { return Lending.getIssuedOnlyWithDelay(); }

      @Override
      protected LendingItem createItem(Lending lending) { return new LendingItem(lending); }

      @Override
      protected void onUpdated(int flags, List<LendingItem> data) {
         count = getItemCount();
         updateMenuItemsAndButtons();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private volatile int     count;
   private volatile boolean searching, noDelay;

   private void updateMenuItemsAndButtons() {
      if (searchView != null) {          // safety net if updateMenuItems is called before onCreateOptionsMenu
         searching = searchView.hasFocus() || !searchView.getQuery().isEmpty();
         filter.setVisible(!searching);
         noDelay = filterNoDelay.isChecked();
      }
      updateButton(printList, R.string.admin_lendings_print_list_0,
            R.string.admin_lendings_print_list_1, R.string.admin_lendings_print_list_n, searching || noDelay);
      updateButton(printDuns, R.string.admin_lendings_print_duns_0,
            R.string.admin_lendings_print_duns_n, R.string.admin_lendings_print_duns_n, searching);
   }

   private void updateButton(Button button, @StringRes int resId0,
         @StringRes int resId1, @StringRes int resIdn, boolean disabled) {
      int count = disabled ? 0 : this.count;
      button.setEnabled(count > 0);
      button.setText(App.getStr(count == 0 ? resId0 : count == 1 ? resId1 : resIdn, count));
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Filters the lending list depending on the currently displayed queryText and the filter menu.
    */
   private final class LendingItemFilter implements Filter<LendingItem> {
      @NonNull
      private final String[] normalizedQueries = SearchString.getNormalizedQueries(searchView);

      private int minDelay = 0;

      LendingItemFilter() {
         if (searchView != null) {          // safety net if updateMenuItems is called before onCreateOptionsMenu
            minDelay = filterDelay28.isChecked() ? 28 :
                       filterDelay21.isChecked() ? 21 :
                       filterDelay14.isChecked() ? 14 :
                       filterDelay07.isChecked() ? 7 : 0;
         }
      }

      @Override
      public boolean matches(LendingItem item) {
         boolean contains = item.contains(normalizedQueries);
         if (normalizedQueries.length > 0) {
            return contains;
         } else {
            return minDelay == 0 || item.row.isDelayed(minDelay);
         }
      }
   }

   /* ============================================================================================================== */

   private LendingsAdapter lendingsAdapter;
   private Button          printList, printDuns;

   private MenuItem filter, filterDelay28, filterDelay21, filterDelay14, filterDelay07, filterNoDelay;

   private ScannerAwareSearchView searchView;

   private final TaskRegistry taskRegistry = new TaskRegistry();

   @Override
   protected int getContentViewId() { return R.layout.admin_lendings; }

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      lendingsAdapter = new LendingsAdapter();
      printList = findView(Button.class, R.id.admin_lendings_print_list);
      printDuns = findView(Button.class, R.id.admin_lendings_print_duns);
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
         lendingsAdapter.updateAsync(Adapter.RELOAD_DATA, new LendingItemFilter());
      }
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onCreateOptionsMenu(menu);
         getMenuInflater().inflate(R.menu.admin_lendings, menu);

         filter = menu.findItem(R.id.action_filter);
         filterDelay28 = menu.findItem(R.id.action_filter_delay_28);
         filterDelay21 = menu.findItem(R.id.action_filter_delay_21);
         filterDelay14 = menu.findItem(R.id.action_filter_delay_14);
         filterDelay07 = menu.findItem(R.id.action_filter_delay_07);
         filterNoDelay = menu.findItem(R.id.action_filter_no_delay);

         searchView = new ScannerAwareSearchView(menu.findItem(R.id.action_search),
               this, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
               new InputFilter.LengthFilter(20), new SearchString.QueryTextFilter());

         if (containsDelay07()) {
            filterDelay07.setChecked(true);
         } else {
            filterNoDelay.setChecked(true);
         }
         lendingsAdapter.updateAsync(0, new LendingItemFilter());
         return true;
      }
   }

   private boolean containsDelay07() {
      ArrayList<Lending> lendings = Lending.getIssuedOnlyWithDelay();
      for (Lending lending : lendings) {
         if (lending.isDelayed(7)) { return true; }
      }
      return false;
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
      if (searchView != null) {
         Idcard idcard = Idcard.parse(barcode);
         if (idcard == null) {
            showErrorSnackbar(R.string.snackbar_error_not_a_idcard);
         } else {
            searchView.expand(SerialNumber.getDecimal(idcard.getId()));
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */
   /*  Override the methods called from {@link ScannerAwareSearchView}                                               */
   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   public void onFocusChange(View v, boolean hasFocus) {
      updateMenuItemsAndButtons();
   }

   @Override
   public void onQueryTextChange(String newText) {
      lendingsAdapter.updateAsync(0, new LendingItemFilter());
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onFilterItemClicked(@NonNull MenuItem item) {
      item.setChecked(!item.isChecked());     // toggle the item
      updateMenuItemsAndButtons();
      lendingsAdapter.updateAsync(0, new LendingItemFilter());
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onListItemClicked(@NonNull View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         final Lending lending = lendingsAdapter.getRowByView(view);

         NoFocusDialog dialog = new NoFocusDialog(this);
         dialog.setMessage(R.string.dialog_message_admin_lendings_delete,
               lending.getUser().getDisplay(), lending.getBook().getDisplay());
         dialog.setButton0(R.string.dialog_button0_admin_lendings_delete, null);
         dialog.setButton1(R.string.dialog_button1_admin_lendings_delete, new ButtonListener() {
            @Override
            public void onClick() { cancel(lending); }
         }).show(R.raw.horn);
      }
   }

   private void cancel(final Lending lending) {
      long canceled = Math.max(lending.getMinReturn(), App.posixTime());
      lending.setCanceled(canceled).update();
      lending.getBook().setVanished(canceled).update();
      lendingsAdapter.updateAsync(Adapter.RELOAD_DATA, new LendingItemFilter());
      showUndoSnackbar(App.getStr(R.string.snackbar_undo_action), new OnClickListener() {
         @Override
         public void onClick(View v) { restore(lending); }
      }, R.string.admin_lendings_snackbar_undo_lending_canceled);
   }

   private void restore(Lending lending) {
      lending.setCanceled(null).update();
      lending.getBook().setVanished(null).update();
      lendingsAdapter.updateAsync(Adapter.RELOAD_DATA, new LendingItemFilter());
      showInfoSnackbar(R.string.admin_lendings_snackbar_info_lending_restored);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private List<Long> getFilteredRids() {
      int size = lendingsAdapter.getItemCount();
      ArrayList<Long> rids = new ArrayList<>(size);
      for (int pos = 0; pos < size; pos++) {
         rids.add(lendingsAdapter.getItemId(pos));
      }
      return rids;
   }

   public void onPrintListClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         printDocument(new ReminderBooks(getFilteredRids()), 0,
               R.string.dialog_message_admin_lendings_printing_list,
               R.string.dialog_button1_admin_lendings_printing_list,
               new ButtonListener() {
                  @Override
                  public void onClick() { taskRegistry.cancel(); }
               });
      }
   }

   public void onPrintDunsClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         List<Long> rids = getFilteredRids();
         Lending.resetDunned(); Lending.setDunned(rids);
         lendingsAdapter.updateAsync(Adapter.RELOAD_DATA, new LendingItemFilter());

         printDocument(new DunningLetters(rids), rids.size(),
               R.string.dialog_message_admin_lendings_printing_duns,
               R.string.dialog_button1_admin_lendings_printing_duns,
               new ButtonListener() {
                  @Override
                  public void onClick() {
                     taskRegistry.cancel();
                     Lending.resetDunned();
                     lendingsAdapter.updateAsync(Adapter.RELOAD_DATA, new LendingItemFilter());
                  }
               });
      }
   }

   private void printDocument(Document document, final int pages, @StringRes int msgId,
         @StringRes final int button1, ButtonListener button0Listener) {
      final NoFocusDialog dialog = new NoFocusDialog(this);
      dialog.setMessage(msgId);
      dialog.setButton0(R.string.app_cancel, button0Listener);
      dialog.setButton1(R.string.dialog_button1_admin_lendings_printing_init, null);
      dialog.show().setButtonEnabled(1, false);    // show dialog and disable button 1

      Document.writeAsync(taskRegistry, new WriterListener() {
         private int page = 0;
         private long lastUpdateTime = 0;

         @Override
         public void onPageWrite() {
            page += 1;
            if (pages == 0 || page == pages || SystemClock.uptimeMillis() > lastUpdateTime + 500) {
               lastUpdateTime = SystemClock.uptimeMillis();
               dialog.setButtonText(1, button1, page, pages);
            }
         }

         @Override
         public void onPostWrite() {
            dialog.setButtonEnabled(0, false).setButtonEnabled(1, true);
            dialog.setButtonText(1, R.string.app_done);
         }
      }, document);
   }

}