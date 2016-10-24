/*
 * Activity.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app.scanner;

import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;


import java.util.HashSet;
import java.util.Set;

import de.fahimu.android.app.Activity;
import de.fahimu.android.app.Log;

/**
 * An activity that supports a barcode scanner.
 */
public abstract class ScannerActivity extends Activity {

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      takeKeyEvents(true);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private final Set<View> views = new HashSet<>(16);

   @Override
   protected void onResume() {
      super.onResume();
      views.clear();
      traverse(contentView);
   }

   private final View.OnLayoutChangeListener layoutChangeListener = new OnLayoutChangeListener() {
      @Override
      public void onLayoutChange(View view, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
         traverse(view);
      }
   };

   /**
    * Walks down the views tree, starting with the specified {@code view}, and sets all views
    * to non focusable that are focusable in keyboard mode but not in touch mode, e. g. Buttons.
    * Additionally, to each view an {@link OnLayoutChangeListener} is added, that will again call
    * {@code traverse}. This ensures, that new views added to the layout after {@link #onResume()}
    * will also be processed.
    * For performance optimization, every processed view will be added to the {@link #views} set.
    *
    * @param view
    *       the view where to start walking down.
    */
   private void traverse(View view) {
      if (!views.contains(view)) {
         views.add(view);
         view.addOnLayoutChangeListener(layoutChangeListener);
         if (view.isFocusable() && !view.isFocusableInTouchMode()) {
            view.setFocusable(false);
         }
      }
      if (view instanceof ViewGroup) {
         ViewGroup group = (ViewGroup) view;
         for (int i = 0, count = group.getChildCount(); i < count; i++) {
            traverse(group.getChildAt(i));
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   /**
    * A set of {@link InputDevice#getDescriptor() descriptor} strings from input devices
    * that are not barcode scanners, but as for instance hardware keyboards attached to the tablet.
    */
   private static final Set<String> noScannerDevices = new HashSet<>(1);

   /**
    * Adds the specified {@code inputDevice} to the set of devices that are not barcode scanners.
    * If the {@code inputDevice} {@link InputDevice#isVirtual() isVirtual}, it will be ignored.
    *
    * @param inputDevice
    *       the non-virtual device.
    */
   @MainThread
   public static synchronized void setNoScannerDevice(@NonNull InputDevice inputDevice) {
      if (inputDevice.isVirtual()) { return; }
      noScannerDevices.add(inputDevice.getDescriptor());
   }

   /**
    * The string builder where the barcode is assembled.
    */
   private final StringBuilder barcodeAssembler = new StringBuilder(23);

   /**
    * Collects {@link KeyEvent}s from non-virtual devices until {@link KeyEvent#KEYCODE_ENTER KEYCODE_ENTER}
    * is detected, and then delivers the barcode with {@link #onBarcode(String) onBarcode}.
    *
    * @param event
    *       the KeyEvent.
    * @return true if the {@code event} was consumed, otherwise false.
    */
   @Override
   public final boolean dispatchKeyEvent(KeyEvent event) {
      int keyCode = event.getKeyCode();
      InputDevice inputDevice = InputDevice.getDevice(event.getDeviceId());
      if (inputDevice.isVirtual() || noScannerDevices.contains(inputDevice.getDescriptor())) {
         return super.dispatchKeyEvent(event);
      } else {
         if (event.getAction() == KeyEvent.ACTION_UP && event.getMetaState() == 0) {
            if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
               barcodeAssembler.append((char) ('0' + keyCode - KeyEvent.KEYCODE_0));
            } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
               String barcode = barcodeAssembler.toString();
               Log.d("barcode=" + barcode);
               onUserInteraction();
               onBarcode(barcode);
               barcodeAssembler.setLength(0);    // reset
            }
         }
         return true;     // event has been consumed
      }
   }

   protected void onBarcode(String barcode) { /* if not overridden, ignore the barcode */ }

   /* -------------------------------------------------------------------------------------------------------------- */
   /*  Methods called from {@link ScannerAwareSearchView}                                                            */
   /* -------------------------------------------------------------------------------------------------------------- */

   public void onFocusChange(View v, boolean hasFocus) {}

   public void onQueryTextChange(String newText) {}

}