/*
 * ScannerAwareEditText.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app.scanner;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.Collections;

import de.fahimu.android.app.Log;
import de.fahimu.android.app.SearchString;
import de.fahimu.android.db.Row;

/**
 * A {@link AppCompatAutoCompleteTextView} view that intercepts {@link KeyEvent}s
 * from a barcode scanner and prevents them to be delivered unprocessed to the view.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2016
 * @since SchoolLibrary 1.0
 */
public final class ScannerAwareEditText extends AppCompatAutoCompleteTextView {

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

   /**
    * Copied from the private method {@code getActivity()} in
    * <a href="https://developer.android.com/reference/android/support/v7/app/MediaRouteButton.html">
    * MediaRouteButton</a>.
    */
   @NonNull
   private ScannerActivity getScannerActivity() {
      if (scannerActivity == null) {
         Context context = getContext();
         while (context instanceof ContextWrapper) {
            if (context instanceof ScannerActivity) {
               return scannerActivity = (ScannerActivity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
         }
         throw new IllegalStateException(getClass().getCanonicalName() + " not part of a ScannerActivity");
      }
      return scannerActivity;
   }

   /**
    * Sets an {@code adapter} for auto completing.
    *
    * @param adapter
    *       the adapter.
    * @return this {@code ScannerAwareEditText}
    */
   public ScannerAwareEditText setColumnAdapter(ColumnAdapter adapter) {
      setThreshold(1);
      setAdapter(adapter.initialize(getScannerActivity()));
      return this;
   }

   @Override
   public boolean onKeyPreIme(int keyCode, KeyEvent event) {
      return getScannerActivity().processKeyEvent(event) || super.onKeyPreIme(keyCode, event);
   }

   /* ============================================================================================================== */

   public static abstract class AbstractTextWatcher implements TextWatcher {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) { }

      @Override
      public void afterTextChanged(Editable editable) { }
   }

   /* ============================================================================================================== */

   public static abstract class ColumnItem implements Comparable<ColumnItem> {
      final long         rid;       // cached row.getOid()
      final String       column;
      final SearchString searchString;

      @WorkerThread
      protected ColumnItem(Row row, String column) {
         this.rid = row.getOid();
         this.column = column;
         this.searchString = new SearchString.Builder(1).add(column).buildSearchString();
      }

      @Override
      public final int compareTo(@NonNull ColumnItem another) {
         int cmp = searchString.compareTo(another.searchString);
         if (cmp != 0) { return cmp; }
         return column.compareTo(another.column);
      }
   }

   /* ============================================================================================================== */

   public static abstract class ColumnAdapter<I extends ColumnItem>
         extends BaseAdapter implements Filterable {

      private final ArrayList<I> data;
      private final ColumnFilter filter = new ColumnFilter();

      private ArrayList<I>   list;
      private LayoutInflater layoutInflater;

      private volatile boolean loadingComplete = false;

      protected ColumnAdapter() {
         data = list = new ArrayList<>();
      }

      final ColumnAdapter initialize(ScannerActivity context) {
         layoutInflater = LayoutInflater.from(context);
         AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
               try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
                  loadData(data);
                  loadingComplete = true;
               }
            }
         });
         return this;
      }

      @WorkerThread
      protected abstract void loadData(ArrayList<I> data);

      /** {@inheritDoc} */
      @Override
      public final int getCount() {
         return loadingComplete ? list.size() : 0;
      }

      /** {@inheritDoc} */
      @Override
      public final String getItem(int position) {
         return list.get(position).column;
      }

      /** {@inheritDoc} */
      @Override
      public final long getItemId(int position) {
         return list.get(position).rid;
      }

      /** {@inheritDoc} */
      @Override
      public final boolean hasStableIds() {
         return true;
      }

      /** {@inheritDoc} */
      @Override
      public final View getView(int position, @Nullable View convertView, ViewGroup parent) {
         TextView textView = (TextView) ((convertView != null) ? convertView :
                                         layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false));
         I item = list.get(position);
         item.searchString.setText(0, textView, item.column);
         return textView;
      }

      @Override
      public final Filter getFilter() {
         return filter;
      }

      /* ---------------------------------------------------------------------------------------------------------- */

      private final class ColumnFilter extends Filter {
         @Override
         @WorkerThread
         protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (!loadingComplete || constraint == null || constraint.length() == 0) {
               // restore the original data
               results.values = data;
               results.count = data.size();
            } else {
               String[] normalizedQueries = SearchString.getNormalizedQueries(constraint);
               ArrayList<I> list = new ArrayList<>();
               for (final I item : data) {
                  if (item.searchString.contains(normalizedQueries)) {
                     list.add(item);
                  }
               }
               Collections.sort(list);
               results.values = list;
               results.count = list.size();
            }
            return results;
         }

         @Override
         @UiThread
         protected void publishResults(CharSequence constraint, FilterResults results) {
            //noinspection unchecked
            list = (ArrayList<I>) results.values;
            if (results.count > 0) {
               notifyDataSetChanged();
            } else {
               notifyDataSetInvalidated();
            }
         }
      }

   }

}