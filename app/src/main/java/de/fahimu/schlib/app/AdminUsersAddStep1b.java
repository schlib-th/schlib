/*
 * AdminUsersAddStep1b.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


import java.util.ArrayList;

import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.ScannerAwareEditText;
import de.fahimu.android.app.scanner.ScannerAwareEditText.AbstractTextWatcher;
import de.fahimu.android.app.scanner.ScannerAwareEditText.ColumnAdapter;
import de.fahimu.android.app.scanner.ScannerAwareEditText.ColumnItem;
import de.fahimu.android.db.SQLite;
import de.fahimu.schlib.anw.StringType;
import de.fahimu.schlib.db.User;

/**
 * Step 1b of adding users to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersAddStep1b extends StepFragment {

   @Override
   StepFragment getNext() { return nextFragment; }

   private final AdminUsersAddStep2 nextFragment = new AdminUsersAddStep2();

   @Override
   int getTabNameId() { return R.string.admin_users_add_step_1b_label; }

   /* ============================================================================================================== */

   @Override
   int getContentViewId() { return R.layout.admin_users_add_step_1b; }

   private ScannerAwareEditText name1;
   private RadioGroup           name2Group;
   private RadioButton          name2Year0;
   private RadioButton          name2Year1;
   private TextView             countText;
   private SeekBar              countSeek;

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);
         name1 = findView(ScannerAwareEditText.class, R.id.admin_users_add_step_1b_name1);
         name2Group = findView(RadioGroup.class, R.id.admin_users_add_step_1b_name2_group);
         name2Year0 = findView(RadioButton.class, R.id.admin_users_add_step_1b_name2_year_0);
         name2Year1 = findView(RadioButton.class, R.id.admin_users_add_step_1b_name2_year_1);
         countText = findView(TextView.class, R.id.admin_users_add_step_1b_count_text);
         countSeek = findView(SeekBar.class, R.id.admin_users_add_step_1b_count_seek);

         activity = (AdminUsersAddActivity) stepperActivity;

         name1.addTextChangedListener(new TextChangedListener());
         name1.setColumnAdapter(new PupilName1Adapter());
         name2Group.setOnCheckedChangeListener(new RadioGroupListener());
         countSeek.setOnSeekBarChangeListener(new SeekBarListener());
      }
   }

   private AdminUsersAddActivity activity;

   /* -------------------------------------------------------------------------------------------------------------- */

   private final class TextChangedListener extends AbstractTextWatcher {
      @Override
      public void afterTextChanged(Editable editable) {
         String text = editable.toString();
         // prevent entering of leading blanks (just because it doesn't make sense)
         if (!text.isEmpty() && text.trim().isEmpty()) { editable.clear(); }
         activity.name1 = name1.getText().toString().trim();
         Log.d("name1=" + activity.name1);
         updateCountMaxAndText();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private final static class PupilName1Item extends ColumnItem {
      PupilName1Item(@NonNull User user) {
         super(user, user.getName1());
      }
   }

   private final class PupilName1Adapter extends ColumnAdapter<PupilName1Item> {
      @Override
      protected void loadData(ArrayList<PupilName1Item> data) {
         ArrayList<User> users = User.getPupilsName1();
         for (User user : users) {
            data.add(new PupilName1Item(user));
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private final class RadioGroupListener implements OnCheckedChangeListener {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
         RadioButton button = (checkedId == R.id.admin_users_add_step_1b_name2_year_0 ? name2Year0 : name2Year1);
         activity.name2 = button.getText().toString();
         Log.d("name2=" + activity.name2);
         updateCountMaxAndText();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private int oldCount;

   private void updateCountMaxAndText() {
      oldCount = User.countPupils(activity.name1, activity.name2);
      int available = 100 - User.getNextAvailableSerial(activity.name1, activity.name2);
      countSeek.setMax(available > 40 ? 40 : available);
      updateCountText();
   }

   private void updateCountText() {
      final String text;
      final int newCount = activity.count;
      Log.d("oldCount=" + oldCount + ", newCount=" + newCount);
      if (newCount == 0) {
         text = App.getStr(R.string.admin_users_add_step_1b_count_error);
         activity.showErrorSnackbar(R.string.admin_users_add_step_1b_count_error);
      } else if (oldCount == 0) {
         if (newCount <= 1) {
            text = App.getStr(R.string.admin_users_add_step_1b_count_new_1);
         } else {
            text = App.getStr(R.string.admin_users_add_step_1b_count_new_2, newCount);
         }
      } else {
         if (newCount <= 1) {
            text = App.getStr(R.string.admin_users_add_step_1b_count_add_1, oldCount + 1);
         } else {
            text = App.getStr(R.string.admin_users_add_step_1b_count_add_2, newCount, oldCount + newCount);
         }
      }
      countText.setText(text);
      activity.refreshGUI();
   }

   private final class SeekBarListener implements OnSeekBarChangeListener {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
         activity.count = (countSeek.getMax() == 0) ? 0 : (progress == 0) ? 1 : progress;
         updateCountText();
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) { }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) { }
   }

   /* ============================================================================================================== */

   @Override
   void clearInput() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         name1.setText(""); name1.setError(null);
         // Get SQLite DATETIME('NOW') and set the previous and the next school year
         String now = SQLite.getDatetimeNow();
         int y = Integer.parseInt(now.substring(0, 4));
         int m = Integer.parseInt(now.substring(5, 7));
         name2Year0.setText(App.format("%d/%d", (y - 1), y % 100));
         name2Year1.setText(App.format("%d/%d", y, (y + 1) % 100));
         // From January to June PRESELECT the first school year, from July to December the second one.
         name2Group.clearCheck();
         name2Group.check(m <= 6 ? R.id.admin_users_add_step_1b_name2_year_0 :
                          R.id.admin_users_add_step_1b_name2_year_1);
         countSeek.setProgress(1);
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   boolean isDoneEnabled() {
      return !activity.name1.isEmpty() && !activity.name2.isEmpty() && activity.count > 0;
   }

   @Override
   boolean isDone() {
      int failPosition = StringType.CLASS.matches(activity.name1);
      if (failPosition >= 0) {
         name1.requestFocus();
         char illegalChar = activity.name1.charAt(failPosition);
         name1.setError(App.getStr(R.string.admin_users_add_step_1b_name1_error, illegalChar));
      }
      return failPosition < 0;
   }

}