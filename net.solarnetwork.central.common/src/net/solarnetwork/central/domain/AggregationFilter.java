/* ==================================================================
 * AggregationFilter.java - Feb 24, 2014 3:54:51 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain;

import org.joda.time.DateTime;

/**
 * Extension of {@link Filter} specific to aggregated results. This API designed
 * to support DAOs that want to implement both "raw" filter results and
 * "aggregate" filter results.
 * 
 * @author matt
 * @version 1.0
 */
public interface AggregationFilter extends Filter {

	/**
	 * Get an aggregation to apply to the query.
	 * 
	 * @return the aggregation, or <em>null</em> for default
	 */
	Aggregation getAggregation();

	/**
	 * Get a start date.
	 * 
	 * @return the start date
	 */
	public DateTime getStartDate();

	/**
	 * Get an end date.
	 * 
	 * @return the end date
	 */
	public DateTime getEndDate();

}
