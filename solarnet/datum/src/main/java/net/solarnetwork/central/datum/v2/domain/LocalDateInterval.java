/* ==================================================================
 * DateInterval.java - 29/11/2020 8:47:14 pm
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

import java.time.LocalDateTime;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * A date/time interval.
 * 
 * @author matt
 * @version 1.1
 * @since 2.8
 */
public class LocalDateInterval {

	private final LocalDateTime start;
	private final LocalDateTime end;
	private final Aggregation aggregation;

	/**
	 * Constructor.
	 * 
	 * @param start
	 *        the start date
	 * @param end
	 *        the end date
	 * @param aggregation
	 *        the aggregation
	 */
	public LocalDateInterval(LocalDateTime start, LocalDateTime end, Aggregation aggregation) {
		super();
		this.start = start;
		this.end = end;
		this.aggregation = aggregation;
	}

	/**
	 * Get the start date.
	 * 
	 * @return the start date
	 */
	public LocalDateTime getStart() {
		return start;
	}

	/**
	 * Get the end date.
	 * 
	 * @return the end date
	 */
	public LocalDateTime getEnd() {
		return end;
	}

	/**
	 * Get the aggregation.
	 * 
	 * @return the aggregation
	 */
	public Aggregation getAggregation() {
		return aggregation;
	}

}
