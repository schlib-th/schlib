/*
 * NumberPicker.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

/**
 * A {@link android.widget.NumberPicker} with medium sized non editable numbers.
 */
public final class NumberPicker extends android.widget.NumberPicker {

   public NumberPicker(Context context) {
      super(context);
   }

   public NumberPicker(Context context, AttributeSet attrs) {
      super(context, attrs);
   }

   public NumberPicker(Context context, AttributeSet attrs, int defStyle) {
      super(context, attrs, defStyle);
   }

   @Override
   public void addView(View child) {
      super.addView(child);
      modifyView(child);
   }

   @Override
   public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
      super.addView(child, index, params);
      modifyView(child);
   }

   @Override
   public void addView(View child, android.view.ViewGroup.LayoutParams params) {
      super.addView(child, params);
      modifyView(child);
   }

   /**
    * If the {@code EditText} is added, modify it in the intended way.
    *
    * @param view
    *       the View to modify if it is an instance of {@code EditText}.
    */
   private void modifyView(View view) {
      if (view instanceof EditText) {
         EditText editText = (EditText) view;
         editText.setTextAppearance(android.R.style.TextAppearance_Medium);
         editText.setFocusable(false);
         editText.setClickable(false);
      }
   }

}