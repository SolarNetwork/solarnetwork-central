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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.datum.domain.CombiningType;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.dao.OptimizedQueryCriteria;
import net.solarnetwork.dao.RecentCriteria;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Basic implementation of {@link DatumCriteria}.
 * 
 * @author matt
 * @version 1.2
 * @since 2.8
 */
public class BasicDatumCriteria extends BasicCoreCriteria
		implements DatumCriteria, AuditDatumCriteria, DatumAuxiliaryCriteria {

	private UUID[] streamIds;
	private Instant startDate;
	private Instant endDate;
	private LocalDateTime localStartDate;
	private LocalDateTime localEndDate;
	private boolean mostRecent = false;
	private boolean withoutTotalResultsCount = true;
	private Aggregation aggregation;
	private Aggregation partialAggregation;
	private DatumReadingType readingType;
	private Period timeTolerance;
	private ObjectDatumKind objectKind;

	private DatumAuxiliaryType datumAuxiliaryType;

	private DatumRollupType[] datumRollupTypes;

	private CombiningType combiningType;
	private Map<Long, Set<Long>> objectIdMappings;
	private Map<String, Set<String>> sourceIdMappings;

	private String[] propertyNames;
	private String[] instantaneousPropertyNames;
	private String[] accumulatingPropertyNames;
	private String[] statusPropertyNames;

	/**
	 * Default constructor.
	 */
	public BasicDatumCriteria() {
		super();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(datumRollupTypes);
		result = prime * result + Arrays.hashCode(streamIds);
		result = prime * result + Objects.hash(aggregation, combiningType, datumAuxiliaryType, endDate,
				localEndDate, localStartDate, mostRecent, objectIdMappings, objectKind,
				partialAggregation, readingType, sourceIdMappings, startDate, timeTolerance,
				withoutTotalResultsCount);
		result = prime * result + Arrays.hashCode(propertyNames);
		result = prime * result + Arrays.hashCode(instantaneousPropertyNames);
		result = prime * result + Arrays.hashCode(accumulatingPropertyNames);
		result = prime * result + Arrays.hashCode(statusPropertyNames);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof BasicDatumCriteria) ) {
			return false;
		}
		BasicDatumCriteria other = (BasicDatumCriteria) obj;
		return aggregation == other.aggregation && combiningType == other.combiningType
				&& datumAuxiliaryType == other.datumAuxiliaryType
				&& Arrays.equals(datumRollupTypes, other.datumRollupTypes)
				&& Objects.equals(endDate, other.endDate)
				&& Objects.equals(localEndDate, other.localEndDate)
				&& Objects.equals(localStartDate, other.localStartDate) && mostRecent == other.mostRecent
				&& Objects.equals(objectIdMappings, other.objectIdMappings)
				&& objectKind == other.objectKind && partialAggregation == other.partialAggregation
				&& readingType == other.readingType
				&& Objects.equals(sourceIdMappings, other.sourceIdMappings)
				&& Objects.equals(startDate, other.startDate)
				&& Arrays.equals(streamIds, other.streamIds)
				&& Objects.equals(timeTolerance, other.timeTolerance)
				&& withoutTotalResultsCount == other.withoutTotalResultsCount
				&& Arrays.equals(propertyNames, other.propertyNames)
				&& Arrays.equals(instantaneousPropertyNames, other.instantaneousPropertyNames)
				&& Arrays.equals(accumulatingPropertyNames, other.accumulatingPropertyNames)
				&& Arrays.equals(statusPropertyNames, other.statusPropertyNames);
	}

	/**
	 * Copy the properties of another criteria into this instance.
	 * 
	 * <p>
	 * This method will test for conformance to all the various criteria
	 * interfaces implemented by this class, and copy those properties as well.
	 * </p>
	 * 
	 * @param criteria
	 *        the criteria to copy
	 */
	public void copyFrom(ObjectStreamCriteria criteria) {
		super.copyFrom(criteria);
		setStreamIds(criteria.getStreamIds());
		setStartDate(criteria.getStartDate());
		setEndDate(criteria.getEndDate());
		setLocalStartDate(criteria.getLocalStartDate());
		setLocalEndDate(criteria.getLocalEndDate());
		setAggregation(criteria.getAggregation());
		setPartialAggregation(criteria.getPartialAggregation());
		setObjectKind(criteria.getObjectKind());
		setCombiningType(criteria.getCombiningType());
		setObjectIdMappings(criteria.getObjectIdMappings());
		setSourceIdMappings(criteria.getSourceIdMappings());
		setPropertyNames(criteria.getPropertyNames());
		setInstantaneousPropertyNames(criteria.getInstantaneousPropertyNames());
		setAccumulatingPropertyNames(criteria.getAccumulatingPropertyNames());
		setStatusPropertyNames(criteria.getStatusPropertyNames());
		if ( criteria instanceof RecentCriteria ) {
			setMostRecent(((RecentCriteria) criteria).isMostRecent());
		}
		if ( criteria instanceof OptimizedQueryCriteria ) {
			setWithoutTotalResultsCount(
					((OptimizedQueryCriteria) criteria).isWithoutTotalResultsCount());
		}
		if ( criteria instanceof ReadingTypeCriteria ) {
			setReadingType(((ReadingTypeCriteria) criteria).getReadingType());
		}
		if ( criteria instanceof TimeToleranceCriteria ) {
			setTimeTolerance(((TimeToleranceCriteria) criteria).getTimeTolerance());
		}
		if ( criteria instanceof DatumAuxiliaryCriteria ) {
			setDatumAuxiliaryType(((DatumAuxiliaryCriteria) criteria).getDatumAuxiliaryType());
		}
		if ( criteria instanceof DatumRollupCriteria ) {
			setDatumRollupTypes(((DatumRollupCriteria) criteria).getDatumRollupTypes());
		}
	}

	/**
	 * Create a copy of a criteria.
	 * 
	 * @param criteria
	 *        the criteria to copy
	 * @return the copy
	 */
	public static BasicDatumCriteria copy(ObjectStreamCriteria criteria) {
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.copyFrom(criteria);
		return c;
	}

	@Override
	public BasicDatumCriteria clone() {
		return (BasicDatumCriteria) super.clone();
	}

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
		if ( propertyNames != null ) {
			builder.append("propertyNames=");
			builder.append(Arrays.toString(propertyNames));
			builder.append(", ");
		}
		if ( instantaneousPropertyNames != null ) {
			builder.append("instantaneousPropertyNames=");
			builder.append(Arrays.toString(instantaneousPropertyNames));
			builder.append(", ");
		}
		if ( accumulatingPropertyNames != null ) {
			builder.append("accumulatingPropertyNames=");
			builder.append(Arrays.toString(accumulatingPropertyNames));
			builder.append(", ");
		}
		if ( statusPropertyNames != null ) {
			builder.append("accumulatingPropertyNames=");
			builder.append(Arrays.toString(accumulatingPropertyNames));
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

	@Override
	public DatumAuxiliaryType getDatumAuxiliaryType() {
		return datumAuxiliaryType;
	}

	/**
	 * Set the datum auxiliary type.
	 * 
	 * @param datumAuxiliaryType
	 *        the type to set
	 */
	public void setDatumAuxiliaryType(DatumAuxiliaryType datumAuxiliaryType) {
		this.datumAuxiliaryType = datumAuxiliaryType;
	}

	@Override
	public CombiningType getCombiningType() {
		return combiningType;
	}

	/**
	 * Set the combining type.
	 * 
	 * @param combiningType
	 *        the type to set
	 */
	public void setCombiningType(CombiningType combiningType) {
		this.combiningType = combiningType;
	}

	@Override
	public Map<Long, Set<Long>> getObjectIdMappings() {
		return objectIdMappings;
	}

	/**
	 * Set the object ID mappings.
	 * 
	 * @param objectIdMappings
	 *        the objectIdMappings to set
	 */
	public void setObjectIdMappings(Map<Long, Set<Long>> objectIdMappings) {
		this.objectIdMappings = objectIdMappings;
	}

	/**
	 * Set the object ID mappings as an encoded string array.
	 * 
	 * @param mappings
	 *        the mapping values
	 * @see ObjectMappingCriteria#mappingsFrom(String[])
	 */
	public void setObjectIdMaps(String[] mappings) {
		setObjectIdMappings(ObjectMappingCriteria.mappingsFrom(mappings));
	}

	@Override
	public Map<String, Set<String>> getSourceIdMappings() {
		return sourceIdMappings;
	}

	/**
	 * Set the source ID mappings.
	 * 
	 * @param sourceIdMappings
	 *        the sourceIdMappings to set
	 */
	public void setSourceIdMappings(Map<String, Set<String>> sourceIdMappings) {
		this.sourceIdMappings = sourceIdMappings;
	}

	/**
	 * Set the source ID mappings as an encoded string array.
	 * 
	 * @param mappings
	 *        the mapping values
	 * @see SourceMappingCriteria#mappingsFrom(String[])
	 */
	public void setSourceIdMaps(String[] mappings) {
		setSourceIdMappings(SourceMappingCriteria.mappingsFrom(mappings));
	}

	@Override
	public String[] getPropertyNames() {
		return propertyNames;
	}

	/**
	 * Set the property names.
	 * 
	 * @param propertyNames
	 *        the names to set
	 * @since 1.2
	 */
	public void setPropertyNames(String[] propertyNames) {
		this.propertyNames = propertyNames;
	}

	@Override
	public String[] getInstantaneousPropertyNames() {
		return instantaneousPropertyNames;
	}

	/**
	 * Set the instantaneous property names.
	 * 
	 * @param propertyNames
	 *        the names to set
	 * @since 1.2
	 */
	public void setInstantaneousPropertyNames(String[] instantaneousPropertyNames) {
		this.instantaneousPropertyNames = instantaneousPropertyNames;
	}

	@Override
	public String[] getAccumulatingPropertyNames() {
		return accumulatingPropertyNames;
	}

	/**
	 * Set the accumulating property names.
	 * 
	 * @param propertyNames
	 *        the names to set
	 * @since 1.2
	 */
	public void setAccumulatingPropertyNames(String[] accumulatingPropertyNames) {
		this.accumulatingPropertyNames = accumulatingPropertyNames;
	}

	@Override
	public String[] getStatusPropertyNames() {
		return statusPropertyNames;
	}

	/**
	 * Set the status property names.
	 * 
	 * @param propertyNames
	 *        the names to set
	 * @since 1.2
	 */
	public void setStatusPropertyNames(String[] statusPropertyNames) {
		this.statusPropertyNames = statusPropertyNames;
	}

}
