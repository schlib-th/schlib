/*
 * SerialNumber.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.anw;

import android.support.annotation.NonNull;

/**
 * A {@code SerialNumber} uniquely identifies an {@link de.fahimu.schlib.db.Idcard Idcard} or a
 * {@link de.fahimu.schlib.db.Label Label}. It has two external string representations. The first one is
 * a decimal representation with an appended error checking digit, the second one is a representation for
 * generating a <a href="http://en.wikipedia.org/wiki/Code_128">Code 128</a> barcode.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class SerialNumber {

   /**
    * Returns the value encoded as a 22-digit decimal string.
    * First the value is {@link IntCipher#encrypt(int) encrypted} and then converted into a
    * modified octal representation. This {@code String} object can efficiently be encoded in
    * <a href="http://en.wikipedia .org/wiki/Code_128#Subtypes">Code Set C of Code 128</a>.
    *
    * @param serial
    *       the serial to encode.
    * @return the value encoded as a 22-digit decimal string.
    */
   @NonNull
   public static String getCode128(int serial) {
      long cipher = IntCipher.encrypt(serial);
      char[] buf = new char[22];
      for (int p = 22; --p >= 0; ) {
         char base = (p & 1) == 0 ? '2' : '0';
         buf[p] = (char) (base + (cipher & 7));
         cipher >>>= 3;
      }
      return new String(buf);
   }

   /**
    * Returns the integer value of the specified string
    * or 0 if {@code code128} is not the result string of a call to {@link #getCode128(int) getCode128}.
    *
    * @param code128
    *       the string to parse.
    * @return the integer value of the specified string or 0.
    */
   public static int parseCode128(String code128) {
      if (code128.length() != 22) { return 0; }

      long cipher = 0;
      for (int p = 0; p < 22; p++) {
         int v = code128.charAt(p) - ((p & 1) == 0 ? '2' : '0');
         if (v < 0 || v > 7) { return 0; }
         cipher = (cipher << 3) | v;
      }
      return IntCipher.decrypt(cipher);
   }

   /**
    * Returns the specified value as a decimal string with an appended check digit calculated by the
    * <a href="http://en.wikipedia.org/wiki/Damm_algorithm">Damm algorithm</a>.
    *
    * @param serial
    *       the serial to convert.
    * @return the specified value as a decimal string with an appended check digit.
    */
   @NonNull
   public static String getDecimal(int serial) {
      String plain = Integer.toString(serial);
      char digit = '0';
      for (int p = 0; p < plain.length(); p++) {
         digit = TASQG[digit - '0'].charAt(plain.charAt(p) - '0');
      }
      return plain + digit;
   }

   /** Totally anti-symmetric quasigroup from Damm's dissertation, page 106. */
   private static final String[] TASQG = {
         "0123456789", "2035648197", "5784290361", "6471932850", "7968501243",
         "8206315974", "1359874026", "9547023618", "4810769532", "3692187405"
   };

   @NonNull
   public static String getDisplay(int serial) {
      return "#\u00a0" + getDecimal(serial) + "\u00a0#";
   }

}