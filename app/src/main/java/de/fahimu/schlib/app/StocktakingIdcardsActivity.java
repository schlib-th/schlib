/*
 * StocktakingIdcardsActivity.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
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
   ArrayList<Idcard> loadData() { return Idcard.get(); }

   @Override
   Idcard getSerial(int number) { return Idcard.getNullable(number); }

   @StringRes
   private static final int[] SNACKBAR_IDS = {
         R.string.snackbar_error_not_a_idcard,
         R.string.snackbar_info_idcard_was_lost,
         R.string.snackbar_info_idcard_registered
   };

   @Override
   int[] getSnackbarIds() { return SNACKBAR_IDS; }

   /** {@inheritDoc} */
   @NonNull
   @Override
   NoFocusDialog configInfoDialog(@NonNull NoFocusDialog dialog, Idcard idcard) {
      dialog.setTitle(R.string.dialog_title_error);
      dialog.setMessage(R.string.dialog_message_idcard_used, User.getNonNull(idcard.getUid()).getDisplay());
      return dialog;
   }

}