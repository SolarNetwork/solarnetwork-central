/* ==================================================================
 * RegistrationStatus.java - 11/08/2022 4:09:57 pm
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

import com.fasterxml.jackson.annotation.JsonCreator;
import net.solarnetwork.domain.CodedValue;

/**
 * Enumeration of registration status values.
 * 
 * @author matt
 * @version 1.0
 */
public enum RegistrationStatus implements CodedValue {

	/** The registration is pending. */
	Pending('p'),

	/** The registration is complete. */
	Registered('r'),

	/** The registration failed. */
	Failed('f')

	;

	private final char code;

	private RegistrationStatus(char c) {
		this.code = c;
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
	public static RegistrationStatus forCode(int code) {
		for ( RegistrationStatus e : RegistrationStatus.values() ) {
			if ( code == e.code ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Invalid RegistrationStatus code [" + code + "]");
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
	public static RegistrationStatus fromValue(String value) {
		if ( value != null && value.length() > 0 ) {
			final boolean coded = (value.length() == 1);
			final char code = value.charAt(0);
			for ( RegistrationStatus e : RegistrationStatus.values() ) {
				if ( coded && code == e.code ) {
					return e;
				} else if ( e.name().equalsIgnoreCase(value) ) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException("Invalid RegistrationStatus value [" + value + "]");
	}

}
