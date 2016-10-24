/*
 * ExternalInputStream.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.share;

import android.support.annotation.NonNull;


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link java.io.BufferedInputStream BufferedInputStream} that reads bytes from a
 * {@link ExternalFile ExternalFile}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class ExternalInputStream extends InputStream {

   private final ExternalFile externalFile;
   private final InputStream  inputStream;

   private ExternalInputStream(ExternalFile ef) throws FileNotFoundException {
      externalFile = ef;
      inputStream = new BufferedInputStream(new FileInputStream(ef.getFile()));
   }

   public static ExternalInputStream newInstance(ExternalFile ef) {
      try {
         return new ExternalInputStream(ef);
      } catch (FileNotFoundException e) {
         throw new RuntimeException("ExternalInputStream:newInstance", e);
      }
   }

   @Override
   public int read() {
      try { return inputStream.read(); } catch (IOException e) {
         throw new RuntimeException("ExternalInputStream:readInt", e);
      }
   }

   @Override
   public int read(@NonNull byte[] buffer, int byteOffset, int byteCount) {
      try { return inputStream.read(buffer, byteOffset, byteCount); } catch (IOException e) {
         throw new RuntimeException("ExternalInputStream:readBuffer", e);
      }
   }

   @Override
   public int read(@NonNull byte[] buffer) { return read(buffer, 0, buffer.length); }

   @Override
   public void close() {
      try { inputStream.close(); } catch (IOException e) {
         throw new RuntimeException("ExternalInputStream:close", e);
      }
      externalFile.scanFile();
   }

}