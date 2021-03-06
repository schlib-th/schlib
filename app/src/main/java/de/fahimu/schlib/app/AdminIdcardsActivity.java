/*
 * AdminIdcardsActivity.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;


import java.util.ArrayList;

import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.User;

/**
 * Activity to administrate {@link Idcard}s.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class AdminIdcardsActivity extends AdminSerialsActivity<Idcard> {

   @Override
   @LayoutRes
   protected int getContentViewId() { return R.layout.admin_idcards; }

   @Override
   @IdRes
   int getListViewId() { return R.id.admin_idcards_list; }

   @Override
   @StringRes
   int getEmptyStringId() { return R.string.admin_idcards_empty; }

   @Override
   @WorkerThread
   ArrayList<Idcard> loadData() { return Idcard.get(); }

   @Override
   int canCallCreateOnePage() { return Idcard.canCallCreateOnePage(); }

   @Override
   int getMenuId() { return R.menu.admin_idcards; }

   @Override
   int getMaxLength() { return 5; }

   @Nullable
   @Override
   Idcard getSerial(String barcode) { return Idcard.parse(barcode); }

   @Override
   int countPrinted() { return Idcard.countPrinted(); }

   @Override
   int deleteOnePage() { return Idcard.deleteOnePage(); }

   @Override
   int createOnePage() { return Idcard.createOnePage(); }

   @StringRes
   private static final int[] SNACKBAR_IDS = {
         R.string.snackbar_info_idcard_created_first,
         R.string.snackbar_info_idcard_created,
         R.string.snackbar_info_idcard_deleted,
         R.string.snackbar_info_idcard_deleted_last,
         R.string.snackbar_error_not_a_idcard
   };

   @Override
   int[] getSnackbarIds() { return SNACKBAR_IDS; }

   /** {@inheritDoc} */
   @Override
   void showErrorDialog(Idcard idcard) {
      NoFocusDialog dialog = new NoFocusDialog(this);
      if (idcard.isPrinted()) {
         dialog.setMessage(R.string.dialog_message_admin_idcards_printed, idcard.getDisplayId());
      } else {
         dialog.setMessage(R.string.dialog_message_admin_idcards_used, idcard.getDisplayId(),
               User.getByIdcard(idcard).getDisplay());
      }
      dialog.show();
   }

}