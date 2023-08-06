/* ==================================================================
 * ControlType.java - 22/02/2019 5:22:50 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.domain;

import net.solarnetwork.domain.CodedValue;

/**
 * A DNP3 control type.
 * 
 * @author matt
 * @version 1.0
 */
public enum ControlType implements CodedValue {

	Analog('A', "Analog"),

	Binary('B', "Binary");

	private final char code;
	private final String title;

	private ControlType(char code, String title) {
		this.code = code;
		this.title = title;
	}

	/**
	 * Get the code value.
	 * 
	 * @return the code, as an integer (can be cast to char)
	 */
	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get the title.
	 * 
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Get an enum from a code value.
	 * 
	 * @param code
	 *        the code of the enum to get
	 * @return the code
	 * @throws IllegalArgumentException
	 *         if {@code code} is not supported
	 */
	public static ControlType forCode(char code) {
		ControlType type = CodedValue.forCodeValue(code, ControlType.class, null);
		if ( type == null ) {
			throw new IllegalArgumentException("Unsupported ControlType [" + code + "]");
		}
		return type;
	}

}
