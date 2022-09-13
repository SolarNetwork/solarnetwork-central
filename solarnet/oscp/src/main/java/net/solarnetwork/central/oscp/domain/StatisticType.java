/* ==================================================================
 * StatisticType.java - 7/09/2022 10:11:26 am
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

package net.solarnetwork.central.oscp.domain;

import static java.lang.String.format;
import net.solarnetwork.domain.CodedValue;

/**
 * A statistic type.
 * 
 * @author matt
 * @version 1.0
 */
public enum StatisticType implements CodedValue {

	/** The average of the property values seen within the aggregate period. */
	Average('a'),

	/**
	 * The count of property values that participated in the aggregate value.
	 */
	Count('c'),

	/** The minimum property value seen within the aggregate period. */
	Minimum('m'),

	/** The maximum property value seen within the aggregate period. */
	Maximum('M'),

	/** The first property value seen within the aggregate period. */
	Start('s'),

	/** The last property value seen within the aggregate period. */
	End('e'),

	/**
	 * The difference between the end and start property values within the
	 * aggregate period.
	 */
	Difference('d'),

	;

	private final char code;

	private StatisticType(char code) {
		this.code = code;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Create an enum instance from a code value.
	 * 
	 * @param code
	 *        the code value
	 * @return the enum instance
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid enum value
	 */
	public static StatisticType forCode(int code) {
		for ( StatisticType e : StatisticType.values() ) {
			if ( code == e.code ) {
				return e;
			}
		}
		throw new IllegalArgumentException(format("Invalid StatisticType code [%s]", code));
	}

}
