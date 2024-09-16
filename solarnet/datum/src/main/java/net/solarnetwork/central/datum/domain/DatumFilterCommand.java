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

import java.io.Serializable;
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
 * @version 2.4
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

	private static final long serialVersionUID = -2340410285910280329L;

	private final SolarLocation location;
	private Instant startDate;
	private Instant endDate;
	private LocalDateTime localStartDate;
	private LocalDateTime localEndDate;
	private boolean mostRecent = false;
	private String type; // e.g. Power, Consumption, etc.
	private List<MutableSortDescriptor> sorts;
	private Integer offset;
	private Integer max;
	private String dataPath; // bean path expression to a data value, e.g. "i.watts"

	private DatumReadingType readingType;
	private Aggregation aggregation;
	private Aggregation partialAggregation;
	private boolean withoutTotalResultsCount;

	private CombiningType combiningType;
	private Map<Long, Set<Long>> nodeIdMappings;
	private Map<String, Set<String>> sourceIdMappings;

	private DatumRollupType[] datumRollupTypes;

	private String[] propertyNames;
	private String[] instantaneousPropertyNames;
	private String[] accumulatingPropertyNames;
	private String[] statusPropertyNames;

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
		if ( loc instanceof SolarLocation ) {
			location = (SolarLocation) loc;
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
	public DatumFilterCommand(AggregateGeneralNodeDatumFilter other) {
		this((GeneralNodeDatumFilter) other);
		if ( other == null ) {
			return;
		}
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the filter to copy
	 * @since 1.10
	 */
	public DatumFilterCommand(GeneralNodeDatumFilter other) {
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
	public DatumFilterCommand(CommonFilter other, Location loc) {
		super();
		if ( loc instanceof SolarLocation ) {
			location = (SolarLocation) loc;
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
		if ( location != null ) {
			filter.putAll(location.getFilter());
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

	@JsonIgnore
	public boolean isHasLocationCriteria() {
		return (location != null && location.getFilter().size() > 0);
	}

	@Override
	public Location getLocation() {
		return location;
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

	@Override
	public String getType() {
		return type;
	}

	public void setType(String datumType) {
		this.type = datumType;
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
	 * @since 1.9
	 */
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
	 * @since 1.9
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

	/**
	 * Get the partial aggregation.
	 *
	 * @return the partial aggregation
	 * @since 1.15
	 */
	@Override
	public Aggregation getPartialAggregation() {
		return partialAggregation;
	}

	/**
	 * Set the partial aggregation.
	 *
	 * @param partialAggregation
	 *        the aggregation to set
	 * @since 1.15
	 */
	public void setPartialAggregation(Aggregation partialAggregation) {
		this.partialAggregation = partialAggregation;
	}

	/**
	 * Get the aggregation key.
	 *
	 * @return the aggregation key, never {@literal null}
	 * @since 1.15
	 */
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
	 * @since 1.15
	 */
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

	public void setMostRecent(boolean mostRecent) {
		this.mostRecent = mostRecent;
	}

	@Override
	public String getDataPath() {
		return dataPath;
	}

	public void setDataPath(String dataPath) {
		this.dataPath = dataPath;
	}

	@JsonIgnore
	@Override
	public String[] getDataPathElements() {
		String path = this.dataPath;
		if ( path == null ) {
			return null;
		}
		return path.split("\\.");
	}

	@JsonSetter
	@Override
	public void setLocationId(Long id) {
		location.setId(id);
	}

	@JsonIgnore
	@Override
	public Long getLocationId() {
		if ( location.getId() != null ) {
			return location.getId();
		}
		final Long[] locationIds = getLocationIds();
		if ( locationIds != null && locationIds.length > 0 ) {
			return locationIds[0];
		}
		return null;
	}

	@Override
	public Long[] getLocationIds() {
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
	public boolean isWithoutTotalResultsCount() {
		return withoutTotalResultsCount;
	}

	/**
	 * Toggle the total results count flag.
	 *
	 * @param withoutTotalResultsCount
	 *        the value to set
	 * @since 1.9
	 */
	public void setWithoutTotalResultsCount(boolean withoutTotalResultsCount) {
		this.withoutTotalResultsCount = withoutTotalResultsCount;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 1.10
	 */
	@Override
	public CombiningType getCombiningType() {
		return combiningType;
	}

	/**
	 * Set the combining type.
	 *
	 * @param combiningType
	 *        the type
	 * @since 1.10
	 */
	public void setCombiningType(CombiningType combiningType) {
		this.combiningType = combiningType;
	}

	/**
	 * Get the combining type key.
	 *
	 * @return the combining type key, or {@literal null} if not defined
	 * @since 1.10
	 */
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
	 * @since 1.10
	 */
	public void setCombiningTypeKey(String key) {
		CombiningType type = null;
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
	public Map<Long, Set<Long>> getNodeIdMappings() {
		return nodeIdMappings;
	}

	/**
	 * Set the node ID mappings.
	 *
	 * @param nodeIdMappings
	 *        the mappings to set
	 * @since 1.10
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
	 * @since 1.10
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

	/**
	 * {@inheritDoc}
	 *
	 * @since 1.10
	 */
	@Override
	public Map<String, Set<String>> getSourceIdMappings() {
		return sourceIdMappings;
	}

	/**
	 * Set the source ID mappings.
	 *
	 * @param sourceIdMappings
	 *        the mappings to set
	 * @since 1.10
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
	 * @since 1.10
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
	 * Get the datum rollups as key values.
	 *
	 * @return the datum rollup type key values, or {@literal null} if not
	 *         defined
	 * @since 1.11
	 */
	@JsonProperty("rollupTypeKeys")
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

	/**
	 * Set the datum rollup types to use.
	 *
	 * @param datumRollupTypes
	 *        the rollup types
	 * @since 1.11
	 */
	@JsonProperty("rollupTypes")
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
	 * @since 2.3
	 */
	public void setReadingType(DatumReadingType readingType) {
		this.readingType = readingType;
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
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof DatumFilterCommand) ) {
			return false;
		}
		DatumFilterCommand other = (DatumFilterCommand) obj;
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
	@Override
	public String getPropertyName() {
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
	public void setPropertyName(String name) {
		setPropertyNames(name == null ? null : new String[] { name });
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
	 * @since 2.4
	 */
	public void setPropertyNames(String[] propertyNames) {
		this.propertyNames = propertyNames;
	}

	@JsonIgnore
	@Override
	public String getInstantaneousPropertyName() {
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
	public void setInstantaneousPropertyName(String name) {
		setInstantaneousPropertyNames(name == null ? null : new String[] { name });
	}

	@Override
	public String[] getInstantaneousPropertyNames() {
		return instantaneousPropertyNames;
	}

	/**
	 * Set the instantaneous property names.
	 *
	 * @param instantaneousPropertyNames
	 *        the names to set
	 * @since 2.4
	 */
	public void setInstantaneousPropertyNames(String[] instantaneousPropertyNames) {
		this.instantaneousPropertyNames = instantaneousPropertyNames;
	}

	@JsonIgnore
	@Override
	public String getAccumulatingPropertyName() {
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
	public void setAccumulatingPropertyName(String name) {
		setAccumulatingPropertyNames(name == null ? null : new String[] { name });
	}

	@Override
	public String[] getAccumulatingPropertyNames() {
		return accumulatingPropertyNames;
	}

	/**
	 * Set the accumulating property names.
	 *
	 * @param accumulatingPropertyNames
	 *        the names to set
	 * @since 2.4
	 */
	public void setAccumulatingPropertyNames(String[] accumulatingPropertyNames) {
		this.accumulatingPropertyNames = accumulatingPropertyNames;
	}

	@JsonIgnore
	@Override
	public String getStatusPropertyName() {
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
	public void setStatusPropertyName(String name) {
		setStatusPropertyNames(name == null ? null : new String[] { name });
	}

	@Override
	public String[] getStatusPropertyNames() {
		return statusPropertyNames;
	}

	/**
	 * Set the status property names.
	 *
	 * @param statusPropertyNames
	 *        the names to set
	 * @since 2.4
	 */
	public void setStatusPropertyNames(String[] statusPropertyNames) {
		this.statusPropertyNames = statusPropertyNames;
	}

}
