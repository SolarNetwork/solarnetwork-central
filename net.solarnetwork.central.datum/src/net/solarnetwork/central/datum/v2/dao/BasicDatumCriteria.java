/* ==================================================================
 * BasicDatumCriteria.java - 27/10/2020 7:42:38 am
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
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Basic implementation of {@link DatumCriteria}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class BasicDatumCriteria extends BasicCoreCriteria implements DatumCriteria {

	private UUID[] streamIds;
	private Instant startDate;
	private Instant endDate;
	private LocalDateTime localStartDate;
	private LocalDateTime localEndDate;
	private boolean mostRecent = false;

	@Override
	public Instant getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date.
	 * 
	 * @param startDate
	 *        the date to set
	 */
	public void setStartDate(Instant startDate) {
		this.startDate = startDate;
	}

	@Override
	public Instant getEndDate() {
		return endDate;
	}

	/**
	 * Set the end date.
	 * 
	 * @param endDate
	 *        the date to set
	 */
	public void setEndDate(Instant endDate) {
		this.endDate = endDate;
	}

	@Override
	public LocalDateTime getLocalStartDate() {
		return localStartDate;
	}

	/**
	 * Set the local start date.
	 * 
	 * @param localStartDate
	 *        the date to set
	 */
	public void setLocalStartDate(LocalDateTime localStartDate) {
		this.localStartDate = localStartDate;
	}

	@Override
	public LocalDateTime getLocalEndDate() {
		return localEndDate;
	}

	/**
	 * Set the local end date.
	 * 
	 * @param localEndDate
	 *        the date to set
	 */
	public void setLocalEndDate(LocalDateTime localEndDate) {
		this.localEndDate = localEndDate;
	}

	@JsonIgnore
	@Override
	public UUID getStreamId() {
		return (streamIds != null && streamIds.length > 0 ? streamIds[0] : null);
	}

	/**
	 * Set a single stream ID.
	 * 
	 * <p>
	 * This will completely replace any existing {@code streamIds} value with a
	 * new array with a single value.
	 * </p>
	 * 
	 * @param streamId
	 *        the stream ID to set
	 */
	@JsonSetter
	public void setStreamId(UUID streamId) {
		setStreamIds(streamId == null ? null : new UUID[] { streamId });
	}

	@Override
	public UUID[] getStreamIds() {
		return streamIds;
	}

	/**
	 * Set the stream IDs.
	 * 
	 * @param streamIds
	 *        the location IDs to set
	 */
	public void setStreamIds(UUID[] streamIds) {
		this.streamIds = streamIds;
	}

	@Override
	public boolean isMostRecent() {
		return mostRecent;
	}

	/**
	 * Set the "most recent" flag.
	 * 
	 * @param mostRecent
	 *        the "most recent" flag to set
	 */
	public void setMostRecent(boolean mostRecent) {
		this.mostRecent = mostRecent;
	}

}
