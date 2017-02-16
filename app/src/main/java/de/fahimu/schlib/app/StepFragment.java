/*
 * StepFragment.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import de.fahimu.android.app.Log;

/**
 * A fragment implementing one step of a {@link StepperActivity}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
abstract class StepFragment<SA extends StepperActivity> extends Fragment {

   StepperActivity stepperActivity;

   @NonNull
   final SA getActivity(Class<SA> type) {
      return type.cast(stepperActivity);
   }

   @Override
   public final void onAttach(Context context) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onAttach(context);
         stepperActivity = (StepperActivity) getActivity();
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @LayoutRes
   abstract int getContentViewId();

   @StringRes
   abstract int getTabNameId();

   @Nullable
   abstract StepFragment getNext();

   @Override
   public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         return inflater.inflate(getContentViewId(), container, false);
      }
   }

   @Override
   public final void onStart() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onStart();
         stepperActivity.setCurrentFragment(this);
      }
   }

   @Override
   public void onResume() {
      super.onResume();
      updateModel();
   }

   @NonNull
   final <V extends View> V findView(@NonNull Class<V> type, @IdRes int resId) {
      View view = getActivity().findViewById(resId);
      if (view == null) { throw new ClassCastException("Missing resource " + resId); }
      return type.cast(view);
   }

   abstract boolean isDoneEnabled();

   abstract boolean onDoneClicked();

   abstract void updateModel();

}