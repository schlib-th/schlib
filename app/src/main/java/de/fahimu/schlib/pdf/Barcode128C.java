/*
 * Barcode128C.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.pdf;

import android.support.annotation.WorkerThread;

/**
 * A {@code Barcode128C} represents a <a href="https://en.wikipedia.org/wiki/Code_128">Code 128</a> with Code Set C.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
final class Barcode128C extends Element {

   private static final String[] CODES = {
         "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213",  // 00-09
         "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132",  // 10-19
         "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211",  // 20-29
         "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",  // 30-39
         "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331",  // 40-49
         "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111",  // 50-59
         "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",  // 60-69
         "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",  // 70-79
         "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141",  // 80-89
         "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141",  // 90-99
         "114131", "311141", "411131"                                                                       // 100-102
   };

   private static final String START = "211232";         // 105
   private static final String STOP  = "2331112" + "0";  // 106 + '0': make STOP.length() even to simplify algorithms

   private final String data;
   private final double width;
   private final double height;

   /**
    * Creates a new Barcode128C from the specified data with the specified width and height (in pt). The length of
    * {@code data} must be even, and Unicode characters outside {@code +U0030} to {@code +U0039} are not allowed.
    *
    * @param data
    *       the data string.
    * @param width
    *       the width of the barcode in pt.
    * @param height
    *       the height of the barcode in pt.
    */
   @WorkerThread
   Barcode128C(String data, double width, double height) {
      this.data = data;
      this.width = width;
      this.height = height;
   }

   @WorkerThread
   private String calculateCode() {
      int checksum = 105;          // value of START CODE C
      int len = data.length() / 2;
      StringBuilder code = new StringBuilder(6 * (1 + len + 1) + 8).append(START);

      for (int i = 0; i < len; i++) {
         int value = 10 * (data.charAt(2 * i) - '0') + (data.charAt(2 * i + 1) - '0');
         checksum += (i + 1) * value;
         code.append(CODES[value]);
      }
      return code.append(CODES[checksum % 103]).append(STOP).toString();
   }

   /**
    * Writes the drawing instructions to the specified PDF Document.
    *
    * @param document
    *       the PDF Document where the instructions should be written to.
    */
   @Override
   @WorkerThread
   Document write(Document document) {
      String code = calculateCode();
      // We know that the sum of the 6 numeric values of every code is 11, except STOP (sum is 13)
      final double sum = 11 * (1 + data.length() / 2 + 1) + 13;

      // q: Push the current graphics state on the graphics state stack (Section 4.3.3)
      // 0 g: Set non-stroking color space to DeviceGray and non-stroking gray level to 0 (Section 4.5.7)
      // w 0 0 h 0 0 cm: Modify CTM to [w 0 0 h 0 0], meaning scale to [w h] (Section 4.3.3 and 4.2.2)
      document.write("q 0 g %.8f 0 0 %.8f 0 0 cm\n", width, height);

      for (int x = 0, i = 0; i < code.length(); i += 2) {
         int s1 = code.charAt(i) - '0', s2 = code.charAt(i + 1) - '0';
         // x/sum 0 s1/sum 1 re: Append a rectangle to the current path as a complete subpath,
         // with lower-left corner (x/sum, 0) and dimensions (s1/sum, 1) (Section 4.4.1)
         document.write("%.8f 0 %.8f 1 re\n", x / sum, s1 / sum);
         x += (s1 + s2);
      }
      // f: Fills the insides of all rectangles with the non-stroking color (Section 4.4.2)
      // Q: Pop the graphics state stack (Section 4.3.3)
      return document.write("f Q\n");
   }

}