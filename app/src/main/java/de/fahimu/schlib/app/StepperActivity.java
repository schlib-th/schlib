/*
 * StepperActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
 * A {@link SchlibActivity} with support for Material Design Steppers.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @see <a href="https://material.io/guidelines/components/steppers.html">Steppers</a>
 * @since SchoolLibrary 1.0
 */
abstract class StepperActivity extends SchlibActivity {

   private final class StepTab {

      private final String   number;
      private final TextView oval, name, optional;

      StepTab(int number, View stepTab) {
         this.number = App.format("%d", number);
         this.oval = App.findView(stepTab, TextView.class, R.id.step_tab_oval);
         this.name = App.findView(stepTab, TextView.class, R.id.step_tab_name);
         this.optional = App.findView(stepTab, TextView.class, R.id.step_tab_optional);
      }

      void setAttributes(int state, StepFragment fragment) {
         oval.setText(state < 0 ? "\u2714" : number);          // HEAVY CHECK MARK or number
         oval.setBackgroundResource(state <= 0 ? ovalActive : ovalInactive);
         name.setText(App.getStr(fragment.getTabNameId()));
         name.setTypeface(state != 0 ? regular : medium);
         name.setTextColor(state <= 0 ? black87Percent : black38Percent);
         optional.setTextColor(state <= 0 ? black54Percent : black38Percent);
         optional.setVisibility(fragment.maybeOptional() ? View.VISIBLE : View.GONE);
      }
   }

   @DrawableRes
   private static final int ovalActive   = R.drawable.sh_oval_stepper_active_24dp;
   @DrawableRes
   private static final int ovalInactive = R.drawable.sh_oval_stepper_inactive_24dp;

   private static final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
   private static final Typeface medium  = Typeface.create("sans-serif-medium", Typeface.NORMAL);

   @ColorInt
   private static final int black87Percent = App.getColorFromRes(R.color.color_87_percent_black);
   @ColorInt
   private static final int black54Percent = App.getColorFromRes(R.color.color_54_percent_black);
   @ColorInt
   private static final int black38Percent = App.getColorFromRes(R.color.color_38_percent_black);

   /* -------------------------------------------------------------------------------------------------------------- */

   private Button back, done;

   private StepFragment firstFragment;

   private final List<StepTab> stepTabs = new ArrayList<>();

   @Override
   protected final void onCreate(@Nullable Bundle savedInstanceState) {
      Log.d(getClass().getCanonicalName());
      super.onCreate(savedInstanceState);

      back = findView(Button.class, R.id.stepper_back);
      done = findView(Button.class, R.id.stepper_done);

      firstFragment = createFirstFragment();
      getSupportFragmentManager().beginTransaction().add(R.id.stepper_fragments, firstFragment).commit();

      ViewGroup tabs = findView(ViewGroup.class, R.id.stepper_tabs);
      for (StepFragment f = firstFragment; f != null; f = f.getNext()) {
         if (f != firstFragment) { tabs.addView(buildGreyLine()); }
         View stepTab = getLayoutInflater().inflate(R.layout.step_tab, tabs, false);
         tabs.addView(stepTab);
         stepTabs.add(new StepTab(1 + stepTabs.size(), stepTab));
      }
   }

   @NonNull
   abstract StepFragment createFirstFragment();

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

   private boolean finishingActivity;

   @Override
   protected final void onResume() {
      super.onResume();
      Log.d(getClass().getCanonicalName());
      finishingActivity = false;
      refreshGUI();
   }

   @Override
   protected final void onPause() {
      super.onPause();
      Log.d(getClass().getCanonicalName());
   }

   final void refreshGUI() {
      Log.d(getClass().getCanonicalName());
      back.setEnabled(currentFragment != firstFragment);
      done.setEnabled(currentFragment.isDoneEnabled());
      done.setText(getNext() == null ? R.string.app_done : R.string.app_cont);

      int state = -1;      // <0: left of currentFragment, 0: currentFragment, >0: right of currentFragment
      Iterator<StepTab> iterator = stepTabs.iterator();
      for (StepFragment f = firstFragment; f != null; f = f.getNext()) {
         if (f == currentFragment) {
            iterator.next().setAttributes(0, f);
            state = 1;
         } else {
            iterator.next().setAttributes(state, f);
         }
      }
   }

   @Nullable
   private StepFragment getNext() {
      StepFragment next = currentFragment.getNext();
      while (next != null && next.isOptional()) { next = next.getNext(); }
      return next;
   }

   @Override
   protected final void onBarcode(String barcode) {
      if (!finishingActivity) {
         currentFragment.onBarcode(barcode);       // forward to fragment
      }
   }

   @Override
   public final void onBackPressed() {
      if (!finishingActivity) {
         super.onBackPressed();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public final void onBackClicked(View view) {
      onBackPressed();
   }

   public final void onClearInputClicked(View view) {
      if (!finishingActivity) {
         currentFragment.clearInput();             // forward to fragment
      }
   }

   public final void onDoneClicked(View view) {
      Log.d(getClass().getCanonicalName());
      if (!finishingActivity && currentFragment.isDone()) {
         if (getNext() != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
            transaction.replace(R.id.stepper_fragments, getNext()).addToBackStack(null).commit();
         } else {
            finishingActivity = true;
            // set HEAVY CHECK MARK on all tabs
            Iterator<StepTab> iterator = stepTabs.iterator();
            for (StepFragment f = firstFragment; f != null; f = f.getNext()) {
               iterator.next().setAttributes(-1, f);
            }
            // disable all views
            disable(contentView.getRootView());
            new Handler().postDelayed(new Runnable() {
               @Override
               public void run() { finishActivity(); }
            }, 250);
         }
      }
   }

   private void disable(View view) {
      view.setEnabled(false);
      if (view instanceof ViewGroup) {
         ViewGroup group = (ViewGroup) view;
         for (int i = 0, count = group.getChildCount(); i < count; i++) {
            disable(group.getChildAt(i));
         }
      }
   }

   abstract void finishActivity();

}