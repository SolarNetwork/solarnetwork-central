/* ==================================================================
 * AggregateDatum.java - 5/11/2020 10:28:33 am
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

package net.solarnetwork.central.datum.v2.domain;

import net.solarnetwork.central.domain.Aggregation;

/**
 * API for an aggregate "rollup" object that exists within a unique stream at a
 * specific point in time and a set of property values.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface AggregateDatum extends Datum {

	/**
	 * Get the aggregation associated with this datum.
	 * 
	 * @return the aggregation
	 */
	Aggregation getAggregation();

	/**
	 * Get the property statistics.
	 * 
	 * @return the statistics
	 */
	DatumPropertiesStatistics getStatistics();

}
