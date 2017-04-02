/*
 * AdminBooksAddStep2.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.widget.NumberPicker;


import java.util.ArrayList;
import java.util.HashMap;

import de.fahimu.android.app.Log;
import de.fahimu.android.app.SearchString;
import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.android.app.scanner.NoFocusDialog.ButtonListener;
import de.fahimu.android.app.scanner.ScannerAwareEditText;
import de.fahimu.android.app.scanner.ScannerAwareEditText.AbstractTextWatcher;
import de.fahimu.android.app.scanner.ScannerAwareEditText.ColumnAdapter;
import de.fahimu.android.app.scanner.ScannerAwareEditText.ColumnItem;
import de.fahimu.schlib.anw.StringType;
import de.fahimu.schlib.db.Book;

/**
 * Step 2 of adding books to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminBooksAddStep2 extends StepFragment<AdminBooksAddActivity> {

   @Override
   void passActivityToNextFragments() {
      nextFragment.setActivity(activity);
   }

   @Override
   StepFragment getNext() { return nextFragment; }

   private final AdminBooksAddStep3 nextFragment = new AdminBooksAddStep3();

   @Override
   int getTabNameId() { return R.string.admin_books_add_step_2_label; }

   /* ============================================================================================================== */

   @Override
   int getContentViewId() { return R.layout.admin_books_add_step_2; }

   private ScannerAwareEditText shelf;
   private NumberPicker         number;

   @Override
   public void onActivityCreated(@Nullable Bundle savedInstanceState) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onActivityCreated(savedInstanceState);
         shelf = findView(ScannerAwareEditText.class, R.id.admin_books_add_step_2_shelf);
         number = findView(NumberPicker.class, R.id.admin_books_add_step_2_number_picker);

         shelf.setColumnAdapter(new ShelfAdapter());
         shelf.addTextChangedListener(new TextChangedListener());
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private static final class ShelfItem extends ColumnItem {
      ShelfItem(@NonNull Book book) {
         super(book, book.getShelf());
      }
   }

   private static final class ShelfAdapter extends ColumnAdapter<ShelfItem> {
      @Override
      protected void loadData(ArrayList<ShelfItem> data) {
         for (Book book : Book.getShelfValues()) {
            data.add(new ShelfItem(book));
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   private final class TextChangedListener extends AbstractTextWatcher {
      @Override
      public void afterTextChanged(Editable editable) {
         String text = editable.toString();
         // prevent entering of leading blanks (just because it doesn't make sense)
         if (!text.isEmpty() && text.trim().isEmpty()) { editable.clear(); }
         activity.shelf = shelf.getText().toString().trim();
         updatePickerValues();
      }
   }

   private static final String[] allPickerValues = new String[999];

   static {
      for (int i = 0; i < allPickerValues.length; i++) {
         allPickerValues[i] = App.format("%03d", i + 1);
      }
   }

   private void updatePickerValues() {
      lastUsedNumber = confirmedNumber = 0;
      String[] pickerValues = allPickerValues;
      ArrayList<Integer> usedNumbers = Book.getNumbers(activity.shelf);
      if (usedNumbers.size() > 0) {
         // Build a new pickerValues array that doesn't contain numbers that are already in use.
         final ArrayList<String> pickerValuesList = new ArrayList<>(999);
         // First add the 'gap' values to the list
         int value = 0;
         for (int usedNumber : usedNumbers) {
            while (++value < usedNumber) {
               pickerValuesList.add(App.format("%03d", value));
            }
         }
         lastUsedNumber = value;
         // Then add the remaining values to the list
         while (value <= 999 - 1) {
            pickerValuesList.add(allPickerValues[value++]);
         }
         pickerValues = pickerValuesList.toArray(new String[pickerValuesList.size()]);
      }
      if (pickerValues != currentPickerValues) {
         currentPickerValues = pickerValues;
         number.setMinValue(0);
         number.setMaxValue(0);   // set max to 0 before setDisplayedValues to prevent IndexOutOfBoundsException
         number.setDisplayedValues(currentPickerValues);
         number.setMaxValue(currentPickerValues.length - 1);
      }
      activity.refreshGUI();
   }

   private int lastUsedNumber, confirmedNumber;

   private String confirmedShelf;

   private String[] currentPickerValues;

   /* ============================================================================================================== */

   @Override
   void clearInput() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         confirmedShelf = null;
         currentPickerValues = null;
         shelf.setText(""); shelf.setError(null);
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   boolean isDoneEnabled() {
      return !activity.shelf.isEmpty();
   }

   @Override
   boolean isDone() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         if (!StringType.SHELF.matches(shelf, activity.shelf)) { return false; }

         final HashMap<String,String> shelfsMap = new HashMap<>();
         for (Book book : Book.getShelfValues()) {
            final String shelf = book.getShelf();
            shelfsMap.put(simplify(shelf), shelf);
         }
         return noSimilarName(shelfsMap) && noNewShelf(shelfsMap) && noGapInNumber();
      }
   }

   /**
    * Returns the normalized queries of the specified string concatenated to one string.
    *
    * @param s
    *       the string to simplify.
    * @return the normalized queries of the specified string concatenated to one string.
    */
   private static String simplify(String s) {
      final StringBuilder b = new StringBuilder(s.length());
      final String[] normalizedQueries = SearchString.getNormalizedQueries(s);
      for (String normalized : normalizedQueries) {
         b.append(normalized);
      }
      return b.toString();
   }

   private boolean noSimilarName(HashMap<String,String> shelfs) {
      String similarShelf = shelfs.get(simplify(activity.shelf));
      if (similarShelf == null || similarShelf.equals(activity.shelf)) {
         return true;
      } else {
         NoFocusDialog dialog = new NoFocusDialog(activity);
         dialog.setMessage(R.string.dialog_message_admin_books_add_step_2_similar_name, activity.shelf, similarShelf);
         dialog.show(R.raw.horn);
         return false;
      }
   }

   private boolean noNewShelf(HashMap<String,String> shelfs) {
      if (activity.shelf.equals(confirmedShelf) || shelfs.containsValue(activity.shelf)) {
         return true;
      } else {
         NoFocusDialog dialog = new NoFocusDialog(activity);
         dialog.setMessage(R.string.dialog_message_admin_books_add_step_2_new_shelf, activity.shelf);
         dialog.setButton0(R.string.app_no, null);
         dialog.setButton1(R.string.app_yes, new ButtonListener() {
            @Override
            public void onClick() { confirmedShelf = activity.shelf; }
         }).show();
         return false;
      }
   }

   private boolean noGapInNumber() {
      String displayedValue = number.getDisplayedValues()[number.getValue()];
      activity.number = Integer.parseInt(displayedValue);
      if (activity.number == confirmedNumber || activity.number <= lastUsedNumber + 1) {
         return true;
      } else {
         NoFocusDialog dialog = new NoFocusDialog(activity);
         if (lastUsedNumber == 0) {
            dialog.setMessage(R.string.dialog_message_admin_books_add_step_2_gap_in_number_1,
                  displayedValue, activity.shelf);
         } else {
            dialog.setMessage(R.string.dialog_message_admin_books_add_step_2_gap_in_number_2,
                  displayedValue, App.format("%03d", lastUsedNumber));
         }
         dialog.setButton0(R.string.app_no, null);
         dialog.setButton1(R.string.app_yes, new ButtonListener() {
            @Override
            public void onClick() { confirmedNumber = activity.number; }
         }).show();
         return false;
      }
   }

}