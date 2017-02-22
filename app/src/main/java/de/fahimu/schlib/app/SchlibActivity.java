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
   }

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
   protected void onPostResume() {
      super.onPostResume();
      if (!userDeniedPermission) {
         requestPermission(permission.WRITE_EXTERNAL_STORAGE);
      }
   }

   /**
    * Called after the permission was granted.
    */

   void onPermissionGranted() {}

   private void requestPermission(String permission) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted();
         } else {
            showRationaleDialogAndRequestPermission(permission);
         }
      }
   }

   private static final int REQUEST_ID = 0x24011972;

   /**
    * Shows a NoFocusDialog that explains why we need this permission and then requests for it.
    */
   private void showRationaleDialogAndRequestPermission(final String permission) {
      NoFocusDialog dialog = new NoFocusDialog(this, NoFocusDialog.IGNORE_CANCEL);
      dialog.setTitle(R.string.schlib_rationale_dialog_title);
      dialog.setMessage(R.string.schlib_rationale_dialog_message);
      dialog.setPositiveButton(R.string.app_cont, new ButtonListener() {
         @Override
         public void onClick() {
            Log.d("requestPermissions");
            SchlibActivity.this.requestPermissions(new String[] { permission }, REQUEST_ID);
         }
      }).show();
   }

   @Override
   public void onRequestPermissionsResult(int reqId, @NonNull String[] permissions, @NonNull int[] grantResults) {
      if (reqId == REQUEST_ID) {
         if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            userDeniedPermission = true;
            showFatalDialogAndStop();
         }
      }
   }

   /**
    * Shows a NoFocusDialog that informs the user that the app must be stopped.
    */
   private void showFatalDialogAndStop() {
      NoFocusDialog dialog = new NoFocusDialog(this, NoFocusDialog.IGNORE_CANCEL);
      dialog.setTitle(R.string.schlib_fatal_dialog_title);
      dialog.setMessage(R.string.schlib_fatal_dialog_message).show();
   }

   @Override
   public final void onBackPressed() {
      if (isBackButtonEnabled()) {
         super.onBackPressed();
      }
   }

   protected boolean isBackButtonEnabled() { return true; }

   /* ============================================================================================================== */

   protected final void showErrorSnackbar(@StringRes int resId, Object... formatArgs) {
      showSnackbar(android.R.color.holo_red_dark, resId, formatArgs);
   }

   protected final void showInfoSnackbar(@StringRes int resId, Object... formatArgs) {
      showSnackbar(R.color.color_primary_dark, resId, formatArgs);
   }

   protected final void showUndoSnackbar(String undo, OnClickListener onUndoListener,
         @StringRes int resId, Object... formatArgs) {
      showSnackbar(undo, android.R.color.holo_red_light,
            onUndoListener, R.color.color_primary_dark, resId, formatArgs);
   }

}