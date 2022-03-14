/* ==================================================================
 * DatumRecordCounts.java - 5/12/2020 3:24:48 pm
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
 * Counts of datum records for a date range.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface DatumRecordCounts {

	/**
	 * Get the associated timestamp of this entity.
	 * 
	 * @return the timestamp for this datum
	 */
	Instant getTimestamp();

	/**
	 * Get the count of datum count.
	 * 
	 * @return the datum count
	 */
	Long getDatumCount();

	/**
	 * Get the count of hourly aggregate datum.
	 * 
	 * @return the hourly aggregate datum count
	 */
	Long getDatumHourlyCount();

	/**
	 * Get the count of daily aggregate datum.
	 * 
	 * @return the daily aggregate datum count
	 */
	Integer getDatumDailyCount();

	/**
	 * Get the count of monthly aggregate datum.
	 * 
	 * @return the monthly aggregate datum count
	 */
	Integer getDatumMonthlyCount();

	/**
	 * Get the sum total of all datum counts.
	 * 
	 * @return the sum total of the datum count properties
	 */
	default long getDatumTotalCount() {
		long t = 0;
		Long l = getDatumCount();
		if ( l != null ) {
			t += l.longValue();
		}
		l = getDatumHourlyCount();
		if ( l != null ) {
			t += l.longValue();
		}
		Integer i = getDatumDailyCount();
		if ( i != null ) {
			t += i.longValue();
		}
		i = getDatumMonthlyCount();
		if ( i != null ) {
			t += i.longValue();
		}
		return t;
	}

}
