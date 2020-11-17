/* ==================================================================
 * ReadingDatum.java - 17/11/2020 7:51:57 am
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

import java.time.Instant;

/**
 * API for a "reading" object that represents a calculated reading for a
 * specific time period.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface ReadingDatum extends AggregateDatum {

	/**
	 * Get the timestamp associated with the end of the reading.
	 * 
	 * <p>
	 * The {@link Datum#getTimestamp()} value represents the starting timestamp
	 * of this reading. The start and end timestamp values can be equal, for
	 * reading calculations for a single point in time, or different for
	 * calculations representing a time range.
	 * </p>
	 * 
	 * @return the end timestamp
	 */
	Instant getEndTimestamp();

}
