/*
 * AdminUsersEditActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import de.fahimu.android.app.ListView.Adapter;
import de.fahimu.android.app.ListView.Item;
import de.fahimu.android.app.ListView.ViewHolder;
import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.android.app.scanner.NoFocusDialog.ButtonListener;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.Lending;
import de.fahimu.schlib.db.Use;
import de.fahimu.schlib.db.User;
import de.fahimu.schlib.db.User.Role;

import static de.fahimu.android.app.ListView.Adapter.RELOAD_DATA;
import static de.fahimu.schlib.db.User.Role.ADMIN;
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
         return Lending.getByUserWithDelay(prevUser);
      }

      @Override
      protected LendingItem createItem(Lending lending) {
         return new LendingItem(lending);
      }

      @Override
      protected void onUpdated(int flags, List<LendingItem> data) { }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private User prevUser, currUser;       // the edited user's attributes onCreate and the current attributes

   private boolean self, hasBooks;

   private TextView        userDisplay;
   private LendingsAdapter lendingsAdapter;
   private Button          deleteAccount, withdrawIdcard;

   private RadioGroup  roleGroup;
   private RadioButton roleTutor, roleAdmin;

   @Nullable
   private Idcard     prevIdcard;         // non null if this idcard should be set to lost
   @Nullable
   private LostDialog lostDialog;         // non null if we need to ask whether the previous idcard was lost

   @Override
   protected int getContentViewId() { return R.layout.admin_users_edit; }

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onCreate(savedInstanceState);
         prevUser = User.getNonNull(getIntent().getLongExtra("uid", -1L));
         currUser = User.getNonNull(getIntent().getLongExtra("uid", -1L));

         self = Use.getLoggedInNonNull().getUser().equals(prevUser);
         hasBooks = Lending.getByUser(prevUser, true).size() > 0;

         userDisplay = findView(TextView.class, R.id.admin_users_edit_user_display);
         lendingsAdapter = new LendingsAdapter();
         deleteAccount = findView(Button.class, R.id.admin_users_edit_delete_account);
         withdrawIdcard = findView(Button.class, R.id.admin_users_edit_withdraw_idcard);

         SpinnerAdapterListener spinnerAdapterListener = new SpinnerAdapterListener(this);
         Spinner nBooksSpinner = findView(Spinner.class, R.id.admin_users_edit_nbooks_spinner);
         nBooksSpinner.setAdapter(spinnerAdapterListener);
         nBooksSpinner.setSelection(prevUser.getNbooks() - 1);
         nBooksSpinner.setOnItemSelectedListener(spinnerAdapterListener);

         roleGroup = findView(RadioGroup.class, R.id.admin_users_edit_role);
         roleTutor = findView(RadioButton.class, R.id.admin_users_edit_tutor);
         roleAdmin = findView(RadioButton.class, R.id.admin_users_edit_admin);

         if (prevUser.hasIdcard()) {
            prevIdcard = prevUser.getIdcard();
            lostDialog = new LostDialog();
         }
      }
   }

   @Override
   protected void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
         refreshGUI();
         lendingsAdapter.updateAsync(RELOAD_DATA);

         Role role = currUser.getRole();
         if (role == PUPIL) {
            roleGroup.clearCheck();
         } else {
            roleGroup.check(role == TUTOR ? R.id.admin_users_edit_tutor : R.id.admin_users_edit_admin);
            roleGroup.setOnCheckedChangeListener(new RadioGroupListener());
         }
         roleTutor.setEnabled(!self && role != PUPIL);
         roleAdmin.setEnabled(!self && role != PUPIL);
      }
   }

   private void refreshGUI() {
      User user = currUser;
      userDisplay.setText(App.getStr(R.string.admin_users_edit_display, user.getDisplay(), user.getDisplayIdcard()));
      deleteAccount.setEnabled(!self && !hasBooks);
      withdrawIdcard.setEnabled(!self && user.hasIdcard());
   }

   @Override
   protected void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
         if (currUser == null) {
            prevUser.delete();
            if (prevIdcard != null) {
               prevIdcard.setLost(true).update();
            }
         } else if (currUser.equals(prevUser)) {
            scope.d("user NOT changed");
         } else {
            currUser.update();
            // set prevIdcard to lost if
            // a. the user had and idcard on starting the activity and it was never scanned (prevIdcard != null) and
            // b. now the user (currUser) doesn't have an idcard OR another one was assigned
            if (prevIdcard != null && (!currUser.hasIdcard() || currUser.getIdcardId() != prevUser.getIdcardId())) {
               prevIdcard.setLost(true).update();
            }
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   protected void onBarcode(String barcode) {
      Idcard idcard = Idcard.parse(barcode);
      if (lostDialog != null && lostDialog.isShown) {
         if (prevIdcard != null && prevIdcard.equals(idcard)) {
            prevIdcard = null;         // The previous idcard is obviously not lost.
            lostDialog.toggleButtons();
         }
      } else {
         if (prevIdcard != null && prevIdcard.equals(idcard)) {
            prevIdcard = null;         // The previous idcard is obviously not lost,
            lostDialog = null;         // so we don't need to question the user for it.
         }
         onBarcode(idcard);
      }
   }

   private final class LostDialog {
      private final NoFocusDialog dialog;
      private       boolean       isShown;

      LostDialog() {
         dialog = new NoFocusDialog(AdminUsersEditActivity.this).activateScannerListener();
      }

      private void show(ButtonListener buttonListener) {
         isShown = true;
         dialog.setMessage(R.string.dialog_message_admin_users_edit_idcard_lost, prevUser.getDisplayIdcard());
         dialog.setButton0(R.string.dialog_button0_admin_users_edit_idcard_lost, buttonListener);
         dialog.setButton1(R.string.dialog_button1_admin_users_edit_idcard_lost, buttonListener);
         dialog.show().setButtonEnabled(1, false);       // show dialog and disable button 1
      }

      private void toggleButtons() {
         dialog.setButtonEnabled(0, false).setButtonEnabled(1, true);
      }
   }

   private void possiblyShowLostDialogAndRun(@NonNull final Runnable action) {
      if (lostDialog == null) {
         action.run();
      } else {
         lostDialog.show(new ButtonListener() {
            @Override
            public void onClick(int id) {
               lostDialog = null;            // Don't question the user again
               action.run();
            }
         });
      }
   }

   private void onBarcode(final @Nullable Idcard idcard) {
      if (idcard == null) {
         showErrorSnackbar(R.string.snackbar_error_not_a_idcard);
      } else if (currUser.hasIdcard() && currUser.getIdcardId() == idcard.getId()) {
         showInfoSnackbar(R.string.admin_users_edit_snackbar_info_idcard_identical);
      } else if (idcard.isUsed() && idcard.getUid() != prevUser.getUid()) {
         NoFocusDialog dialog = new NoFocusDialog(this);
         dialog.setMessage(R.string.dialog_message_idcard_used, User.getByIdcard(idcard).getDisplay());
         dialog.show(R.raw.horn);
      } else {
         if (idcard.isLost()) {
            idcard.setLost(false).update();     // Surprise! The idcard isn't lost, set this idcard to 'Stocked'.
         } else if (idcard.isPrinted()) {
            // Promote all idcards from 'Printed' to 'Stocked' which are on the same page as the scanned idcard.
            Idcard.setStocked(idcard);
         }
         App.playSound(R.raw.bell);
         possiblyShowLostDialogAndRun(new Runnable() {
            @Override
            public void run() { reassignIdcard(idcard); }
         });
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onReassignIdcardClicked(View view) {
      showInfoSnackbar(R.string.admin_users_edit_snackbar_info_please_scan_idcard);
   }

   private void reassignIdcard(@NonNull Idcard currIdcard) {
      currUser.setIdcard(currIdcard);
      refreshGUI();
      showUndoSnackbar(App.getStr(R.string.snackbar_undo_action), new OnClickListener() {
         @Override
         public void onClick(View v) { restorePrevIdcard(); }
      }, R.string.admin_users_edit_snackbar_undo_idcard_reassigned, currIdcard.getDisplayId());
   }

   public void onWithdrawIdcardClicked(View view) {
      possiblyShowLostDialogAndRun(new Runnable() {
         @Override
         public void run() { withdrawIdcard(); }
      });
   }

   private void withdrawIdcard() {
      currUser.setIdcard(null);
      refreshGUI();
      showUndoSnackbar(App.getStr(R.string.snackbar_undo_action), new OnClickListener() {
         @Override
         public void onClick(View v) { restorePrevIdcard(); }
      }, R.string.admin_users_edit_snackbar_undo_idcard_withdrawed);
   }

   private void restorePrevIdcard() {
      currUser.setIdcard(prevUser.hasIdcard() ? prevUser.getIdcard() : null);
      refreshGUI();
      showInfoSnackbar(R.string.admin_users_edit_snackbar_info_idcard_restored);
   }

   public void onDeleteAccountClicked(View view) {
      possiblyShowLostDialogAndRun(new Runnable() {
         @Override
         public void run() { deleteAccount(); }
      });
   }

   private void deleteAccount() {
      NoFocusDialog dialog = new NoFocusDialog(this);
      dialog.setMessage(R.string.dialog_message_admin_users_edit_clicked_deleted, currUser.getDisplay());
      dialog.setButton0(R.string.app_no, null);
      dialog.setButton1(R.string.app_yes, new ButtonListener() {
         @Override
         public void onClick(int id) { currUser = null; finish(); }
      }).show();
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private final class SpinnerAdapterListener extends ArrayAdapter<String> implements OnItemSelectedListener {
      SpinnerAdapterListener(@NonNull Context context) {
         super(context, android.R.layout.simple_spinner_dropdown_item);
         add(getString(R.string.admin_users_edit_nbooks_book_1));
         for (int i = 0; i < 4; i++) {
            add(getString(R.string.admin_users_edit_nbooks_book_n, 2 + i));
         }
      }

      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
         currUser.setNbooks(position + 1);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {}
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private final class RadioGroupListener implements OnCheckedChangeListener {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
         currUser.setRole(checkedId == R.id.admin_users_edit_tutor ? TUTOR : ADMIN);
         refreshGUI();
      }
   }

}