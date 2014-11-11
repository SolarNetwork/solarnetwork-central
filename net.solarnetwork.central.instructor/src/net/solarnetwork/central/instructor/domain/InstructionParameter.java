/* ==================================================================
 * InstructionParameter.java - Sep 29, 2011 9:13:06 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.instructor.domain;

import java.io.Serializable;

/**
 * Helper class for instruction parameters.
 * 
 * @author matt
 * @version 1.1
 */
public class InstructionParameter implements Serializable {

	private static final long serialVersionUID = 2828143065346415324L;

	private String name;
	private String value;

	/**
	 * Default constructor.
	 */
	public InstructionParameter() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the name
	 * @param value
	 *        the value
	 */
	public InstructionParameter(String name, String value) {
		super();
		setName(name);
		setValue(value);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/**
	 * Compare two {@link InstructionParameter} objects for equality. Only the
	 * {@code name} property is used for comparison.
	 */
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		InstructionParameter other = (InstructionParameter) obj;
		if ( name == null ) {
			if ( other.name != null ) {
				return false;
			}
		} else if ( !name.equals(other.name) ) {
			return false;
		}
		return true;
	}

}
