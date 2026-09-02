/* ==================================================================
 * SolarEdgeMeasurementUnit.java - 15 Aug 2026 7:28:37 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.solaredge;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import net.solarnetwork.util.NumberUtils;

/**
 * SolarEdge measurement unit enumeration.
 *
 * @author matt
 * @version 1.0
 */
public enum SolarEdgeMeasurementUnit {

	NONE(0),

	W(0),

	KW(3),

	MW(6),

	WH(0),

	KWH(3),

	MWH(6),

	PERCENTAGE(0),

	PERCENTAGE_100(0),

	VOLT(0),

	AMPERE(0),

	HERTZ(0),

	;

	private final int scaleFactor;

	private SolarEdgeMeasurementUnit(int scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	/**
	 * Get the decimal scale factor to apply to normalize this unit into a base
	 * unit.
	 *
	 * @return the scale factor, as in the number of decimal places to shift
	 *         right
	 */
	public final int getScaleFactor() {
		return scaleFactor;
	}

	/**
	 * Get a scaled version of a number value.
	 *
	 * @param n
	 *        the number value to scale
	 * @return the scaled number
	 */
	public Number scaled(Number n) {
		if ( scaleFactor == 0 ) {
			return n;
		}
		return nonnull(NumberUtils.bigDecimalForNumber(n), "Decimal").movePointRight(scaleFactor);
	}

	/**
	 * Get an enum instance for a name.
	 *
	 * @param value
	 *        the enumeration name, case-insensitve
	 * @return the enum; if {@code value} is {@code null} or not supported then
	 *         {@link #NONE} is returned
	 */
	@JsonCreator
	public static SolarEdgeMeasurementUnit fromValue(@Nullable String value) {
		if ( value == null || value.isEmpty() ) {
			return NONE;
		}
		for ( SolarEdgeMeasurementUnit e : SolarEdgeMeasurementUnit.values() ) {
			if ( value.equalsIgnoreCase(e.name()) ) {
				return e;
			}
		}
		return NONE;
	}

}
