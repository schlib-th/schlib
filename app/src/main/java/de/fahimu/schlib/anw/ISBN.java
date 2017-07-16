/*
 * ISBN.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.anw;

/**
 * An {@code ISBN} is an <a href="http://en.wikipedia.org/wiki/International_Article_Number_%28EAN%29">EAN-13</a>
 * with either {@code 978} or {@code 979-1} to {@code 979-9} as its GS1 prefix.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public final class ISBN {

   public static final int LENGTH = 13;

   public static final long MIN = 9_780000_000000L;
   public static final long MAX = 9_799999_999999L;

   private final long value;

   private static boolean outOfRange(long value) {
      return value < MIN || value > MAX;
   }

   /**
    * Allocates a new {@code ISBN} with the specified value.
    *
    * @param value
    *       the value of the new {@code ISBN} instance.
    * @throws IllegalArgumentException
    *       if {@code value} is out of range.
    */
   public ISBN(long value) {
      if (outOfRange(value)) {
         throw new IllegalArgumentException(value + " out of range");
      }
      this.value = value;
   }

   /**
    * Returns the value of this {@code ISBN}.
    *
    * @return the value of this {@code ISBN}.
    */
   public long getValue() { return value; }

   /**
    * Returns a new {@code ISBN} with a value represented by the specified {@code String}
    * or {@code null} if the argument is not a valid ISBN.
    *
    * @param s
    *       the string to parse.
    * @return a new {@code ISBN} or {@code null}.
    */
   public static ISBN parse(String s) {
      // an ISBN must be an EAN-13
      if (s.length() != LENGTH) { return null; }

      // an ISBN must start with GS1 prefix 978 or 979-1 to 979-9
      String gs1Prefix = s.substring(0, 3);
      if (!(gs1Prefix.equals("978") || gs1Prefix.equals("979"))) { return null; }
      if (gs1Prefix.equals("979") && s.charAt(3) == '0') { return null; }

      // check each single character for being a digit and calculate the check sum; for more information see
      // http://en.wikipedia.org/wiki/International_Standard_Book_Number#ISBN-13_check_digit_calculation
      int checksum = 0;
      for (int i = 0; i < LENGTH; i++) {
         char c = s.charAt(i);
         if (c < '0' || c > '9') { return null; }
         checksum += (c - '0') * ((i & 1) == 0 ? 1 : 3);
      }
      return (checksum % 10 != 0) ? null : new ISBN(Long.parseLong(s));
   }

   public String getDisplay() {
      String isbn = toString();
      return isbn.substring(0, 1) + "-" + isbn.substring(1, 7) + "-" + isbn.substring(7);
   }

   @Override
   public int hashCode() {
      return Long.valueOf(value).hashCode();
   }

   @Override
   public boolean equals(Object other) {
      return (other instanceof ISBN) && ((ISBN) other).value == this.value;
   }

   @Override
   public String toString() {
      return Long.toString(value);
   }

}