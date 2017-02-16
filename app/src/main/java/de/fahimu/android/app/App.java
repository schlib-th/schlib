/*
 * App.java
 *
 * Copyright 2015 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app;

import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.view.View;


import java.util.Locale;

/**
 * The object returned by {@link android.content.Context#getApplicationContext()}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2015
 * @since SchoolLibrary 1.0
 */
public abstract class App extends android.app.Application {

   @Nullable
   private static App singleton;

   @NonNull
   public static App getInstance() {
      if (singleton == null) {
         throw new IllegalStateException("getInstance() called before onCreate()");
      } else {
         return singleton;
      }
   }

   @Override
   public void onCreate() {
      super.onCreate();
      singleton = this;
      Log.d("App " + singleton.getClass() + " (" + getName() + ") created");
   }

   @NonNull
   public abstract String getName();

   /* ============================================================================================================== */

   @Nullable
   private static SQLiteDatabase database;

   @NonNull
   public static synchronized SQLiteDatabase getDb() {
      if (database == null) {
         database = getInstance().getOpenHelper().getWritableDatabase();
      }
      return database;
   }

   protected abstract SQLiteOpenHelper getOpenHelper();

   /* ============================================================================================================== */

   /**
    * Returns a color value in the form {@code 0xAARRGGBB} associated with the specified color resource ID.
    *
    * @param colorResId
    *       the color resource ID, as generated by the aapt tool.
    * @return a color value in the form {@code 0xAARRGGBB} associated with the specified color resource ID.
    */
   @ColorInt
   public static int getColorFromRes(@ColorRes int colorResId) {
      return ContextCompat.getColor(getInstance(), colorResId);
   }

   /**
    * Returns the number of screen pixels for the specified density independent pixels value.
    *
    * @param dp
    *       the density independent pixels value.
    * @return the number of screen pixels for the specified density independent pixels value.
    */
   public static int dpToPx(float dp) {
      return Math.round(dp * Resources.getSystem().getDisplayMetrics().density);
   }

   /**
    * Returns a localized formatted string from the application's package's
    * default string table, substituting the format arguments as defined in
    * {@link java.util.Formatter} and {@link java.lang.String#format}.
    *
    * @param resId
    *       resource id for the format string
    * @param formatArgs
    *       the format arguments that will be used for substitution.
    * @return a localized formatted string.
    */
   @NonNull
   public static String getStr(@StringRes int resId, Object... formatArgs) {
      return getInstance().getString(resId, formatArgs);
   }

   /**
    * Returns a {@link Locale#US} localized formatted string,
    * substituting the format arguments as defined in
    * {@link java.util.Formatter} and {@link java.lang.String#format}.
    *
    * @param format
    *       the format string (see {@link java.util.Formatter#format}).
    * @param formatArgs
    *       the format arguments that will be used for substitution.
    * @return the formatted string.
    */
   @NonNull
   public static String format(String format, Object... formatArgs) {
      return String.format(Locale.US, format, formatArgs);
   }

   /**
    * Returns the child view with the specified {@code resId}.
    * If there is no such child view defined, or if the view cannot be cast to the specified {@code type},
    * an {@link ClassCastException} will be thrown.
    *
    * @param parent
    *       the parent view where to search.
    * @param type
    *       the type of the requested view.
    * @param resId
    *       the id attribute of the view from the XML file.
    * @return the child view with the specified {@code resId}.
    *
    * @throws ClassCastException
    *       if there is no such child view defined.
    */
   @NonNull
   public static <V extends View> V findView(@NonNull View parent, @NonNull Class<V> type, @IdRes int resId) {
      View child = parent.findViewById(resId);
      if (child == null) { throw new ClassCastException("resource not found"); }
      return type.cast(child);
   }

}