/* ===================================================================
 * DatumQueryCommand.java
 * 
 * Created Sep 12, 2008 3:06:03 PM
 * 
 * Copyright (c) 2008 Solarnetwork.net Dev Team.
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.central.datum.domain;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.util.Cachable;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;

/**
 * Command object for specifying datum query criteria.
 * 
 * @author matt
 * @version $Revision$ $Date$
 */
public class DatumQueryCommand implements Cachable {

	private Long[] nodeIds;
	private String[] sourceIds;
	private Long[] locationIds;
	private DateTime startDate;
	private DateTime endDate;
	private Aggregation aggregate;
	private boolean mostRecent = false;
	private Integer precision; // specify aggregate precision, e.g. *5* minutes
	private Integer resultOffset; // result starting offset
	private Integer resultMax; // result page size
	private String datumType; // e.g. Power, Consumption, etc.
	private Map<String, Object> properties = new LinkedHashMap<String, Object>();

	@Override
	public String getCacheKey() {
		String data = Arrays.toString(nodeIds) + Arrays.toString(sourceIds)
				+ Arrays.toString(locationIds) + startDate + endDate + aggregate + mostRecent
				+ precision + resultOffset + resultMax + datumType + properties;
		return DigestUtils.shaHex(data);
	}

	@Override
	public Long getTti() {
		return null;
	}

	@Override
	public Long getTtl() {
		return null;
	}

	/**
	 * Test if a specific Aggregation is set.
	 * 
	 * <p>
	 * This method will return <em>false</em> if no {@code aggregate} value has
	 * been set.
	 * </p>
	 * 
	 * @param agg
	 *        the aggregation to test
	 * @return boolean
	 */
	public boolean isAggregateAtMost(Aggregation agg) {
		return this.aggregate != null && this.aggregate.compareLevel(agg) <= 0;
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
	public void setSourceId(String sourceId) {
		this.sourceIds = new String[] { sourceId };
	}

	/**
	 * Get the first location ID.
	 * 
	 * <p>
	 * This returns the first available source ID from the {@code locationIds}
	 * array, or <em>null</em> if not available.
	 * </p>
	 * 
	 * @return the first node ID
	 */
	public Long getLocationId() {
		return this.locationIds == null || this.locationIds.length < 1 ? null : this.locationIds[0];
	}

	/**
	 * Set a single location ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single location ID
	 * at a time. The location ID is still stored on the {@code locationIds}
	 * array, just as the first value. Calling this method replaces any existing
	 * {@code locationIds} value with a new array containing just the ID passed
	 * into this method.
	 * </p>
	 * 
	 * @param nodeId
	 *        the ID of the node
	 */
	public void setLocationId(Long locationId) {
		this.locationIds = new Long[] { locationId };
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
	public String getSourceId() {
		return this.sourceIds == null || this.sourceIds.length < 1 ? null : this.sourceIds[0];
	}

	/**
	 * Set a property value.
	 * 
	 * <p>
	 * This will replace any existing property value for the same key.
	 * </p>
	 * 
	 * @param key
	 *        the key
	 * @param value
	 *        the value
	 */
	public void setProperty(String key, Object value) {
		if ( this.properties == null ) {
			this.properties = new LinkedHashMap<String, Object>();
		}
		this.properties.put(key, value);
	}

	/**
	 * Set the datum type.
	 * 
	 * <p>
	 * This is an alias for {@link #setDatumType(String)}.
	 * </p>
	 * 
	 * @see #setDatumType(String)
	 * @param datumType
	 */
	public void setType(String datumType) {
		setDatumType(datumType);
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public Long[] getNodeIds() {
		return nodeIds;
	}

	public String[] getSourceIds() {
		return sourceIds;
	}

	public void setSourceIds(String[] sourceIds) {
		this.sourceIds = sourceIds;
	}

	public void setNodeIds(Long[] nodeIds) {
		this.nodeIds = nodeIds;
	}

	public DateTime getStartDate() {
		return startDate;
	}

	public void setStartDate(DateTime startDate) {
		this.startDate = startDate;
	}

	public DateTime getEndDate() {
		return endDate;
	}

	public void setEndDate(DateTime endDate) {
		this.endDate = endDate;
	}

	public Integer getPrecision() {
		return precision;
	}

	public void setPrecision(Integer precision) {
		this.precision = precision;
	}

	public Aggregation getAggregate() {
		return aggregate;
	}

	public void setAggregate(Aggregation aggregate) {
		this.aggregate = aggregate;
	}

	public Long[] getLocationIds() {
		return locationIds;
	}

	public void setLocationIds(Long[] locationIds) {
		this.locationIds = locationIds;
	}

	public boolean isMostRecent() {
		return mostRecent;
	}

	public void setMostRecent(boolean mostRecent) {
		this.mostRecent = mostRecent;
	}

	public Integer getResultOffset() {
		return resultOffset;
	}

	public void setResultOffset(Integer resultOffset) {
		this.resultOffset = resultOffset;
	}

	public Integer getResultMax() {
		return resultMax;
	}

	public void setResultMax(Integer resultMax) {
		this.resultMax = resultMax;
	}

	public String getDatumType() {
		return datumType;
	}

	public void setDatumType(String datumType) {
		this.datumType = datumType;
	}

}
