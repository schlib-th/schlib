/*
 * Activity.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app;

import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.design.widget.Snackbar.SnackbarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * An activity with some utility methods.
 */
public abstract class Activity extends AppCompatActivity {

   protected View contentView;

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(getContentViewId());
      contentView = findView(View.class, android.R.id.content);
      setSupportActionBar(findView(Toolbar.class, getToolbarId()));
   }

   @LayoutRes
   protected abstract int getContentViewId();

   @IdRes
   protected abstract int getToolbarId();

   /**
    * Returns the view with the specified {@code resId}.
    * If there is no such view defined, or if the view cannot be cast to the specified {@code type},
    * an {@link ClassCastException} will be thrown.
    *
    * @param type
    *       the type of the requested view.
    * @param resId
    *       the id attribute of the view from the XML file.
    * @return the view with the specified {@code resId}.
    *
    * @throws ClassCastException
    *       if there is no such view defined.
    */
   @NonNull
   protected final <V extends View> V findView(@NonNull Class<V> type, @IdRes int resId) {
      View view = findViewById(resId);
      if (view == null) { throw new ClassCastException("Missing resource " + resId); }
      return type.cast(view);
   }

   /**
    * Set whether home will be displayed as an "up" arrow or as an launcher icon.
    *
    * @param showHomeAsUp
    *       if {@code true}, home will be displayed as an "up" arrow, else as an launcher icon.
    */
   protected final void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
      ActionBar actionBar = getSupportActionBar();
      if (actionBar != null) {
         if (showHomeAsUp) {
            actionBar.setDisplayHomeAsUpEnabled(true);
         } else {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(getIconId());
         }
      }
   }

   @DrawableRes
   protected abstract int getIconId();

   /* ============================================================================================================== */

   private Snackbar snackbar;

   protected final void showSnackbar(@ColorRes int backgroundColorId, @StringRes int resId, Object... formatArgs) {
      snackbar = Snackbar.make(contentView, App.getStr(resId, formatArgs), Snackbar.LENGTH_LONG);
      configSnackbar(snackbar, backgroundColorId);
      snackbar.show();
   }

   protected final void showSnackbar(String undo, @ColorRes int undoColorId, OnClickListener onUndoListener,
         @ColorRes int backgroundColorId, @StringRes int resId, Object... formatArgs) {
      snackbar = Snackbar.make(contentView, App.getStr(resId, formatArgs), Snackbar.LENGTH_INDEFINITE);
      snackbar.setAction(undo, onUndoListener);
      snackbar.setActionTextColor(App.getColorFromRes(undoColorId));
      configSnackbar(snackbar, backgroundColorId);
      snackbar.show();
   }

   private void configSnackbar(Snackbar snackbar, @ColorRes int backgroundColorId) {
      SnackbarLayout layout = (SnackbarLayout) snackbar.getView();
      TextView textView = (TextView) layout.findViewById(android.support.design.R.id.snackbar_text);

      textView.setGravity(Gravity.CENTER);
      textView.setTextAppearance(android.R.style.TextAppearance_Medium);
      layout.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
      layout.setBackgroundColor(App.getColorFromRes(backgroundColorId));
   }

   @Override
   public final void onUserInteraction() {
      if (snackbar != null) { snackbar.dismiss(); }
   }

}