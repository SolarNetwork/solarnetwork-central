/* ==================================================================
 * CsvUtils.java - 3/04/2022 4:12:59 PM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.datum.imp.standard;

import static java.lang.String.format;
import net.solarnetwork.util.IntRangeSet;

/**
 * CSV utilities.
 * 
 * @author matt
 * @version 1.0
 */
public final class CsvUtils {

	private CsvUtils() {
		// do not call
	}

	/**
	 * Parse a single 1-based column reference value, either a number or string.
	 * 
	 * @param ref
	 *        the column reference to parse
	 * @return the column number (1-based)
	 * @throws IllegalArgumentException
	 *         if {@code ref} cannot be parsed as either an integer or a
	 *         spreadsheet-style column name using characters {@literal A} -
	 *         {@literal Z}, e.g. {@literal A} for column 1, {@code AA} for
	 *         column 27, and so on
	 */
	public static int parseColumnReference(String ref) {
		int c = 0;
		try {
			c = Integer.parseInt(ref);
		} catch ( NumberFormatException e ) {
			char[] chars = ref.toCharArray();
			for ( int i = 0, len = chars.length; i < len; i++ ) {
				char l = Character.toUpperCase(chars[i]);
				if ( l < 'A' || l > 'Z' ) {
					throw new IllegalArgumentException(
							format("The character [%c] is not a valid column reference.", chars[i]));
				}
				c *= 26;
				c += (l - 'A' + 1);
			}
		}
		return c;
	}

	/**
	 * Parse a delimited columns reference into an {@link IntRangeSet} of
	 * 1-based column numbers.
	 * 
	 * <p>
	 * The {@code value} format is a comma-delimited list of numbers or
	 * spreadsheet-style column names using the characters {@literal A} -
	 * {@literal Z}. Ranges may be specified by using a dash delimiter. For
	 * example {@literal 4-6,10} would result in a set including 4, 5, 6, and
	 * 10; {@literal D-F,J} would result in the same. {@literal AA} would result
	 * in a singleton set of 27. Names and numbers can be freely mixed. For
	 * example {@literal 4-F,10} would result in the same set as the previous
	 * examples.
	 * </p>
	 * 
	 * @param value
	 *        a delimited list of column references
	 * @return the column numbers as a set, or {@literal null} if {@code value}
	 *         is {@literal null} or empty or has no valid references
	 */
	public static IntRangeSet parseColumnsReference(String value) {
		if ( value == null || value.trim().isEmpty() ) {
			return null;
		}
		IntRangeSet set = new IntRangeSet();
		String[] ranges = value.trim().split("\\s*,\\s*");
		for ( String range : ranges ) {
			String[] components = range.split("\\s*-\\s*", 2);
			try {
				int a = parseColumnReference(components[0]);
				int b = (components.length > 1 ? parseColumnReference(components[1]) : a);
				if ( a > 0 && b > 0 ) {
					set.addRange(a, b);
				}
			} catch ( IllegalArgumentException e ) {
				// ignore and continue
			}
		}
		return (set.isEmpty() ? null : set);
	}

}
