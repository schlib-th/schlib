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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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

import static de.fahimu.android.app.ListView.Adapter.RELOAD_DATA;
import static de.fahimu.android.app.ListView.Adapter.SHOW_DELAYED;

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

      SerialItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
         super(inflater, parent, R.layout.row_serial);
         key = App.findView(itemView, TextView.class, R.id.row_serial_key);
         info = App.findView(itemView, TextView.class, R.id.row_serial_info);
      }

      protected void bind(SerialItem item) {
         key.setText(item.getText(0));
         info.setText(item.getText(1));
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
      protected void onUpdated(int flags, List<SerialItem> data) { }
   }

   @IdRes
   abstract int getListViewId();

   @StringRes
   abstract int getEmptyStringId();

   @WorkerThread
   abstract ArrayList<S> loadData();

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Shows only serials that are not an element of the {@link #scannedSerials} set.
    */
   private final class SerialItemFilter implements Filter<SerialItem> {
      /** Clone the {@link #scannedSerials} set to prevent {@link ConcurrentModificationException}s */
      private final Set<Long> serials = new HashSet<>(scannedSerials);

      @Override
      public boolean matches(SerialItem item) {
         return !serials.contains(item.rid);
      }
   }

   private final Set<Long> scannedSerials = new HashSet<>();      // OID of scanned serials

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
         scannedSerials.clear();
         serialsAdapter.updateAsync(RELOAD_DATA);
      }
   }

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
         boolean added = scannedSerials.add(serial.getOid());
         if (serial.isLost()) {
            serial.setLost(false).update();       // Surprise! The serial isn't lost, set this serial to 'Stocked'.
            showInfoSnackbar(getSnackbarIds()[1]);
            serialsAdapter.updateAsync(RELOAD_DATA, new SerialItemFilter());
         } else if (serial.isPrinted()) {
            // Promote all serials from 'Printed' to 'Stocked' which are on the same page as the scanned serial.
            Serial.setStocked(serial);
            showInfoSnackbar(getSnackbarIds()[2]);
            serialsAdapter.updateAsync(RELOAD_DATA, new SerialItemFilter());
         } else {          // isStocked()
            showInfoSnackbar(getSnackbarIds()[added ? 3 : 4], serial.getDisplayId());
            serialsAdapter.setSelection(serial.getOid());
            serialsAdapter.updateAsync(SHOW_DELAYED, new SerialItemFilter());
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
      final S serial = serialsAdapter.getItemByView(view).row;
      serial.setLost(true).update();
      serialsAdapter.updateAsync(RELOAD_DATA, new SerialItemFilter());
      showUndoSnackbar(App.getStr(R.string.snackbar_undo_action), new OnClickListener() {
         @Override
         public void onClick(View v) { restore(serial); }
      }, getSnackbarIds()[5], serial.getDisplayId());
   }

   private void restore(S serial) {
      serial.setLost(false).update();
      serialsAdapter.updateAsync(RELOAD_DATA, new SerialItemFilter());
      showInfoSnackbar(getSnackbarIds()[6], serial.getDisplayId());
   }

}