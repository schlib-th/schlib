/*
 * StocktakingLabelsActivity.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;


import java.util.ArrayList;

import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.schlib.db.Book;
import de.fahimu.schlib.db.Label;

/**
 * Activity for stocktaking {@link Label}s.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class StocktakingLabelsActivity extends StocktakingSerialsActivity<Label> {

   @Override
   @LayoutRes
   protected int getContentViewId() { return R.layout.stocktaking_labels; }

   @Override
   @IdRes
   int getListViewId() { return R.id.stocktaking_labels_list; }

   @Override
   @StringRes
   int getEmptyStringId() { return R.string.stocktaking_labels_empty; }

   @Override
   @WorkerThread
   ArrayList<Label> loadData() { return Label.getStocked(); }

   @Override
   Label getSerial(String barcode) { return Label.parse(barcode); }

   @StringRes
   private static final int[] SNACKBAR_IDS = {
         R.string.snackbar_error_not_a_label,
         R.string.snackbar_info_label_was_lost,
         R.string.snackbar_info_label_registered,
         R.string.stocktaking_labels_snackbar_info_scanned,
         R.string.stocktaking_labels_snackbar_info_already_scanned,
         R.string.stocktaking_labels_snackbar_undo_lost,
         R.string.stocktaking_labels_snackbar_info_stocked
   };

   @Override
   int[] getSnackbarIds() { return SNACKBAR_IDS; }

   /** {@inheritDoc} */
   @Override
   void showErrorDialog(Label label) {
      NoFocusDialog dialog = new NoFocusDialog(this);
      dialog.setMessage(R.string.dialog_message_label_used, Book.getNonNull(label.getBid()).getDisplay());
      dialog.show(R.raw.horn);
   }

}