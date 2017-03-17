/*
 * LoginActivity.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.animation.Animator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;


import java.util.Arrays;

import de.fahimu.android.app.DelayedTask;
import de.fahimu.android.app.Log;
import de.fahimu.android.app.SmartAnimator;
import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.android.app.scanner.NoFocusDialog.ButtonListener;
import de.fahimu.android.db.BackupDatabase;
import de.fahimu.android.share.ExternalFile;
import de.fahimu.schlib.anw.ISBN;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.db.Book;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.Label;
import de.fahimu.schlib.db.Use;
import de.fahimu.schlib.db.User;
import de.fahimu.schlib.db.User.Role;
import de.fahimu.schlib.share.FileType;

/**
 * Login with Idcard.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class LoginActivity extends SchlibActivity {

   private View     scan;
   private View     card0;
   private View     scan0;
   private View     card1;
   private View     scan1;
   private View     stop;
   private TextView message1;
   private TextView message2;

   private SmartAnimator scanAnimator, stopAnimator;

   private NoFocusDialog adminOrTutorDialog;

   /* ============================================================================================================== */

   private void createScanAnimator() {
      scanAnimator = new SmartAnimator("ScanAnimator", scan, 5000) {
         @Override
         protected void create() {
            Animator up = moveYAnimator(scan0, new DecelerateInterpolator(), 500, 800, scan0.getHeight(), 0f);
            Animator r1 = multiAnimator(400, alphaSetter(card0, 0f), alphaSetter(card1, 1f));
            Animator b1 = multiAnimator(300, alphaSetter(scan0, 0f), alphaSetter(scan1, 1f));
            Animator r0 = multiAnimator(100, alphaSetter(card1, 0f), alphaSetter(card0, 1f));
            Animator b0 = multiAnimator(200, alphaSetter(scan1, 0f), alphaSetter(scan0, 1f));
            Animator dn = moveYAnimator(scan0, new AccelerateInterpolator(), 400, 800, 0f, scan0.getHeight());

            getAnimator().playSequentially(up, r1, b1, r0, b0, dn);
         }

         @Override
         protected void reset() {
            card0.setAlpha(1f);
            card1.setAlpha(0f);
            scan0.setAlpha(1f);
            scan1.setAlpha(0f);
            scan0.setTranslationY(scan0.getHeight());
         }
      };
   }

   private void createStopAnimator() {
      stopAnimator = new SmartAnimator("StopAnimator", scan, -1) {
         @Override
         protected void create() {
            Animator show = multiAnimator(0,
                  alphaAnimator(scan, new DecelerateInterpolator(), 0, 300, 1f, 0.2f),
                  scaleAnimator(stop, new OvershootInterpolator(), 0, 300, 0f, 1f));
            Animator hide = multiAnimator(3000,
                  alphaAnimator(scan, new AccelerateInterpolator(), 0, 900, 0.2f, 1f),
                  scaleAnimator(stop, new AccelerateInterpolator(), 0, 300, 1f, 0f));

            getAnimator().playSequentially(show, hide);
         }

         @Override
         protected void reset() { scan.setAlpha(1f); stop.setScaleX(0f); stop.setScaleY(0f); }
      };
   }

   private void createAdminOrTutorDialog() {
      adminOrTutorDialog = new NoFocusDialog(this, NoFocusDialog.IGNORE_CANCEL);
      adminOrTutorDialog.setTitle(R.string.login_dialog_title);
      adminOrTutorDialog.setMessage(R.string.login_dialog_message);
      adminOrTutorDialog.setNegativeButton(R.string.login_dialog_tutor, new ButtonListener() {
         @Override
         public void onClick() { playSoundAndStartActivity(TutorActivity.class); }
      });
      adminOrTutorDialog.setPositiveButton(R.string.login_dialog_admin, new ButtonListener() {
         @Override
         public void onClick() { playSoundAndStartActivity(AdminActivity.class); }
      });
   }

   /* ============================================================================================================== */

   @Override
   protected int getContentViewId() { return R.layout.login; }

   private static BroadcastReceiver screenOffReceiver;

   /**
    * When the device goes to sleep and becomes non-interactive, start this activity again.
    * Please note that this activity is always launched in mode "singleTask" (see AndroidManifest.xml)
    */
   private static synchronized void registerScreenOffReceiver() {
      if (screenOffReceiver == null) {
         screenOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
               context.startActivity(new Intent(context, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
         };
         App.getInstance().registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
      }
   }

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      registerScreenOffReceiver();

      scan = findView(View.class, R.id.login_scan);
      card0 = findView(ImageView.class, R.id.login_card_0);
      scan0 = findView(ImageView.class, R.id.login_scan_0);
      card1 = findView(ImageView.class, R.id.login_card_1);
      scan1 = findView(ImageView.class, R.id.login_scan_1);
      stop = findView(ImageView.class, R.id.login_stop);

      message1 = findView(TextView.class, R.id.login_message_1);
      message2 = findView(TextView.class, R.id.login_message_2);

      /*message1.setOnTouchListener(new OnTouchListener() {     // TODO remove after development
         private long lastTime = 0;

         @Override
         public synchronized boolean onTouch(View v, MotionEvent event) {
            if (event.getEventTime() > lastTime + 3000) {     // ignore multi clicks
               lastTime = event.getEventTime();
               Use.login(User.getNonNull(2));
               playSoundAndStartActivity(AdminActivity.class);
            }
            return true;
         }
      });                                                     // TODO remove after development */
      createScanAnimator();
      createStopAnimator();
      createAdminOrTutorDialog();
   }

   @Override
   boolean isHomeShownAsUp() { return false; }

   @Override
   protected void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
         message1.setText("");
         message2.setText("");
      }
   }

   @Override
   protected void onPermissionGranted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         scanAnimator.start();
         // assure that no one is logged in
         Use use = Use.getLoggedInNullable();
         if (use != null) {
            use.setLogoutToNow().update();
            // Delete the oldest backup files if there are more than 9 files, and make a new backup
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
               @Override
               public void run() {
                  String[] backupFiles = new ExternalFile(FileType.BACKUP, null).listNames("sqlite3.gzip");
                  Arrays.sort(backupFiles);
                  for (int i = 0; i < backupFiles.length - 9; i++) {
                     new ExternalFile(FileType.BACKUP, backupFiles[i]).delete();
                  }
                  new BackupDatabase(FileType.BACKUP).execute();
               }
            });
         }
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      return false;
   }

   @Override
   protected void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
         scanAnimator.cancel();
         stopAnimator.cancel();
         adminOrTutorDialog.cancel();
      }
   }

   @Override
   public void onBackPressed() { /* IGNORE back button*/ }

   /* ============================================================================================================== */

   @Override
   public void onBarcode(String barcode) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         scope.d("barcode=" + barcode);
         int sn = SerialNumber.parseCode128(barcode);
         Idcard idcard = Idcard.getNullable(sn);
         if (idcard != null) {
            if (!idcard.isUsed()) {
               if (idcard.isLost()) {
                  setError(R.string.login_message_1_lost, R.string.login_message_2_please_return_idcard);
               } else if (idcard.isPrinted()) {
                  setError(R.string.login_message_1_printed, R.string.login_message_2_please_return_idcard);
               } else {   // not used, not lost, not printed => idcard is stocked
                  setError(R.string.login_message_1_stocked, R.string.login_message_2_please_return_idcard);
               }
            } else {
               User user = User.getNonNull(idcard.getUid());
               if (user.getRole() == Role.PUPIL) {
                  setError(R.string.login_message_1_idcard_from_pupil, R.string.login_message_2_idcard_from_pupil);
               } else {
                  loginTutorOrAdmin(user);
               }
            }
         } else {
            Label label = Label.getNullable(sn);
            ISBN isbn = ISBN.parse(barcode);
            if (label != null && label.isUsed() ||
                  isbn != null && Book.getIdentifiedByISBN(isbn.getValue()) != null) {
               setError(R.string.login_message_1_barcode_from_book, R.string.login_message_2_please_login_first);
            } else {
               setError(R.string.login_message_1_barcode_unknown, R.string.login_message_2_please_login_first);
            }
         }
      }
   }

   private void setError(int resId1, int resId2) {
      App.getSoundPlayer().play(R.raw.horn);
      stopAnimator.start();
      message1.setText(resId1);
      message2.setText(resId2);
   }

   private void loginTutorOrAdmin(User user) {
      scanAnimator.stop();
      message1.setText("");
      message2.setText("");

      Use.login(user);
      if (user.getRole() == Role.ADMIN) {
         adminOrTutorDialog.show();
      } else {                       // user.getRole() == Role.TUTOR
         playSoundAndStartActivity(TutorActivity.class);
      }
   }

   private void playSoundAndStartActivity(final Class<? extends Activity> activity) {
      App.getSoundPlayer().play(R.raw.lock);
      new DelayedTask(300) {
         @Override
         protected void executeDelayed(boolean cancelled) {
            startActivity(new Intent(LoginActivity.this, activity));
         }
      }.execute();
   }

}