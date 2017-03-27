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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import de.fahimu.android.app.ListView.Adapter;
import de.fahimu.android.app.ListView.Item;
import de.fahimu.android.app.ListView.ViewHolder;
import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.db.Book;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.Lending;
import de.fahimu.schlib.db.User;
import de.fahimu.schlib.db.User.Role;

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
         super(user, user.getDisplay(), new SerialNumber(user.getIdcard()).getDisplay());
      }
   }

   private final class UserViewHolder extends ViewHolder<UserItem> {
      private final TextView roleName, idcard;

      UserViewHolder(LayoutInflater inflater, ViewGroup parent) {
         super(inflater, parent, R.layout.row_user);
         roleName = App.findView(itemView, TextView.class, R.id.row_user_role_name);
         idcard = App.findView(itemView, TextView.class, R.id.row_user_idcard);

         ImageButton action = App.findView(itemView, ImageButton.class, R.id.row_user_action);
         action.setImageResource(R.drawable.ic_delete_black_24dp);
         action.setContentDescription(App.getStr(R.string.row_user_action_delete));
      }

      protected void bind(UserItem item) {
         roleName.setText(item.getText(0));
         idcard.setText(item.getText(1));
      }
   }

   private final class UsersAdapter extends Adapter<User,UserItem,UserViewHolder> {

      UsersAdapter() {
         super(StocktakingUsersActivity.this, R.id.stocktaking_users_list, R.string.stocktaking_users_empty);
      }

      @Override
      protected UserViewHolder createViewHolder(LayoutInflater inflater, ViewGroup parent) {
         return new UserViewHolder(inflater, parent);
      }

      @Override
      protected ArrayList<User> loadData() { return User.getPupils(); }

      @Override
      protected UserItem createItem(User user) { return new UserItem(user); }

      @Override
      protected void onUpdated(int flags, List<UserItem> data) { }
   }

   /* ============================================================================================================== */

   private UsersAdapter usersAdapter;

   @Override
   @LayoutRes
   protected int getContentViewId() { return R.layout.stocktaking_users; }

   @Override
   protected final void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      usersAdapter = new UsersAdapter();
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
         usersAdapter.updateAsync(Adapter.RELOAD_DATA);
      }
   }

   @Override
   protected final void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
      }
   }

   @Override
   protected void onBarcode(String barcode) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         Idcard idcard = Idcard.getNullable(SerialNumber.parseCode128(barcode));
         if (idcard == null) {
            showErrorSnackbar(R.string.snackbar_error_not_a_idcard);
         } else if (idcard.isUsed()) {
            User user = User.getNonNull(idcard.getUid());
            if (user.getRole() != Role.PUPIL) {
               showErrorSnackbar(R.string.stocktaking_users_snackbar_error_no_pupil, user.getDisplay());
            } else {
               ArrayList<Lending> lendings = Lending.getUsersLendings(user.getUid(), true);
               if (lendings.isEmpty()) {
                  showInfoSnackbar(R.string.stocktaking_users_snackbar_info_deleted, user.getDisplay());
                  user.delete();
                  usersAdapter.updateAsync(Adapter.RELOAD_DATA);
               } else {
                  // user book size
                  NoFocusDialog dialog = new NoFocusDialog(this);
                  if (lendings.size() == 1) {
                     dialog.setMessage(R.string.dialog_message_stocktaking_users_book_issued,
                           user.getDisplay(), Book.getNonNull(lendings.get(0).getBid()).getDisplay());
                  } else {
                     dialog.setMessage(R.string.dialog_message_stocktaking_users_books_issued,
                           user.getDisplay(), Book.getNonNull(lendings.get(0).getBid()).getDisplay(), lendings.size());
                  }
                  dialog.show(R.raw.horn);
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
            showInfoSnackbar(R.string.stocktaking_users_snackbar_info_stocked);
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onListItemClicked(@NonNull View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         User user = usersAdapter.getItemByView(view).row;
         showInfoSnackbar(R.string.stocktaking_users_snackbar_info_deleted_lost);
         Idcard.getNonNull(user.getIdcard()).setLost(true).update();
         user.delete();
         usersAdapter.updateAsync(Adapter.RELOAD_DATA);
      }
   }

}