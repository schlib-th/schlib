/*
 * ExternalOutputStream.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.android.share;

import android.support.annotation.NonNull;


import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A {@link java.io.BufferedOutputStream BufferedOutputStream} that writes bytes to a
 * {@link ExternalFile ExternalFile}.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class ExternalOutputStream extends OutputStream {

   private final ExternalFile externalFile;
   private final OutputStream outputStream;

   private ExternalOutputStream(ExternalFile ef) throws FileNotFoundException {
      externalFile = ef;
      outputStream = new BufferedOutputStream(new FileOutputStream(ef.getFile()));
   }

   public static ExternalOutputStream newInstance(ExternalFile ef) {
      try {
         return new ExternalOutputStream(ef);
      } catch (FileNotFoundException e) {
         throw new RuntimeException("ExternalOutputStream:newInstance", e);
      }
   }

   @Override
   public void write(int i) {
      try { outputStream.write(i); } catch (IOException e) {
         throw new RuntimeException("ExternalOutputStream:writeInt", e);
      }
   }

   @Override
   public void write(@NonNull byte[] buffer, int offset, int count) {
      try { outputStream.write(buffer, offset, count); } catch (IOException e) {
         throw new RuntimeException("ExternalOutputStream:writeBuffer", e);
      }
   }

   @Override
   public void write(@NonNull byte[] buffer) { write(buffer, 0, buffer.length); }

   @Override
   public void flush() {
      try { outputStream.flush(); } catch (IOException e) {
         throw new RuntimeException("ExternalOutputStream:flush", e);
      }
   }

   @Override
   public void close() {
      try { outputStream.close(); } catch (IOException e) {
         throw new RuntimeException("ExternalOutputStream:close", e);
      }
      externalFile.scanFile();
   }

}