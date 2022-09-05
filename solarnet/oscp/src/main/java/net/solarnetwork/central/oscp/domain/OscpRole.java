/* ==================================================================
 * OscpRole.java - 17/08/2022 10:30:39 am
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
 * Enumeration of OSCP actor roles.
 * 
 * @author matt
 * @version 1.0
 */
public enum OscpRole implements CodedValue {

	/** A flexibility provider. */
	FlexibilityProvider('f', "fp"),

	/** A capacity provider. */
	CapacityProvider('p', "cp"),

	/** A capacity optimizer. */
	CapacityOptimizer('o', "co"),

	;

	private char code;
	private final String alias;

	private OscpRole(char code, String alias) {
		this.code = code;
		this.alias = alias;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get the token type alias.
	 * 
	 * @return the alias
	 */
	public String getAlias() {
		return alias;
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
	public static OscpRole forCode(int code) {
		for ( OscpRole e : OscpRole.values() ) {
			if ( code == e.code ) {
				return e;
			}
		}
		throw new IllegalArgumentException(format("Invalid OscpRole code [%s]", code));
	}

	/**
	 * Get an enumeration value for an alias.
	 * 
	 * @param alias
	 *        the alias to get the enumeration value for
	 * @return the enumeration
	 * @throws IllegalArgumentException
	 *         if {@code alias} is not a supported value
	 */
	public static OscpRole forAlias(String alias) {
		return switch (alias) {
			case "fp" -> FlexibilityProvider;
			case "cp" -> CapacityProvider;
			case "co" -> CapacityOptimizer;
			default -> throw new IllegalArgumentException(
					String.format("Unknown OscpRole alias [%s]", alias));
		};
	}

}
