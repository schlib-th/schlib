/*
 * SchlibStepperActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.fahimu.android.app.Log;

/**
 * An activity with support for Material Design Steppers.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @see <a href="https://material.io/guidelines/components/steppers.html">Steppers</a>
 * @since SchoolLibrary 1.0
 */
abstract class StepperActivity extends SchlibActivity {

   final class StepTab {

      private final String   number;
      private final TextView oval, name;

      StepTab(int number, View stepTab) {
         this.number = App.format("%d", number);
         this.oval = App.findView(stepTab, TextView.class, R.id.step_tab_oval);
         this.name = App.findView(stepTab, TextView.class, R.id.step_tab_name);
      }

      void setAttributes(boolean active, boolean heavyCheckMark, @StringRes int tabNameId) {
         if (active) {
            oval.setBackgroundResource(R.drawable.sh_oval_stepper_active_24dp);
            name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            name.setTextColor(App.getColorFromRes(R.color.color_87_percent_black));
         } else {
            oval.setBackgroundResource(R.drawable.sh_oval_stepper_inactive_24dp);
            name.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            name.setTextColor(App.getColorFromRes(R.color.color_38_percent_black));
         }
         oval.setText(heavyCheckMark ? "\u2714" : number);     // HEAVY CHECK MARK or number
         name.setText(App.getStr(tabNameId));
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private Button back, done;

   private final List<StepTab> stepTabs = new ArrayList<>();

   @Override
   protected final void onCreate(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onCreate(savedInstanceState);
         setDisplayHomeAsUpEnabled(true);

         back = findView(Button.class, R.id.stepper_back);
         done = findView(Button.class, R.id.stepper_done);

         getSupportFragmentManager().beginTransaction().add(R.id.stepper_fragments, getFirstFragment()).commit();

         ViewGroup tabs = findView(ViewGroup.class, R.id.stepper_tabs);
         for (StepFragment f = getFirstFragment(); f != null; f = f.getNext()) {
            if (f != getFirstFragment()) { tabs.addView(buildGreyLine()); }
            View stepTab = getLayoutInflater().inflate(R.layout.step_tab, tabs, false);
            tabs.addView(stepTab);
            stepTabs.add(new StepTab(1 + stepTabs.size(), stepTab));
         }
      }
   }

   private View buildGreyLine() {
      LinearLayout line = new LinearLayout(this);
      LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(App.dpToPx(0), App.dpToPx(1), 1f);
      layoutParams.gravity = Gravity.CENTER;
      layoutParams.setMarginEnd(App.dpToPx(8));
      layoutParams.setMarginStart(App.dpToPx(8));
      line.setLayoutParams(layoutParams);
      line.setBackgroundColor(App.getColorFromRes(R.color.color_google_material_grey_400));
      return line;
   }

   @NonNull
   abstract StepFragment getFirstFragment();

   private StepFragment currentFragment;

   /**
    * Called by a {@link StepFragment} {@link StepFragment#onStart() on start}.
    *
    * @param fragment
    *       the current visible fragment.
    */
   final void setCurrentFragment(@NonNull StepFragment fragment) {
      currentFragment = fragment;
   }

   @Override
   protected void onResume() {
      super.onResume();
      refreshGUI();
   }

   final void refreshGUI() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         back.setEnabled(currentFragment != getFirstFragment());
         done.setEnabled(currentFragment.isDoneEnabled());
         done.setText(currentFragment.getNext() == null ? R.string.app_done : R.string.app_cont);

         boolean active = true;
         Iterator<StepTab> iterator = stepTabs.iterator();
         for (StepFragment f = getFirstFragment(); f != null; f = f.getNext()) {
            if (f == currentFragment) {
               iterator.next().setAttributes(true, false, f.getTabNameId());
               active = false;
            } else {
               iterator.next().setAttributes(active, active, f.getTabNameId());
            }
         }
      }
   }

   @Override
   protected final boolean isBackButtonEnabled() {
      return true;
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   protected final void onBarcode(String barcode) {
      currentFragment.onBarcode(barcode);       // forward to fragment
   }

   public final void onBackClicked(View view) {
      onBackPressed();
   }

   public final void onDoneClicked(View view) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         if (currentFragment.onDoneClicked() && currentFragment.getNext() != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
            transaction.replace(R.id.stepper_fragments, currentFragment.getNext()).addToBackStack(null).commit();
         }
      }
   }

}