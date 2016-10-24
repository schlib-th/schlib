/*
 * TaskRegistry.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app;

import android.os.AsyncTask;


import java.util.ArrayList;
import java.util.List;

/**
 * A task registry for convenient cancelling of multiple running {@link android.os.AsyncTask AsyncTasks}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class TaskRegistry {

   private final List<AsyncTask<Void,?,?>> asyncTasks;

   /**
    * Constructs a new registry for {@link AsyncTask}s.
    */
   public TaskRegistry() {
      asyncTasks = new ArrayList<>(5);
   }

   /**
    * Adds the specified {@code task} to the registry and executes it immediately.
    *
    * @param task
    *       the task to register and execute.
    */
   public synchronized void add(AsyncTask<Void,?,?> task) {
      Log.d("task=" + task);
      asyncTasks.add(task);
      task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
   }

   /**
    * Removes the specified {@code task} from the registry.
    *
    * @param task
    *       the task to remove.
    */
   public synchronized void remove(AsyncTask<Void,?,?> task) {
      Log.d("task=" + task);
      asyncTasks.remove(task);
   }

   /**
    * Cancels all registered task and removes them from the registry.
    */
   public synchronized void cancel() {
      for (AsyncTask<Void,?,?> task : asyncTasks) {
         Log.d("task=" + task);
         task.cancel(true);
      }
      asyncTasks.clear();
   }

}