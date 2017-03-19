/*
 * SoundPlayer.java
 *
 * Copyright 2015 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app;

import android.app.Activity;
import android.content.Context;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.support.annotation.RawRes;
import android.util.SparseIntArray;

/**
 * Plays short (a few seconds) sound samples.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2015
 * @since SchoolLibrary 1.0
 */
public final class SoundPlayer {

   private final SparseIntArray soundMap;
   private final SoundPool      soundPool;

   private int pendingLoads;

   /**
    * Constructs a new {@code SoundPlayer} that can play the sound files specified by
    * the list of resource IDs.
    * The resource IDs are the names of sound files (without their extension) residing in {@code res/raw}.
    * The constructor should be called from {@link Activity#onResume()}.
    *
    * @param context
    *       needed for loading the sound files.
    * @param resIds
    *       the resource IDs specifying the sound files.
    */
   public SoundPlayer(Context context, @RawRes int... resIds) {
      soundMap = new SparseIntArray(resIds.length);
      soundPool = new SoundPool.Builder().setMaxStreams(4).build();
      pendingLoads = resIds.length;
      Log.d("number of supplied sound files: " + pendingLoads);
      soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
         @Override
         public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            if (status == 0) { pendingLoads--; }
         }
      });
      for (@RawRes int resId : resIds) {
         soundMap.put(resId, soundPool.load(context, resId, 1));
      }
   }

   /**
    * Play the specified sound file.
    *
    * @param resId
    *       the name of the sound file without its extension.
    */
   synchronized void play(@RawRes int resId) {
      if (soundPool != null && pendingLoads == 0 && soundMap.get(resId, -1) != -1) {
         soundPool.play(soundMap.get(resId), 1f, 1f, 0, 0, 1f);
      }
   }

}