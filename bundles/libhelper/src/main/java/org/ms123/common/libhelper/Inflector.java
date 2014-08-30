/**
 * This file is part of SIMPL4(http://simpl4.org).
 *
 * 	Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
 *
 * SIMPL4 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SIMPL4 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SIMPL4.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ms123.common.libhelper;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Transforms words to singular, plural, humanized (human readable), underscore, camel case, or ordinal form. This is inspired by
 */
public class Inflector {
	protected static final Inflector INSTANCE = new Inflector();
	public static final Inflector getInstance() {
		return INSTANCE;
	}
	private Inflector() {}

	public String pluralize(Object word) {
		if (word == null) {
			return null;
		}
		String wordStr = word.toString().trim();
		if (wordStr.length() == 0) {
			return wordStr;
		}
		if (!wordStr.endsWith("_list")) {
			return wordStr + "_list";
		}
		return wordStr;
	}

	public String singularize(Object word) {
		if (word == null) {
			return "";
		}
		String wordStr = word.toString().trim();
		if (wordStr.length() == 0) {
			return wordStr;
		}
		if (wordStr.endsWith("_list")) {
			return wordStr.substring(0, wordStr.length() - 5);
		}
		return wordStr;
	}

	public String  getClassName(Object word) {
		return capitalize(singularize(word));
	}

	public String  getEntityName(Object word) {
		return singularize(word).toLowerCase();
	}
	public String  getModuleName(Object word) {
		return singularize(word).toLowerCase();
	}

	/**
	 * Converts strings to lowerCamelCase. This method will also use any extra delimiter characters to identify word boundaries.
	 * 
	 * Examples:
	 * 
	 * <pre>
	 *   inflector.lowerCamelCase(&quot;active_record&quot;)       #=&gt; &quot;activeRecord&quot;
	 *   inflector.lowerCamelCase(&quot;first_name&quot;)          #=&gt; &quot;firstName&quot;
	 *   inflector.lowerCamelCase(&quot;name&quot;)                #=&gt; &quot;name&quot;
	 *   inflector.lowerCamelCase(&quot;the-first_name&quot;,'-')  #=&gt; &quot;theFirstName&quot;
	 * </pre>
	 * 
	 * 
	 * 
	 * @param lowerCaseAndUnderscoredWord the word that is to be converted to camel case
	 * @param delimiterChars optional characters that are used to delimit word boundaries
	 * @return the lower camel case version of the word
	 * @see #underscore(String, char[])
	 * @see #camelCase(String, boolean, char[])
	 * @see #upperCamelCase(String, char[])
	 */
	public String lowerCamelCase(String lowerCaseAndUnderscoredWord, char... delimiterChars) {
		return camelCase(lowerCaseAndUnderscoredWord, false, delimiterChars);
	}

	/**
	 * Converts strings to UpperCamelCase. This method will also use any extra delimiter characters to identify word boundaries.
	 * 
	 * Examples:
	 * 
	 * <pre>
	 *   inflector.upperCamelCase(&quot;active_record&quot;)       #=&gt; &quot;SctiveRecord&quot;
	 *   inflector.upperCamelCase(&quot;first_name&quot;)          #=&gt; &quot;FirstName&quot;
	 *   inflector.upperCamelCase(&quot;name&quot;)                #=&gt; &quot;Name&quot;
	 *   inflector.lowerCamelCase(&quot;the-first_name&quot;,'-')  #=&gt; &quot;TheFirstName&quot;
	 * </pre>
	 * 
	 * 
	 * 
	 * @param lowerCaseAndUnderscoredWord the word that is to be converted to camel case
	 * @param delimiterChars optional characters that are used to delimit word boundaries
	 * @return the upper camel case version of the word
	 * @see #underscore(String, char[])
	 * @see #camelCase(String, boolean, char[])
	 * @see #lowerCamelCase(String, char[])
	 */
	public String upperCamelCase(String lowerCaseAndUnderscoredWord, char... delimiterChars) {
		return camelCase(lowerCaseAndUnderscoredWord, true, delimiterChars);
	}

	/**
	 * By default, this method converts strings to UpperCamelCase. If the <code>uppercaseFirstLetter</code> argument to false,
	 * then this method produces lowerCamelCase. This method will also use any extra delimiter characters to identify word
	 * boundaries.
	 * 
	 * Examples:
	 * 
	 * <pre>
	 *   inflector.camelCase(&quot;active_record&quot;,false)    #=&gt; &quot;activeRecord&quot;
	 *   inflector.camelCase(&quot;active_record&quot;,true)     #=&gt; &quot;ActiveRecord&quot;
	 *   inflector.camelCase(&quot;first_name&quot;,false)       #=&gt; &quot;firstName&quot;
	 *   inflector.camelCase(&quot;first_name&quot;,true)        #=&gt; &quot;FirstName&quot;
	 *   inflector.camelCase(&quot;name&quot;,false)             #=&gt; &quot;name&quot;
	 *   inflector.camelCase(&quot;name&quot;,true)              #=&gt; &quot;Name&quot;
	 * </pre>
	 * 
	 * 
	 * 
	 * @param lowerCaseAndUnderscoredWord the word that is to be converted to camel case
	 * @param uppercaseFirstLetter true if the first character is to be uppercased, or false if the first character is to be
	 *        lowercased
	 * @param delimiterChars optional characters that are used to delimit word boundaries
	 * @return the camel case version of the word
	 * @see #underscore(String, char[])
	 * @see #upperCamelCase(String, char[])
	 * @see #lowerCamelCase(String, char[])
	 */
	public String camelCase(String lowerCaseAndUnderscoredWord, boolean uppercaseFirstLetter, char... delimiterChars) {
		if (lowerCaseAndUnderscoredWord == null) {
			return null;
		}
		lowerCaseAndUnderscoredWord = lowerCaseAndUnderscoredWord.trim();
		if (lowerCaseAndUnderscoredWord.length() == 0) {
			return "";
		}
		if (uppercaseFirstLetter) {
			String result = lowerCaseAndUnderscoredWord;
			// Replace any extra delimiters with underscores (before the underscores are converted in the next step)...
			if (delimiterChars != null) {
				for (char delimiterChar : delimiterChars) {
					result = result.replace(delimiterChar, '_');
				}
			}

			// Change the case at the beginning at after each underscore ...
			return replaceAllWithUppercase(result, "(^|_)(.)", 2);
		}
		if (lowerCaseAndUnderscoredWord.length() < 2) {
			return lowerCaseAndUnderscoredWord;
		}
		return "" + Character.toLowerCase(lowerCaseAndUnderscoredWord.charAt(0)) + camelCase(lowerCaseAndUnderscoredWord, true, delimiterChars).substring(1);
	}

	/**
	 * Makes an underscored form from the expression in the string (the reverse of the {@link #camelCase(String, boolean, char[])
	 * camelCase} method. Also changes any characters that match the supplied delimiters into underscore.
	 * 
	 * Examples:
	 * 
	 * <pre>
	 *   inflector.underscore(&quot;activeRecord&quot;)     #=&gt; &quot;active_record&quot;
	 *   inflector.underscore(&quot;ActiveRecord&quot;)     #=&gt; &quot;active_record&quot;
	 *   inflector.underscore(&quot;firstName&quot;)        #=&gt; &quot;first_name&quot;
	 *   inflector.underscore(&quot;FirstName&quot;)        #=&gt; &quot;first_name&quot;
	 *   inflector.underscore(&quot;name&quot;)             #=&gt; &quot;name&quot;
	 *   inflector.underscore(&quot;The.firstName&quot;)    #=&gt; &quot;the_first_name&quot;
	 * </pre>
	 * 
	 * 
	 * 
	 * @param camelCaseWord the camel-cased word that is to be converted;
	 * @param delimiterChars optional characters that are used to delimit word boundaries (beyond capitalization)
	 * @return a lower-cased version of the input, with separate words delimited by the underscore character.
	 */
	public String underscore(String camelCaseWord, char... delimiterChars) {
		if (camelCaseWord == null) {
			return null;
		}
		String result = camelCaseWord.trim();
		if (result.length() == 0) {
			return "";
		}
		result = result.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2");
		result = result.replaceAll("([a-z\\d])([A-Z])", "$1_$2");
		result = result.replace('-', '_');
		if (delimiterChars != null) {
			for (char delimiterChar : delimiterChars) {
				result = result.replace(delimiterChar, '_');
			}
		}
		return result.toLowerCase();
	}

	/**
	 * Returns a copy of the input with the first character converted to uppercase and the remainder to lowercase.
	 * 
	 * @param words the word to be capitalized
	 * @return the string with the first character capitalized and the remaining characters lowercased
	 */
	public String capitalize(String words) {
		if (words == null) {
			return null;
		}
		String result = words.trim();
		if (result.length() == 0) {
			return "";
		}
		if (result.length() == 1) {
			return result.toUpperCase();
		}
		return "" + Character.toUpperCase(result.charAt(0)) + result.substring(1).toLowerCase();
	}

	public String capitalizeFirst(String words) {
		if (words == null) {
			return null;
		}
		String result = words.trim();
		if (result.length() == 0) {
			return "";
		}
		if (result.length() == 1) {
			return result.toUpperCase();
		}
		return "" + Character.toUpperCase(result.charAt(0)) + result.substring(1);
	}

	/**
	 * Capitalizes the first word and turns underscores into spaces and strips trailing "_id" and any supplied removable tokens.
	 * Like {@link #titleCase(String, String[])}, this is meant for creating pretty output.
	 * 
	 * Examples:
	 * 
	 * <pre>
	 *   inflector.humanize(&quot;employee_salary&quot;)       #=&gt; &quot;Employee salary&quot;
	 *   inflector.humanize(&quot;author_id&quot;)             #=&gt; &quot;Author&quot;
	 * </pre>
	 * 
	 * 
	 * 
	 * @param lowerCaseAndUnderscoredWords the input to be humanized
	 * @param removableTokens optional array of tokens that are to be removed
	 * @return the humanized string
	 * @see #titleCase(String, String[])
	 */
	public String humanize(String lowerCaseAndUnderscoredWords, String... removableTokens) {
		if (lowerCaseAndUnderscoredWords == null) {
			return null;
		}
		String result = lowerCaseAndUnderscoredWords.trim();
		if (result.length() == 0) {
			return "";
		}
		// Remove a trailing "_id" token
		result = result.replaceAll("_id$", "");
		// Remove all of the tokens that should be removed
		if (removableTokens != null) {
			for (String removableToken : removableTokens) {
				result = result.replaceAll(removableToken, "");
			}
		}
		result = result.replaceAll("_+", " "); // replace all adjacent underscores with a single space
		return capitalize(result);
	}

	/**
	 * Capitalizes all the words and replaces some characters in the string to create a nicer looking title. Underscores are
	 * changed to spaces, a trailing "_id" is removed, and any of the supplied tokens are removed. Like
	 * {@link #humanize(String, String[])}, this is meant for creating pretty output.
	 * 
	 * Examples:
	 * 
	 * <pre>
	 *   inflector.titleCase(&quot;man from the boondocks&quot;)       #=&gt; &quot;Man From The Boondocks&quot;
	 *   inflector.titleCase(&quot;x-men: the last stand&quot;)        #=&gt; &quot;X Men: The Last Stand&quot;
	 * </pre>
	 * 
	 * 
	 * 
	 * @param words the input to be turned into title case
	 * @param removableTokens optional array of tokens that are to be removed
	 * @return the title-case version of the supplied words
	 */
	public String titleCase(String words, String... removableTokens) {
		String result = humanize(words, removableTokens);
		result = replaceAllWithUppercase(result, "\\b([a-z])", 1); // change first char of each word to uppercase
		return result;
	}

	/**
	 * Utility method to replace all occurrences given by the specific backreference with its uppercased form, and remove all
	 * other backreferences.
	 * 
	 * The Java {@link Pattern regular expression processing} does not use the preprocessing directives <code>\l</code>,
	 * <code>&#92;u</code>, <code>\L</code>, and <code>\U</code>. If so, such directives could be used in the replacement string
	 * to uppercase or lowercase the backreferences. For example, <code>\L1</code> would lowercase the first backreference, and
	 * <code>&#92;u3</code> would uppercase the 3rd backreference.
	 * 
	 * 
	 * @param input
	 * @param regex
	 * @param groupNumberToUppercase
	 * @return the input string with the appropriate characters converted to upper-case
	 */
	protected static String replaceAllWithUppercase(String input, String regex, int groupNumberToUppercase) {
		Pattern underscoreAndDotPattern = Pattern.compile(regex);
		Matcher matcher = underscoreAndDotPattern.matcher(input);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(sb, matcher.group(groupNumberToUppercase).toUpperCase());
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
}
