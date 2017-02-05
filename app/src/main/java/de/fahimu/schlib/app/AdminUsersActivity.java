/*
 * AdminSerialsActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

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
import android.widget.ImageView;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import de.fahimu.android.app.ListView.Adapter;
import de.fahimu.android.app.ListView.Filter;
import de.fahimu.android.app.ListView.Item;
import de.fahimu.android.app.ListView.ViewHolder;
import de.fahimu.android.app.Log;
import de.fahimu.android.app.SearchString;
import de.fahimu.android.app.scanner.ScannerAwareSearchView;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.User;
import de.fahimu.schlib.db.User.Role;

/**
 * An activity for administrating users.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersActivity extends SchlibActivity {

   final class UserItem extends Item<User> {
      UserItem(@NonNull User user) {
         super(user, user.getDisplayRoleName(), SerialNumber.getDisplay(user.getIdcards()));
      }
   }

   final class UserViewHolder extends ViewHolder<UserItem> {
      private final ImageView icon;
      private final TextView  roleName, idcards;

      UserViewHolder(LayoutInflater inflater, ViewGroup parent) {
         super(inflater, parent, R.layout.admin_users_row);
         icon = App.findView(itemView, ImageView.class, R.id.admin_users_row_icon);
         roleName = App.findView(itemView, TextView.class, R.id.admin_users_row_role_name);
         idcards = App.findView(itemView, TextView.class, R.id.admin_users_row_idcards);
      }

      protected void bind(UserItem item) {
         User user = item.row;
         if (user.getRole() != Role.PUPIL) {
            icon.setImageResource(R.drawable.ic_account);
            icon.setContentDescription(App.getStr(R.string.app_user_icon));
         } else {
            icon.setImageResource(R.drawable.ic_account_multiple);
            icon.setContentDescription(App.getStr(R.string.app_users_icon));
         }
         item.searchString.setText(0, roleName, user.getDisplayRoleName());
         item.searchString.setText(1, idcards, SerialNumber.getDisplay(user.getIdcards()));
      }
   }

   final class UsersAdapter extends Adapter<User,UserItem,AdminUsersActivity.UserViewHolder> {

      UsersAdapter() {
         super(AdminUsersActivity.this, R.id.admin_users_list, R.string.admin_users_empty);
      }

      @Override
      protected AdminUsersActivity.UserViewHolder createViewHolder(LayoutInflater inflater, ViewGroup parent) {
         return new AdminUsersActivity.UserViewHolder(inflater, parent);
      }

      @Override
      protected ArrayList<User> loadData() { return User.getGrouped(); }

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
   final class UserItemFilter implements Filter<UserItem> {
      @NonNull
      private final String[] normalizedQueries =
            (searchView == null) ? new String[0] : splitQueryString(searchView.getQuery());

      private String[] splitQueryString(String queryString) {
         return queryString.isEmpty() ? new String[0] : queryString.split(" ");
      }

      @Override
      public boolean matches(UserItem item) {
         return item.searchString.contains(normalizedQueries);
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
      setDisplayHomeAsUpEnabled(true);
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
            int number = SerialNumber.parseCode128(barcode);
            if (Idcard.getNullable(number) != null) {       // it's a serial from the expected type
               searchView.expand(new SerialNumber(number).getDecimal());
            } else {
               showErrorSnackbar(R.string.snackbar_error_not_a_idcard);
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
         scope.d("clicked on create");
         // TODO
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onListItemClicked(@NonNull View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         User user = usersAdapter.getItemByView(view).row;
         scope.d(user.getOid() + ": " + user.getDisplay());
         // TODO
      }
   }

}