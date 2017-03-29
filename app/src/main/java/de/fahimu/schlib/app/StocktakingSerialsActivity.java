/*
 * StocktakingSerialsActivity.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
import de.fahimu.schlib.db.Serial;

/**
 * Base class for {@link StocktakingLabelsActivity} and {@link StocktakingIdcardsActivity}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
abstract class StocktakingSerialsActivity<S extends Serial> extends SchlibActivity {

   private final class SerialItem extends Item<S> {
      SerialItem(@NonNull S serial) {
         super(serial, serial.getDisplayId(), serial.getDisplay());
      }
   }

   private final class SerialItemViewHolder extends ViewHolder<SerialItem> {
      private final TextView key, info;
      private final ImageButton action;

      SerialItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
         super(inflater, parent, R.layout.row_serial);
         key = App.findView(itemView, TextView.class, R.id.row_serial_key);
         info = App.findView(itemView, TextView.class, R.id.row_serial_info);
         action = App.findView(itemView, ImageButton.class, R.id.row_serial_action);
      }

      protected void bind(SerialItem item) {
         key.setText(item.getText(0));
         info.setText(item.getText(1));
         if (item.row.isLost()) {
            action.setImageResource(R.drawable.ic_restore_black_24dp);
            action.setContentDescription(App.getStr(R.string.row_serial_action_restore));
         } else {       // item.row.isStocked()
            action.setImageResource(R.drawable.ic_delete_black_24dp);
            action.setContentDescription(App.getStr(R.string.row_serial_action_delete));
         }
      }
   }

   private final class SerialsAdapter extends Adapter<S,SerialItem,SerialItemViewHolder> {

      SerialsAdapter() {
         super(StocktakingSerialsActivity.this, getListViewId(), getEmptyStringId());
      }

      @Override
      protected SerialItemViewHolder createViewHolder(LayoutInflater inflater, ViewGroup parent) {
         return new SerialItemViewHolder(inflater, parent);
      }

      @Override
      protected ArrayList<S> loadData() {
         return StocktakingSerialsActivity.this.loadData();
      }

      @Override
      protected SerialItem createItem(S row) {
         return new SerialItem(row);
      }

      @Override
      protected void onUpdated(int flags, List<SerialItem> data) {
         if ((flags & DATA_INITIALIZED) == DATA_INITIALIZED) {
            // The SerialsFilter shows stocked and lost serials, but we don't want lost serials to be shown
            // that were already lost onResume(), so these serials must be added to the hiddenRows set.
            for (SerialItem item : data) {
               if (item.row.isLost()) { hiddenSerials.add(item.row.getId()); }
            }
         }
      }
   }

   @IdRes
   abstract int getListViewId();

   @StringRes
   abstract int getEmptyStringId();

   @WorkerThread
   abstract ArrayList<S> loadData();

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Shows only serials that are stocked or lost and not an element of the {@link #hiddenSerials} set.
    */
   private final class SerialItemFilter implements Filter<SerialItem> {
      /** Clone the {@link #hiddenSerials} set to prevent {@link ConcurrentModificationException}s */
      private final Set<Integer> hidden = new HashSet<>(hiddenSerials);

      @Override
      public boolean matches(SerialItem item) {
         return !item.row.isUsed() && !item.row.isPrinted() && !hidden.contains(item.row.getId());
      }
   }

   /**
    * Stores the {@link Serial#getId() ID} of serials that are scanned or lost {@link #onResume()}.
    */
   private final Set<Integer> hiddenSerials = new HashSet<>();

   /* ============================================================================================================== */

   private SerialsAdapter serialsAdapter;

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
         hiddenSerials.clear();
         // After resuming, we only show the stocked serials (not the lost ones).
         Filter<SerialItem> initializeFilter = new Filter<SerialItem>() {
            @Override
            public boolean matches(SerialItem item) { return item.row.isStocked(); }
         };
         serialsAdapter.updateAsync(DATA_INITIALIZED | Adapter.RELOAD_DATA, initializeFilter);
      }
   }

   private final static int DATA_NOT_CHANGED = 0x00;
   private final static int DATA_WAS_CHANGED = 0x01;
   private final static int DATA_INITIALIZED = DATA_WAS_CHANGED | 0x02;

   @Override
   protected final void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
      }
   }

   @Override
   public final void onBarcode(String barcode) {
      S serial = getSerial(barcode);
      if (serial == null) {
         showErrorSnackbar(getSnackbarIds()[0]);
      } else if (serial.isUsed()) {
         showErrorDialog(serial);
      } else {
         hiddenSerials.add(serial.getId());
         if (serial.isLost()) {
            serial.setLost(false).update();       // Surprise! The serial isn't lost, set this serial to 'Stocked'.
            showInfoSnackbar(getSnackbarIds()[1]);
            serialsAdapter.updateAsync(DATA_WAS_CHANGED | Adapter.RELOAD_DATA, new SerialItemFilter());
         } else if (serial.isPrinted()) {
            // Promote all serials from 'Printed' to 'Stocked' which are on the same page as the scanned serial.
            Serial.setStocked(serial);
            showInfoSnackbar(getSnackbarIds()[2]);
            serialsAdapter.updateAsync(DATA_WAS_CHANGED | Adapter.RELOAD_DATA, new SerialItemFilter());
         } else {          // isStocked()
            serialsAdapter.updateAsync(DATA_NOT_CHANGED, new SerialItemFilter());
         }
      }
   }

   @Nullable
   abstract S getSerial(String barcode);

   @StringRes
   abstract int[] getSnackbarIds();

   /**
    * Shows the ErrorDialog.
    * <p> Precondition: {@code serial.isUsed()}. </p>
    *
    * @param serial
    *       the serial.
    */
   abstract void showErrorDialog(S serial);

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onListItemClicked(@NonNull View view) {
      SerialItem item = serialsAdapter.getItemByView(view);
      // Assertion: item.row.isLost() || item.row.isStocked()
      item.row.setLost(!item.row.isLost()).update();           // toggle lost <-> stocked
      serialsAdapter.setData(item);
      serialsAdapter.updateAsync(DATA_WAS_CHANGED, new SerialItemFilter());
   }

}