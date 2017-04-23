/*
 * App.java
 *
 * Copyright 2015 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;


import de.fahimu.android.app.Log;
import de.fahimu.android.app.SoundPlayer;
import de.fahimu.schlib.db.OpenHelper;

/**
 * The object returned by {@link android.content.Context#getApplicationContext()}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2015
 * @since SchoolLibrary 1.0
 */
public final class App extends de.fahimu.android.app.App {

   @RawRes
   private static final int[] SOUND_IDS = {
         R.raw.horn, R.raw.lock, R.raw.beep,
         R.raw.bell, R.raw.bell_issue, R.raw.bell_return
   };

   private static SoundPlayer soundPlayer;

   @Override
   public void onCreate() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onCreate();
         soundPlayer = new SoundPlayer(this, SOUND_IDS);
      }
   }

   @Override
   @NonNull
   public String getName() { return getStr(R.string.app_name); }

   @NonNull
   @Override
   protected SoundPlayer getSoundPlayer() { return soundPlayer; }

   @Override
   @NonNull
   public SQLiteOpenHelper getOpenHelper() { return new OpenHelper(); }

}