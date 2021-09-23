/* ==================================================================
 * ReadingDatum.java - 27/04/2020 3:35:57 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain;

import java.util.Map;

/**
 * API for a datum that represents a "reading" such as of a meter.
 * 
 * @author matt
 * @version 1.0
 * @since 2.4
 */
public interface ReadingDatum {

	/**
	 * A standardized suffix to add to start reading property names when merging
	 * with other properties.
	 */
	String START_PROPERTY_SUFFIX = "_start";

	/**
	 * A standardized suffix to add to final reading property names when merging
	 * with other properties.
	 */
	String FINAL_PROPERTY_SUFFIX = "_end";

	/**
	 * Get all available "start" reading sample data as a Map.
	 * 
	 * @return the sample data as a Map, or {@literal null} if no final sample
	 *         data available
	 */
	Map<String, ?> getSampleDataStart();

	/**
	 * Get all available "final" reading sample data as a Map.
	 * 
	 * @return the sample data as a Map, or {@literal null} if no final sample
	 *         data available
	 */
	Map<String, ?> getSampleDataFinal();
}
