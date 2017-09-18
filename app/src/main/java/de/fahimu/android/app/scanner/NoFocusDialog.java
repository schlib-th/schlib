/*
 * NoFocusDialog.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app.scanner;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;
import android.view.KeyEvent;
import android.view.ViewGroup;


import de.fahimu.android.app.App;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;

/**
 * A convenience class for building {@link AlertDialog}s that cannot receive the focus.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class NoFocusDialog {

   @NonNull
   private final Builder builder;

   @NonNull
   private final ScannerActivity scannerActivity;

   private volatile AlertDialog alertDialog = null;

   public NoFocusDialog(@NonNull ScannerActivity activity) {
      builder = new Builder(scannerActivity = activity).setCancelable(true);
   }

   @NonNull
   public NoFocusDialog setMessage(@StringRes int resId, Object... args) {
      String text = App.getStr(resId, args);
      int newline = text.indexOf('\n');
      builder.setTitle(text.substring(0, newline)).setMessage(text.substring(newline + 1));
      return this;
   }

   @NonNull
   public NoFocusDialog setOnCancelListener(OnCancelListener listener) {
      builder.setCancelable(listener != null).setOnCancelListener(listener);
      return this;
   }

   @NonNull
   public NoFocusDialog activateScannerListener() {
      builder.setOnKeyListener(new OnKeyListener() {
         @Override
         public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            return scannerActivity.processKeyEvent(event);
         }
      });
      return this;
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public interface ButtonListener {
      void onClick(int id);
   }

   private static final ButtonListener IGNORE_BUTTON = new ButtonListener() {
      @Override
      public void onClick(int id) { /* ignore */ }
   };

   private final class OnButtonClickListener implements OnClickListener {
      @NonNull
      private final ButtonListener listener;

      OnButtonClickListener(@Nullable ButtonListener listener) {
         this.listener = (listener == null) ? IGNORE_BUTTON : listener;
      }

      @Override
      public void onClick(DialogInterface dialog, int which) {
         synchronized (NoFocusDialog.this) { alertDialog = null; }
         switch (which) {
         case BUTTON_NEGATIVE: listener.onClick(0); break;
         case BUTTON_POSITIVE: listener.onClick(1); break;
         }
      }
   }

   @NonNull
   public NoFocusDialog setButton0(@StringRes int resId, @Nullable ButtonListener listener) {
      builder.setNegativeButton(App.getStr(resId), new OnButtonClickListener(listener)).setCancelable(false);
      return this;
   }

   @NonNull
   public NoFocusDialog setButton1(@StringRes int resId, @Nullable ButtonListener listener) {
      builder.setPositiveButton(App.getStr(resId), new OnButtonClickListener(listener)).setCancelable(false);
      return this;
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * Plays the specified sound {@code resId}, builds the {@link AlertDialog} and {@link #show() shows} it.
    *
    * @param resId
    *       the name of the sound file without its extension.
    */
   @MainThread
   public void show(@RawRes int resId) {
      App.playSound(resId); show();
   }

   /**
    * Builds the {@link AlertDialog} and shows it.
    * <p> Here the important part takes place. After showing the AlertDialog, its content view is set to
    * {@link ViewGroup#FOCUS_BLOCK_DESCENDANTS}. Hence no element, especially the buttons, can get the focus
    * and react to key events from a barcode scanner. </p>
    */
   @MainThread
   public synchronized NoFocusDialog show() {
      alertDialog = builder.create();
      alertDialog.show();              // must be called BEFORE findViewById is called
      ViewGroup group = (ViewGroup) alertDialog.findViewById(android.R.id.content);
      group.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
      return this;
   }

   /**
    * Changes the button's text after showing the dialog.
    *
    * @param id
    *       0 to change button 0, 1 to change button 1.
    * @param resId
    *       resource id for the format string
    * @param formatArgs
    *       the format arguments that will be used for substitution.
    */
   public synchronized NoFocusDialog setButtonText(int id, @StringRes int resId, Object... formatArgs) {
      String text = App.getStr(resId, formatArgs);
      switch (id) {
      case 0: alertDialog.getButton(BUTTON_NEGATIVE).setText(text); break;
      case 1: alertDialog.getButton(BUTTON_POSITIVE).setText(text); break;
      }
      return this;
   }

   /**
    * Enables or disables a button after showing the dialog.
    *
    * @param id
    *       0 to change button 0, 1 to change button 1.
    */
   public synchronized NoFocusDialog setButtonEnabled(int id, boolean enabled) {
      switch (id) {
      case 0: alertDialog.getButton(BUTTON_NEGATIVE).setEnabled(enabled); break;
      case 1: alertDialog.getButton(BUTTON_POSITIVE).setEnabled(enabled); break;
      }
      return this;
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