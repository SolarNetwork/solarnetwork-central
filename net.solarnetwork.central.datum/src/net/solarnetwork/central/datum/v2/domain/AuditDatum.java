/* ==================================================================
 * AuditDatumDaily.java - 8/11/2020 5:16:47 pm
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
import java.util.UUID;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.domain.Identity;

/**
 * API for an audit record count of datum properties.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface AuditDatum extends Identity<DatumPK> {

	/**
	 * Get the unique ID of the stream this audit datum is a part of.
	 * 
	 * <p>
	 * This is a shortcut for {@code getId().getStreamId()}.
	 * </p>
	 * 
	 * @return the stream ID
	 */
	UUID getStreamId();

	/**
	 * Get the associated timestamp of this audit datum.
	 * 
	 * <p>
	 * This value represents the point in time the properties associated with
	 * this datum were observed, collected, inferred, predicted, etc.
	 * </p>
	 * 
	 * <p>
	 * This is a shortcut for {@code getId().getTimestamp()}.
	 * </p>
	 * 
	 * @return the timestamp for this datum
	 */
	Instant getTimestamp();

	/**
	 * Get time period associated with this audit datum.
	 * 
	 * @return the aggregation
	 */
	Aggregation getAggregation();

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
	 * Get the count of datum properties.
	 * 
	 * @return the datum property count
	 */
	Long getDatumPropertyCount();

	/**
	 * Get the count of datum queried.
	 * 
	 * @return the datum query count
	 */
	Long getDatumQueryCount();

}
