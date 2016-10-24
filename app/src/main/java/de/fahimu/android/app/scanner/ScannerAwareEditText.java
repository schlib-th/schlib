/*
 * ScannerAwareEditText.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app.scanner;

import android.content.Context;
import android.support.design.widget.TextInputEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;

/**
 * A {@link TextInputEditText} view that intercepts {@link KeyEvent}s from a barcode scanner
 * and prevents them to be delivered unprocessed to the view.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public class ScannerAwareEditText extends TextInputEditText {

   private ScannerActivity scannerActivity;

   public ScannerAwareEditText(Context context) {
      super(context);
   }

   public ScannerAwareEditText(Context context, AttributeSet attrs) {
      super(context, attrs);
   }

   public ScannerAwareEditText(Context context, AttributeSet attrs, int defStyleAttr) {
      super(context, attrs, defStyleAttr);
   }

   public void setScannerActivity(ScannerActivity scannerActivity) {
      this.scannerActivity = scannerActivity;
   }

   @Override
   public boolean onKeyPreIme(int keyCode, KeyEvent event) {
      // let the scannerActivity filter the key events for scanner key events
      return scannerActivity.dispatchKeyEvent(event);
   }

}