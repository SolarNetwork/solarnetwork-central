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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.MutableSortDescriptor;
import net.solarnetwork.util.StringUtils;

/**
 * Implementation of {@link LocationDatumFilter}, {@link NodeDatumFilter}, and
 * {@link AggregateNodeDatumFilter}, and {@link GeneralNodeDatumFilter}.
 * 
 * @author matt
 * @version 1.13
 */
@JsonPropertyOrder({ "locationIds", "nodeIds", "sourceIds", "userIds", "aggregation", "aggregationKey",
		"combiningType", "combiningTypeKey", "nodeIdMappings", "sourceIdMappings", "rollupTypes",
		"rollupTypeKeys", "tags", "dataPath", "mostRecent", "startDate", "endDate", "localStartDate",
		"localEndDate", "max", "offset", "sorts", "type", "location" })
public class DatumFilterCommand implements LocationDatumFilter, NodeDatumFilter,
		AggregateNodeDatumFilter, GeneralLocationDatumFilter, AggregateGeneralLocationDatumFilter,
		GeneralNodeDatumFilter, AggregateGeneralNodeDatumFilter, GeneralLocationDatumMetadataFilter,
		GeneralNodeDatumAuxiliaryFilter, GeneralNodeDatumMetadataFilter, SolarNodeMetadataFilter,
		Serializable {

	private static final long serialVersionUID = -1991217374281570027L;

	private final SolarLocation location;
	private DateTime startDate;
	private DateTime endDate;
	private LocalDateTime localStartDate;
	private LocalDateTime localEndDate;
	private boolean mostRecent = false;
	private String type; // e.g. Power, Consumption, etc.
	private List<MutableSortDescriptor> sorts;
	private Integer offset = 0;
	private Integer max;
	private String dataPath; // bean path expression to a data value, e.g. "i.watts"

	private Long[] locationIds;
	private Long[] nodeIds;
	private String[] sourceIds;
	private Long[] userIds;
	private String[] tags;
	private Aggregation aggregation;
	private boolean withoutTotalResultsCount;

	private CombiningType combiningType;
	private Map<Long, Set<Long>> nodeIdMappings;
	private Map<String, Set<String>> sourceIdMappings;

	private DatumRollupType[] datumRollupTypes;

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
		setAggregate(other.getAggregation());
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
		if ( other instanceof DatumRollupFilter ) {
			setDatumRollupTypes(((DatumRollupFilter) other).getDatumRollupTypes());
		}
	}

	@Override
	public String toString() {
		return "DatumFilterCommand{aggregation=" + aggregation + ",mostRecent=" + mostRecent
				+ ",startDate=" + startDate + ",endDate=" + endDate + ",withoutTotalResultsCount="
				+ withoutTotalResultsCount
				+ (nodeIds != null && nodeIds.length > 0 ? ",nodeIds=" + Arrays.toString(nodeIds)
						: locationIds != null && locationIds.length > 0
								? ",locationIds=" + Arrays.toString(locationIds)
								: "")
				+ ",sourceIds=" + Arrays.toString(sourceIds) + "}";
	}

	@JsonIgnore
	@Override
	public Map<String, ?> getFilter() {
		Map<String, Object> filter = new LinkedHashMap<String, Object>();
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
		if ( nodeIds != null ) {
			filter.put("nodeIds", nodeIds);
		}
		if ( sourceIds != null ) {
			filter.put("sourceIds", sourceIds);
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
		return filter;
	}

	@JsonIgnore
	public boolean isHasLocationCriteria() {
		return (location != null && location.getFilter().size() > 0);
	}

	public void setLocationId(Long id) {
		location.setId(id);
	}

	@Override
	public Long getLocationId() {
		if ( location.getId() != null ) {
			return location.getId();
		}
		if ( locationIds != null && locationIds.length > 0 ) {
			return locationIds[0];
		}
		return null;
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public DateTime getStartDate() {
		return startDate;
	}

	public void setStartDate(DateTime startDate) {
		this.startDate = startDate;
	}

	@Override
	public DateTime getEndDate() {
		return endDate;
	}

	public void setEndDate(DateTime endDate) {
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
	}

	/**
	 * Set a single node ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single node ID at a
	 * time. The node ID is still stored on the {@code nodeIds} array, just as
	 * the first value. Calling this method replaces any existing
	 * {@code nodeIds} value with a new array containing just the ID passed into
	 * this method.
	 * </p>
	 * 
	 * @param nodeId
	 *        the ID of the node
	 */
	@JsonSetter
	public void setNodeId(Long nodeId) {
		this.nodeIds = new Long[] { nodeId };
	}

	/**
	 * Get the first node ID.
	 * 
	 * <p>
	 * This returns the first available node ID from the {@code nodeIds} array,
	 * or <em>null</em> if not available.
	 * </p>
	 * 
	 * @return the first node ID
	 */
	@JsonIgnore
	@Override
	public Long getNodeId() {
		return this.nodeIds == null || this.nodeIds.length < 1 ? null : this.nodeIds[0];
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
	 * @param nodeId
	 *        the ID of the node
	 */
	@JsonSetter
	public void setSourceId(String sourceId) {
		if ( sourceId == null ) {
			this.sourceIds = null;
		} else {
			this.sourceIds = new String[] { sourceId };
		}
	}

	/**
	 * Get the first source ID.
	 * 
	 * <p>
	 * This returns the first available source ID from the {@code sourceIds}
	 * array, or <em>null</em> if not available.
	 * </p>
	 * 
	 * @return the first node ID
	 */
	@JsonIgnore
	@Override
	public String getSourceId() {
		return this.sourceIds == null || this.sourceIds.length < 1 ? null : this.sourceIds[0];
	}

	@Override
	public Long[] getNodeIds() {
		return nodeIds;
	}

	public void setNodeIds(Long[] nodeIds) {
		this.nodeIds = nodeIds;
	}

	@Override
	public String[] getSourceIds() {
		return sourceIds;
	}

	public void setSourceIds(String[] sourceIds) {
		this.sourceIds = sourceIds;
	}

	@Override
	public String getTag() {
		return this.tags == null || this.tags.length < 1 ? null : this.tags[0];
	}

	@Override
	public String[] getTags() {
		return tags;
	}

	public void setTags(String[] tags) {
		this.tags = tags;
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

	@Override
	public Long[] getLocationIds() {
		if ( locationIds != null ) {
			return locationIds;
		}
		if ( location != null && location.getId() != null ) {
			return new Long[] { location.getId() };
		}
		return null;
	}

	public void setLocationIds(Long[] locationIds) {
		this.locationIds = locationIds;
	}

	/**
	 * Set a single user ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single user ID at a
	 * time. The user ID is still stored on the {@code userIds} array, just as
	 * the first value. Calling this method replaces any existing
	 * {@code userIds} value with a new array containing just the ID passed into
	 * this method.
	 * </p>
	 * 
	 * @param userId
	 *        the ID of the user
	 */
	@JsonSetter
	public void setUserId(Long userId) {
		this.userIds = new Long[] { userId };
	}

	/**
	 * Get the first user ID.
	 * 
	 * <p>
	 * This returns the first available user ID from the {@code userIds} array,
	 * or <em>null</em> if not available.
	 * </p>
	 * 
	 * @return the first user ID
	 */
	@JsonIgnore
	@Override
	public Long getUserId() {
		return this.userIds == null || this.userIds.length < 1 ? null : this.userIds[0];
	}

	@Override
	public Long[] getUserIds() {
		return userIds;
	}

	public void setUserIds(Long[] userIds) {
		this.userIds = userIds;
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

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.8
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aggregation == null) ? 0 : aggregation.hashCode());
		result = prime * result + ((dataPath == null) ? 0 : dataPath.hashCode());
		result = prime * result + ((endDate == null) ? 0 : endDate.hashCode());
		result = prime * result + ((localEndDate == null) ? 0 : localEndDate.hashCode());
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + Arrays.hashCode(locationIds);
		result = prime * result + ((max == null) ? 0 : max.hashCode());
		result = prime * result + (mostRecent ? 1231 : 1237);
		result = prime * result + Arrays.hashCode(nodeIds);
		result = prime * result + ((offset == null) ? 0 : offset.hashCode());
		result = prime * result + ((sorts == null) ? 0 : sorts.hashCode());
		result = prime * result + Arrays.hashCode(sourceIds);
		result = prime * result + ((startDate == null) ? 0 : startDate.hashCode());
		result = prime * result + ((localStartDate == null) ? 0 : localStartDate.hashCode());
		result = prime * result + Arrays.hashCode(tags);
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + Arrays.hashCode(userIds);
		result = prime * result + ((combiningType == null) ? 0 : combiningType.hashCode());
		result = prime * result + ((nodeIdMappings == null) ? 0 : nodeIdMappings.hashCode());
		result = prime * result + ((sourceIdMappings == null) ? 0 : sourceIdMappings.hashCode());
		result = prime * result + Arrays.hashCode(datumRollupTypes);
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
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof DatumFilterCommand) ) {
			return false;
		}
		DatumFilterCommand other = (DatumFilterCommand) obj;
		if ( aggregation != other.aggregation ) {
			return false;
		}
		if ( dataPath == null ) {
			if ( other.dataPath != null ) {
				return false;
			}
		} else if ( !dataPath.equals(other.dataPath) ) {
			return false;
		}
		if ( endDate == null ) {
			if ( other.endDate != null ) {
				return false;
			}
		} else if ( !endDate.isEqual(other.endDate) ) {
			return false;
		}
		if ( localEndDate == null ) {
			if ( other.localEndDate != null ) {
				return false;
			}
		} else if ( !localEndDate.isEqual(other.localEndDate) ) {
			return false;
		}
		if ( location == null ) {
			if ( other.location != null ) {
				return false;
			}
		} else if ( !location.equals(other.location) ) {
			return false;
		}
		if ( !Arrays.equals(locationIds, other.locationIds) ) {
			return false;
		}
		if ( max == null ) {
			if ( other.max != null ) {
				return false;
			}
		} else if ( !max.equals(other.max) ) {
			return false;
		}
		if ( mostRecent != other.mostRecent ) {
			return false;
		}
		if ( !Arrays.equals(nodeIds, other.nodeIds) ) {
			return false;
		}
		if ( offset == null ) {
			if ( other.offset != null ) {
				return false;
			}
		} else if ( !offset.equals(other.offset) ) {
			return false;
		}
		if ( sorts == null ) {
			if ( other.sorts != null ) {
				return false;
			}
		} else if ( !sorts.equals(other.sorts) ) {
			return false;
		}
		if ( !Arrays.equals(sourceIds, other.sourceIds) ) {
			return false;
		}
		if ( startDate == null ) {
			if ( other.startDate != null ) {
				return false;
			}
		} else if ( !startDate.isEqual(other.startDate) ) {
			return false;
		}
		if ( localStartDate == null ) {
			if ( other.localStartDate != null ) {
				return false;
			}
		} else if ( !localStartDate.isEqual(other.localStartDate) ) {
			return false;
		}
		if ( !Arrays.equals(tags, other.tags) ) {
			return false;
		}
		if ( type == null ) {
			if ( other.type != null ) {
				return false;
			}
		} else if ( !type.equals(other.type) ) {
			return false;
		}
		if ( !Arrays.equals(userIds, other.userIds) ) {
			return false;
		}
		if ( combiningType != other.combiningType ) {
			return false;
		}
		if ( nodeIdMappings == null ) {
			if ( other.nodeIdMappings != null ) {
				return false;
			}
		} else if ( !nodeIdMappings.equals(other.nodeIdMappings) ) {
			return false;
		}
		if ( sourceIdMappings == null ) {
			if ( other.sourceIdMappings != null ) {
				return false;
			}
		} else if ( !sourceIdMappings.equals(other.sourceIdMappings) ) {
			return false;
		}
		if ( !Arrays.equals(datumRollupTypes, other.datumRollupTypes) ) {
			return false;
		}
		return true;
	}

}
