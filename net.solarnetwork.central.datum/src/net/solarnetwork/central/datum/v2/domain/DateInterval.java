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

import java.time.Instant;
import java.time.ZoneId;

/**
 * A date/time interval.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class DateInterval {

	private final Instant start;
	private final Instant end;
	private final ZoneId zone;

	/**
	 * Constructor.
	 * 
	 * @param start
	 *        the start date
	 * @param end
	 *        the end date
	 * @param zone
	 *        the time zone
	 */
	public DateInterval(Instant start, Instant end, ZoneId zone) {
		super();
		this.start = start;
		this.end = end;
		this.zone = zone;
	}

	/**
	 * Constructor.
	 * 
	 * @param start
	 *        the start date
	 * @param end
	 *        the end date
	 * @param zone
	 *        the time zone ID
	 */
	public DateInterval(Instant start, Instant end, String timeZoneId) {
		this(start, end, ZoneId.of(timeZoneId));
	}

	/**
	 * Get the start date.
	 * 
	 * @return the start date
	 */
	public Instant getStart() {
		return start;
	}

	/**
	 * Get the end date.
	 * 
	 * @return the end date
	 */
	public Instant getEnd() {
		return end;
	}

	/**
	 * Get the time zone
	 * 
	 * @return the zone
	 */
	public ZoneId getZone() {
		return zone;
	}

}
