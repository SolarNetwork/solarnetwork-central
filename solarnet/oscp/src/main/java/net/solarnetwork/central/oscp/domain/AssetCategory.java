/* ==================================================================
 * AssetCategory.java - 14/08/2022 5:50:41 pm
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
import com.fasterxml.jackson.annotation.JsonCreator;
import net.solarnetwork.domain.CodedValue;

/**
 * An enumeration of category types for an asset.
 * 
 * @author matt
 * @version 1.0
 */
public enum AssetCategory implements CodedValue {

	/** A charging asset like an EV charger (possibly bi-directional). */
	Charging('v'),

	/** Consumption asset, neither {@code Charging} nor {@code Storage}. */
	Consumption('c'),

	/** Generation asset like PV. */
	Generation('g'),

	/** Storage asset, like a battery. */
	Storage('s'),

	;

	private final char code;

	private AssetCategory(char code) {
		this.code = code;
	}

	/**
	 * Get the number of seconds represented by this period.
	 * 
	 * @return the number of seconds
	 */
	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get an OSCP 2.0 value for this instance.
	 * 
	 * @return the OSCP 2.0 value
	 */
	public oscp.v20.AssetCategory toOscp20Value() {
		return switch (this) {
			case Charging -> oscp.v20.AssetCategory.CHARGING;
			case Consumption -> oscp.v20.AssetCategory.CONSUMPTION;
			case Generation -> oscp.v20.AssetCategory.CONSUMPTION;
			case Storage -> oscp.v20.AssetCategory.STORAGE;
		};
	}

	/**
	 * Get an instance for an OSCP 2.0 value.
	 * 
	 * @param category
	 *        the OSCP 2.0 value to get an instance for
	 * @return the instance
	 */
	public static AssetCategory forOscp20Value(oscp.v20.AssetCategory category) {
		return switch (category) {
			case CHARGING -> Charging;
			case CONSUMPTION -> Consumption;
			case GENERATION -> Generation;
			case STORAGE -> Storage;
		};
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
	public static AssetCategory forCode(int code) {
		for ( AssetCategory e : AssetCategory.values() ) {
			if ( code == e.code ) {
				return e;
			}
		}
		throw new IllegalArgumentException(format("Invalid AssetCategory code [%s]", code));
	}

	/**
	 * Create an enum instance from a string value.
	 * 
	 * @param value
	 *        the string representation; both enum names and code values are
	 *        supported
	 * @return the enum instance
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid enum value
	 */
	@JsonCreator
	public static AssetCategory fromValue(String value) {
		if ( value != null && value.length() > 0 ) {
			final boolean coded = (value.length() == 1);
			final char code = value.charAt(0);
			for ( AssetCategory e : AssetCategory.values() ) {
				if ( coded && code == e.code ) {
					return e;
				} else if ( e.name().equalsIgnoreCase(value) ) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException(format("Invalid AssetCategory value [%s]", value));
	}

}
