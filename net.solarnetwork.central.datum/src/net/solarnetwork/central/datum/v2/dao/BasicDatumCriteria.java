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
import java.time.Period;
import java.util.Arrays;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Basic implementation of {@link DatumCriteria}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class BasicDatumCriteria extends BasicCoreCriteria
		implements DatumCriteria, AuditDatumCriteria, ReadingDatumCriteria {

	private UUID[] streamIds;
	private Instant startDate;
	private Instant endDate;
	private LocalDateTime localStartDate;
	private LocalDateTime localEndDate;
	private boolean mostRecent = false;
	private boolean withoutTotalResultsCount = true;
	private Aggregation aggregation;
	private Aggregation partialAggregation;
	private DatumRollupType[] datumRollupTypes;
	private DatumReadingType readingType;
	private Period timeTolerance;
	private ObjectDatumKind objectKind;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicDatumCriteria{");
		if ( getNodeIds() != null ) {
			builder.append("nodeIds=");
			builder.append(Arrays.toString(getNodeIds()));
			builder.append(", ");
		}
		if ( getSourceIds() != null ) {
			builder.append("sourceIds=");
			builder.append(Arrays.toString(getSourceIds()));
			builder.append(", ");
		}
		if ( getUserIds() != null ) {
			builder.append("userIds=");
			builder.append(Arrays.toString(getUserIds()));
		}
		if ( streamIds != null ) {
			builder.append("streamIds=");
			builder.append(Arrays.toString(streamIds));
			builder.append(", ");
		}
		if ( startDate != null ) {
			builder.append("startDate=");
			builder.append(startDate);
			builder.append(", ");
		}
		if ( endDate != null ) {
			builder.append("endDate=");
			builder.append(endDate);
			builder.append(", ");
		}
		if ( localStartDate != null ) {
			builder.append("localStartDate=");
			builder.append(localStartDate);
			builder.append(", ");
		}
		if ( localEndDate != null ) {
			builder.append("localEndDate=");
			builder.append(localEndDate);
			builder.append(", ");
		}
		if ( mostRecent ) {
			builder.append("mostRecent=true, ");
		}
		if ( aggregation != null ) {
			builder.append("aggregation=");
			builder.append(aggregation);
			builder.append(", ");
		}
		if ( partialAggregation != null ) {
			builder.append("partialAggregation=");
			builder.append(partialAggregation);
			builder.append(", ");
		}
		if ( datumRollupTypes != null ) {
			builder.append("datumRollupTypes=");
			builder.append(Arrays.toString(datumRollupTypes));
			builder.append(", ");
		}
		if ( readingType != null ) {
			builder.append("readingType=");
			builder.append(readingType);
			builder.append(", ");
		}
		builder.append("}");
		return builder.toString();
	}

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

	/**
	 * Toggle the "without total results" mode.
	 * 
	 * @param mode
	 *        the mode to set
	 */
	public void setWithoutTotalResultsCount(boolean mode) {
		this.withoutTotalResultsCount = mode;
	}

	@Override
	public boolean isWithoutTotalResultsCount() {
		return withoutTotalResultsCount;
	}

	@Override
	public Aggregation getAggregation() {
		return aggregation;
	}

	/**
	 * Set the aggregation level to use.
	 * 
	 * @param aggregation
	 *        the aggregation to set
	 */
	public void setAggregation(Aggregation aggregation) {
		this.aggregation = aggregation;
	}

	@Override
	public Aggregation getPartialAggregation() {
		return partialAggregation;
	}

	/**
	 * Set the partial aggregation to include.
	 * 
	 * @param partialAggregation
	 *        the partialAggregation to set
	 */
	public void setPartialAggregation(Aggregation partialAggregation) {
		this.partialAggregation = partialAggregation;
	}

	@Override
	public DatumRollupType getDatumRollupType() {
		DatumRollupType[] types = getDatumRollupTypes();
		return types != null && types.length > 0 ? types[0] : null;
	}

	@Override
	public DatumRollupType[] getDatumRollupTypes() {
		return datumRollupTypes;
	}

	/**
	 * Set the datum rollup types.
	 * 
	 * @param datumRollupTypes
	 *        the types to set
	 */
	public void setDatumRollupTypes(DatumRollupType[] datumRollupTypes) {
		this.datumRollupTypes = datumRollupTypes;
	}

	@Override
	public DatumReadingType getReadingType() {
		return readingType;
	}

	/**
	 * Set the reading type.
	 * 
	 * @param readingType
	 *        the type to set
	 */
	public void setReadingType(DatumReadingType readingType) {
		this.readingType = readingType;
	}

	@Override
	public Period getTimeTolerance() {
		return timeTolerance;
	}

	/**
	 * Set the time tolerance.
	 * 
	 * @param timeTolerance
	 *        the period to set
	 */
	public void setTimeTolerance(Period timeTolerance) {
		this.timeTolerance = timeTolerance;
	}

	@Override
	public ObjectDatumKind getObjectKind() {
		return objectKind;
	}

	/**
	 * Set the object kind.
	 * 
	 * @param objectKind
	 *        the object kind to set
	 */
	public void setObjectKind(ObjectDatumKind objectKind) {
		this.objectKind = objectKind;
	}

}
