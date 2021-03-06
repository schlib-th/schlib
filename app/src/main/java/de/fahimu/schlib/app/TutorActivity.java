/*
 * TutorActivity.java
 *
 * Copyright 2015 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.animation.Animator;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import de.fahimu.android.app.Log;
import de.fahimu.android.app.SmartAnimator;
import de.fahimu.schlib.anw.ISBN;
import de.fahimu.schlib.db.Book;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.Label;
import de.fahimu.schlib.db.Lending;
import de.fahimu.schlib.db.Use;
import de.fahimu.schlib.db.User;

/**
 * Issue or take back books from users.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2015
 * @since SchoolLibrary 1.0
 */
public final class TutorActivity extends SchlibActivity {

   private View scan;
   private View item0;
   private View card0;    // set to visible or invisible
   private View book0;    // set to visible or invisible
   private View item1;
   private View card1;    // set to visible or invisible
   private View book1;    // set to visible or invisible
   private View scan0;
   private View scan1;
   private View stop;

   private TextView displayIssue;
   private TextView displayReturn;
   private TextView displayProgress;

   private TextView message1;
   private TextView message2;

   private SmartAnimator initAnimator, scanAnimator, beepAnimator, showAnimator, stopAnimator;

   private Book scannedBook;

   /* ============================================================================================================== */

   private void selectBook() {
      show(book0);
      show(book1);
      hide(card0);
      hide(card1);
   }

   private void selectCard() {
      hide(book0);
      hide(book1);
      show(card0);
      show(card1);
   }

   private static void show(@NonNull View view) { view.setVisibility(View.VISIBLE); }

   private static void hide(@NonNull View view) { view.setVisibility(View.GONE); }

   /* -------------------------------------------------------------------------------------------------------------- */

   private void createInitAnimator() {
      initAnimator = new SmartAnimator("InitAnimator", scan, -1) {
         @Override
         protected void create() {
            Animator bookIn = moveXAnimator(item0, new DecelerateInterpolator(), 200, 1000, item0.getWidth(), 0f);
            Animator scanUp = moveYAnimator(scan0, new DecelerateInterpolator(), 500, 800, scan0.getHeight(), 0f);
            getAnimator().playTogether(bookIn, scanUp);
         }

         @Override
         protected void onAnimationStart() {
            selectBook();
            item0.setAlpha(1f);
            item1.setAlpha(0f);
            scan0.setAlpha(1f);
            scan1.setAlpha(0f);
            item0.setTranslationX(item0.getWidth());
            scan0.setTranslationY(scan0.getHeight());
         }

         @Override
         protected void onAnimationEnd() {
            if (isCancelled()) {
               item0.setTranslationX(0f);
               scan0.setTranslationY(0f);
            } else { scanAnimator.start(); }
         }
      };
   }

   private void createScanAnimator() {
      scanAnimator = new SmartAnimator("ScanAnimator", scan, 8000) {
         @Override
         protected void create() {
            Animator s1 = multiAnimator(250, alphaSetter(item0, 0f), alphaSetter(item1, 1f));
            Animator s0 = multiAnimator(250, alphaSetter(item0, 1f), alphaSetter(item1, 0f));
            getAnimator().playSequentially(s1, s0);
         }
      };
   }

   private void createBeepAnimator() {
      beepAnimator = new SmartAnimator("BeepAnimator", scan, -1) {
         @Override
         protected void create() {
            Animator r1 = multiAnimator(0, alphaSetter(item0, 0f), alphaSetter(item1, 1f));    // red on
            Animator b1 = multiAnimator(50, alphaSetter(scan0, 0f), alphaSetter(scan1, 1f));   // blue on
            Animator r0 = multiAnimator(50, alphaSetter(item0, 1f), alphaSetter(item1, 0f));   // red off
            Animator b0 = multiAnimator(50, alphaSetter(scan0, 1f), alphaSetter(scan1, 0f));   // blue off
            Animator mv = moveXAnimator(item0, new AccelerateInterpolator(), 0, 250, 0f, -item0.getWidth());
            getAnimator().playSequentially(r1, b1, r0, b0, mv);
         }

         @Override
         protected void onAnimationStart() {
            initAnimator.cancel(); scanAnimator.cancel(); showAnimator.cancel();
         }

         @Override
         protected void onAnimationEnd() {
            if (isCancelled()) {
               item0.setTranslationX(0f);
            } else if (scannedBook == null) {
               selectBook(); showAnimator.start();
            } else {
               selectCard(); showAnimator.start();
            }
         }
      };
   }

   private void createShowAnimator() {
      showAnimator = new SmartAnimator("ShowAnimator", scan, -1) {
         @Override
         protected void create() {
            Animator mv = moveXAnimator(item0, new DecelerateInterpolator(), 0, 250, item0.getWidth(), 0f);
            getAnimator().playSequentially(mv);
         }

         @Override
         protected void onAnimationEnd() {
            if (isCancelled()) { item0.setTranslationX(0f); } else { scanAnimator.start(); }
         }
      };
   }


   /* -------------------------------------------------------------------------------------------------------------- */

   private void createStopAnimator() {
      stopAnimator = new SmartAnimator("StopAnimator", scan, -1) {
         @Override
         protected void create() {
            Animator show = multiAnimator(0,
                  alphaAnimator(scan, new DecelerateInterpolator(), 0, 300, 1f, 0.2f),
                  scaleAnimator(stop, new OvershootInterpolator(), 0, 300, 0f, 1f));
            Animator hide = multiAnimator(2000,
                  alphaAnimator(scan, new AccelerateInterpolator(), 0, 900, 0.2f, 1f),
                  scaleAnimator(stop, new AccelerateInterpolator(), 0, 300, 1f, 0f));

            getAnimator().playSequentially(show, hide);
         }

         @Override
         protected void reset() { scan.setAlpha(1f); stop.setScaleX(0f); stop.setScaleY(0f); }
      };
   }

   /* ============================================================================================================== */

   @Override
   protected int getContentViewId() { return R.layout.tutor; }

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      User user = Use.getLoggedInNonNull().getUser();
      TextView loggedIn = findView(TextView.class, R.id.tutor_logged_in);
      loggedIn.setText(App.getStr(R.string.tutor_logged_in, user.getDisplay(), user.getDisplayIdcard()));

      scan = findView(View.class, R.id.tutor_scan);
      item0 = findView(View.class, R.id.tutor_item_0);
      card0 = findView(View.class, R.id.tutor_card_0);
      book0 = findView(View.class, R.id.tutor_book_0);
      item1 = findView(View.class, R.id.tutor_item_1);
      card1 = findView(View.class, R.id.tutor_card_1);
      book1 = findView(View.class, R.id.tutor_book_1);
      scan0 = findView(View.class, R.id.tutor_scan_0);
      scan1 = findView(View.class, R.id.tutor_scan_1);
      stop = findView(View.class, R.id.tutor_stop);

      displayIssue = findView(TextView.class, R.id.tutor_display_issue);
      displayReturn = findView(TextView.class, R.id.tutor_display_return);
      displayProgress = findView(TextView.class, R.id.tutor_display_progress);

      message1 = findView(TextView.class, R.id.tutor_message_1);
      message2 = findView(TextView.class, R.id.tutor_message_2);

      createInitAnimator();
      createScanAnimator();
      createBeepAnimator();
      createShowAnimator();
      createStopAnimator();
   }

   @Override
   protected void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
         message1.setText("");
         message2.setText("");
         displayIssue.setTextColor(App.getColorFromRes(R.color.color_tutor_text_0));
         displayIssue.setBackgroundColor(App.getColorFromRes(R.color.color_tutor_back_0));
         displayReturn.setTextColor(App.getColorFromRes(R.color.color_tutor_text_0));
         displayReturn.setBackgroundColor(App.getColorFromRes(R.color.color_tutor_back_0));
         displayProgress.setText(R.string.tutor_progress_please_scan_book);
      }
   }

   @Override
   protected void onPermissionGranted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         initAnimator.start();
         scannedBook = null;
         message1.setText(""); message2.setText("");
      }
   }

   @Override
   protected void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
         initAnimator.cancel();
         scanAnimator.cancel();
         beepAnimator.cancel();
         showAnimator.cancel();
         stopAnimator.cancel();
      }
   }

   /* ============================================================================================================== */

   @Override
   public void onBarcode(String barcode) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         message1.setText(""); message2.setText("");
         Idcard idcard = Idcard.parse(barcode);
         if (idcard != null) {
            onIdcardScanned(idcard);
         } else {
            ISBN isbn = ISBN.parse(barcode);
            if (isbn != null) {
               onIsbnScanned(isbn);
            } else {
               Label label = Label.parse(barcode);
               if (label != null) {
                  onLabelScanned(label);
               } else {
                  setError(R.string.tutor_message_1_barcode_unknown, R.string.tutor_message_2_only_idcard_or_book);
               }
            }
         }
      }
   }

   private void onIdcardScanned(Idcard idcard) {
      if (scannedBook == null) {
         setError(R.string.tutor_message_1_expected_book, R.string.tutor_message_2_book_before_user);
      } else {
         if (!idcard.isUsed()) {
            if (idcard.isLost()) {
               setError(R.string.login_message_1_lost, R.string.login_message_2_please_return_idcard);
            } else if (idcard.isPrinted()) {
               setError(R.string.login_message_1_printed, R.string.login_message_2_please_return_idcard);
            } else {   // not used, not lost, not printed => idcard is stocked
               setError(R.string.login_message_1_stocked, R.string.login_message_2_please_return_idcard);
            }
            setDisplay(3, 0, R.string.tutor_progress_idcard_not_used);
         } else {
            User user = User.getByIdcard(idcard);
            ArrayList<Lending> lendings = Lending.getByUser(user, true);
            if (lendings.size() == user.getNbooks()) {
               App.playSound(R.raw.horn);
               String lentBook = lendings.get(0).getBook().getDisplay();
               if (user.getNbooks() == 1) {
                  setDisplay(3, 0, R.string.tutor_progress_limit_1, user.getDisplay(), lentBook);
               } else {
                  setDisplay(3, 0, R.string.tutor_progress_limit_n, user.getDisplay(), lentBook, lendings.size());
               }
               stopAnimator.start();
            } else {
               Lending.issueBook(scannedBook, user);
               App.playSound(R.raw.bell_issue);
               setDisplay(1, 0, R.string.tutor_progress_issue_done, scannedBook.getDisplay(), user.getDisplay());
            }
         }
         scannedBook = null;
         selectCard();
         beepAnimator.start();
      }
   }

   private void onIsbnScanned(ISBN isbn) {
      Book book = Book.getIdentifiedByISBN(isbn);
      if (book == null) {
         setError(R.string.tutor_message_1_book_not_registered, R.string.tutor_message_2_please_return_book);
      } else {
         onBookScanned(book);
      }
   }

   private void onLabelScanned(Label label) {
      if (!label.isUsed()) {
         if (label.isLost()) {
            setError(R.string.tutor_message_1_label_lost, R.string.tutor_message_2_please_return_book);
         } else {
            setError(R.string.tutor_message_1_label_not_used, R.string.tutor_message_2_please_return_book);
         }
      } else {
         onBookScanned(Book.getByLabel(label));
      }
   }

   private void onBookScanned(@NonNull Book book) {
      if (scannedBook != null) {
         setError(R.string.tutor_message_1_expected_user, R.string.tutor_message_2_book_before_user);
      } else {
         if (book.isVanished()) {
            book.setVanished(null).update();    // book re-emerged magically after being set to vanished
         }
         ArrayList<Lending> lendings = Lending.getByBook(book, true);
         if (lendings.isEmpty()) {
            App.playSound(R.raw.bell);
            setDisplay(2, 0, R.string.tutor_progress_issue_init, book.getDisplay());
            scannedBook = book;
         } else {
            App.playSound(R.raw.bell_return);
            User user = lendings.get(0).getUser();
            int delay = lendings.get(0).returnBook();
            if (delay < 2) {
               setDisplay(0, 1, R.string.tutor_progress_return_in_time, book.getDisplay(), user.getDisplay());
            } else {
               setDisplay(0, 2, R.string.tutor_progress_return_belated, book.getDisplay(), user.getDisplay(), delay);
            }
         }
         selectBook();
         beepAnimator.start();
      }
   }

   private void setError(int resId1, int resId2) {
      App.playSound(R.raw.horn);
      stopAnimator.start();
      message1.setText(resId1);
      message2.setText(resId2);
      if (scannedBook == null) {
         setDisplay(0, 0, R.string.tutor_progress_please_scan_book);
      } else {
         setDisplay(2, 0, R.string.tutor_progress_please_scan_card);
      }
   }

   /* ============================================================================================================== */

   private static class TextViewColor {
      @ColorRes
      private static final int[] TEXT_COLORS = {
            R.color.color_tutor_text_0, R.color.color_tutor_text_1,
            R.color.color_tutor_text_2, R.color.color_tutor_text_3
      };
      @ColorRes
      private static final int[] BACK_COLORS = {
            R.color.color_tutor_back_0, R.color.color_tutor_back_1,
            R.color.color_tutor_back_2, R.color.color_tutor_back_3
      };

      @ColorInt
      private final int textColor, backColor;

      TextViewColor(int mode) {
         textColor = App.getColorFromRes(TEXT_COLORS[mode]);
         backColor = App.getColorFromRes(BACK_COLORS[mode]);
      }

      TextViewColor(TextView textView) {
         textColor = textView.getCurrentTextColor();
         backColor = ((ColorDrawable) textView.getBackground()).getColor();
      }

      boolean isGrey() {
         return textColor == App.getColorFromRes(TEXT_COLORS[0]) && backColor == App.getColorFromRes(BACK_COLORS[0]);
      }
   }

   private SmartAnimator displayAnimator = null;

   private void setDisplay(int modeIssue, int modeReturn, int resId, Object... formatArgs) {
      if (displayAnimator != null) { displayAnimator.cancel(); }

      CharSequence oldText = displayProgress.getText();
      CharSequence newText = App.getStr(resId, formatArgs);
      TextViewColor oldIssue = new TextViewColor(displayIssue);
      TextViewColor newIssue = new TextViewColor(modeIssue);
      TextViewColor oldReturn = new TextViewColor(displayReturn);
      TextViewColor newReturn = new TextViewColor(modeReturn);

      animateDisplay(oldText, newText, oldIssue, newIssue, oldReturn, newReturn);
   }

   private void animateDisplay(
         final CharSequence oldText, final CharSequence newText,
         final TextViewColor oldIssue, final TextViewColor newIssue,
         final TextViewColor oldReturn, final TextViewColor newReturn) {

      displayAnimator = new SmartAnimator("DisplayAnimator", displayProgress, -1) {
         @Override
         protected void create() {
            List<Animator> list = new ArrayList<>();
            if (!(oldIssue.isGrey() && newIssue.isGrey())) {
               list.add(createDisplayColorAnimator(displayIssue, oldIssue, newIssue));
            }
            if (!(oldReturn.isGrey() && newReturn.isGrey())) {
               list.add(createDisplayColorAnimator(displayReturn, oldReturn, newReturn));
            }
            list.add(charsAnimator(displayProgress, new LinearInterpolator(), 0, oldText, newText));
            getAnimator().playTogether(list);
         }

         private Animator createDisplayColorAnimator(TextView textView, TextViewColor oldTVC, TextViewColor newTVC) {
            return multiAnimator(0,
                  colorAnimator(textView, new LinearInterpolator(), 25, 100,
                        "textColor", oldTVC.textColor, newTVC.textColor),
                  colorAnimator(textView, new LinearInterpolator(), 25, 100,
                        "backgroundColor", oldTVC.backColor, newTVC.backColor),
                  scaleAnimator(textView, new CycleInterpolator(1.0f), 0, 150, 1.0f, 1.1f));
         }
      };
      displayAnimator.start();
   }

}