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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.ScannerAwareEditText;
import de.fahimu.android.app.scanner.ScannerAwareEditText.AbstractTextWatcher;
import de.fahimu.android.app.scanner.ScannerAwareEditText.ColumnAdapter;
import de.fahimu.android.app.scanner.ScannerAwareEditText.ColumnItem;
import de.fahimu.schlib.anw.StringType;
import de.fahimu.schlib.db.User;

/**
 * Step 1b of adding users to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersAddStep1b extends StepFragment<AdminUsersAddActivity> {

   @Override
   void passActivityToNextFragments() {
      nextFragment.setActivity(activity);
   }

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

         name1.setColumnAdapter(new PupilName1Adapter());
         name1.addTextChangedListener(new TextChangedListener());
         name2Group.setOnCheckedChangeListener(new RadioGroupListener());
         countSeek.setOnSeekBarChangeListener(new SeekBarListener());
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private static final class PupilName1Item extends ColumnItem {
      PupilName1Item(@NonNull User user) {
         super(user, user.getName1());
      }
   }

   private static final class PupilName1Adapter extends ColumnAdapter<PupilName1Item> {
      @Override
      protected void loadData(ArrayList<PupilName1Item> data) {
         ArrayList<User> users = User.getPupilsName1();
         for (User user : users) {
            data.add(new PupilName1Item(user));
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private final class TextChangedListener extends AbstractTextWatcher {
      @Override
      public void afterTextChanged(Editable editable) {
         String text = editable.toString();
         // prevent entering of leading blanks (just because it doesn't make sense)
         if (!text.isEmpty() && text.trim().isEmpty()) { editable.clear(); }
         activity.name1 = name1.getText().toString().trim();
         updateCountMaxAndText();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private final class RadioGroupListener implements OnCheckedChangeListener {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
         RadioButton button = (checkedId == R.id.admin_users_add_step_1b_name2_year_0 ? name2Year0 : name2Year1);
         activity.name2 = button.getText().toString();
         updateCountMaxAndText();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private int oldCount;

   private void updateCountMaxAndText() {
      oldCount = User.countPupils(activity.name2, activity.name1);
      int available = 100 - User.getNextAvailableSerial(activity.name2, activity.name1);
      countSeek.setMax(available > 40 ? 40 : available);
      updateCountText();
   }

   private void updateCountText() {
      final String text;
      final int newCount = activity.count;
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
         Calendar calendar = new GregorianCalendar(TimeZone.getDefault(), Locale.US);
         int year = calendar.get(Calendar.YEAR);
         name2Year0.setText(App.format("%d/%02d", (year - 1), year % 100));
         name2Year1.setText(App.format("%d/%02d", year, (year + 1) % 100));
         // From January to June PRESELECT the first school year, from July to December the second one.
         name2Group.clearCheck();
         name2Group.check(calendar.get(Calendar.MONTH) < Calendar.JULY ?
                          R.id.admin_users_add_step_1b_name2_year_0 :
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
      return StringType.CLASS.matches(name1, activity.name1);
   }

}