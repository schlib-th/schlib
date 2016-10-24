/*
 * ExternalFile.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.share;

import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.annotation.StringRes;


import java.io.File;
import java.io.FilenameFilter;

import de.fahimu.android.app.App;
import de.fahimu.android.app.Log;

/**
 * An {@code ExternalFile} represents a file in the external storage of an Android device.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class ExternalFile {

   public interface Type {
      @StringRes
      int getResId();
   }

   private final File absolute, relative;

   /**
    * Creates a new {@code ExternalFile} with the specified {@code type} and {@code fileName}.
    *
    * @param type
    *       the {@link ExternalFile.Type type} of the {@code ExternalFile}.
    * @param fileName
    *       the name of the {@code ExternalFile}.
    */
   public ExternalFile(Type type, String fileName) {
      String typeName = App.getStr(type.getResId());
      File absolute = new File(getRoot(), typeName);
      File relative = new File(getRoot().getName(), typeName);

      if (!(absolute.isDirectory() || absolute.mkdirs())) {
         throw new RuntimeException("cannot create directory " + absolute);
      }
      this.absolute = (fileName == null) ? absolute : new File(absolute, fileName);
      this.relative = (fileName == null) ? relative : new File(relative, fileName);
   }

   private static File getRoot() {
      return new File(Environment.getExternalStorageDirectory(), App.getInstance().getName());
   }

   File getFile() { return absolute; }

   public String getName() { return relative.getPath(); }

   /* ============================================================================================================== */

   /**
    * Deletes this file.
    */
   public void delete() { delete(absolute); }

   /**
    * Deletes the root file.
    */
   public static void deleteRoot() { delete(getRoot()); }

   private static void delete(File file) {
      if (file.isDirectory()) {
         // first delete all files in this directory
         File[] files = file.listFiles();
         for (File f : files) { delete(f); }
      }
      if (file.delete()) {
         Log.d("deleted " + file);
         scanFile(file);
      }
   }

   /* ============================================================================================================== */

   void scanFile() { scanFile(absolute); }

   private static void scanFile(File file) {
      String path = file.getAbsolutePath();
      Log.d("scanning " + path);
      MediaScannerConnection.scanFile(App.getInstance(), new String[] { path }, null, null);
   }

   /* ============================================================================================================== */

   /**
    * Returns the names of the files in this directory.
    *
    * @param extension
    *       if not {@code null}, restricts the file list to files with the specified file extension.
    * @return the names of the files in this directory.
    */
   public String[] listNames(String extension) {
      final String ext = (extension == null) ? "" : "." + extension.toUpperCase();
      return absolute.list(new FilenameFilter() {
         @Override
         public boolean accept(File dir, String filename) {
            return filename.toUpperCase().endsWith(ext);
         }
      });
   }

}