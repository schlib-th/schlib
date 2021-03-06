/*
 * SchlibActivity.java
 *
 * Copyright 2016 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.Manifest.permission;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;


import de.fahimu.android.app.Log;
import de.fahimu.android.app.scanner.NoFocusDialog;
import de.fahimu.android.app.scanner.NoFocusDialog.ButtonListener;
import de.fahimu.android.app.scanner.ScannerActivity;

/**
 * An activity that ensures, each time after the activity was resumed, that the
 * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE} permission is granted.
 */
abstract class SchlibActivity extends ScannerActivity {

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setDisplayHomeAsUpEnabled(isHomeShownAsUp());
   }

   /**
    * The default implementation returns {@code true}. May be overridden by subclasses.
    */
   boolean isHomeShownAsUp() { return true; }

   @Override
   protected final int getToolbarId() { return R.id.toolbar; }

   @Override
   protected final int getIconId() { return R.drawable.ic_launcher; }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.schlib, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case R.id.action_help:
         Log.d("action_help");    // TODO
         return true;
      case R.id.action_logout:
         startActivity(new Intent(this, LoginActivity.class));
         return true;
      default:
         return super.onOptionsItemSelected(item);
      }
   }

   /* ============================================================================================================== */

   /**
    * When presenting the system dialog to grant the permission, the activity will be paused.
    * After granting or denying the permission, the activity will be resumed again.
    * That's OK for granting, but if the user denies the permission, calling {@link #onResume()}
    * will result in an infinite permission check. To prevent this, we will flag this situation.
    */
   private boolean userDeniedPermission = false;

   @Override
   protected final void onPostResume() {
      super.onPostResume();
      if (!userDeniedPermission) {
         requestPermission(permission.WRITE_EXTERNAL_STORAGE);
      }
   }

   /**
    * Called after the permission was granted.
    */
   void onPermissionGranted() {}

   private void requestPermission(final String permission) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted();
         } else {
            NoFocusDialog dialog = new NoFocusDialog(this);
            dialog.setMessage(R.string.dialog_message_schlib_no_permission);
            dialog.setButton1(R.string.app_cont, new ButtonListener() {
               @Override
               public void onClick(int id) {
                  SchlibActivity.this.requestPermissions(new String[] { permission }, REQUEST_ID);
               }
            }).show();
         }
      }
   }

   private static final int REQUEST_ID = 0x21082007;

   @Override
   public final void onRequestPermissionsResult(int reqId, @NonNull String[] permissions, @NonNull int[] grantResults) {
      if (reqId == REQUEST_ID) {
         if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            userDeniedPermission = true;
            NoFocusDialog dialog = new NoFocusDialog(this);
            dialog.setMessage(R.string.dialog_message_schlib_permission_not_granted);
            dialog.setOnCancelListener(null).show();
         }
      }
   }

   /* ============================================================================================================== */

   final void showErrorSnackbar(@StringRes int resId, Object... formatArgs) {
      App.playSound(R.raw.horn);
      showSnackbar(android.R.color.holo_red_dark, resId, formatArgs);
   }

   final void showInfoSnackbar(@StringRes int resId, Object... formatArgs) {
      showSnackbar(R.color.color_primary_dark, resId, formatArgs);
   }

   final void showUndoSnackbar(String undo, OnClickListener onUndoListener, @StringRes int resId, Object... args) {
      showSnackbar(undo, android.R.color.holo_red_light, onUndoListener, R.color.color_primary_dark, resId, args);
   }

}