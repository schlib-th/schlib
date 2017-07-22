/*
 * AdminUsersActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import de.fahimu.android.app.ListView.SearchableItem;
import de.fahimu.android.app.ListView.ViewHolder;
import de.fahimu.android.app.Log;
import de.fahimu.android.app.SearchString;
import de.fahimu.android.app.scanner.ScannerAwareSearchView;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.User;

/**
 * Activity for administrating users.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersActivity extends SchlibActivity {

   private final class UserItem extends SearchableItem<User> {
      UserItem(@NonNull User user) {
         super(user, user.getDisplay(), user.getDisplayIdcard());
      }
   }

   private final class UserViewHolder extends ViewHolder<UserItem> {
      private final TextView roleName, idcard;

      UserViewHolder(LayoutInflater inflater, ViewGroup parent) {
         super(inflater, parent, R.layout.row_user);
         roleName = App.findView(itemView, TextView.class, R.id.row_user_role_name);
         idcard = App.findView(itemView, TextView.class, R.id.row_user_idcard);

         final ImageButton action = App.findView(itemView, ImageButton.class, R.id.row_user_action);
         action.setImageResource(R.drawable.ic_info_black_24dp);
         action.setContentDescription(App.getStr(R.string.row_user_action_info));
      }

      protected void bind(UserItem item) {
         item.setText(0, roleName);
         item.setText(1, idcard);
      }
   }

   private final class UsersAdapter extends Adapter<User,UserItem,UserViewHolder> {

      UsersAdapter() {
         super(AdminUsersActivity.this, R.id.admin_users_list, R.string.admin_users_empty);
      }

      @Override
      protected UserViewHolder createViewHolder(LayoutInflater inflater, ViewGroup parent) {
         return new UserViewHolder(inflater, parent);
      }

      @Override
      protected ArrayList<User> loadData() { return User.getAll(); }

      @Override
      protected UserItem createItem(User user) { return new UserItem(user); }

      @Override
      protected void onUpdated(int flags, List<UserItem> data) {
         updateMenuItems();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private void updateMenuItems() {
      if (searchView != null) {          // safety net if updateMenuItems is called before onCreateOptionsMenu
         boolean searching = searchView.hasFocus() || !searchView.getQuery().isEmpty();
         create.setVisible(!searching);
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Filters the user list depending on the currently displayed queryText.
    */
   private final class UserItemFilter implements Filter<UserItem> {
      @NonNull
      private final String[] normalizedQueries = SearchString.getNormalizedQueries(searchView);

      @Override
      public boolean matches(UserItem item) {
         return item.contains(normalizedQueries);
      }
   }

   /* ============================================================================================================== */

   private UsersAdapter usersAdapter;

   private MenuItem create;

   private ScannerAwareSearchView searchView;

   @Override
   protected int getContentViewId() { return R.layout.admin_users; }

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      usersAdapter = new UsersAdapter();
   }

   @Override
   protected void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
      }
   }

   @Override
   protected void onPermissionGranted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         usersAdapter.updateAsync(Adapter.RELOAD_DATA, new UserItemFilter());
      }
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onCreateOptionsMenu(menu);
         getMenuInflater().inflate(R.menu.admin_users, menu);

         create = menu.findItem(R.id.action_create);
         searchView = new ScannerAwareSearchView(menu.findItem(R.id.action_search),
               this, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
               new InputFilter.LengthFilter(20), new SearchString.QueryTextFilter());

         usersAdapter.updateAsync(0, new UserItemFilter());
         return true;
      }
   }

   @Override
   protected void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
         if (searchView != null) { searchView.collapse(); }
      }
   }

   @Override
   protected void onBarcode(String barcode) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         if (searchView != null) {
            Idcard idcard = Idcard.parse(barcode);
            if (idcard == null) {
               showErrorSnackbar(R.string.snackbar_error_not_a_idcard);
            } else {
               searchView.expand(SerialNumber.getDecimal(idcard.getId()));
            }
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */
   /*  Override the methods called from {@link ScannerAwareSearchView}                                               */
   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   public void onFocusChange(View v, boolean hasFocus) {
      updateMenuItems();
   }

   @Override
   public void onQueryTextChange(String newText) {
      usersAdapter.updateAsync(0, new UserItemFilter());
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onCreateClicked(MenuItem item) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         startActivity(new Intent(this, AdminUsersAddActivity.class));
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onListItemClicked(@NonNull View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         long uid = usersAdapter.getRowByView(view).getUid();
         // TODO startActivity(new Intent(this, AdminUsersEditActivity.class).putExtra("uid", uid));
      }
   }

}