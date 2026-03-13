/* ==================================================================
 * DatumFilterCommand.java - Dec 2, 2013 5:39:51 PM
 *
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.domain.AggregationFilter;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.support.FilterSupport;
import net.solarnetwork.domain.MutableSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.util.StringUtils;

/**
 * Implementation of {@link LocationDatumFilter}, {@link NodeDatumFilter}, and
 * {@link AggregateNodeDatumFilter}, and {@link GeneralNodeDatumFilter}.
 *
 * @author matt
 * @version 2.9
 */
@JsonPropertyOrder({ "locationIds", "nodeIds", "sourceIds", "userIds", "aggregation", "aggregationKey",
		"partialAggregation", "partialAggregationKey", "readingType", "combiningType",
		"combiningTypeKey", "nodeIdMappings", "sourceIdMappings", "propertyNames",
		"instantaneousPropertyNames", "accumulatingPropertyNames", "statusPropertyNames", "rollupTypes",
		"rollupTypeKeys", "tags", "metadataFilter", "dataPath", "mostRecent", "startDate", "endDate",
		"localStartDate", "localEndDate", "max", "offset", "sorts", "type", "location",
		"withoutTotalResultsCount" })
public class DatumFilterCommand extends FilterSupport implements LocationDatumFilter, NodeDatumFilter,
		AggregateNodeDatumFilter, GeneralLocationDatumFilter, AggregateGeneralLocationDatumFilter,
		GeneralNodeDatumFilter, AggregateGeneralNodeDatumFilter, GeneralLocationDatumMetadataFilter,
		GeneralNodeDatumAuxiliaryFilter, GeneralNodeDatumMetadataFilter, SolarNodeMetadataFilter,
		ReadingTypeFilter, Serializable {

	@Serial
	private static final long serialVersionUID = -2340410285910280329L;

	private final SolarLocation location;
	private @Nullable Instant startDate;
	private @Nullable Instant endDate;
	private @Nullable LocalDateTime localStartDate;
	private @Nullable LocalDateTime localEndDate;
	private boolean mostRecent = false;
	private @Nullable String type; // e.g. Power, Consumption, etc.
	private @Nullable List<MutableSortDescriptor> sorts;
	private @Nullable Long offset;
	private @Nullable Integer max;
	private @Nullable String dataPath; // bean path expression to a data value, e.g. "i.watts"

	private @Nullable DatumReadingType readingType;
	private @Nullable Aggregation aggregation;
	private @Nullable Aggregation partialAggregation;
	private boolean withoutTotalResultsCount;

	private @Nullable CombiningType combiningType;
	private @Nullable Map<Long, Set<Long>> nodeIdMappings;
	private @Nullable Map<String, Set<String>> sourceIdMappings;

	private DatumRollupType @Nullable [] datumRollupTypes;

	private String @Nullable [] propertyNames;
	private String @Nullable [] instantaneousPropertyNames;
	private String @Nullable [] accumulatingPropertyNames;
	private String @Nullable [] statusPropertyNames;

	/**
	 * Default constructor.
	 */
	public DatumFilterCommand() {
		super();
		location = new SolarLocation();
	}

	/**
	 * Construct from a Location filter.
	 *
	 * @param loc
	 *        the location
	 */
	public DatumFilterCommand(Location loc) {
		super();
		if ( loc instanceof SolarLocation l ) {
			location = l;
		} else {
			location = new SolarLocation(loc);
		}
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the filter to copy
	 * @since 1.9
	 */
	public DatumFilterCommand(@Nullable AggregateGeneralNodeDatumFilter other) {
		this((GeneralNodeDatumFilter) other);
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the filter to copy
	 * @since 1.10
	 */
	public DatumFilterCommand(@Nullable GeneralNodeDatumFilter other) {
		this(other, new SolarLocation());
		if ( other == null ) {
			return;
		}
		setNodeIds(other.getNodeIds());
		setUserIds(other.getUserIds());
		setCombiningType(other.getCombiningType());
		setNodeIdMappings(other.getNodeIdMappings());
		setSourceIdMappings(other.getSourceIdMappings());
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the filter to copy
	 * @param loc
	 *        the location to use
	 * @since 1.9
	 */
	public DatumFilterCommand(@Nullable CommonFilter other, Location loc) {
		super();
		if ( loc instanceof SolarLocation l ) {
			location = l;
		} else {
			location = new SolarLocation(loc);
		}
		if ( other == null ) {
			return;
		}
		setDataPath(other.getDataPath());
		setEndDate(other.getEndDate());
		setLocalEndDate(other.getLocalEndDate());
		setLocalStartDate(other.getLocalStartDate());
		setSourceIds(other.getSourceIds());
		setStartDate(other.getStartDate());
		setMostRecent(other.isMostRecent());
		setWithoutTotalResultsCount(other.isWithoutTotalResultsCount());
		if ( other instanceof DatumRollupFilter f ) {
			setDatumRollupTypes(f.getDatumRollupTypes());
		}
		if ( other instanceof AggregationFilter f ) {
			setAggregate(f.getAggregation());
			setPartialAggregation(f.getPartialAggregation());
		}
		if ( other instanceof ReadingTypeFilter f ) {
			setReadingType(f.getReadingType());
		}
		if ( other instanceof GeneralDatumMetadataFilter f ) {
			setPropertyNames(f.getPropertyNames());
			setInstantaneousPropertyNames(f.getInstantaneousPropertyNames());
			setAccumulatingPropertyNames(f.getAccumulatingPropertyNames());
			setStatusPropertyNames(f.getStatusPropertyNames());
		}
	}

	@Override
	public String toString() {
		final Long[] nodeIds = getNodeIds();
		final Long[] locationIds = getLocationIds();
		final String[] sourceIds = getSourceIds();
		StringBuilder builder = new StringBuilder();
		builder.append("DatumFilterCommand{");
		if ( aggregation != null ) {
			builder.append("aggregation=");
			builder.append(aggregation);
			builder.append(", ");
		}
		if ( readingType != null ) {
			builder.append("readingType=");
			builder.append(readingType);
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
		if ( locationIds != null && locationIds.length > 0 ) {
			builder.append("locationIds=");
			builder.append(Arrays.toString(locationIds));
			builder.append(", ");
		}
		if ( nodeIds != null && nodeIds.length > 0 ) {
			builder.append("nodeIds=");
			builder.append(Arrays.toString(nodeIds));
			builder.append(", ");
		}
		if ( sourceIds != null && sourceIds.length > 0 ) {
			builder.append("sourceIds=");
			builder.append(Arrays.toString(sourceIds));
			builder.append(", ");
		}
		builder.append("mostRecent=");
		builder.append(mostRecent);
		builder.append(", ");
		if ( sorts != null ) {
			builder.append("sorts=");
			builder.append(sorts);
			builder.append(", ");
		}
		if ( offset != null ) {
			builder.append("offset=");
			builder.append(offset);
			builder.append(", ");
		}
		if ( max != null ) {
			builder.append("max=");
			builder.append(max);
			builder.append(", ");
		}
		if ( type != null ) {
			builder.append("type=");
			builder.append(type);
			builder.append(", ");
		}
		if ( partialAggregation != null ) {
			builder.append("partialAggregation=");
			builder.append(partialAggregation);
			builder.append(", ");
		}
		builder.append("withoutTotalResultsCount=");
		builder.append(withoutTotalResultsCount);
		builder.append(", ");
		if ( combiningType != null ) {
			builder.append("combiningType=");
			builder.append(combiningType);
			builder.append(", ");
		}
		if ( nodeIdMappings != null ) {
			builder.append("nodeIdMappings=");
			builder.append(nodeIdMappings);
			builder.append(", ");
		}
		if ( sourceIdMappings != null ) {
			builder.append("sourceIdMappings=");
			builder.append(sourceIdMappings);
			builder.append(", ");
		}
		if ( datumRollupTypes != null ) {
			builder.append("datumRollupTypes=");
			builder.append(Arrays.toString(datumRollupTypes));
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

	@JsonIgnore
	@Override
	public Map<String, ?> getFilter() {
		@SuppressWarnings("unchecked")
		Map<String, Object> filter = (Map<String, Object>) super.getFilter();
		if ( location.getId() != null ) {
			filter.put("locationId", location.getId());
		}
		if ( startDate != null ) {
			filter.put("start", startDate);
		}
		if ( endDate != null ) {
			filter.put("end", endDate);
		}
		if ( localStartDate != null ) {
			filter.put("localStart", localStartDate);
		}
		if ( localEndDate != null ) {
			filter.put("localEnd", localEndDate);
		}
		filter.putAll(location.getFilter());
		if ( startDate != null ) {
			filter.put("start", startDate);
		}
		if ( endDate != null ) {
			filter.put("end", endDate);
		}
		if ( aggregation != null ) {
			filter.put("aggregation", aggregation.toString());
		}
		if ( readingType != null ) {
			filter.put("readingType", readingType.toString());
		}
		if ( combiningType != null ) {
			filter.put("combiningType", combiningType.toString());
		}
		if ( nodeIdMappings != null ) {
			filter.put("nodeIdMappings", nodeIdMappings);
		}
		if ( sourceIdMappings != null ) {
			filter.put("sourceIdMappings", sourceIdMappings);
		}
		if ( propertyNames != null ) {
			filter.put("propertyNames", propertyNames);
		}
		if ( instantaneousPropertyNames != null ) {
			filter.put("instantaneousPropertyNames", instantaneousPropertyNames);
		}
		if ( accumulatingPropertyNames != null ) {
			filter.put("accumulatingPropertyNames", accumulatingPropertyNames);
		}
		if ( statusPropertyNames != null ) {
			filter.put("statusPropertyNames", statusPropertyNames);
		}
		return filter;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 1.8
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(datumRollupTypes);
		result = prime * result + Objects.hash(aggregation, readingType, combiningType, dataPath,
				endDate, localEndDate, localStartDate, location, max, mostRecent, nodeIdMappings, offset,
				sorts, sourceIdMappings, startDate, type, withoutTotalResultsCount);
		result = prime * result + Arrays.hashCode(propertyNames);
		result = prime * result + Arrays.hashCode(instantaneousPropertyNames);
		result = prime * result + Arrays.hashCode(accumulatingPropertyNames);
		result = prime * result + Arrays.hashCode(statusPropertyNames);
		return result;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 1.8
	 */
	@Override
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) || !(obj instanceof DatumFilterCommand other) ) {
			return false;
		}
		return aggregation == other.aggregation && combiningType == other.combiningType
				&& readingType == other.readingType && Objects.equals(dataPath, other.dataPath)
				&& Arrays.equals(datumRollupTypes, other.datumRollupTypes)
				&& Objects.equals(endDate, other.endDate)
				&& Objects.equals(localEndDate, other.localEndDate)
				&& Objects.equals(localStartDate, other.localStartDate)
				&& Objects.equals(location, other.location) && Objects.equals(max, other.max)
				&& mostRecent == other.mostRecent && Objects.equals(nodeIdMappings, other.nodeIdMappings)
				&& Objects.equals(offset, other.offset) && Objects.equals(sorts, other.sorts)
				&& Objects.equals(sourceIdMappings, other.sourceIdMappings)
				&& Objects.equals(startDate, other.startDate) && Objects.equals(type, other.type)
				&& withoutTotalResultsCount == other.withoutTotalResultsCount
				&& Arrays.equals(propertyNames, other.propertyNames)
				&& Arrays.equals(instantaneousPropertyNames, other.instantaneousPropertyNames)
				&& Arrays.equals(accumulatingPropertyNames, other.accumulatingPropertyNames)
				&& Arrays.equals(statusPropertyNames, other.statusPropertyNames);
	}

	@JsonIgnore
	public boolean isHasLocationCriteria() {
		return !location.getFilter().isEmpty();
	}

	/**
	 * Get the first available location ID, falling back to {@code location.id}
	 * if no other is configured.
	 *
	 * @return the first available location ID
	 * @since 3.0
	 */
	@JsonIgnore
	public @Nullable Long locationId() {
		final Long[] ids = locationIds();
		return (ids != null && ids.length > 0 ? ids[0] : null);
	}

	/**
	 * Get the location IDs, falling back to {@code location.id} if no other is
	 * configured.
	 *
	 * @return the location IDs
	 * @since 3.0
	 */
	@JsonIgnore
	public Long @Nullable [] locationIds() {
		final Long[] locationIds = super.getLocationIds();
		if ( locationIds != null ) {
			return locationIds;
		}
		if ( location != null && location.getId() != null ) {
			return new Long[] { location.getId() };
		}
		return null;
	}

	@Override
	public final Location getLocation() {
		return location;
	}

	@Override
	public final @Nullable Instant getStartDate() {
		return startDate;
	}

	public final void setStartDate(@Nullable Instant startDate) {
		this.startDate = startDate;
	}

	@Override
	public final @Nullable Instant getEndDate() {
		return endDate;
	}

	public final void setEndDate(@Nullable Instant endDate) {
		this.endDate = endDate;
	}

	@Override
	public final @Nullable LocalDateTime getLocalStartDate() {
		return localStartDate;
	}

	public final void setLocalStartDate(@Nullable LocalDateTime localStartDate) {
		this.localStartDate = localStartDate;
	}

	@Override
	public final @Nullable LocalDateTime getLocalEndDate() {
		return localEndDate;
	}

	public final void setLocalEndDate(@Nullable LocalDateTime localEndDate) {
		this.localEndDate = localEndDate;
	}

	@Override
	public final @Nullable String getType() {
		return type;
	}

	public final void setType(@Nullable String datumType) {
		this.type = datumType;
	}

	public final @Nullable List<MutableSortDescriptor> getSorts() {
		return sorts;
	}

	public final void setSorts(@Nullable List<MutableSortDescriptor> sorts) {
		this.sorts = sorts;
	}

	@JsonIgnore
	public final List<SortDescriptor> getSortDescriptors() {
		if ( sorts == null ) {
			return new ArrayList<>(2);
		}
		return new ArrayList<>(sorts);
	}

	public final @Nullable Long getOffset() {
		return offset;
	}

	public final void setOffset(@Nullable Long offset) {
		this.offset = offset;
	}

	public final @Nullable Integer getMax() {
		return max;
	}

	public final void setMax(@Nullable Integer max) {
		this.max = max;
		if ( this.offset == null ) {
			this.offset = 0L;
		}
	}

	@Override
	public final @Nullable Aggregation getAggregation() {
		return aggregation;
	}

	public final void setAggregation(@Nullable Aggregation aggregation) {
		this.aggregation = aggregation;
	}

	/**
	 * Calls {@link #setAggregation(Aggregation)} for backwards API
	 * compatibility.
	 *
	 * @param aggregate
	 *        the aggregation to set
	 */
	public final void setAggregate(@Nullable Aggregation aggregate) {
		setAggregation(aggregate);
	}

	/**
	 * Get the aggregation key.
	 *
	 * @return the aggregation key, never {@code null}
	 * @since 1.9
	 */
	public final String getAggregationKey() {
		Aggregation agg = getAggregation();
		return (agg != null ? agg : Aggregation.None).getKey();
	}

	/**
	 * Set the aggregation as a key value.
	 *
	 * <p>
	 * If {@literal key} is not a supported {@link Aggregation} key value, then
	 * {@link Aggregation#None} will be used.
	 * </p>
	 *
	 * @param key
	 *        the key to set
	 * @since 1.9
	 */
	public final void setAggregationKey(@Nullable String key) {
		Aggregation agg;
		try {
			agg = Aggregation.forKey(key);
		} catch ( IllegalArgumentException e ) {
			agg = Aggregation.None;
		}
		setAggregation(agg);
	}

	/**
	 * Get the partial aggregation.
	 *
	 * @return the partial aggregation
	 * @since 1.15
	 */
	@Override
	public final @Nullable Aggregation getPartialAggregation() {
		return partialAggregation;
	}

	/**
	 * Set the partial aggregation.
	 *
	 * @param partialAggregation
	 *        the aggregation to set
	 * @since 1.15
	 */
	public final void setPartialAggregation(@Nullable Aggregation partialAggregation) {
		this.partialAggregation = partialAggregation;
	}

	/**
	 * Get the aggregation key.
	 *
	 * @return the aggregation key, never {@code null}
	 * @since 1.15
	 */
	public final String getPartialAggregationKey() {
		Aggregation agg = getPartialAggregation();
		return (agg != null ? agg : Aggregation.None).getKey();
	}

	/**
	 * Set the aggregation as a key value.
	 *
	 * <p>
	 * If {@literal key} is not a supported {@link Aggregation} key value, then
	 * {@link Aggregation#None} will be used.
	 * </p>
	 *
	 * @param key
	 *        the key to set
	 * @since 1.15
	 */
	public final void setPartialAggregationKey(@Nullable String key) {
		Aggregation agg;
		try {
			agg = Aggregation.forKey(key);
		} catch ( IllegalArgumentException e ) {
			agg = Aggregation.None;
		}
		setPartialAggregation(agg);
	}

	@Override
	public final boolean isMostRecent() {
		return mostRecent;
	}

	public final void setMostRecent(boolean mostRecent) {
		this.mostRecent = mostRecent;
	}

	@Override
	public final @Nullable String getDataPath() {
		return dataPath;
	}

	public final void setDataPath(@Nullable String dataPath) {
		this.dataPath = dataPath;
	}

	@JsonIgnore
	@Override
	public final String @Nullable [] getDataPathElements() {
		String path = this.dataPath;
		if ( path == null ) {
			return null;
		}
		return path.split("\\.");
	}

	@Override
	public final boolean isWithoutTotalResultsCount() {
		return withoutTotalResultsCount;
	}

	/**
	 * Toggle the total results count flag.
	 *
	 * @param withoutTotalResultsCount
	 *        the value to set
	 * @since 1.9
	 */
	public final void setWithoutTotalResultsCount(boolean withoutTotalResultsCount) {
		this.withoutTotalResultsCount = withoutTotalResultsCount;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 1.10
	 */
	@Override
	public final @Nullable CombiningType getCombiningType() {
		return combiningType;
	}

	/**
	 * Set the combining type.
	 *
	 * @param combiningType
	 *        the type
	 * @since 1.10
	 */
	public final void setCombiningType(@Nullable CombiningType combiningType) {
		this.combiningType = combiningType;
	}

	/**
	 * Get the combining type key.
	 *
	 * @return the combining type key, or {@code null} if not defined
	 * @since 1.10
	 */
	public final @Nullable String getCombiningTypeKey() {
		CombiningType type = getCombiningType();
		return (type != null ? type.getKey() : null);
	}

	/**
	 * Set the aggregation as a key value.
	 *
	 * <p>
	 * If {@literal key} is not a supported {@link CombiningType} key value,
	 * then {@code null} will be used.
	 * </p>
	 *
	 * @param key
	 *        the key to set
	 * @since 1.10
	 */
	public final void setCombiningTypeKey(@Nullable String key) {
		CombiningType type;
		try {
			type = CombiningType.forKey(key);
		} catch ( IllegalArgumentException e ) {
			type = null;
		}
		setCombiningType(type);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 1.10
	 */
	@Override
	public final @Nullable Map<Long, Set<Long>> getNodeIdMappings() {
		return nodeIdMappings;
	}

	/**
	 * Set the node ID mappings.
	 *
	 * @param nodeIdMappings
	 *        the mappings to set
	 * @since 1.10
	 */
	public final void setNodeIdMappings(@Nullable Map<Long, Set<Long>> nodeIdMappings) {
		this.nodeIdMappings = nodeIdMappings;
	}

	/**
	 * Set the node ID mappings value via a list of string encoded mappings.
	 *
	 * <p>
	 * Each mapping in {@code mappings} must be encoded as
	 * {@literal VIRT_NODE_ID:NODE_ID1,NODE_ID2,...}. That is, a virtual node ID
	 * followed by a colon followed by a comma-delimited list of real node IDs.
	 * </p>
	 * <p>
	 * A special case is handled when the mappings are such that the first
	 * includes the colon delimiter, and the remaining values are simple
	 * strings. In that case a single virtual node ID mapping is created.
	 * </p>
	 *
	 * @param mappings
	 *        the mappings to set
	 * @since 1.10
	 */
	public final void setNodeIdMaps(String @Nullable [] mappings) {
		SequencedMap<Long, Set<Long>> result;
		if ( mappings == null || mappings.length < 1 ) {
			result = null;
		} else {
			result = new LinkedHashMap<>(mappings.length);
			for ( String map : mappings ) {
				int vIdDelimIdx = map.indexOf(':');
				if ( vIdDelimIdx < 1 && result.size() == 1 ) {
					// special case, when Spring maps single query param into 3 fields split on comma like 1:2, 3, 4
					try {
						Set<Long> firstValue = nonnull(result.firstEntry(), "First result").getValue();
						firstValue.add(Long.valueOf(map));
					} catch ( NumberFormatException e ) {
						// ignore
					}
					continue;
				} else if ( vIdDelimIdx < 1 || vIdDelimIdx + 1 >= map.length() ) {
					continue;
				}
				try {
					Long vId = Long.valueOf(map.substring(0, vIdDelimIdx));
					Set<String> rIds = StringUtils
							.commaDelimitedStringToSet(map.substring(vIdDelimIdx + 1));
					if ( rIds != null ) {
						Set<Long> rNodeIds = new LinkedHashSet<>(rIds.size());
						for ( String rId : rIds ) {
							rNodeIds.add(Long.valueOf(rId));
						}
						result.put(vId, rNodeIds);
					}
				} catch ( NumberFormatException e ) {
					// ignore and continue
				}
			}
		}
		setNodeIdMappings(result == null || result.isEmpty() ? null : result);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 1.10
	 */
	@Override
	public final @Nullable Map<String, Set<String>> getSourceIdMappings() {
		return sourceIdMappings;
	}

	/**
	 * Set the source ID mappings.
	 *
	 * @param sourceIdMappings
	 *        the mappings to set
	 * @since 1.10
	 */
	public final void setSourceIdMappings(@Nullable Map<String, Set<String>> sourceIdMappings) {
		this.sourceIdMappings = sourceIdMappings;
	}

	/**
	 * Set the source ID mappings value via a list of string encoded mappings.
	 *
	 * <p>
	 * Each mapping in {@code mappings} must be encoded as
	 * {@literal VIRT_SOURCE_ID:SOURCE_ID1,SOURCE_ID2,...}. That is, a virtual
	 * source ID followed by a colon followed by a comma-delimited list of real
	 * source IDs.
	 * </p>
	 *
	 * <p>
	 * A special case is handled when the mappings are such that the first
	 * includes the colon delimiter, and the remaining values are simple
	 * strings. In that case a single virtual source ID mapping is created.
	 * </p>
	 *
	 * @param mappings
	 *        the mappings to set
	 * @since 1.10
	 */
	public final void setSourceIdMaps(String @Nullable [] mappings) {
		SequencedMap<String, Set<String>> result;
		if ( mappings == null || mappings.length < 1 ) {
			result = null;
		} else {
			result = new LinkedHashMap<>(mappings.length);
			for ( String map : mappings ) {
				int vIdDelimIdx = map.indexOf(':');
				if ( vIdDelimIdx < 1 && result.size() == 1 ) {
					// special case, when Spring maps single query param into 3 fields split on comma like A:B, C, D
					try {
						Set<String> firstValue = nonnull(result.firstEntry(), "First result").getValue();
						firstValue.add(map);
					} catch ( NumberFormatException e ) {
						// ignore
					}
					continue;
				} else if ( vIdDelimIdx < 1 || vIdDelimIdx + 1 >= map.length() ) {
					continue;
				}
				String vId = map.substring(0, vIdDelimIdx);
				Set<String> rSourceIds = StringUtils
						.commaDelimitedStringToSet(map.substring(vIdDelimIdx + 1));
				result.put(vId, rSourceIds);
			}
		}
		setSourceIdMappings(result == null || result.isEmpty() ? null : result);
	}

	@JsonIgnore
	@Override
	public final @Nullable DatumRollupType getDatumRollupType() {
		return datumRollupTypes != null && datumRollupTypes.length > 0 ? datumRollupTypes[0] : null;
	}

	@JsonIgnore
	@Override
	public final DatumRollupType @Nullable [] getDatumRollupTypes() {
		return datumRollupTypes;
	}

	/**
	 * Set the datum rollup types to use.
	 *
	 * @param datumRollupTypes
	 *        the rollup types
	 * @since 1.11
	 */
	@JsonIgnore
	public final void setDatumRollupTypes(DatumRollupType @Nullable [] datumRollupTypes) {
		this.datumRollupTypes = datumRollupTypes;
	}

	/**
	 * Get the datum rollups as key values.
	 *
	 * @return the datum rollup type key values, or {@code null} if not defined
	 * @since 1.11
	 */
	@JsonProperty("rollupTypeKeys")
	public final String @Nullable [] getDatumRollupTypeKeys() {
		DatumRollupType[] types = getDatumRollupTypes();
		String[] keys = null;
		if ( types != null && types.length > 0 ) {
			keys = new String[types.length];
			int i = 0;
			for ( DatumRollupType type : types ) {
				keys[i++] = type.getKey();
			}
		}
		return keys;
	}

	/**
	 * Get a single datum rollup type.
	 *
	 * @return the type to use; will return the first available value from
	 *         {@link #getDatumRollupTypes()} or {@code null}
	 * @since 2.7
	 */
	@JsonIgnore
	public final @Nullable DatumRollupType getRollupType() {
		final DatumRollupType[] types = getDatumRollupTypes();
		return (types != null && types.length > 0 ? types[0] : null);
	}

	/**
	 * Set a single datum rollup type to use.
	 *
	 * @param datumRollupType
	 *        the rollup type; completely replaces the {@code datumRollupTypes}
	 *        value
	 * @since 2.7
	 */
	@JsonSetter
	@SuppressWarnings("InvalidParam")
	public final void setRollupType(@Nullable DatumRollupType datumRollupType) {
		setDatumRollupTypes(datumRollupType == null ? null : new DatumRollupType[] { datumRollupType });
	}

	/**
	 * Get the datum rollup types.
	 *
	 * <p>
	 * This is an alias for {@link #getDatumRollupTypes()}.
	 * </p>
	 *
	 * @return the datum rollup types
	 * @see #getDatumRollupTypes()
	 * @since 2.8
	 */
	public final DatumRollupType @Nullable [] getRollupTypes() {
		return getDatumRollupTypes();
	}

	/**
	 * Set the datum rollup types.
	 *
	 * <p>
	 * This is an alias for {@link #setDatumRollupTypes(DatumRollupType[])}.
	 * </p>
	 *
	 * @param types
	 *        the types to set
	 * @see #setDatumRollupTypes(DatumRollupType[])
	 * @since 2.8
	 */
	public final void setRollupTypes(DatumRollupType @Nullable [] types) {
		setDatumRollupTypes(types);
	}

	@Override
	public final @Nullable DatumReadingType getReadingType() {
		return readingType;
	}

	/**
	 * Set the reading type.
	 *
	 * @param readingType
	 *        the type to set
	 * @since 2.3
	 */
	public final void setReadingType(@Nullable DatumReadingType readingType) {
		this.readingType = readingType;
	}

	@JsonIgnore
	@Override
	public final @Nullable String getPropertyName() {
		return GeneralLocationDatumMetadataFilter.super.getPropertyName();
	}

	/**
	 * Set a single property name.
	 *
	 * <p>
	 * This is a convenience method for requests that use a single property name
	 * at a time. The name is still stored on the {@code propertyNames} array,
	 * as the first value. Calling this method replaces any existing
	 * {@code propertyNames} value with a new array containing just the name
	 * passed into this method.
	 * </p>
	 *
	 * @param name
	 *        the name to set
	 * @since 2.4
	 */
	@JsonSetter
	public final void setPropertyName(@Nullable String name) {
		setPropertyNames(name == null ? null : new String[] { name });
	}

	@Override
	public final String @Nullable [] getPropertyNames() {
		return propertyNames;
	}

	/**
	 * Set the property names.
	 *
	 * @param propertyNames
	 *        the names to set
	 * @since 2.4
	 */
	public final void setPropertyNames(String @Nullable [] propertyNames) {
		this.propertyNames = propertyNames;
	}

	@JsonIgnore
	@Override
	public final @Nullable String getInstantaneousPropertyName() {
		return GeneralLocationDatumMetadataFilter.super.getInstantaneousPropertyName();
	}

	/**
	 * Set a single instantaneous property name.
	 *
	 * <p>
	 * This is a convenience method for requests that use a single property name
	 * at a time. The name is still stored on the
	 * {@code instantaneousPropertyNames} array, as the first value. Calling
	 * this method replaces any existing {@code instantaneousPropertyNames}
	 * value with a new array containing just the name passed into this method.
	 * </p>
	 *
	 * @param name
	 *        the name to set
	 * @since 2.4
	 */
	@JsonSetter
	public final void setInstantaneousPropertyName(@Nullable String name) {
		setInstantaneousPropertyNames(name == null ? null : new String[] { name });
	}

	@Override
	public final String @Nullable [] getInstantaneousPropertyNames() {
		return instantaneousPropertyNames;
	}

	/**
	 * Set the instantaneous property names.
	 *
	 * @param instantaneousPropertyNames
	 *        the names to set
	 * @since 2.4
	 */
	public final void setInstantaneousPropertyNames(String @Nullable [] instantaneousPropertyNames) {
		this.instantaneousPropertyNames = instantaneousPropertyNames;
	}

	@JsonIgnore
	@Override
	public final @Nullable String getAccumulatingPropertyName() {
		return GeneralLocationDatumMetadataFilter.super.getAccumulatingPropertyName();
	}

	/**
	 * Set a single accumulating property name.
	 *
	 * <p>
	 * This is a convenience method for requests that use a single property name
	 * at a time. The name is still stored on the
	 * {@code accumulatingPropertyNames} array, as the first value. Calling this
	 * method replaces any existing {@code accumulatingPropertyNames} value with
	 * a new array containing just the name passed into this method.
	 * </p>
	 *
	 * @param name
	 *        the name to set
	 * @since 2.4
	 */
	@JsonSetter
	public final void setAccumulatingPropertyName(@Nullable String name) {
		setAccumulatingPropertyNames(name == null ? null : new String[] { name });
	}

	@Override
	public final String @Nullable [] getAccumulatingPropertyNames() {
		return accumulatingPropertyNames;
	}

	/**
	 * Set the accumulating property names.
	 *
	 * @param accumulatingPropertyNames
	 *        the names to set
	 * @since 2.4
	 */
	public final void setAccumulatingPropertyNames(String @Nullable [] accumulatingPropertyNames) {
		this.accumulatingPropertyNames = accumulatingPropertyNames;
	}

	@JsonIgnore
	@Override
	public final @Nullable String getStatusPropertyName() {
		return GeneralLocationDatumMetadataFilter.super.getStatusPropertyName();
	}

	/**
	 * Set a single status property name.
	 *
	 * <p>
	 * This is a convenience method for requests that use a single property name
	 * at a time. The name is still stored on the {@code statusPropertyNames}
	 * array, as the first value. Calling this method replaces any existing
	 * {@code statusPropertyNames} value with a new array containing just the
	 * name passed into this method.
	 * </p>
	 *
	 * @param name
	 *        the name to set
	 * @since 2.4
	 */
	@JsonSetter
	public final void setStatusPropertyName(@Nullable String name) {
		setStatusPropertyNames(name == null ? null : new String[] { name });
	}

	@Override
	public final String @Nullable [] getStatusPropertyNames() {
		return statusPropertyNames;
	}

	/**
	 * Set the status property names.
	 *
	 * @param statusPropertyNames
	 *        the names to set
	 * @since 2.4
	 */
	public final void setStatusPropertyNames(String @Nullable [] statusPropertyNames) {
		this.statusPropertyNames = statusPropertyNames;
	}

}
