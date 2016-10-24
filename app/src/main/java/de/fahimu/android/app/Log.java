/*
 * Log.java
 *
 * Copyright 2015 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app;

import android.os.SystemClock;


import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A convenience class for Logging.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2015
 * @since SchoolLibrary 1.0
 */
public final class Log {

   public static class Scope implements AutoCloseable {
      private final String clsMtd;
      private final long   timestamp;

      private Scope(String clsMtd) {
         this.clsMtd = clsMtd;
         this.timestamp = (clsMtd == null) ? SystemClock.uptimeMillis() : 0;
      }

      /**
       * Sends the specified {@code msg} as a {@link android.util.Log#v(String, String) VERBOSE} log message.
       *
       * @param msg
       *       the message.
       */
      public void d(String msg) { debug(stacks.get(), this, msg); }

      @Override
      public void close() { leave(stacks.get(), this); }
   }

   private static final AtomicInteger nextId = new AtomicInteger(10000);

   private static final ThreadLocal<Integer> tid = new ThreadLocal<Integer>() {
      @Override
      protected Integer initialValue() { return nextId.getAndIncrement(); }
   };

   private static final ThreadLocal<Stack<Scope>> stacks = new ThreadLocal<Stack<Scope>>() {
      @Override
      protected Stack<Scope> initialValue() { return new Stack<>(); }
   };

	/* ------------------------------------------------------------------------------------------------------------- */

   private static String inset = "";

   /**
    * Sends a {@link android.util.Log#v(String, String) VERBOSE} log message. The {@code tag} is set to the ID of the
    * calling thread, the {@code msg} will be shifted two blanks to the right for each element on the {@code stack}.
    *
    * @param stack
    *       the {@link Log.Scope} stack.
    * @param msg
    *       the message.
    */
   private static void log(Stack<Scope> stack, String msg) {
      int size = stack.size(), tid = Log.tid.get();
      if (inset.length() < 2 * size) { inset = inset + "          "; }
      if (msg.contains("******")) {
         android.util.Log.e(Integer.toString(tid), inset.substring(0, 2 * size) + msg);
      } else if (tid == 10000) {
         android.util.Log.v(Integer.toString(tid), inset.substring(0, 2 * size) + msg);
      } else {
         android.util.Log.d(Integer.toString(tid), inset.substring(0, 2 * size) + msg);
      }
   }

   /**
    * Pushes a new {@link Log.Scope} on the stack.
    * First it determines the class and method name of the calling method. Then, if {@code doLog} is true,
    * both are logged with an appended left curly brace (<code>'&#123;'</code>), otherwise both are stored with the
    * {@code Log.Scope}.
    *
    * @param stack
    *       the {@link Log.Scope} stack.
    * @param doLog
    *       log the class and method name of the calling method.
    * @return the new {@code Log.Scope}.
    */
   private static Scope entry(Stack<Scope> stack, boolean doLog) {
      StackTraceElement ste = new Throwable().getStackTrace()[2];
      String clsMtd = ste.getClassName() + ':' + ste.getMethodName();
      if (doLog) { log(stack, clsMtd + " {"); }
      Scope scope = new Scope(doLog ? null : clsMtd);
      stack.push(scope);
      return scope;
   }

   /**
    * Pops all elements from the stack until the specified {@code scope} is found.
    * If the class and method name of the calling method were logged when the element was pushed on the stack, a right
    * curly brace (<code>'&#125;'</code>) is logged after popping.
    *
    * @param stack
    *       the {@link Log.Scope} stack.
    * @param scope
    *       the {@code Log.Scope} to search for.
    */
   private static void leave(Stack<Scope> stack, Scope scope) {
      Scope elem;
      do {
         elem = stack.pop();
         if (elem.clsMtd == null) {
            long duration = SystemClock.uptimeMillis() - elem.timestamp;
            if (duration < 10) { log(stack, "}"); } else { log(stack, "} (" + duration + ")"); }
         }
      } while (elem != scope);
   }

   /**
    * Sends the specified {@code msg} as a {@link android.util.Log#v(String, String) VERBOSE} log message.
    * First all elements are {@link #leave(Stack, Scope) popped} from stack until the specified {@code scope} is the
    * top element on the {@code stack}. Then, if the class and method name of the calling method were <b>not</b>
    * logged when the element was pushed on the stack, both are inserted before the specified {@code msg}.
    *
    * @param stack
    *       the {@link Log.Scope} stack.
    * @param scope
    *       the {@code Log.Scope} of the message.
    * @param msg
    *       the message to log.
    */
   private static void debug(Stack<Scope> stack, Scope scope, String msg) {
      while (scope != stack.peek()) { leave(stack, scope); }
      log(stack, scope.clsMtd == null ? msg : scope.clsMtd + ": " + msg);
   }

   /**
    * Logs the entry of a method. This call should be the first statement in a method
    * and called from inside a Java 7 {@code try}-with-resources statement.
    *
    * @return the {@link Log.Scope} for subsequent calls of {@link Log.Scope#d(String) d}.
    */
   public static Scope e() { return entry(stacks.get(), true); }

   /**
    * Sends the specified {@code msg} as a {@link android.util.Log#v(String, String) VERBOSE} log message.
    *
    * @param msg
    *       the message.
    */
   public static void d(String msg) {
      Stack<Scope> stack = stacks.get();
      try (Scope scope = entry(stack, false)) {
         debug(stack, scope, msg);
      }
   }

}