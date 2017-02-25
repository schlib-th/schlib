/*
 * StringType.java
 *
 * Copyright 2014 by Thomas Hirsch, schlib@fahimu.de
 */

package de.fahimu.schlib.anw;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.fahimu.schlib.app.App;

/**
 * An enumeration of types for strings, restricted by regular expressions.
 *
 * @author Thomas Hirsch, schlib@fahimu.de
 * @version 1.0, 01.09.2014
 * @since SchoolLibrary 1.0
 */
public enum StringType {

   NAME1("name1", false, Regex.NAME1),
   NAME2("name2", false, Regex.NAME2),
   CLASS("class", false, Regex.CLASS),

   SHELF("shelf", false, Regex.SHELF),
   NUMBER("number", false, Regex.NUMBER),
   TITLE("title", false, Regex.TITLE),
   PUBLISHER("publisher", true, Regex.PUBLISHER),
   AUTHOR("author", true, Regex.AUTHOR),
   KEYWORDS("keywords", true, Regex.KEYWORDS),
   STOCKED("stocked", false, Regex.DATE);

   private static final class Regex {
      /* letter upper case */
      private static final String UPP = "[A-Z\\xC0-\\xD6\\xD8-\\xDE]";
      /* letter lower case */
      private static final String LOW = "[a-z\\xDF-\\xF6\\xF8-\\xFF]";
      /* apostrophe */
      private static final String APO = "('[ns]?)";
      /* word separators */
      private static final String SEP = "([:,-]? | [&-] )";

      /* an abbreviation terminated by a dot */
      private static final String ABB   = App.format("(%s(%s){0,3}\\.)", UPP, LOW);
      /* a first name component */
      private static final String N1C   = App.format("(%s|%s%s{1,})", ABB, UPP, LOW);
      /* a first name */
      private static final String NAME1 = App.format("(%s([ -]%s)*)", N1C, N1C);
      /* a class name */
      private static final String CLASS = App.format("(%s|%s|[0-9]){1,}", UPP, LOW);
      /* a last name component */
      private static final String N2C   = App.format("((Mac|Mc|[DO]'|Di)?%s%s{1,}%s?)", UPP, LOW, APO);
      /* a last name */
      private static final String NAME2 = App.format("(((v[ao]n( der?)?|de) )?%s(-%s)*)", N2C, N2C);
      /* a name */
      private static final String NAME  = App.format("((%s )*%s)", NAME1, NAME2);

      /* a number */
      private static final String NUM = "(([0-9]{1,}[-/:,][0-9]{1,})|[0-9]{1,}(\\.)?)";
      /* an abbreviation consisting of two or three capital letters */
      private static final String CAP = App.format("(%s{2,4}|\\?\\?\\?)", UPP);
      /* an upper case word component */
      private static final String WUC = App.format("(%s|%s|%s|%s|%s)", UPP, NAME1, NAME2, NUM, CAP);
      /* an upper case word */
      private static final String WUP = App.format("(%s(-%s)*)", WUC, WUC);
      /* a word starting lower case */
      private static final String WLO = App.format("(%s{1,}%s?)", LOW, APO);

      /* a word component */
      private static final String WDC      = App.format("(%s|%s)", WUP, WLO);
      /* a word */
      private static final String WORD     = App.format("(\"?%s(-%s)*\"?)", WDC, WDC);
      /* a series of words */
      private static final String WORDS    = App.format("(%s(%s%s)*)", WORD, SEP, WORD);
      /* a sentence */
      private static final String SENTENCE = App.format("%s( \\(%s\\)|%s%s)*[\\.?!]?", WUP, WORDS, SEP, WORDS);

      /* a date formatted DD.MM.YYYY */
      private static final String DATE = "[0-9]{2}.[0-9]{2}.2[0-9]{3}";

      private static final String SHELF     = App.format("%s([ \\-]?(%s|%s))*", UPP, UPP, LOW);
      private static final String NUMBER    = "[0-9]{1,3}";
      private static final String TITLE     = App.format("%s( (%s|\\(%s\\)))*", SENTENCE, SENTENCE, SENTENCE);
      private static final String PUBLISHER = App.format("%s{3}|(%s(( | & | und )%s)*)", LOW, WUP, WUP);
      private static final String AUTHOR    = App.format("%s((, | & | und )%s)*", NAME, NAME);
      private static final String KEYWORDS  = App.format("%s( %s)*", WUP, WUP);
   }

   private final transient String  name;
   private final transient boolean empty;
   private final transient Pattern pattern;

   StringType(String name, boolean empty, String regex) {
      this.name = name;
      this.empty = empty;
      this.pattern = Pattern.compile(regex);
   }

   public String getName() { return name; }

   public int matches(String s) {
      int failPosition;
      if (s.isEmpty()) {
         failPosition = empty ? -1 : 0;
      } else {
         Matcher m = pattern.matcher(s);
         if (m.matches()) {
            failPosition = -1;
         } else {
            for (failPosition = s.length(); failPosition >= 0; failPosition--) {
               m = pattern.matcher(s.substring(0, failPosition));
               if (m.matches() || m.hitEnd()) { break; }
            }
         }
      }
      return failPosition;
   }

}