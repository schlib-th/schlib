/*
 * ListView.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A RecyclerView that swaps the empty list for a text view if {@code getAdapter().getItemCount()} becomes zero.
 * For the first time after creation, the text view will be replaced by a progress bar.
 */
public final class ListView extends RecyclerView {

   public ListView(Context context) {
      super(context);
   }

   public ListView(Context context, AttributeSet attrs) {
      super(context, attrs);
   }

   public ListView(Context context, AttributeSet attrs, int defStyle) {
      super(context, attrs, defStyle);
   }

   final private AdapterDataObserver observer = new AdapterDataObserver() {
      @Override
      public void onChanged() { checkIfEmpty(); }

      @Override
      public void onItemRangeInserted(int positionStart, int itemCount) { checkIfEmpty(); }

      @Override
      public void onItemRangeRemoved(int positionStart, int itemCount) { checkIfEmpty(); }
   };

   @Override
   public void setAdapter(@NonNull RecyclerView.Adapter newAdapter) {
      RecyclerView.Adapter oldAdapter = getAdapter();
      if (oldAdapter != null) {
         oldAdapter.unregisterAdapterDataObserver(observer);
      }
      super.setAdapter(newAdapter);
      newAdapter.registerAdapterDataObserver(observer);
      checkIfEmpty();
   }

   private View emptyView;
   private View emptyTextView;

   private void setEmptyText(@StringRes int resId) {
      ProgressBar progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleLarge);
      RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams
            (RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
      layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
      progressBar.setLayoutParams(layoutParams);

      RelativeLayout progressLayout = new RelativeLayout(getContext());
      progressLayout.setLayoutParams(this.getLayoutParams());
      progressLayout.setBackground(getBackground());
      progressLayout.addView(progressBar);

      TextView textView = new TextView(getContext());
      textView.setLayoutParams(this.getLayoutParams());
      textView.setBackground(getBackground());
      textView.setGravity(Gravity.CENTER);
      textView.setText(resId);
      textView.setTextAppearance(android.R.style.TextAppearance_Large);
      textView.setTextColor(App.getColorFromRes(android.R.color.darker_gray));
      textView.setVisibility(GONE);                      // for the first time, make the text view gone

      ViewGroup parent = (ViewGroup) getParent();
      parent.addView(this.emptyView = progressLayout);   // for the first time, show the progress bar
      parent.addView(this.emptyTextView = textView);     // the text view replaces the progress bar later on
      checkIfEmpty();
   }

   private void checkIfEmpty() {
      if (emptyView != null && getAdapter() != null) {
         if (getAdapter().getItemCount() == 0) {
            setVisibility(GONE); emptyView.setVisibility(VISIBLE);
         } else {
            setVisibility(VISIBLE); emptyView.setVisibility(GONE);
            emptyView = emptyTextView;                   // now replace the progress bar by the text view
         }
      }
   }

   /* ============================================================================================================== */

   public static abstract class Item<Row> implements Comparable<Item<Row>> {
      public final Row          row;
      public final int          rid;
      public final SearchString searchString;

      @WorkerThread
      protected Item(Row row, int rid, String... searchFields) {
         this.row = row; this.rid = rid;
         SearchString searchString = null;
         if (searchFields.length > 0) {
            SearchString.Builder builder = new SearchString.Builder(searchFields.length);
            for (String searchField : searchFields) { builder.add(searchField); }
            searchString = builder.createSearchString();
         }
         this.searchString = searchString;
      }

      @Override
      public final int compareTo(@NonNull Item<Row> another) {
         return this.rid - another.rid;
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public static abstract class ViewHolder<I extends Item> extends RecyclerView.ViewHolder {

      protected ViewHolder(LayoutInflater inflater, ViewGroup parent, @LayoutRes int resId) {
         super(inflater.inflate(resId, parent, false));
      }

      protected abstract void bind(I item);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public interface Filter<I extends Item> {
      @WorkerThread
      boolean matches(I item);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public static abstract class Adapter<Row, I extends Item<Row>, VH extends ViewHolder<I>>
         extends RecyclerView.Adapter<VH> {

      private final ArrayList<I>        data;
      private final ArrayList<I>        list;        // the filtered data
      private final ListView            listView;
      private final LayoutInflater      inflater;
      private final LinearLayoutManager layoutManager;

      protected Adapter(Activity activity, @IdRes int listViewId, @StringRes int emptyStringId) {
         this.data = new ArrayList<>(0);
         this.list = new ArrayList<>(16);
         this.listView = activity.findView(ListView.class, listViewId);
         this.inflater = LayoutInflater.from(listView.getContext());
         this.layoutManager = new LinearLayoutManager(activity);
         listView.setLayoutManager(layoutManager);
         listView.setAdapter(this);
         listView.setEmptyText(emptyStringId);
      }

      @Override
      public final int getItemCount() { return list.size(); }

      protected abstract VH createViewHolder(LayoutInflater inflater, ViewGroup parent);

      @Override
      public final VH onCreateViewHolder(ViewGroup parent, int viewType) {
         return createViewHolder(inflater, parent);
      }

      @Override
      public final void onBindViewHolder(VH holder, int position) {
         I item = list.get(position);
         boolean selected = (selectedItem != null && selectedItem.rid == item.rid);
         holder.itemView.setBackgroundColor(selected ? Color.LTGRAY : Color.TRANSPARENT);
         holder.bind(item);
      }

      @Nullable
      private I selectedItem = null;

      protected final void setSelection(@Nullable I item) {
         int prevPosition = (selectedItem == null) ? -1 : Collections.binarySearch(list, selectedItem);
         selectedItem = item;
         int currPosition = (selectedItem == null) ? -1 : Collections.binarySearch(list, selectedItem);

         if (prevPosition != currPosition) {
            if (prevPosition >= 0) { notifyItemChanged(prevPosition); }
            if (currPosition >= 0) { notifyItemChanged(currPosition); }
         }
         if (currPosition >= 0) {
            layoutManager.scrollToPositionWithOffset(currPosition, App.dpToPx(2.5f * 48));
         }
      }

      @Nullable
      public final I getSelectedItem() { return selectedItem; }

      /**
       * Returns the {@code Item} from list, currently displayed by the specified {@code view}s parent.
       * The {@link View#getParent() parent} of the view must be the view hierarchy inflated by
       * {@link #onCreateViewHolder(ViewGroup, int) onCreateViewHolder}, and its parent in turn must be
       * the {@link #listView}.
       *
       * @param view
       *       the view to search for.
       * @return the {@code Item} from list, currently displayed by the specified {@code view}s parent.
       */
      @NonNull
      public final I getItemByView(@NonNull View view) {
         return list.get(layoutManager.getPosition((View) view.getParent()));
      }

      public void setData(@NonNull I listItem) {
         int index = Collections.binarySearch(data, listItem);
         if (index >= 0) {
            data.set(index, createItem(data.get(index).row));
         }
      }

      /* ----------------------------------------------------------------------------------------------------------- */

      @WorkerThread
      protected abstract ArrayList<Row> loadData();

      @WorkerThread
      protected abstract int getRid(Row row);

      @WorkerThread
      protected abstract I createItem(Row row);

      @WorkerThread
      private void reloadData() {
         try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
            ArrayList<Row> rows = loadData();
            data.ensureCapacity(rows.size());
            int d = 0, r = 0, rs = rows.size();
            while (d < data.size() && r < rs) {
               I item = data.get(d);
               Row row = rows.get(r++);
               int rid = getRid(row);
               if (item.rid < rid) {
                  data.remove(d); r--;
               } else if (item.rid > rid) {
                  data.add(d++, createItem(row));
               } else if (item.row.equals(row)) {
                  d++;
               } else {
                  data.set(d++, createItem(row));
               }
            }
            while (d < data.size()) {       // remove remaining items from data
               data.remove(d);
            }
            while (r < rs) {                // add remaining rows to data
               data.add(createItem(rows.get(r++)));
            }
         }
      }

      private boolean firstRun = true;

      public static final  int RELOAD_DATA = 0x1000000;
      private static final int RELOAD_MASK = 0x0ffffff;

      @MainThread
      protected abstract void onUpdated(int flags, @Nullable List<I> data);

      private AsyncTask<Void,Void,Void> createUpdateTask(final int flags, final Filter<I> filter) {
         return new AsyncTask<Void,Void,Void>() {
            @Override
            @WorkerThread
            protected Void doInBackground(Void... params) {
               try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
                  long start = SystemClock.uptimeMillis();
                  if ((flags & RELOAD_DATA) == RELOAD_DATA) { reloadData(); }
                  calculateModifications(filter);
                  if (firstRun) {          // on first run, display the ProgressBar for at least 800 ms
                     long sleep = start + 800 - SystemClock.uptimeMillis();
                     if (sleep >= 10) { SystemClock.sleep(sleep); }
                     firstRun = false;
                  }
                  return null;
               }
            }

            @Override
            @MainThread
            protected void onPostExecute(Void aVoid) {
               try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
                  int offset = 0;
                  for (Adapter.Mod mod : mods) {
                     offset = mod.apply(offset);
                  }
                  if (!list.isEmpty() && list.get(0).searchString != null) {
                     // searchString changed, so redraw complete list after 500 ms (after mods are applied)
                     new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() { notifyDataSetChanged(); }
                     }, mods.isEmpty() ? 10 : 500);
                  }
                  mods = null;      // let the GC do it's work

                  onUpdated(flags & RELOAD_MASK, Collections.unmodifiableList(data));
                  executeNext();
               }
            }
         };
      }

      private int nextTaskFlags;

      private AsyncTask<Void,Void,Void> activeTask, nextTask;

      @MainThread
      private synchronized void executeNext() {
         nextTaskFlags = 0;
         activeTask = nextTask; nextTask = null;
         if (activeTask != null) {
            activeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
         }
      }

      @MainThread
      public final void updateAsync(int flags) {
         updateAsync(flags, new Filter<I>() {
            @Override
            public boolean matches(I item) { return true; }
         });
      }

      @MainThread
      public final synchronized void updateAsync(int flags, Filter<I> filter) {
         if (activeTask == null) {
            activeTask = createUpdateTask(flags, filter);
            activeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
         } else {
            // 'Merge' the tasks; the new flags are the previous next task's user flags
            // or-ed with this user flags, the filter is always the most recent filter.
            nextTask = createUpdateTask(nextTaskFlags |= flags, filter);
         }
      }

      /* ----------------------------------------------------------------------------------------------------------- */

      private LinkedList<Adapter.Mod> mods;

      @WorkerThread
      private void calculateModifications(Filter<I> filter) {
         mods = new LinkedList<>();
         int l = 0, d = 0, ls = list.size(), ds = data.size();
         while (l < ls && d < ds) {
            I dataItem = data.get(d++);
            if (filter.matches(dataItem)) {
               I listItem = list.get(l);
               if (listItem == dataItem) {
                  l++;
               } else if (listItem.rid < dataItem.rid) {
                  add(new Remove(l++)); d--;
               } else if (listItem.rid > dataItem.rid) {
                  add(new Insert(l, dataItem));
               } else {            // listItem.rid == dataItem.rid && listItem != dataItem
                  add(new Change(l++, dataItem));
               }
            }
         }
         while (l < ls) {            // remove remaining items from list
            add(new Remove(l++));
         }
         while (d < ds) {            // add remaining items in data to list if filter matches
            I dataItem = data.get(d++);
            if (filter.matches(dataItem)) {
               add(new Insert(l, dataItem));
            }
         }
      }

      @WorkerThread
      private void add(Adapter.Mod next) {
         if (mods.isEmpty() || !mods.getLast().merge(next)) { mods.addLast(next); }
      }

      /* ----------------------------------------------------------------------------------------------------------- */

      private abstract class Mod {
         final int index;
         final ArrayList<I> items = new ArrayList<>(1);

         Mod(int index, I item) { this.index = index; items.add(item); }

         @WorkerThread
         final boolean merge(Mod next, int size) {
            if (next.getClass() == this.getClass() && next.index == this.index + size) {
               items.add(next.items.get(0));
               return true;
            }
            return false;
         }

         @WorkerThread
         abstract boolean merge(Mod next);

         /**
          * The {@link #index} of this {@code Mod} is taken from the unmodified original list.
          * Because the n-th modification is applied on the list after modification 1 to n-1,
          * index must be incremented for previous inserts or decremented for previous removes.
          * This is accomplished by the offset variable, which is modified in apply if necessary.
          *
          * @param offset
          *       the offset before applying this modification.
          * @return the new offset taking in account the applied modification.
          */
         @MainThread
         abstract int apply(int offset);
      }

      private final class Remove extends Mod {
         Remove(int index) { super(index, null); }

         @Override
         boolean merge(Mod next) { return merge(next, items.size()); }

         @Override
         int apply(int offset) {
            int start = offset + index, size = items.size();
            list.subList(start, start + size).clear();       // bulk remove
            notifyItemRangeRemoved(start, size);
            return offset - size;
         }
      }

      private final class Change extends Mod {
         Change(int index, I item) { super(index, item); }

         @Override
         boolean merge(Mod next) { return merge(next, items.size()); }

         @Override
         int apply(int offset) {
            int start = offset + index, i = start;
            for (I item : items) { list.set(i++, item); }
            notifyItemRangeChanged(start, items.size());
            return offset;
         }
      }

      private final class Insert extends Mod {
         Insert(int index, I item) { super(index, item); }

         @Override
         boolean merge(Mod next) { return merge(next, 0); }

         @Override
         int apply(int offset) {
            int start = offset + index, size = items.size();
            list.addAll(start, items);
            // notifyItemRangeInserted after inserting in an empty list causes strange flickering,
            if (list.size() == size) {
               notifyDataSetChanged();      // so calling notifyDataSetChanged in this case is better.
            } else {
               notifyItemRangeInserted(start, size);
            }
            return offset + size;
         }
      }

   }

}