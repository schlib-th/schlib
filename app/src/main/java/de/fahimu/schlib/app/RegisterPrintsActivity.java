/*
 * RegisterPrintsActivity.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.fahimu.android.app.DelayedTask;
import de.fahimu.android.app.ListView.Adapter;
import de.fahimu.android.app.ListView.Item;
import de.fahimu.android.app.ListView.ViewHolder;
import de.fahimu.android.app.Log;
import de.fahimu.android.app.SmartAnimator;
import de.fahimu.android.app.TaskRegistry;
import de.fahimu.android.db.Row;
import de.fahimu.android.share.ExternalFile;
import de.fahimu.schlib.anw.SerialNumber;
import de.fahimu.schlib.db.Idcard;
import de.fahimu.schlib.db.Label;
import de.fahimu.schlib.db.Preference;
import de.fahimu.schlib.db.Serial;
import de.fahimu.schlib.pdf.Document;
import de.fahimu.schlib.pdf.Document.WriterListener;
import de.fahimu.schlib.pdf.Idcards85x54;
import de.fahimu.schlib.pdf.Labels70x36;
import de.fahimu.schlib.share.FileType;

/**
 * An activity for registering printed labels or idcards.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class RegisterPrintsActivity extends SchlibActivity {

   final class Page extends Row {
      @StringRes
      final int pdfTitleId;
      final int group, number;

      Page(@StringRes int pdfTitleId, int group, int number) {
         this.pdfTitleId = pdfTitleId; this.group = group; this.number = number;
      }

      @Override
      protected String getTable() {
         throw new UnsupportedOperationException("read only");
      }

      @Override
      public long getOid() {
         return (group << 16) + number;
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   final class PageItem extends Item<Page> {
      final String text;

      @WorkerThread
      PageItem(@NonNull Page page) {
         super(page);
         text = App.format("%s - %s %d", App.getStr(page.pdfTitleId), App.getStr(R.string.pdf_page), page.number);
      }
   }

   final class PageViewHolder extends ViewHolder<PageItem> {
      private final TextView text;

      PageViewHolder(LayoutInflater inflater, ViewGroup parent) {
         super(inflater, parent, R.layout.register_prints_row);
         text = App.findView(itemView, TextView.class, R.id.register_prints_row_text);
      }

      protected void bind(PageItem item) { text.setText(item.text); }
   }

   final class PagesAdapter extends Adapter<Page,PageItem,PageViewHolder> {

      PagesAdapter() {
         super(RegisterPrintsActivity.this, R.id.register_prints_list, R.string.register_prints_empty);
      }

      @Override
      protected PageViewHolder createViewHolder(LayoutInflater inflater, ViewGroup parent) {
         return new PageViewHolder(inflater, parent);
      }

      @Override
      protected ArrayList<Page> loadData() {
         ArrayList<Page> data = new ArrayList<>(32);
         addPageNumbers(data, 0, Label.getPageNumbers(), R.string.pdf_labels_title);
         addPageNumbers(data, 1, Idcard.getPageNumbers(), R.string.pdf_idcards_title);
         return data;
      }

      private void addPageNumbers(ArrayList<Page> data, int group, List<Integer> numbers, int pdfTitleId) {
         for (int number : numbers) {
            data.add(new Page(pdfTitleId, group, number));
         }
      }

      @Override
      protected PageItem createItem(Page page) { return new PageItem(page); }

      @Override
      protected void onUpdated(int flags, List<PageItem> data) {
         if (data.isEmpty()) {
            writeAgain.setEnabled(false);
            new ExternalFile(FileType.PRINTS, null).delete();
            if (firstRun == 2) {
               // Continue automatically with FirstRun3Activity after 2.5 seconds
               new DelayedTask(2500, taskRegistry) {
                  @Override
                  protected void executeDelayed(boolean cancelled) {
                     Preference.getNonNull(Preference.FIRST_RUN).setValue("3").update();
                     startActivity(new Intent(RegisterPrintsActivity.this, FirstRun3Activity.class));
                     RegisterPrintsActivity.this.finish();
                  }
               }.execute();
            }
         }
      }
   }

   /* ============================================================================================================== */

   private SmartAnimator createFadeInExplanation() {
      return new SmartAnimator("fadeIn explanation", explanation, -1) {
         @Override
         protected void create() {
            getAnimator().play(alphaAnimator(explanation, new LinearInterpolator(), 0, 350, 0, 1));
         }

         @Override
         protected void onAnimationEnd() { explanation.setAlpha(1); }
      };
   }

   private SmartAnimator createFadeOutExplanation() {
      return new SmartAnimator("fadeOut explanation", explanation, -1) {
         @Override
         protected void create() {
            getAnimator().play(alphaAnimator(explanation, new LinearInterpolator(), 0, 250, 1, 0));
         }

         @Override
         protected void onAnimationEnd() { explanation.setAlpha(0); }
      };
   }

   /* ============================================================================================================== */

   private int firstRun;

   private TextView     message;
   private Button       writeAgain;
   private TextView     explanation;
   private PagesAdapter pagesAdapter;

   private SmartAnimator fadeInExplanation, fadeOutExplanation;

   private final TaskRegistry taskRegistry = new TaskRegistry();

   @Override
   protected int getContentViewId() { return R.layout.register_prints; }

   @Override
   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      Preference firstRun = Preference.getNullable(Preference.FIRST_RUN);
      this.firstRun = (firstRun == null) ? 0 : Integer.parseInt(firstRun.getValue());
      Log.d("firstRun=" + this.firstRun);
      setDisplayHomeAsUpEnabled(this.firstRun != 2);

      message = findView(TextView.class, R.id.register_prints_message);
      writeAgain = findView(Button.class, R.id.register_prints_write_again);
      explanation = findView(TextView.class, R.id.register_prints_explanation);
      pagesAdapter = new PagesAdapter();

      fadeInExplanation = createFadeInExplanation();
      fadeOutExplanation = createFadeOutExplanation();
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   protected void onResume() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onResume();
         writeDocumentsPrepare();
      }
   }

   @Override
   protected void onPermissionGranted() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         writeDocumentsAsync();
         pagesAdapter.updateAsync(Adapter.RELOAD_DATA);
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      // If firstRun > 0, we don't want the help or logout menu items, but the home button.
      // So don't call super.onCreateOptionsMenu(menu), but return true to build the menu.
      return firstRun > 0 || super.onCreateOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      if (firstRun > 0) {
         // The only option item is the home button
         onBackPressed();
         return true;
      } else {
         return super.onOptionsItemSelected(item);
      }
   }

   @Override
   protected void onPause() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         super.onPause();
         taskRegistry.cancel();
      }
   }

   @Override
   public void onBackPressed() {
      if (firstRun != 2) {
         super.onBackPressed();     // ignore back button on first run
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   public void onWriteAgainClicked(View view) {
      fadeInExplanation.cancel(); fadeOutExplanation.start();
      writeDocumentsPrepare(); writeDocumentsAsync();
   }

   private final AtomicInteger pagesToWrite = new AtomicInteger();

   private void writeDocumentsPrepare() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         pagesToWrite.set(Label.getPageNumbers().size() + Idcard.getPageNumbers().size());
         writeAgain.setEnabled(false);
         if (pagesToWrite.get() == 0) {
            message.setText(App.getStr(R.string.register_prints_empty));
         } else {
            message.setText(App.getStr(R.string.register_prints_writing_pdf, pagesToWrite.get()));
         }
      }
   }

   private void writeDocumentsAsync() {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         if (pagesToWrite.get() > 0) {
            Document.writeAsync(taskRegistry, new WriterListener() {
               private long lastUpdateTime = 0;

               @Override
               public void onPageWrite() {
                  int left = pagesToWrite.decrementAndGet();
                  if (left >= 2 && SystemClock.uptimeMillis() > lastUpdateTime + 500) {
                     lastUpdateTime = SystemClock.uptimeMillis();
                     message.setText(App.getStr(R.string.register_prints_writing_pdf, left));
                  }
               }

               @Override
               public void onPostWrite() {
                  message.setText(App.getStr(R.string.register_prints_writing_done));
                  writeAgain.setEnabled(true);
                  fadeOutExplanation.cancel(); fadeInExplanation.start();
               }
            }, new Labels70x36(), new Idcards85x54());
         }
      }
   }

   /* -------------------------------------------------------------------------------------------------------------- */

   @Override
   public void onBarcode(String barcode) {
      try (@SuppressWarnings ("unused") Log.Scope scope = Log.e()) {
         int number = SerialNumber.parseCode128(barcode);
         if (!processSerial(Label.getNullable(number)) && !processSerial(Idcard.getNullable(number))) {
            showErrorSnackbar(R.string.register_prints_error_invalid);
         }
      }
   }

   private boolean processSerial(@Nullable Serial serial) {
      if (serial == null) {
         return false;
      } else if (serial.isPrinted()) {
         Serial.setStocked(serial);
         showInfoSnackbar(R.string.register_prints_ok);
         pagesAdapter.updateAsync(Adapter.RELOAD_DATA);
         return true;
      } else {
         if (serial.isLost()) {
            serial.setLost(false).update();     // Surprise! The serial isn't lost, set this serial to 'Stocked'.
         }
         showErrorSnackbar(R.string.register_prints_error_not_printed);
         return true;
      }
   }

}