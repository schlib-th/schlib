/*
 * DelayedTask.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

/**
 * An AsyncTask that waits for a specified time before running {@link #executeDelayed(boolean)} on the UI thread.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public abstract class DelayedTask extends AsyncTask<Void,Void,Void> {

   private final long         delay;
   @NonNull
   private final TaskRegistry taskRegistry;

   /**
    * Constructs a new {@code DelayedTask}.
    *
    * @param delay
    *       time in milliseconds to wait until {@link #executeDelayed(boolean)} will be called.
    */
   protected DelayedTask(long delay) {
      this.delay = delay;
      this.taskRegistry = new TaskRegistry();
   }

   /**
    * Constructs a new {@code DelayedTask}
    * that will be registered at the specified {@link TaskRegistry} when executed.
    *
    * @param delay
    *       time in milliseconds to wait until {@link #executeDelayed(boolean)} will be called.
    * @param taskRegistry
    *       the {@code TaskRegistry} where to register this {@code DelayedTask}.
    */
   protected DelayedTask(long delay, @NonNull TaskRegistry taskRegistry) {
      this.delay = delay;
      this.taskRegistry = taskRegistry;
   }

   /**
    * Execute this {@code DelayedTask}.
    */
   public final void execute() { taskRegistry.add(this); }

   @Override
   protected final Void doInBackground(Void... voids) {
      try { Thread.sleep(delay); } catch (InterruptedException e) { /* IGNORE */ }
      taskRegistry.remove(this);
      return null;
   }

   /**
    * The method that will be executed delayed on the UI thread.
    *
    * @param cancelled
    *       {@code true} if this task was cancelled.
    */
   protected abstract void executeDelayed(boolean cancelled);

   /**
    * Does nothing.
    */
   @Override
   protected final void onPreExecute() { /* do nothing */ }

   /**
    * Does nothing.
    */
   @Override
   protected final void onProgressUpdate(Void... values) { /* do nothing */ }

   /**
    * Calls {@link #executeDelayed(boolean)} with value {@code true}.
    */
   @Override
   protected final void onCancelled(Void aVoid) { executeDelayed(true); }

   /**
    * Calls {@link #executeDelayed(boolean)} with value {@code false}.
    */
   @Override
   protected final void onPostExecute(Void aVoid) { executeDelayed(false); }

}