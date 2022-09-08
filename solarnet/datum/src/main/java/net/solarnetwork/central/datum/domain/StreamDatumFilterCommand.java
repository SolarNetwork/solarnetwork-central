/* ==================================================================
 * StreamDatumFilterCommand.java - 29/04/2022 4:34:08 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.domain.AggregationFilter;
import net.solarnetwork.central.domain.NodeMappingFilter;
import net.solarnetwork.central.domain.SourceMappingFilter;
import net.solarnetwork.central.support.BaseFilterSupport;
import net.solarnetwork.dao.OptimizedQueryCriteria;
import net.solarnetwork.domain.MutableSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.util.StringUtils;

/**
 * Implementation of {@link StreamDatumFilter}.
 * 
 * @author matt
 * @version 1.1
 * @since 1.3
 */
@JsonPropertyOrder({ "streamIds", "kind", "objectIds", "sourceIds", "userIds", "aggregation",
		"partialAggregation", "combiningType", "nodeIdMappings", "sourceIdMappings", "rollupTypes",
		"tags", "metadataFilter", "mostRecent", "startDate", "endDate", "localStartDate", "localEndDate",
		"max", "offset", "sorts", "withoutTotalResultsCount" })
public class StreamDatumFilterCommand extends BaseFilterSupport
		implements StreamDatumFilter, AggregationFilter, CombiningFilter, DatumRollupFilter,
		NodeMappingFilter, SourceMappingFilter, OptimizedQueryCriteria {

	private static final long serialVersionUID = 5720733579900923990L;

	private UUID[] streamIds;
	private ObjectDatumKind kind;
	private Long[] objectIds;
	private String[] sourceIds;

	private Instant startDate;
	private Instant endDate;
	private LocalDateTime localStartDate;
	private LocalDateTime localEndDate;
	private boolean mostRecent = false;

	private List<MutableSortDescriptor> sorts;
	private Integer offset;
	private Integer max;

	private Aggregation aggregation;
	private Aggregation partialAggregation;
	private boolean withoutTotalResultsCount;

	private CombiningType combiningType;
	private Map<Long, Set<Long>> nodeIdMappings;
	private Map<String, Set<String>> sourceIdMappings;

	private DatumRollupType[] datumRollupTypes;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(datumRollupTypes);
		result = prime * result + Arrays.hashCode(objectIds);
		result = prime * result + Arrays.hashCode(sourceIds);
		result = prime * result + Arrays.hashCode(streamIds);
		result = prime * result + Objects.hash(aggregation, combiningType, endDate, kind, localEndDate,
				localStartDate, max, mostRecent, nodeIdMappings, offset, partialAggregation, sorts,
				sourceIdMappings, startDate, withoutTotalResultsCount);
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
		if ( !(obj instanceof StreamDatumFilterCommand) ) {
			return false;
		}
		StreamDatumFilterCommand other = (StreamDatumFilterCommand) obj;
		return aggregation == other.aggregation && combiningType == other.combiningType
				&& Arrays.equals(datumRollupTypes, other.datumRollupTypes)
				&& Objects.equals(endDate, other.endDate) && kind == other.kind
				&& Objects.equals(localEndDate, other.localEndDate)
				&& Objects.equals(localStartDate, other.localStartDate) && Objects.equals(max, other.max)
				&& mostRecent == other.mostRecent && Objects.equals(nodeIdMappings, other.nodeIdMappings)
				&& Arrays.equals(objectIds, other.objectIds) && Objects.equals(offset, other.offset)
				&& partialAggregation == other.partialAggregation && Objects.equals(sorts, other.sorts)
				&& Objects.equals(sourceIdMappings, other.sourceIdMappings)
				&& Arrays.equals(sourceIds, other.sourceIds)
				&& Objects.equals(startDate, other.startDate)
				&& Arrays.equals(streamIds, other.streamIds)
				&& withoutTotalResultsCount == other.withoutTotalResultsCount;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("StreamDatumFilterCommand{");
		if ( aggregation != null ) {
			builder.append("aggregation=");
			builder.append(aggregation);
			builder.append(", ");
		}
		if ( kind != null ) {
			builder.append("kind=");
			builder.append(kind);
			builder.append(", ");
		}
		if ( streamIds != null && streamIds.length > 0 ) {
			builder.append("streamIds=");
			builder.append(Arrays.toString(streamIds));
			builder.append(", ");
		}
		if ( objectIds != null && objectIds.length > 0 ) {
			builder.append("objectIds=");
			builder.append(Arrays.toString(objectIds));
			builder.append(", ");
		}
		if ( sourceIds != null && sourceIds.length > 0 ) {
			builder.append("sourceIds=");
			builder.append(Arrays.toString(sourceIds));
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
			builder.append("mostRecent=true,");
		}
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
		if ( partialAggregation != null ) {
			builder.append("partialAggregation=");
			builder.append(partialAggregation);
			builder.append(", ");
		}
		if ( !withoutTotalResultsCount ) {
			builder.append("withoutTotalResultsCount=false,");
		}
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
		builder.append("}");
		return builder.toString();
	}

	@JsonIgnore
	@Override
	public Map<String, ?> getFilter() {
		@SuppressWarnings("unchecked")
		Map<String, Object> filter = (Map<String, Object>) super.getFilter();
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
		if ( startDate != null ) {
			filter.put("start", startDate);
		}
		if ( endDate != null ) {
			filter.put("end", endDate);
		}
		if ( aggregation != null ) {
			filter.put("aggregation", aggregation.toString());
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
		if ( kind != null ) {
			filter.put("kind", getKindValue());
		}
		if ( objectIds != null ) {
			filter.put("objectIds", objectIds);
		}
		if ( streamIds != null ) {
			filter.put("streamIds", streamIds);
		}
		return filter;
	}

	@Override
	@JsonIgnore
	public ObjectDatumKind getKind() {
		return kind;
	}

	/**
	 * Set the object datum kind.
	 * 
	 * @param kind
	 *        the kind
	 */
	@JsonIgnore
	public void setKind(ObjectDatumKind kind) {
		this.kind = kind;
	}

	/**
	 * Get the object datum kind as a string value.
	 * 
	 * @return the kind
	 */
	@JsonGetter("kind")
	public String getKindValue() {
		final ObjectDatumKind kind = getKind();
		return (kind != null ? String.valueOf(kind.getKey()) : null);
	}

	/**
	 * Set the object datum kind.
	 * 
	 * @param value
	 *        the kind
	 */
	@JsonSetter("kind")
	public void setKindValue(String value) {
		ObjectDatumKind kind = null;
		if ( value != null && !value.isEmpty() ) {
			kind = ObjectDatumKind.forKey(value);
		}
		setKind(kind);
	}

	@Override
	public UUID[] getStreamIds() {
		return streamIds;
	}

	/**
	 * Set the stream IDs.
	 * 
	 * @param streamIds
	 *        the stream IDs to set
	 */
	public void setStreamIds(UUID[] streamIds) {
		this.streamIds = streamIds;
	}

	/**
	 * Set a single stream ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single stream ID at
	 * a time. The stream ID is still stored on the {@code streamIds} array,
	 * just as the first value. Calling this method replaces any existing
	 * {@code steramIds} value with a new array containing just the ID passed
	 * into this method.
	 * </p>
	 * 
	 * @param streamId
	 *        the stream ID
	 */
	@JsonSetter
	public void setStreamId(UUID streamId) {
		this.streamIds = (streamId == null ? null : new UUID[] { streamId });
	}

	@Override
	@JsonIgnore
	public UUID getStreamId() {
		return (this.streamIds == null || this.streamIds.length < 1 ? null : this.streamIds[0]);
	}

	@Override
	public Long[] getObjectIds() {
		return objectIds;
	}

	/**
	 * Set the object IDs.
	 * 
	 * @param objectIds
	 *        the object IDs to set
	 */
	public void setObjectIds(Long[] objectIds) {
		this.objectIds = objectIds;
	}

	/**
	 * Set a single object ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single object ID at
	 * a time. The object ID is still stored on the {@code objectIds} array,
	 * just as the first value. Calling this method replaces any existing
	 * {@code objectIds} value with a new array containing just the ID passed
	 * into this method.
	 * </p>
	 * 
	 * @param objectId
	 *        the object ID
	 */
	@JsonSetter
	public void setObjectId(Long objectId) {
		this.objectIds = (objectId == null ? null : new Long[] { objectId });
	}

	@Override
	@JsonIgnore
	public Long getObjectId() {
		return (this.objectIds == null || this.objectIds.length < 1 ? null : this.objectIds[0]);
	}

	/**
	 * Set the stream kind to {@literal Node} and configure the object IDs.
	 * 
	 * @param nodeId
	 *        the node ID to set
	 */
	public void setNodeId(Long nodeId) {
		setKind(ObjectDatumKind.Node);
		setObjectId(nodeId);
	}

	/**
	 * Set the stream kind to {@literal Node} and configure the object IDs.
	 * 
	 * @param nodeIds
	 *        the node IDs to set
	 */
	public void setNodeIds(Long[] nodeIds) {
		setKind(ObjectDatumKind.Node);
		setObjectIds(nodeIds);
	}

	/**
	 * Set the stream kind to {@literal Location} and configure the object IDs.
	 * 
	 * @param locId
	 *        the node ID to set
	 */
	public void setLocationId(Long locId) {
		setKind(ObjectDatumKind.Location);
		setObjectId(locId);
	}

	/**
	 * Set the stream kind to {@literal Location} and configure the object IDs.
	 * 
	 * @param locIds
	 *        the node IDs to set
	 */
	public void setLocationIds(Long[] locIds) {
		setKind(ObjectDatumKind.Location);
		setObjectIds(locIds);
	}

	@Override
	public String[] getSourceIds() {
		return sourceIds;
	}

	/**
	 * Set the source IDs.
	 * 
	 * @param sourceIds
	 *        the source IDs to set
	 */
	public void setSourceIds(String[] sourceIds) {
		this.sourceIds = sourceIds;
	}

	/**
	 * Set a single source ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single source ID at
	 * a time. The source ID is still stored on the {@code sourceIds} array,
	 * just as the first value. Calling this method replaces any existing
	 * {@code sourceIds} value with a new array containing just the ID passed
	 * into this method.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID
	 */
	@JsonSetter
	public void setSourceId(String sourceId) {
		this.sourceIds = (sourceId == null ? null : new String[] { sourceId });
	}

	@Override
	@JsonIgnore
	public String getSourceId() {
		return (this.sourceIds == null || this.sourceIds.length < 1 ? null : this.sourceIds[0]);
	}

	@Override
	public Instant getStartDate() {
		return startDate;
	}

	public void setStartDate(Instant startDate) {
		this.startDate = startDate;
	}

	@Override
	public Instant getEndDate() {
		return endDate;
	}

	public void setEndDate(Instant endDate) {
		this.endDate = endDate;
	}

	@Override
	public LocalDateTime getLocalStartDate() {
		return localStartDate;
	}

	public void setLocalStartDate(LocalDateTime localStartDate) {
		this.localStartDate = localStartDate;
	}

	@Override
	public LocalDateTime getLocalEndDate() {
		return localEndDate;
	}

	public void setLocalEndDate(LocalDateTime localEndDate) {
		this.localEndDate = localEndDate;
	}

	public List<MutableSortDescriptor> getSorts() {
		return sorts;
	}

	public void setSorts(List<MutableSortDescriptor> sorts) {
		this.sorts = sorts;
	}

	@JsonIgnore
	public List<SortDescriptor> getSortDescriptors() {
		if ( sorts == null ) {
			return Collections.emptyList();
		}
		return new ArrayList<SortDescriptor>(sorts);
	}

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public Integer getMax() {
		return max;
	}

	public void setMax(Integer max) {
		this.max = max;
		if ( this.offset == null ) {
			this.offset = 0;
		}
	}

	@Override
	public Aggregation getAggregation() {
		return aggregation;
	}

	/**
	 * Set the aggregation.
	 * 
	 * @param aggregation
	 *        the aggregation to set
	 */
	public void setAggregation(Aggregation aggregation) {
		this.aggregation = aggregation;
	}

	/**
	 * Calls {@link #setAggregation(Aggregation)} for backwards API
	 * compatibility.
	 * 
	 * @param aggregate
	 *        the aggregation to set
	 */
	public void setAggregate(Aggregation aggregate) {
		setAggregation(aggregate);
	}

	/**
	 * Get the aggregation key.
	 * 
	 * @return the aggregation key, never {@literal null}
	 */
	@JsonIgnore
	public String getAggregationKey() {
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
	 */
	public void setAggregationKey(String key) {
		Aggregation agg = null;
		try {
			agg = Aggregation.forKey(key);
		} catch ( IllegalArgumentException e ) {
			agg = Aggregation.None;
		}
		setAggregation(agg);
	}

	@Override
	public Aggregation getPartialAggregation() {
		return partialAggregation;
	}

	/**
	 * Set the partial aggregation.
	 * 
	 * @param partialAggregation
	 *        the aggregation to set
	 */
	public void setPartialAggregation(Aggregation partialAggregation) {
		this.partialAggregation = partialAggregation;
	}

	/**
	 * Get the aggregation key.
	 * 
	 * @return the aggregation key, never {@literal null}
	 */
	@JsonIgnore
	public String getPartialAggregationKey() {
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
	 */
	@JsonSetter
	public void setPartialAggregationKey(String key) {
		Aggregation agg = null;
		try {
			agg = Aggregation.forKey(key);
		} catch ( IllegalArgumentException e ) {
			agg = Aggregation.None;
		}
		setPartialAggregation(agg);
	}

	@Override
	public boolean isMostRecent() {
		return mostRecent;
	}

	/**
	 * Set the most recent query flag.
	 * 
	 * @param mostRecent
	 *        {@literal true} to return only the most recent matching data
	 */
	public void setMostRecent(boolean mostRecent) {
		this.mostRecent = mostRecent;
	}

	@Override
	public boolean isWithoutTotalResultsCount() {
		return withoutTotalResultsCount;
	}

	/**
	 * Toggle the total results count flag.
	 * 
	 * @param withoutTotalResultsCount
	 *        the value to set
	 */
	public void setWithoutTotalResultsCount(boolean withoutTotalResultsCount) {
		this.withoutTotalResultsCount = withoutTotalResultsCount;
	}

	@Override
	public CombiningType getCombiningType() {
		return combiningType;
	}

	/**
	 * Set the combining type.
	 * 
	 * @param combiningType
	 *        the type
	 */
	public void setCombiningType(CombiningType combiningType) {
		this.combiningType = combiningType;
	}

	/**
	 * Get the combining type key.
	 * 
	 * @return the combining type key, or {@literal null} if not defined
	 */
	@JsonIgnore
	public String getCombiningTypeKey() {
		CombiningType type = getCombiningType();
		return (type != null ? type.getKey() : null);
	}

	/**
	 * Set the aggregation as a key value.
	 * 
	 * <p>
	 * If {@literal key} is not a supported {@link CombiningType} key value,
	 * then {@literal null} will be used.
	 * </p>
	 * 
	 * @param key
	 *        the key to set
	 */
	@JsonSetter
	public void setCombiningTypeKey(String key) {
		CombiningType type = null;
		try {
			type = CombiningType.forKey(key);
		} catch ( IllegalArgumentException e ) {
			type = null;
		}
		setCombiningType(type);
	}

	@Override
	public Map<Long, Set<Long>> getNodeIdMappings() {
		return nodeIdMappings;
	}

	/**
	 * Set the node ID mappings.
	 * 
	 * @param nodeIdMappings
	 *        the mappings to set
	 */
	public void setNodeIdMappings(Map<Long, Set<Long>> nodeIdMappings) {
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
	 */
	public void setNodeIdMaps(String[] mappings) {
		Map<Long, Set<Long>> result;
		if ( mappings == null || mappings.length < 1 ) {
			result = null;
		} else {
			result = new LinkedHashMap<Long, Set<Long>>(mappings.length);
			for ( String map : mappings ) {
				int vIdDelimIdx = map.indexOf(':');
				if ( vIdDelimIdx < 1 && result.size() == 1 ) {
					// special case, when Spring maps single query param into 3 fields split on comma like 1:2, 3, 4
					try {
						result.get(result.keySet().iterator().next()).add(Long.valueOf(map));
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
					Set<Long> rNodeIds = new LinkedHashSet<Long>(rIds.size());
					for ( String rId : rIds ) {
						rNodeIds.add(Long.valueOf(rId));
					}
					result.put(vId, rNodeIds);
				} catch ( NumberFormatException e ) {
					// ignore and continue
				}
			}
		}
		setNodeIdMappings(result.isEmpty() ? null : result);
	}

	@Override
	public Map<String, Set<String>> getSourceIdMappings() {
		return sourceIdMappings;
	}

	/**
	 * Set the source ID mappings.
	 * 
	 * @param sourceIdMappings
	 *        the mappings to set
	 */
	public void setSourceIdMappings(Map<String, Set<String>> sourceIdMappings) {
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
	 */
	public void setSourceIdMaps(String[] mappings) {
		Map<String, Set<String>> result;
		if ( mappings == null || mappings.length < 1 ) {
			result = null;
		} else {
			result = new LinkedHashMap<String, Set<String>>(mappings.length);
			for ( String map : mappings ) {
				int vIdDelimIdx = map.indexOf(':');
				if ( vIdDelimIdx < 1 && result.size() == 1 ) {
					// special case, when Spring maps single query param into 3 fields split on comma like A:B, C, D
					try {
						result.get(result.keySet().iterator().next()).add(map);
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
		setSourceIdMappings(result.isEmpty() ? null : result);
	}

	@JsonIgnore
	@Override
	public DatumRollupType getDatumRollupType() {
		return datumRollupTypes != null && datumRollupTypes.length > 0 ? datumRollupTypes[0] : null;
	}

	@JsonProperty("rollupTypes")
	@Override
	public DatumRollupType[] getDatumRollupTypes() {
		return datumRollupTypes;
	}

	/**
	 * Set the datum rollup types to use.
	 * 
	 * @param datumRollupTypes
	 *        the rollup types
	 */
	@JsonProperty("rollupTypes")
	public void setDatumRollupTypes(DatumRollupType[] datumRollupTypes) {
		this.datumRollupTypes = datumRollupTypes;
	}

	/**
	 * Get the datum rollups as key values.
	 * 
	 * @return the datum rollup type key values, or {@literal null} if not
	 *         defined
	 */
	@JsonIgnore
	public String[] getDatumRollupTypeKeys() {
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

}
