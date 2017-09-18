/*
 * StocktakingUsersActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
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
import de.fahimu.android.app.TaskRegistry;
import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.android.app.scanner.NoFocusDialog.ButtonListener;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.Lending;
import de.fahimu.schlib.db.User;
import de.fahimu.schlib.db.User.Role;
import de.fahimu.schlib.pdf.Document;
import de.fahimu.schlib.pdf.Document.WriterListener;
import de.fahimu.schlib.pdf.ReminderIdcards;

import static de.fahimu.android.app.ListView.Adapter.RELOAD_DATA;
import static de.fahimu.android.app.ListView.Adapter.SHOW_DELAYED;

/**
 * Activity for stocktaking {@link User}s where {@link User#getRole()} is {@link Role#PUPIL}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class StocktakingUsersActivity extends SchlibActivity {

   private final class UserItem extends Item<User> {
      UserItem(@NonNull User user) {
         super(user, user.getDisplay(), user.getDisplayMultilineIdcardIssued());
      }
   }

   private final class UserItemViewHolder extends ViewHolder<UserItem> {
      private final TextView roleName, idcardIssued;
      private final ImageButton action;

      UserItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
         super(inflater, parent, R.layout.row_user_stocktaking);
         roleName = App.findView(itemView, TextView.class, R.id.row_user_stocktaking_role_name);
         idcardIssued = App.findView(itemView, TextView.class, R.id.row_user_stocktaking_idcard_issued);
         action = App.findView(itemView, ImageButton.class, R.id.row_user_stocktaking_action);
      }

      protected void bind(UserItem item) {
         roleName.setText(item.getText(0));
         idcardIssued.setText(item.getText(1));
         boolean enabled = item.row.hasIdcard() || !item.row.hasBooks();
         action.setEnabled(enabled);
         action.setImageAlpha(enabled ? 255 : 51);
      }
   }

   private final class UsersAdapter extends Adapter<User,UserItem,UserItemViewHolder> {

      UsersAdapter() {
         super(StocktakingUsersActivity.this, R.id.stocktaking_users_list, R.string.stocktaking_users_empty);
      }

      @Override
      protected UserItemViewHolder createViewHolder(LayoutInflater inflater, ViewGroup parent) {
         return new UserItemViewHolder(inflater, parent);
      }

      @Override
      protected ArrayList<User> loadData() {
         return User.getPupilsForStocktaking();
      }

      @Override
      protected UserItem createItem(User user) {
         return new UserItem(user);
      }

      @Override
      protected void onUpdated(int flags, List<UserItem> data) {
         usersWithIdcards.clear();
         for (UserItem item : data) {
            if (item.row.hasIdcard()) {
               usersWithIdcards.add(item.row);
            }
         }
         int count = usersWithIdcards.size();
         printList.setEnabled(count > 0);
         printList.setText(App.getStr(count == 0 ? R.string.stocktaking_users_print_list_0 :
                                      count == 1 ? R.string.stocktaking_users_print_list_1 :
                                      R.string.stocktaking_users_print_list_n, count));
      }
   }

   /* ============================================================================================================== */

   private UsersAdapter usersAdapter;
   private Button       printList;

   private final List<User> usersWithIdcards = new ArrayList<>();

   private final TaskRegistry taskRegistry = new TaskRegistry();

   @Override
   @LayoutRes
   protected int getContentViewId() { return R.layout.stocktaking_users; }

   @Override
   protected final void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      usersAdapter = new UsersAdapter();
      printList = findView(Button.class, R.id.stocktaking_users_print_list);
   }

   @Override
   protected final void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
      }
   }

   @Override
   protected void onPermissionGranted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         usersAdapter.updateAsync(RELOAD_DATA);
      }
   }

   @Override
   protected final void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
      }
   }

   private boolean deleteNoConfirmation;

   private final ButtonListener noButtonListener = new ButtonListener() {
      @Override
      public void onClick(int id) { usersAdapter.setSelection(-1); }
   };

   @Override
   protected void onBarcode(String barcode) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         Idcard idcard = Idcard.parse(barcode);
         if (idcard == null) {
            showErrorSnackbar(R.string.snackbar_error_not_a_idcard);
         } else if (idcard.isUsed()) {
            final User user = User.getByIdcard(idcard);
            if (user.getRole() != Role.PUPIL) {
               showErrorSnackbar(R.string.stocktaking_users_snackbar_error_no_pupil, user.getDisplay());
            } else {
               usersAdapter.setSelection(user.getOid());
               if (deleteNoConfirmation) {
                  deleteScanned(user, SHOW_DELAYED);
               } else {
                  NoFocusDialog dialog = new NoFocusDialog(this);
                  dialog.setMessage(R.string.dialog_message_stocktaking_users_scanned, user.getDisplay());
                  dialog.setButton0(R.string.app_no, noButtonListener);
                  dialog.setButton1(R.string.app_yes, new ButtonListener() {
                     @Override
                     public void onClick(int id) {
                        deleteNoConfirmation = true;
                        deleteScanned(user, 0);
                     }
                  }).show(R.raw.horn);
               }
            }
         } else if (idcard.isLost()) {
            idcard.setLost(false).update();       // Surprise! The idcard isn't lost, set this idcard to 'Stocked'.
            showInfoSnackbar(R.string.snackbar_info_idcard_was_lost);
         } else if (idcard.isPrinted()) {
            // Promote all idcards from 'Printed' to 'Stocked' which are on the same page as the scanned idcard.
            Idcard.setStocked(idcard);
            showInfoSnackbar(R.string.snackbar_info_idcard_registered);
         } else {
            showInfoSnackbar(R.string.stocktaking_users_snackbar_info_stocked_already);
         }
      }
   }

   private void deleteScanned(@NonNull User user, int flags) {
      // user.hasBooks() can only be called on list items, not on scanned Users
      if (Lending.getByUser(user, true).size() > 0) {
         showInfoSnackbar(R.string.stocktaking_users_snackbar_info_stocked, user.getDisplayIdcard());
         user.setIdcard(null).update();      // set idcard to stocked
      } else {
         showInfoSnackbar(R.string.stocktaking_users_snackbar_info_deleted_stocked, user.getDisplay());
         user.delete();
      }
      usersAdapter.updateAsync(flags | RELOAD_DATA);
   }

   public void onListItemClicked(@NonNull View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         User user = usersAdapter.getRowByView(view);
         usersAdapter.setSelection(user.getOid());
         if (user.hasBooks()) {              //   hasBooks &&   hasIdcard
            deleteClicked(user, R.string.dialog_message_stocktaking_users_clicked_lost,
                  true, false, R.string.stocktaking_users_snackbar_info_lost);
         } else if (user.hasIdcard()) {      // ! hasBooks &&   hasIdcard
            deleteClicked(user, R.string.dialog_message_stocktaking_users_clicked_lost_deleted,
                  true, true, R.string.stocktaking_users_snackbar_info_lost_deleted);
         } else {                            // ! hasBooks && ! hasIdcard
            deleteClicked(user, R.string.dialog_message_stocktaking_users_clicked_deleted,
                  false, true, R.string.stocktaking_users_snackbar_info_deleted);
         }
      }
   }

   private void deleteClicked(@NonNull final User user, @StringRes int messageId,
         final boolean setLost, final boolean delete, @StringRes final int snackbarId) {
      NoFocusDialog dialog = new NoFocusDialog(this);
      dialog.setMessage(messageId, user.getDisplay());
      dialog.setButton0(R.string.app_no, noButtonListener);
      dialog.setButton1(R.string.app_yes, new ButtonListener() {
         @Override
         public void onClick(int id) {
            if (setLost) {
               user.getIdcard().setLost(true).update();
            }
            if (delete) {
               user.delete();
               usersAdapter.updateAsync(RELOAD_DATA);
            } else {
               user.setIdcard(null).update();
               usersAdapter.setData(user);
               usersAdapter.updateAsync(0);
            }
            showInfoSnackbar(snackbarId);
         }
      }).show();
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onPrintListClicked(View view) {
      NoFocusDialog dialog = new NoFocusDialog(this);
      dialog.setMessage(R.string.dialog_message_stocktaking_users_print_reminder, usersWithIdcards.size());
      dialog.setButton0(R.string.app_no, null);
      dialog.setButton1(R.string.app_yes, new ButtonListener() {
         @Override
         public void onClick(int id) { printReminder(); }
      }).show();
   }

   public void printReminder() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         final NoFocusDialog dialog = new NoFocusDialog(this);
         dialog.setMessage(R.string.dialog_message_stocktaking_users_printing_list);
         dialog.setButton0(R.string.app_cancel, new ButtonListener() {
            @Override
            public void onClick(int id) { taskRegistry.cancel(); }
         });
         dialog.setButton1(R.string.dialog_button1_stocktaking_users_printing_init, null);
         dialog.show().setButtonEnabled(1, false);    // show dialog and disable button 1

         Document.writeAsync(taskRegistry, new WriterListener() {
            private int page = 0;

            @Override
            public void onPageWrite() {
               dialog.setButtonText(1, R.string.dialog_button1_stocktaking_users_printing_list, ++page);
            }

            @Override
            public void onPostWrite() {
               dialog.setButtonEnabled(0, false).setButtonEnabled(1, true);
               dialog.setButtonText(1, R.string.app_done);
            }
         }, new ReminderIdcards(usersWithIdcards));
      }
   }

}