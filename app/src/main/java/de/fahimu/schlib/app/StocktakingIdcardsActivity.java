/*
 * StocktakingIdcardsActivity.java
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
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.User;

/**
 * Activity for stocktaking {@link Idcard}s.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class StocktakingIdcardsActivity extends StocktakingSerialsActivity<Idcard> {

   @Override
   @LayoutRes
   protected int getContentViewId() { return R.layout.stocktaking_idcards; }

   @Override
   @IdRes
   int getListViewId() { return R.id.stocktaking_idcards_list; }

   @Override
   @StringRes
   int getEmptyStringId() { return R.string.stocktaking_idcards_empty; }

   @Override
   @WorkerThread
   ArrayList<Idcard> loadData() { return Idcard.getStocked(); }

   @Override
   Idcard getSerial(String barcode) { return Idcard.parse(barcode); }

   @StringRes
   private static final int[] SNACKBAR_IDS = {
         R.string.snackbar_error_not_a_idcard,
         R.string.snackbar_info_idcard_was_lost,
         R.string.snackbar_info_idcard_registered,
         R.string.stocktaking_idcards_snackbar_info_scanned,
         R.string.stocktaking_idcards_snackbar_info_already_scanned,
         R.string.stocktaking_idcards_snackbar_undo_lost,
         R.string.stocktaking_idcards_snackbar_info_stocked
   };

   @Override
   int[] getSnackbarIds() { return SNACKBAR_IDS; }

   /** {@inheritDoc} */
   @Override
   void showErrorDialog(Idcard idcard) {
      NoFocusDialog dialog = new NoFocusDialog(this);
      dialog.setMessage(R.string.dialog_message_idcard_used, User.getNonNull(idcard.getUid()).getDisplay());
      dialog.show(R.raw.horn);
   }

}