/* ==================================================================
 * ServerConfigurationsCsvColumn.java - 12/08/2023 8:58:43 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dnp3.support;

import net.solarnetwork.domain.CodedValue;

/**
 * Enumeration of server configurations CSV columns.
 * 
 * @author matt
 * @version 1.0
 */
public enum ServerConfigurationsCsvColumn implements CodedValue {

	/** The node ID. */
	NODE_ID(0, "Node ID"),

	/** The source ID. */
	SOURCE_ID(1, "Source ID"),

	/** The property name. */
	PROPERTY(2, "Property"),

	/** The DNP3 type. */
	TYPE(3, "DNP3 Type"),

	/** The enabled flag. */
	ENABLED(4, "Enabled"),

	/** The multiplier. */
	MULTIPLIER(5, "Multiplier"),

	/** The offset. */
	OFFSET(6, "Offset"),

	/** The decimal scale. */
	DECIMAL_SCALE(7, "Decimal Scale"),

	;

	private final int idx;
	private final String name;

	private ServerConfigurationsCsvColumn(int idx, String name) {
		this.idx = idx;
		this.name = name;
	}

	@Override
	public int getCode() {
		return idx;
	}

	/**
	 * Get a friendly name for the column.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

}
