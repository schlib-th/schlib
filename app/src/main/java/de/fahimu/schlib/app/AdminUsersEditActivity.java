/*
 * AdminUsersEditActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import de.fahimu.android.app.ListView.Adapter;
import de.fahimu.android.app.ListView.Item;
import de.fahimu.android.app.ListView.ViewHolder;
import de.fahimu.android.app.Log;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.Lending;
import de.fahimu.schlib.db.User;
import de.fahimu.schlib.db.User.Role;

import static de.fahimu.android.app.ListView.Adapter.RELOAD_DATA;
import static de.fahimu.schlib.db.User.Role.PUPIL;
import static de.fahimu.schlib.db.User.Role.TUTOR;

/**
 * An activity for editing users.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersEditActivity extends SchlibActivity {

   @ColorInt
   private static final int DELAY_00_COLOR = App.getColorFromRes(R.color.color_tutor_back_1);
   @ColorInt
   private static final int DELAY_07_COLOR = App.getColorFromRes(R.color.color_tutor_back_2);
   @ColorInt
   private static final int DELAY_28_COLOR = App.getColorFromRes(R.color.color_tutor_back_3);

   private final class LendingItem extends Item<Lending> {
      LendingItem(@NonNull Lending lending) {
         super(lending, lending.getDisplayPrevBook(), lending.getDisplayIssueReturnDelay());
      }
   }

   private final class LendingViewHolder extends ViewHolder<LendingItem> {
      private final TextView book, issueReturnDelay;

      LendingViewHolder(LayoutInflater inflater, ViewGroup parent) {
         super(inflater, parent, R.layout.row_lending_admin_users);
         book = App.findView(itemView, TextView.class, R.id.row_lending_admin_users_book);
         issueReturnDelay = App.findView(itemView, TextView.class, R.id.row_lending_admin_users_issue_return_delay);
      }

      protected void bind(LendingItem item) {
         book.setText(item.getText(0));
         issueReturnDelay.setText(item.getText(1));
         issueReturnDelay.setBackgroundTintList(buildBackgroundTintList(item.row));
      }

      private ColorStateList buildBackgroundTintList(Lending lending) {
         int delay = lending.isDelayed() ? lending.getDelay() : 0;
         if (delay < 0) {
            return ColorStateList.valueOf(DELAY_00_COLOR);
         } else if (delay < 7) {
            return ColorStateList.valueOf(getMixedColor(DELAY_00_COLOR, DELAY_07_COLOR, delay / 7.0));
         } else if (delay < 28) {
            return ColorStateList.valueOf(getMixedColor(DELAY_07_COLOR, DELAY_28_COLOR, (delay - 7) / 21.0));
         } else {
            return ColorStateList.valueOf(DELAY_28_COLOR);
         }
      }

      @ColorInt
      private int getMixedColor(@ColorInt int color0, @ColorInt int color1, double fraction) {
         return getMixedChannel(color0, color1, fraction, 24) | getMixedChannel(color0, color1, fraction, 16) |
               getMixedChannel(color0, color1, fraction, 8) | getMixedChannel(color0, color1, fraction, 0);
      }

      private int getMixedChannel(@ColorInt int color0, @ColorInt int color1, double fraction, int shift) {
         int channel0 = (color0 >> shift) & 0xff, channel1 = (color1 >> shift) & 0xff;
         return (channel0 + (int) (fraction * (channel1 - channel0))) << shift;
      }
   }

   private final class LendingsAdapter extends Adapter<Lending,LendingItem,LendingViewHolder> {

      LendingsAdapter() {
         super(AdminUsersEditActivity.this, R.id.admin_users_edit_lending_list, R.string.admin_users_edit_empty);
      }

      @Override
      protected LendingViewHolder createViewHolder(LayoutInflater inflater, ViewGroup parent) {
         return new LendingViewHolder(inflater, parent);
      }

      @Override
      protected ArrayList<Lending> loadData() {
         return Lending.getByUserWithDelay(user);
      }

      @Override
      protected LendingItem createItem(Lending lending) {
         return new LendingItem(lending);
      }

      @Override
      protected void onUpdated(int flags, List<LendingItem> data) { }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private User user;

   private LendingsAdapter lendingsAdapter;
   private Button          reassignIdcard, withdrawIdcard, deleteAccount;

   private RadioGroup  roleGroup;
   private RadioButton roleTutor, roleAdmin;

   @Override
   protected int getContentViewId() { return R.layout.admin_users_edit; }

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onCreate(savedInstanceState);
         user = User.getNonNull(getIntent().getLongExtra("uid", -1L));
         scope.d("user=" + user.getDisplay());

         lendingsAdapter = new LendingsAdapter();
         reassignIdcard = findView(Button.class, R.id.admin_users_edit_reassign_idcard);
         withdrawIdcard = findView(Button.class, R.id.admin_users_edit_withdraw_idcard);
         deleteAccount = findView(Button.class, R.id.admin_users_edit_delete_account);

         roleGroup = findView(RadioGroup.class, R.id.admin_users_edit_role);
         roleTutor = findView(RadioButton.class, R.id.admin_users_edit_tutor);
         roleAdmin = findView(RadioButton.class, R.id.admin_users_edit_admin);
      }
   }

   @Override
   protected void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
         lendingsAdapter.updateAsync(RELOAD_DATA);

         Role role = user.getRole();
         if (role == PUPIL) {
            roleGroup.clearCheck();
         } else {
            roleGroup.check(role == TUTOR ? R.id.admin_users_edit_tutor : R.id.admin_users_edit_admin);
            roleGroup.setOnCheckedChangeListener(new RadioGroupListener());
         }
         roleTutor.setEnabled(role != PUPIL);
         roleAdmin.setEnabled(role != PUPIL);
      }
   }

   @Override
   protected void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
      }
   }

   @Override
   protected void onBarcode(String barcode) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         Idcard idcard = Idcard.parse(barcode);
         if (idcard == null) {
            showErrorSnackbar(R.string.snackbar_error_not_a_idcard);
         } else {
            scope.d("idcard=" + SerialNumber.getDecimal(idcard.getId()));
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onReassignIdcardClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         scope.d("clicked");
      }
   }

   public void onWithdrawIdcardClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         scope.d("clicked");
      }
   }

   public void onDeleteAccountClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         scope.d("clicked");
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private final class RadioGroupListener implements OnCheckedChangeListener {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
         try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
            switch (checkedId) {
            case R.id.admin_users_edit_tutor:
               scope.d("role=TUTOR"); break;
            case R.id.admin_users_edit_admin:
               scope.d("role=ADMIN"); break;
            }
         }
      }
   }

}