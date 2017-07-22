/*
 * AdminUsersAddActivity.java
 *
 * Copyright 2017 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.os.AsyncTask;
import android.support.annotation.NonNull;


import java.util.List;

import de.fahimu.android.app.Log;
import de.fahimu.android.app.TaskRegistry;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.User;
import de.fahimu.schlib.db.User.Role;
import de.fahimu.schlib.pdf.Document;
import de.fahimu.schlib.pdf.Document.WriterListener;
import de.fahimu.schlib.pdf.PupilList;

/**
 * An activity for adding users to the database.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.04.2017
 * @since SchoolLibrary 1.0
 */
public final class AdminUsersAddActivity extends StepperActivity {

   Role         role    = null;
   String       name1   = "";
   String       name2   = "";
   int          count   = 0;
   List<Idcard> scanned = null;

   int getRemaining() {
      return count - scanned.size();
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   protected int getContentViewId() {
      return R.layout.admin_users_add;
   }

   @NonNull
   @Override
   StepFragment createFirstFragment() {
      return new AdminUsersAddStep0().setActivity(this);
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   void finishActivity() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
               try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
                  if (role != Role.PUPIL) {
                     User.insertAdminOrTutor(role, name2, name1, scanned.get(0));
                     finish();
                  } else {
                     User.insertPupils(name2, name1, scanned);
                     Document.writeAsync(new TaskRegistry(), new WriterListener() {
                        @Override
                        public void onPageWrite() { }

                        @Override
                        public void onPostWrite() { finish(); }
                     }, new PupilList(name2, name1, App.localDate()));
                  }
               }
            }
         });
      }
   }

}