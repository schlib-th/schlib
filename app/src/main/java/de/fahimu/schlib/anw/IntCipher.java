/*
 * IntCipher.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.anw;

import android.support.annotation.NonNull;


import java.util.Random;

import de.fahimu.schlib.app.App;
import de.fahimu.schlib.db.Preference;

/**
 * A utility class providing a method to {@link #encrypt(int) encrypt} an int into a long, and an inverse method to
 * {@link #decrypt(long) decrypt} this long into the original int value.
 * <p>
 * For encryption, a <a href="http://en.wikipedia.org/wiki/Feistel_cipher">Feistel cipher</a> will be used,
 * with some concepts from <a href="http://en.wikipedia.org/wiki/Blowfish_(cipher)">Blowfish</a>.
 * </p>
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class IntCipher {

   private IntCipher() { /* prevent instantiation */ }

   /**
    * Creates a 1024 bit random number and returns this number as a 256 character hexadecimal string.
    *
    * @return a 1024 bit random number as a 256 character hexadecimal string.
    */
   @NonNull
   public static String createKey() {
      byte[] key = new byte[1024 / 8];
      new Random().nextBytes(key);
      StringBuilder b = new StringBuilder(256);
      for (byte k : key) { b.append(App.format("%02x", k & 0xff)); }
      return b.toString();
   }

   /** The key for encryption and decryption. */
   private static int[] KEY;

   /** The S-boxes. */
   private static int[][] S;

   private static int parseHexChar(char c) {
      if (c >= '0' && c <= '9') { return c - '0'; }
      if (c >= 'a' && c <= 'f') { return c - 'a' + 10; }
      throw new IllegalArgumentException("c=" + c);
   }

   /**
    * Initializes the KEY and the S-boxes.
    */
   private synchronized static void initKeyAndSBoxes() {
      if (KEY == null) {
         String key = Preference.getNonNull(Preference.CIPHER_KEY).getValue();
         KEY = new int[1024 / 32];
         for (int p = 0, i = 0; i < KEY.length; i++) {
            for (int j = 0; j < 8; j++, p++) {
               KEY[i] = (KEY[i] << 4) | parseHexChar(key.charAt(p));
            }
         }
         S = new int[4][256];
         for (int k = 7, i = 0; i < 4; i++) {
            for (int j = 0; j < 256; j++) {
               S[i][j] = (j + k) ^ KEY[(k *= 13) & 31];
            }
         }
      }
   }

   /**
    * Returns a long with the encrypted value of the specified integer.
    *
    * @param plain
    *       the integer value to encrypt. Must be greater than 0.
    * @return a long with the encrypted value.
    *
    * @throws IllegalArgumentException
    *       if plain is not positive.
    */
   static long encrypt(int plain) {
      if (plain <= 0) {
         throw new IllegalArgumentException("plain must be positive");
      }
      initKeyAndSBoxes();
      int i = 0, l = plain, r = -plain;
      while (i < 32) {
         r ^= KEY[i++];
         l ^= f(r);
         l ^= KEY[i++];
         r ^= f(l);
      }
      return ((long) l << 32) | (r & 0xffffffffL);
   }

   /**
    * Returns an integer with the decrypted value of the specified long
    * or 0 if the specified value is not the result of the {@link #encrypt(int) encrypt} method.
    *
    * @param cipher
    *       the long value to decrypt.
    * @return an integer with the decrypted value or 0.
    */
   static int decrypt(long cipher) {
      initKeyAndSBoxes();
      int i = 32, l = (int) (cipher >>> 32), r = (int) cipher;
      while (i > 0) {
         r ^= f(l);
         l ^= KEY[--i];
         l ^= f(r);
         r ^= KEY[--i];
      }
      return (l != -r || l <= 0) ? 0 : l;
   }

   /** The Feistel round function. */
   private static int f(int x) {
      return ((S[0][x >>> 24] + S[1][x >>> 16 & 0xff]) ^ (S[2][x >>> 8 & 0xff])) + S[3][x & 0xff];
   }

}