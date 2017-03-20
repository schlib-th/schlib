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
abstract class StepFragment extends Fragment {

   StepperActivity stepperActivity;

   @Override
   public final void onAttach(Context context) {
      super.onAttach(context);
      Log.d(getClass().getCanonicalName());
      stepperActivity = (StepperActivity) getActivity();
   }

   @Nullable
   abstract StepFragment getNext();

   @StringRes
   abstract int getTabNameId();

   boolean maybeOptional() { return false; }

   boolean actualOptional() { return false; }

   final boolean isOptional() { return maybeOptional() && actualOptional(); }

   /* ============================================================================================================== */

   @LayoutRes
   abstract int getContentViewId();

   @Override
   public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      Log.d(getClass().getCanonicalName());
      return inflater.inflate(getContentViewId(), container, false);
   }

   @Override
   public final void onStart() {
      super.onStart();
      Log.d(getClass().getCanonicalName());
      stepperActivity.setCurrentFragment(this);
   }

   @NonNull
   final <V extends View> V findView(@NonNull Class<V> type, @IdRes int resId) {
      View view = getActivity().findViewById(resId);
      if (view == null) { throw new ClassCastException("Missing resource " + resId); }
      return type.cast(view);
   }

   @Override
   public final void onResume() {
      super.onResume();
      Log.d(getClass().getCanonicalName());
      clearInput();
   }

   @Override
   public final void onPause() {
      super.onPause();
      Log.d(getClass().getCanonicalName());
   }

   /**
    * Sets all input elements to their state {@link #onResume()}.
    */
   abstract void clearInput();

   void onBarcode(@SuppressWarnings ("unused") String barcode) {
      // default implementation ignores scanned barcodes
   }

   /**
    * Returns {@code true} if the 'done' button should be enabled.
    *
    * @return {@code true} if the 'done' button should be enabled.
    */
   abstract boolean isDoneEnabled();

   /**
    * Returns {@code true} if the data entered at this step is valid.
    * Called by {@link StepperActivity#onDoneClicked(View)} to ensure that we can proceed to the next step.
    * This method will only be called if {@link #isDoneEnabled()} previously returned {@code true}.
    *
    * @return {@code true} if the data entered at this step is valid.
    */
   abstract boolean isDone();

}