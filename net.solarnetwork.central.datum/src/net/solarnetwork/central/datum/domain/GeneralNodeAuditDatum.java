/* ==================================================================
 * GeneralNodeAuditDatum.java - 22/08/2017 9:07:21 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTime;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.util.JsonUtils;
import net.solarnetwork.util.SerializeIgnore;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.22
 */
public class GeneralNodeAuditDatum implements Entity<GeneralNodeDatumPK>, Cloneable, Serializable {

	private static final long serialVersionUID = 1305538772604689957L;

	/** Property key for a {@code Number} based count value. */
	public static final String COUNT_KEY = "count";

	private GeneralNodeDatumPK id = new GeneralNodeDatumPK();
	private Map<String, Object> auditData;
	private String auditDataJson;

	/**
	 * Convenience getter for {@link GeneralNodeDatumPK#getNodeId()}.
	 * 
	 * @return the nodeId
	 */
	public Long getNodeId() {
		return (id == null ? null : id.getNodeId());
	}

	/**
	 * Convenience setter for {@link GeneralNodeDatumPK#setNodeId(Long)}.
	 * 
	 * @param nodeId
	 *        the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		if ( id == null ) {
			id = new GeneralNodeDatumPK();
		}
		id.setNodeId(nodeId);
	}

	/**
	 * Convenience getter for {@link GeneralNodeDatumPK#getSourceId()}.
	 * 
	 * @return the sourceId
	 */
	public String getSourceId() {
		return (id == null ? null : id.getSourceId());
	}

	/**
	 * Convenience setter for {@link GeneralNodeDatumPK#setSourceId(String)}.
	 * 
	 * @param sourceId
	 *        the sourceId to set
	 */
	public void setSourceId(String sourceId) {
		if ( id == null ) {
			id = new GeneralNodeDatumPK();
		}
		id.setSourceId(sourceId);
	}

	/**
	 * Convenience setter for {@link GeneralNodeDatumPK#setCreated(DateTime)}.
	 * 
	 * @param created
	 *        the created to set
	 */
	public void setCreated(DateTime created) {
		if ( id == null ) {
			id = new GeneralNodeDatumPK();
		}
		id.setCreated(created);
	}

	@Override
	public DateTime getCreated() {
		return (id == null ? null : id.getCreated());
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public GeneralNodeDatumPK getId() {
		return id;
	}

	/**
	 * Get the {@literal count} data property.
	 * 
	 * @return the {@literal count} value, or {@code null} if not available
	 */
	@SerializeIgnore
	@JsonIgnore
	public Number getCount() {
		Map<String, Object> data = getAuditData();
		Object o = (data != null ? data.get(COUNT_KEY) : null);
		return (o instanceof Number ? (Number) o : null);
	}

	/**
	 * Set the {@literal count} data property.
	 * 
	 * @param value
	 *        the value to set
	 */
	@JsonProperty
	public void setCount(Number value) {
		Map<String, Object> data = getAuditData();
		if ( data == null ) {
			data = new HashMap<String, Object>(4);
			setAuditData(data);
		}
		data.put(COUNT_KEY, value);
	}

	/**
	 * Convenience method for {@link GeneralNodeDatumSamples#getSampleData()}.
	 * 
	 * @return the sample data, or <em>null</em> if none available
	 */
	@JsonUnwrapped
	@JsonAnyGetter
	public Map<String, Object> getAuditData() {
		if ( auditData == null && auditDataJson != null ) {
			auditData = JsonUtils.getStringMap(auditDataJson);
		}
		return auditData;
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch ( CloneNotSupportedException e ) {
			// should never get here
			throw new RuntimeException(e);
		}
	}

	@Override
	public int compareTo(GeneralNodeDatumPK o) {
		if ( id == null && o == null ) {
			return 0;
		}
		if ( id == null ) {
			return -1;
		}
		if ( o == null ) {
			return 1;
		}
		return id.compareTo(o);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		GeneralNodeAuditDatum other = (GeneralNodeAuditDatum) obj;
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		} else if ( !id.equals(other.id) ) {
			return false;
		}
		return true;
	}

	/**
	 * Get the {@link GeneralNodeDatumSamples} object as a JSON string.
	 * 
	 * <p>
	 * This method will ignore <em>null</em> values.
	 * </p>
	 * 
	 * @return a JSON encoded string, never <em>null</em>
	 */
	@SerializeIgnore
	@JsonIgnore
	public String getAuditDataJson() {
		if ( auditDataJson == null ) {
			auditDataJson = DatumUtils.getJSONString(auditData, null);
		}
		return auditDataJson;
	}

	/**
	 * Set the {@code auditData} object via a JSON string.
	 * 
	 * <p>
	 * This method will remove any previously created value and replace it with
	 * the values parsed from the JSON.
	 * </p>
	 * 
	 * @param json
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setAuditDataJson(String json) {
		auditDataJson = json;
		auditData = null;
	}

	/**
	 * Set the audit data to use.
	 * 
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setAuditDataJson(String)} as well.
	 * </p>
	 * 
	 * @param auditData
	 *        the data to set
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setAuditData(Map<String, Object> auditData) {
		this.auditData = auditData;
		auditDataJson = null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GeneralNodeAuditDatum{id=");
		builder.append(id);
		builder.append(", data=");
		builder.append(auditData);
		builder.append("}");
		return builder.toString();
	}

}
