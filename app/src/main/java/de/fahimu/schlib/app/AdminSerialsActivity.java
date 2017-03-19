/*
 * AdminSerialsActivity.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;
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
import de.fahimu.android.app.ListView.Item;
import de.fahimu.android.app.ListView.ViewHolder;
import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.android.app.scanner.ScannerAwareSearchView;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.db.Serial;

/**
 * Base class for {@link AdminLabelsActivity} and {@link AdminIdcardsActivity}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
abstract class AdminSerialsActivity<S extends Serial> extends SchlibActivity {

   private final class SerialItem extends Item<S> {
      final String key, info;

      @WorkerThread
      SerialItem(@NonNull S serial) {
         super(serial);
         key = serial.getDisplayId();
         info = serial.getDisplay();
      }
   }

   private final class SerialItemViewHolder extends ViewHolder<SerialItem> {
      private final TextView key, info;
      private final ImageButton action;

      SerialItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
         super(inflater, parent, R.layout.admin_serials_row);
         key = App.findView(itemView, TextView.class, R.id.admin_serials_row_key);
         info = App.findView(itemView, TextView.class, R.id.admin_serials_row_info);
         action = App.findView(itemView, ImageButton.class, R.id.admin_serials_row_action);
      }

      protected void bind(SerialItem item) {
         key.setText(item.key);
         info.setText(item.info);
         if (item.row.isLost()) {
            action.setImageResource(R.drawable.ic_restore_black_24dp);
            action.setContentDescription(App.getStr(R.string.admin_serials_row_action_restore));
         } else if (item.row.isStocked()) {
            action.setImageResource(R.drawable.ic_delete_black_24dp);
            action.setContentDescription(App.getStr(R.string.admin_serials_row_action_delete));
         } else {
            action.setImageResource(R.drawable.ic_info_black_24dp);
            action.setContentDescription(App.getStr(R.string.admin_serials_row_action_info));
         }
      }
   }

   private final class SerialsAdapter extends Adapter<S,SerialItem,SerialItemViewHolder> {

      SerialsAdapter() {
         super(AdminSerialsActivity.this, getListViewId(), getEmptyStringId());
      }

      @Override
      protected SerialItemViewHolder createViewHolder(LayoutInflater inflater, ViewGroup parent) {
         return new SerialItemViewHolder(inflater, parent);
      }

      @Override
      protected ArrayList<S> loadData() { return AdminSerialsActivity.this.loadData(); }

      @Override
      protected SerialItem createItem(S row) { return new SerialItem(row); }

      @Override
      protected void onUpdated(int flags, List<SerialItem> data) {
         if ((flags & DATA_WAS_CHANGED) == DATA_WAS_CHANGED) {
            filterPrinted.setVisible(containsPrinted(data));
            filterStocked.setVisible(containsStocked(data));
            filterUsed.setVisible(containsUsed(data));
            filterLost.setVisible(containsLost(data));
         }
         updateMenuItems();
      }
   }

   @IdRes
   abstract int getListViewId();

   @StringRes
   abstract int getEmptyStringId();

   @WorkerThread
   abstract ArrayList<S> loadData();

   private boolean containsPrinted(List<SerialItem> data) {
      for (SerialItem item : data) { if (item.row.isPrinted()) { return true; } } return false;
   }

   private boolean containsStocked(List<SerialItem> data) {
      for (SerialItem item : data) { if (item.row.isStocked()) { return true; } } return false;
   }

   private boolean containsUsed(List<SerialItem> data) {
      for (SerialItem item : data) { if (item.row.isUsed()) { return true; } } return false;
   }

   private boolean containsLost(List<SerialItem> data) {
      for (SerialItem item : data) { if (item.row.isLost()) { return true; } } return false;
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private void updateMenuItems() {
      if (searchView != null) {          // safety net if updateMenuItems is called before onCreateOptionsMenu
         boolean searching = searchView.hasFocus() || !searchView.getQuery().isEmpty();

         filter.setVisible(!searching);
         delete.setVisible(!searching && countPrinted() > 0);
         create.setVisible(!searching && canCallCreateOnePage() > 0);
      }
   }

   abstract int countPrinted();

   abstract int canCallCreateOnePage();

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Filters the serial lists depending on the currently displayed queryText and the filter menu.
    */
   private final class SerialItemFilter implements Filter<SerialItem> {

      private String queryText = "";
      private boolean isFilterPrintedChecked;
      private boolean isFilterStockedChecked;
      private boolean isFilterUsedChecked;
      private boolean isFilterLostChecked;

      SerialItemFilter() {
         if (searchView != null) {          // safety net if updateMenuItems is called before onCreateOptionsMenu
            this.queryText = searchView.getQuery();
            this.isFilterPrintedChecked = filterPrinted.isChecked();
            this.isFilterStockedChecked = filterStocked.isChecked();
            this.isFilterUsedChecked = filterUsed.isChecked();
            this.isFilterLostChecked = filterLost.isChecked();
         }
      }

      @Override
      public boolean matches(SerialItem item) {
         if (!queryText.isEmpty()) {
            return item.key.contains(queryText);
         } else if (item.row.isPrinted()) {
            return isFilterPrintedChecked;
         } else if (item.row.isLost()) {
            return isFilterLostChecked;
         } else if (item.row.isUsed()) {
            return isFilterUsedChecked;
         } else {        // listItem.serial.isStocked()
            return isFilterStockedChecked;
         }
      }
   }

   /* ============================================================================================================== */

   private SerialsAdapter serialsAdapter;

   private MenuItem filter, filterPrinted, filterStocked, filterUsed, filterLost;
   private MenuItem delete, create;

   private ScannerAwareSearchView searchView;

   @Override
   protected final void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      serialsAdapter = new SerialsAdapter();
   }

   @Override
   protected final void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
      }
   }

   @Override
   protected final void onPermissionGranted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         serialsAdapter.updateAsync(DATA_WAS_CHANGED | Adapter.RELOAD_DATA, new SerialItemFilter());
      }
   }

   private final static int DATA_NOT_CHANGED = 0x00;
   private final static int DATA_WAS_CHANGED = 0x01;

   @Override
   public final boolean onCreateOptionsMenu(final Menu menu) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onCreateOptionsMenu(menu);
         getMenuInflater().inflate(getMenuId(), menu);

         filter = menu.findItem(R.id.action_filter);
         filterPrinted = menu.findItem(R.id.action_filter_printed);
         filterStocked = menu.findItem(R.id.action_filter_stocked);
         filterUsed = menu.findItem(R.id.action_filter_used);
         filterLost = menu.findItem(R.id.action_filter_lost);

         delete = menu.findItem(R.id.action_delete);
         create = menu.findItem(R.id.action_create);

         searchView = new ScannerAwareSearchView(menu.findItem(R.id.action_search),
               this, InputType.TYPE_CLASS_NUMBER, new InputFilter.LengthFilter(getMaxLength()));

         serialsAdapter.updateAsync(DATA_NOT_CHANGED, new SerialItemFilter());
         return true;
      }
   }

   @MenuRes
   abstract int getMenuId();

   abstract int getMaxLength();

   @Override
   protected final void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
         if (searchView != null) { searchView.collapse(); }
      }
   }

   @Override
   protected final void onBarcode(String barcode) {
      if (searchView != null) {
         int number = SerialNumber.parseCode128(barcode);
         if (existsSerial(number)) {      // it's a serial from the expected type
            searchView.expand(new SerialNumber(number).getDecimal());
         } else {
            showErrorSnackbar(getSnackbarIds()[4]);
         }
      }
   }

   abstract boolean existsSerial(int number);

   /* -------------------------------------------------------------------------------------------------------------- */
   /*  Override the methods called from {@link ScannerAwareSearchView}                                               */
   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   public final void onFocusChange(View v, boolean hasFocus) {
      updateMenuItems();
   }

   @Override
   public final void onQueryTextChange(String newText) {
      serialsAdapter.updateAsync(DATA_NOT_CHANGED, new SerialItemFilter());
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public final void onFilterItemClicked(@NonNull MenuItem item) {
      item.setChecked(!item.isChecked());     // toggle the item
      serialsAdapter.updateAsync(DATA_NOT_CHANGED, new SerialItemFilter());
   }

   public final void onDeleteClicked(MenuItem item) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         int before = countPrinted();
         int after = before - deleteOnePage();
         updateMenuItems();
         showInfoSnackbar(before, after, getSnackbarIds());
         serialsAdapter.updateAsync(DATA_WAS_CHANGED | Adapter.RELOAD_DATA, new SerialItemFilter());
      }
   }

   public final void onCreateClicked(MenuItem item) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         int before = countPrinted();
         int after = before + createOnePage();
         updateMenuItems();
         showInfoSnackbar(before, after, getSnackbarIds());
         serialsAdapter.updateAsync(DATA_WAS_CHANGED | Adapter.RELOAD_DATA, new SerialItemFilter());
      }
   }

   private void showInfoSnackbar(int before, int after, @StringRes int[] strings) {
      if (before < after) {      // create
         if (before == 0) {
            showInfoSnackbar(strings[0], after - before);
         } else {
            showInfoSnackbar(strings[1], after - before, after);
         }
      } else {                   // delete
         if (after == 0) {
            showInfoSnackbar(strings[3]);
         } else {
            showInfoSnackbar(strings[2], before - after, after);
         }
      }
   }

   abstract int deleteOnePage();

   abstract int createOnePage();

   @StringRes
   abstract int[] getSnackbarIds();

   /* -------------------------------------------------------------------------------------------------------------- */

   public final void onListItemClicked(@NonNull View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         SerialItem item = serialsAdapter.getItemByView(view);
         S serial = item.row;
         if (serial.isUsed() || serial.isPrinted()) {
            configInfoDialog(new NoFocusDialog(this, NoFocusDialog.DEFAULT_CANCEL), serial).show();
         } else {
            serial.setLost(!serial.isLost()).update();           // toggle lost <-> stocked
            serialsAdapter.setData(item);
            serialsAdapter.updateAsync(DATA_WAS_CHANGED, new SerialItemFilter());
         }
      }
   }

   /**
    * Sets the {@link NoFocusDialog#setTitle(int, Object...) title} and
    * {@link NoFocusDialog#setMessage(int, Object...) message} of the InfoDialog.
    * <p> Precondition: {@code serial.isUsed() || serial.isPrinted()}. </p>
    *
    * @param dialog
    *       the dialog to configure.
    * @param serial
    *       the serial.
    * @return the specified {@code dialog}.
    */
   @NonNull
   abstract NoFocusDialog configInfoDialog(@NonNull NoFocusDialog dialog, S serial);

}