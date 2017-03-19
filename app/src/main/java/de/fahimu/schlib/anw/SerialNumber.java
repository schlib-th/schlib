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

   private final int value;

   /**
    * Allocates a new {@code SerialNumber} with the specified value.
    *
    * @param value
    *       the value of the new {@code SerialNumber} instance.
    * @throws IllegalArgumentException
    *       if {@code value} is not greater than zero.
    */
   public SerialNumber(int value) {
      if (value <= 0) {
         throw new IllegalArgumentException(value + " not greater than zero");
      }
      this.value = value;
   }

   /**
    * Returns the value of this {@code SerialNumber} as an {code int}.
    *
    * @return the value of this {@code SerialNumber} as an {code int}.
    */
   public int getValue() { return value; }

   /**
    * Returns a 22-digit decimal string, derived from the octal representation of this
    * {@code SerialNumber}'s encrypted value. This {@code String} object can efficiently be encoded in
    * <a href="http://en.wikipedia .org/wiki/Code_128#Subtypes"> Code Set C of Code 128</a>.
    *
    * @return a 22-digit decimal string.
    */
   @NonNull
   public String getCode128() {
      long cipher = IntCipher.encrypt(value);
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
    * or 0 if the argument is not the result string of a call to {@link #getCode128() getCode128}.
    *
    * @param s
    *       the string to parse.
    * @return the integer value represented by the specified string or 0.
    */
   public static int parseCode128(String s) {
      if (s.length() != 22) { return 0; }

      long cipher = 0;
      for (int p = 0; p < 22; p++) {
         int v = s.charAt(p) - ((p & 1) == 0 ? '2' : '0');
         if (v < 0 || v > 7) { return 0; }
         cipher = (cipher << 3) | v;
      }
      return IntCipher.decrypt(cipher);
   }

   /**
    * Returns this {@code SerialNumber}'s value as a decimal string with an appended check digit calculated by the
    * <a href="http://en.wikipedia.org/wiki/Damm_algorithm">Damm algorithm</a>.
    *
    * @return this {@code SerialNumber}'s value as a decimal string with an appended check digit.
    */
   @NonNull
   public String getDecimal() {
      String plain = Integer.toString(value);
      char digit = '0';
      for (int p = 0; p < plain.length(); p++) {
         digit = TASQG[digit - '0'].charAt(plain.charAt(p) - '0');
      }
      return plain + digit;
   }

   /** Totally anti-symmetric quasigroup from Damm's dissertation, page 106. */
   private static final String[] TASQG = {
         "0123456789", "2035648197", "5784290361", "6471932850", "7968501243", "8206315974", "1359874026", "9547023618",
         "4810769532", "3692187405"
   };

   @Override
   public String toString() {
      return Integer.toString(value);
   }

   @NonNull
   public String getDisplay() {
      return "#\u00a0" + getDecimal() + "\u00a0#";
   }

}