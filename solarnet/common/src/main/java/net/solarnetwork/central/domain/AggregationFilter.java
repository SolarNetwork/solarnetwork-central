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

import java.time.Instant;

/**
 * Extension of {@link Filter} specific to aggregated results. This API designed
 * to support DAOs that want to implement both "raw" filter results and
 * "aggregate" filter results.
 * 
 * @author matt
 * @version 2.0
 */
public interface AggregationFilter extends Filter {

	/**
	 * Get an aggregation to apply to the query.
	 * 
	 * @return the aggregation, or {@literal null} for default
	 */
	Aggregation getAggregation();

	/**
	 * Get an aggregation to apply to partial time ranges if the start or end
	 * dates do not align to exact boundaries defined by the {@code aggregation}
	 * value.
	 * 
	 * <p>
	 * This filter can be used to request incomplete aggregate results for when
	 * the provided start or end dates do not align to exact
	 * {@link #getAggregation()} boundaries. For example if a
	 * {@link Aggregation#Month} aggregation is requested with a start date of
	 * {@literal 2020-01-15} and an end date of {@literal 2020-03-15}, this
	 * property could be set to {@link Aggregation#Day} to request 3 aggregate
	 * month results that are comprised of these date ranges:
	 * </p>
	 * 
	 * <ol>
	 * <li>15 Jan - 31 Jan</li>
	 * <li>1 Feb - 29 Feb</li>
	 * <li>1 Mar - 14 Mar</li>
	 * </ol>
	 * 
	 * <p>
	 * If this property is <b>not</b> provided in that same example, then the
	 * results would be comprised of these date ranges:
	 * </p>
	 * 
	 * <ol>
	 * <li>1 Jan - 31 Jan</li>
	 * <li>1 Feb - 29 Feb</li>
	 * <li>1 Mar - 31 Mar</li>
	 * </ol>
	 * 
	 * @return the partial aggregation, or {@literal null} for none
	 * @since 1.1
	 */
	Aggregation getPartialAggregation();

	/**
	 * Get a start date.
	 * 
	 * @return the start date
	 */
	public Instant getStartDate();

	/**
	 * Get an end date.
	 * 
	 * @return the end date
	 */
	public Instant getEndDate();

}
