/*
 * AdminLabelsActivity.java
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
import de.fahimu.schlib.db.Book;
import de.fahimu.schlib.db.Label;

/**
 * Activity to administrate {@link Label}s.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class AdminLabelsActivity extends AdminSerialsActivity<Label> {

   @Override
   @LayoutRes
   protected int getContentViewId() { return R.layout.admin_labels; }

   @Override
   @IdRes
   int getListViewId() { return R.id.admin_labels_list; }

   @Override
   @StringRes
   int getEmptyStringId() { return R.string.admin_labels_empty; }

   @Override
   @WorkerThread
   ArrayList<Label> loadData() { return Label.get(); }

   @Override
   int canCallCreateOnePage() { return Label.canCallCreateOnePage(); }

   @Override
   int getMenuId() { return R.menu.admin_labels; }

   @Override
   int getMaxLength() { return 8; }

   @Nullable
   @Override
   Label getSerial(String barcode) { return Label.parse(barcode); }

   @Override
   int countPrinted() { return Label.countPrinted(); }

   @Override
   int deleteOnePage() { return Label.deleteOnePage(); }

   @Override
   int createOnePage() { return Label.createOnePage(); }

   @StringRes
   private static final int[] SNACKBAR_IDS = {
         R.string.snackbar_info_label_created_first,
         R.string.snackbar_info_label_created,
         R.string.snackbar_info_label_deleted,
         R.string.snackbar_info_label_deleted_last,
         R.string.snackbar_error_not_a_label
   };

   @Override
   int[] getSnackbarIds() { return SNACKBAR_IDS; }

   /** {@inheritDoc} */
   @Override
   void showErrorDialog(Label label) {
      NoFocusDialog dialog = new NoFocusDialog(this);
      if (label.isPrinted()) {
         dialog.setMessage(R.string.dialog_message_admin_labels_printed, label.getDisplayId());
      } else {
         dialog.setMessage(R.string.dialog_message_admin_labels_used, label.getDisplayId(),
               Book.getByLabel(label).getDisplay());
      }
      dialog.show();
   }

}