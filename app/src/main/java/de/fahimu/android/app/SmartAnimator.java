/*
 * SmartAnimator.java
 *
 * Copyright 2015 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.support.annotation.ColorInt;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

/**
 * A SmartAnimator adds to an {@link AnimatorSet} missing functionality
 * like deferred creation until the view has been measured or repeated animation .
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2015
 * @since SchoolLibrary 1.0
 */
public abstract class SmartAnimator {

   private final String name;

   private final AnimatorSet animator;

   private final TaskRegistry taskRegistry;

   private volatile boolean created = false, started = false, cancelled = false;

   /**
    * Creates a new {@code SmartAnimator} that animates the specified {@code view} and repeats the animation
    * {@code repeatAfter} milliseconds after the animation has ended. If {@code repeatAfter} is lower than zero,
    * the animation will be executed only once.
    *
    * @param name
    *       a descriptive name for the animation; used for logging purposes.
    * @param view
    *       the view to animate.
    * @param repeatAfter
    *       milliseconds to wait after the animation has ended before it will be repeated.
    */
   protected SmartAnimator(String name, View view, final long repeatAfter) {
      this.name = name;
      this.animator = new AnimatorSet();
      this.taskRegistry = new TaskRegistry();

      animator.addListener(new AnimatorListenerAdapter() {
         @Override
         public void onAnimationStart(Animator animation) {
            cancelled = false;
            SmartAnimator.this.onAnimationStart();
         }

         @Override
         public void onAnimationCancel(Animator animation) {
            cancelled = true;
         }

         @Override
         public void onAnimationEnd(Animator animation) {
            synchronized (SmartAnimator.this) {
               SmartAnimator.this.onAnimationEnd();
               if (started) {
                  if (repeatAfter < 0) {
                     started = false;
                  } else {
                     // AnimatorSet lacks the method setRepeatMode; this fixes that deficiency a little bit.
                     new DelayedTask(repeatAfter, taskRegistry) {
                        @Override
                        protected void executeDelayed(boolean cancelled) {
                           if (!cancelled) { animator.start(); }
                        }
                     }.execute();
                  }
               }
            }
         }
      });
      deferCreate(view);
   }

   /**
    * Defers the call of {@link #create()} until all subviews of {@code view} have been measured.
    *
    * @param view
    *       the view to observe.
    */
   private void deferCreate(View view) {
      view.post(new Runnable() {
         @Override
         public void run() {
            synchronized (SmartAnimator.this) {
               Log.d("create " + name);
               create();
               created = true;
               if (started) { animator.start(); }
            }
         }
      });
   }

   /* ============================================================================================================== */

   /**
    * Returns the encapsulated {@link AnimatorSet}. Must be used from {@link #create()} to set the animation.
    *
    * @return the encapsulated {@link AnimatorSet}.
    */
   protected final AnimatorSet getAnimator() { return animator; }

   /**
    * This method must be implemented by the subclass to create the concrete animation.
    */
   protected abstract void create();

   /**
    * This method can be implemented by the subclass to reset the animated objects.
    */
   protected void reset() {}

   /**
    * This method will be called after the animation was started.
    * The default implementation just calls {@link #reset()}.
    */
   protected void onAnimationStart() { reset(); }

   /**
    * This method will be called after the animation has ended.
    * The default implementation just calls {@link #reset()}.
    */
   protected void onAnimationEnd() { reset(); }

   /**
    * Returns true if the animation was cancelled.
    *
    * @return true if the animation was cancelled.
    */
   protected boolean isCancelled() { return cancelled; }

   /**
    * Starts the animation.
    * If the animation is already running, the animation will first be stopped. If the animated view has
    * not been measured, the start will be deferred until the view and all subviews have been measured.
    */
   public final synchronized void start() {
      if (started) { cancel(); }
      started = true;
      if (created) { animator.start(); }
   }

   /**
    * Cancels the animation.
    */
   public final synchronized void cancel() {
      started = false;
      taskRegistry.cancel();
      if (created) { animator.cancel(); }
   }

   /**
    * Stops the animation.
    */
   public final synchronized void stop() {
      started = false;
      taskRegistry.cancel();
   }

   /* ============================================================================================================== */

   /**
    * Returns an {@link ObjectAnimator} that immediately sets
    * the alpha value of the specified {@code view} to the specified {@code value}.
    *
    * @param view
    *       the view to animate.
    * @param value
    *       the new alpha value.
    * @return a new {@code ObjectAnimator}.
    */
   protected final ObjectAnimator alphaSetter(View view, float value) {
      return alphaAnimator(view, new LinearInterpolator(), 0, 0, value);
   }

   protected final ObjectAnimator alphaAnimator(View view, Interpolator interpolator,
         long startDelay, long duration, float... values) {
      return set(ObjectAnimator.ofFloat(view, "alpha", values), interpolator, startDelay, duration);
   }

   protected final ObjectAnimator moveXAnimator(View view, Interpolator interpolator,
         long startDelay, long duration, float... values) {
      return set(ObjectAnimator.ofFloat(view, "translationX", values), interpolator, startDelay, duration);
   }

   protected final ObjectAnimator moveYAnimator(View view, Interpolator interpolator,
         long startDelay, long duration, float... values) {
      return set(ObjectAnimator.ofFloat(view, "translationY", values), interpolator, startDelay, duration);
   }

   protected final ObjectAnimator scaleAnimator(View view, Interpolator interpolator,
         long startDelay, long duration, float... values) {
      PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("scaleX", values);
      PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("scaleY", values);
      return set(ObjectAnimator.ofPropertyValuesHolder(view, pvhX, pvhY), interpolator, startDelay, duration);
   }

   protected final ObjectAnimator colorAnimator(View view, Interpolator interpolator,
         long startDelay, long duration, String propertyName, @ColorInt Integer... color) {
      return set(ObjectAnimator.ofObject(view, propertyName, new ArgbEvaluator(), (Object[]) color),
            interpolator, startDelay, duration);
   }

   protected final ObjectAnimator charsAnimator(TextView textView, Interpolator interpolator,
         long startDelay, CharSequence s0, CharSequence s1) {
      return set(ObjectAnimator.ofObject(textView, Property.of(TextView.class, CharSequence.class, "text"),
            new CharSequenceEvaluator(), s0, s1), interpolator, startDelay, animationDuration(s0, s1));
   }

   private static ObjectAnimator set(ObjectAnimator anim, Interpolator interpolator, long startDelay, long duration) {
      anim.setInterpolator(interpolator);
      anim.setStartDelay(startDelay);
      return anim.setDuration(duration);
   }

   /* ============================================================================================================== */

   /**
    * Returns an {@link AnimatorSet} that plays together the specified {@link Animator} {@code items}
    * after the specified {@code startDelay}.
    *
    * @param startDelay
    *       ms to wait before the animation begins.
    * @param items
    *       the animations to play together.
    * @return a new {@code AnimatorSet}.
    */
   protected final AnimatorSet multiAnimator(long startDelay, Animator... items) {
      AnimatorSet anim = new AnimatorSet();
      anim.playTogether(items);
      anim.setStartDelay(startDelay);
      return anim;
   }

   /* ============================================================================================================== */

   /**
    * Animates between two {@code CharSequence}s by
    * first deleting the characters from the first {@code CharSequence} starting at the end
    * and after that appending the characters from the second one.
    * The deleting will stop when the remaining characters of the first {@code CharSequence}
    * are a prefix of the second one.
    * The resulting animation looks like the characters are first erased by pressing the backspace key
    * and the new string is then typed in on the keyboard (but very very fast!).
    */
   private static class CharSequenceEvaluator implements TypeEvaluator<CharSequence> {
      @Override
      public CharSequence evaluate(float fraction, CharSequence seq0, CharSequence seq1) {
         int start = longestCommonPrefixLength(seq0, seq1);
         int len0 = seq0.length() - start, len1 = seq1.length() - start;

         float middle = (float) len0 / (float) (len0 + len1);
         if (fraction < middle) {
            return seq0.subSequence(0, start + (int) (0.5f + len0 * (middle - fraction) * (1f / middle)));
         } else {
            return seq1.subSequence(0, start + (int) (0.5f + len1 * (fraction - middle) * (1f / (1f - middle))));
         }
      }
   }

   /**
    * Returns the standard animation duration of an {@link ObjectAnimator} returned by
    * {@link SmartAnimator#charsAnimator(TextView, Interpolator, long, CharSequence, CharSequence) charsAnimator}.
    *
    * @param seq0
    *       first {@code CharSequence}.
    * @param seq1
    *       second {@code CharSequence}.
    * @return the standard animation duration of an {@link ObjectAnimator} returned by {@code charsAnimator}.
    */
   private static int animationDuration(CharSequence seq0, CharSequence seq1) {
      int nCharsToAnimate = seq0.length() + seq1.length() - 2 * longestCommonPrefixLength(seq0, seq1);
      // 3 ms for each char, but at least 150 ms and not more than 400 ms
      return Math.max(150, Math.min(400, 3 * nCharsToAnimate));
   }

   /**
    * Returns the length of the longest common prefix of {@code seq0} and {@code seq1}.
    *
    * @param seq0
    *       first {@code CharSequence}.
    * @param seq1
    *       second {@code CharSequence}.
    * @return the length of the longest common prefix of {@code seq0} and {@code seq1}.
    */
   private static int longestCommonPrefixLength(CharSequence seq0, CharSequence seq1) {
      int len0 = seq0.length(), len1 = seq1.length(), start = 0;
      while (start < len0 && start < len1 && seq0.charAt(start) == seq1.charAt(start)) {
         start += 1;
      }
      return start;
   }

}