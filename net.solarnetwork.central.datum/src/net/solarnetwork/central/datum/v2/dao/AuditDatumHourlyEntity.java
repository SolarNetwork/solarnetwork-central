/* ==================================================================
 * AuditDatumHourlyEntity.java - 3/11/2020 1:31:34 pm
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

package net.solarnetwork.central.datum.v2.dao;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for an hourly datum audit record, which is used to track audit
 * statistics for a datum stream on a particular hour.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class AuditDatumHourlyEntity {

	private final UUID streamId;
	private final Instant timestamp;
	private final int datumCount;
	private final int propCount;
	private final int datumQueryCount;

	/**
	 * Constructor.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timestamp
	 *        the timestamp
	 * @param datumCount
	 *        the datum count
	 * @param propCount
	 *        the datum property count
	 * @param datumQueryCount
	 *        the datum query count
	 */
	public AuditDatumHourlyEntity(UUID streamId, Instant timestamp, int datumCount, int propCount,
			int datumQueryCount) {
		super();
		this.streamId = streamId;
		this.timestamp = timestamp;
		this.datumCount = datumCount;
		this.propCount = propCount;
		this.datumQueryCount = datumQueryCount;
	}

	/**
	 * Get the stream ID.
	 * 
	 * @return the streamId
	 */
	public UUID getStreamId() {
		return streamId;
	}

	/**
	 * Get the timestamp.
	 * 
	 * @return the timestamp
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * Get the datum count.
	 * 
	 * @return the datumCount
	 */
	public int getDatumCount() {
		return datumCount;
	}

	/**
	 * Get the datum property count.
	 * 
	 * @return the propCount
	 */
	public int getPropCount() {
		return propCount;
	}

	/**
	 * Get the datum query count.
	 * 
	 * @return the datumQueryCount
	 */
	public int getDatumQueryCount() {
		return datumQueryCount;
	}

}
