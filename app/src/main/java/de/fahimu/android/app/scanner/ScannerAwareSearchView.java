/*
 * ScannerAwareSearchView.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app.scanner;

import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


import de.fahimu.android.app.App;

/**
 * A search view with a associated {@link ScannerActivity} that handles events from a barcode scanner.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class ScannerAwareSearchView implements OnQueryTextListener, OnFocusChangeListener {

   @NonNull
   private final SearchView      searchView;
   @NonNull
   private final ScannerActivity scannerActivity;

   public ScannerAwareSearchView(@NonNull MenuItem searchItem,
         @NonNull final ScannerActivity scannerActivity, int inputType, InputFilter... inputFilters) {
      this.searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
      this.searchView.setOnQueryTextListener(this);
      this.searchView.setOnQueryTextFocusChangeListener(this);
      this.searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
      this.scannerActivity = scannerActivity;

      EditText editText = App.findView(searchView, EditText.class, android.support.v7.appcompat.R.id.search_src_text);
      editText.setFilters(inputFilters);
      editText.setRawInputType(inputType);
      editText.setOnEditorActionListener(new OnEditorActionListener() {
         @Override
         public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            collapse(); return true;
         }
      });
   }

   /**
    * Sets the query to empty string and collapses the search view to an icon.
    */
   public void collapse() {
      searchView.setQuery("", true);
      if (!searchView.isIconified()) {
         searchView.setIconified(true);
      }
   }

   /**
    * Expands the search view and displays the specified {@code text} in it.
    *
    * @param text
    *       the text to display.
    */
   public void expand(@NonNull String text) {
      if (searchView.isIconified()) {
         searchView.setIconified(false);
      }
      searchView.setQuery(text, true);
   }

   /**
    * Returns the currently displayed query text.
    *
    * @return the currently displayed query text.
    */
   @NonNull
   public String getQuery() {
      return searchView.getQuery().toString();
   }

   /**
    * Called when the query text is changed by the user.
    *
    * @param newText
    *       the new content of the query text field.
    * @return always {@code true} because the listener handled the query.
    */
   @Override
   public boolean onQueryTextChange(String newText) {
      scannerActivity.onQueryTextChange(newText);
      return true;
   }

   /**
    * Returns always {@code true} and ignores this event.
    *
    * @return always {@code true} and ignores this event.
    */
   @Override
   public boolean onQueryTextSubmit(String text) {
      return true;
   }

   private boolean hasFocus;

   @Override
   public void onFocusChange(View v, boolean hasFocus) {
      this.hasFocus = hasFocus;
      if (!hasFocus && getQuery().isEmpty()) { collapse(); }
      scannerActivity.onFocusChange(v, hasFocus);
   }

   /**
    * Returns true if the search view is expanded and has the focus.
    *
    * @return true if the search view is expanded and has the focus.
    */
   public boolean hasFocus() {
      return hasFocus;
   }

}