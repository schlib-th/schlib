/*
 * NoFocusDialog.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app.scanner;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.ViewGroup;


import de.fahimu.android.app.App;

/**
 * A convenience class for building {@link AlertDialog}s that cannot receive the focus.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class NoFocusDialog {

   /**
    * Will never be called.
    * Its only purpose is to signal the constructor to make the AlertDialog non cancelable.
    */
   public static final OnCancelListener IGNORE_CANCEL = new OnCancelListener() {
      @Override
      public void onCancel(DialogInterface dialog) { /* ignore */ }
   };

   public static final OnCancelListener DEFAULT_CANCEL = new OnCancelListener() {
      @Override
      public void onCancel(DialogInterface dialog) { /* ignore */ }
   };

   /* -------------------------------------------------------------------------------------------------------------- */

   public interface ButtonListener {
      void onClick();
   }

   public static final ButtonListener IGNORE_BUTTON = new ButtonListener() {
      @Override
      public void onClick() { /* ignore */ }
   };

   /* -------------------------------------------------------------------------------------------------------------- */

   @NonNull
   private final Builder builder;

   private volatile AlertDialog alertDialog = null;

   public NoFocusDialog(@NonNull Context context, @NonNull OnCancelListener onCancelListener) {
      this.builder = new Builder(context);
      this.builder.setCancelable(onCancelListener != IGNORE_CANCEL).setOnCancelListener(onCancelListener);
   }

   @NonNull
   public NoFocusDialog setTitle(@StringRes int resId, Object... args) {
      builder.setTitle(App.getStr(resId, args));
      return this;
   }

   @NonNull
   public NoFocusDialog setMessage(@StringRes int resId, Object... args) {
      builder.setMessage(App.getStr(resId, args));
      return this;
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private final class OnButtonClickListener implements OnClickListener {
      @NonNull
      private final ButtonListener listener;

      OnButtonClickListener(@NonNull ButtonListener listener) { this.listener = listener; }

      @Override
      public void onClick(DialogInterface dialog, int which) {
         synchronized (NoFocusDialog.this) { alertDialog = null; }
         listener.onClick();
      }
   }

   @NonNull
   public NoFocusDialog setNeutralButton(@StringRes int resId, @NonNull ButtonListener listener) {
      builder.setNeutralButton(App.getStr(resId), new OnButtonClickListener(listener)); return this;
   }

   @NonNull
   public NoFocusDialog setNegativeButton(@StringRes int resId, @NonNull ButtonListener listener) {
      builder.setNegativeButton(App.getStr(resId), new OnButtonClickListener(listener)); return this;
   }

   @NonNull
   public NoFocusDialog setPositiveButton(@StringRes int resId, @NonNull ButtonListener listener) {
      builder.setPositiveButton(App.getStr(resId), new OnButtonClickListener(listener)); return this;
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Builds the {@link AlertDialog} and shows it.
    * <p> Here the important part takes place. After showing the AlertDialog, its content view is set to
    * {@link ViewGroup#FOCUS_BLOCK_DESCENDANTS}. Hence no element, especially the buttons, can get the focus
    * and react to key events from a barcode scanner. </p>
    */
   @MainThread
   public synchronized void show() {
      alertDialog = builder.create();
      alertDialog.show();              // must be called BEFORE findViewById is called
      ViewGroup group = (ViewGroup) alertDialog.findViewById(android.R.id.content);
      group.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
   }

   /**
    * Cancels the {@link AlertDialog}.
    */
   public synchronized void cancel() {
      if (alertDialog != null) {
         alertDialog.cancel();
         alertDialog = null;
      }
   }

}