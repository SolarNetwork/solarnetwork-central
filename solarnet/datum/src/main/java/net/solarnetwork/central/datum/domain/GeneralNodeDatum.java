/* ==================================================================
 * GeneralNodeDatum.java - Aug 22, 2014 6:07:02 AM
 *
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.SerializeIgnore;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * Generalized node-based datum.
 *
 * <p>
 * <b>Note</b> that {@link DatumUtils#getObjectFromJSON(String, Class)} is used
 * to manage the JSON value passed to {@link #setSampleJson(String)}.
 * </p>
 *
 * @author matt
 * @version 3.0
 */
@JsonPropertyOrder({ "created", "nodeId", "sourceId" })
public class GeneralNodeDatum implements Entity<GeneralNodeDatumPK>, Cloneable, Serializable,
		GeneralObjectDatum<GeneralNodeDatumPK> {

	@Serial
	private static final long serialVersionUID = -3840727299179538235L;

	private GeneralNodeDatumPK id = new GeneralNodeDatumPK();
	private DatumSamples samples;
	private Instant posted;
	private String sampleJson;

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
	 * Convenience setter for {@link GeneralNodeDatumPK#setCreated(Instant)}.
	 *
	 * @param created
	 *        the created to set
	 */
	public void setCreated(Instant created) {
		if ( id == null ) {
			id = new GeneralNodeDatumPK();
		}
		id.setCreated(created);
	}

	@Override
	public Instant getCreated() {
		return (id == null ? null : id.getCreated());
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public GeneralNodeDatumPK getId() {
		return id;
	}

	/**
	 * Convenience method for {@link DatumSamples#getSampleData()}.
	 *
	 * @return the sample data, or <em>null</em> if none available
	 */
	@JsonUnwrapped
	@JsonAnyGetter
	public Map<String, ?> getSampleData() {
		DatumSamples s = getSamples();
		return (s == null ? null : s.getSampleData());
	}

	@Override
	public GeneralNodeDatum clone() {
		try {
			return (GeneralNodeDatum) super.clone();
		} catch ( CloneNotSupportedException e ) {
			// should never get here
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@SuppressWarnings("EqualsGetClass")
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( (obj == null) || (getClass() != obj.getClass()) ) {
			return false;
		}
		GeneralNodeDatum other = (GeneralNodeDatum) obj;
		if ( id == null ) {
			return other.id == null;
		}
		return id.equals(other.id);
	}

	/**
	 * Get the {@link DatumSamples} object as a JSON string.
	 *
	 * <p>
	 * This method will ignore <em>null</em> values.
	 * </p>
	 *
	 * @return a JSON encoded string, never <em>null</em>
	 */
	@SerializeIgnore
	@JsonIgnore
	public String getSampleJson() {
		if ( sampleJson == null ) {
			sampleJson = DatumUtils.getJSONString(samples, "{}");
		}
		return sampleJson;
	}

	/**
	 * Set the {@link DatumSamples} object via a JSON string.
	 *
	 * <p>
	 * This method will remove any previously created GeneralNodeDatumSamples
	 * and replace it with the values parsed from the JSON. All floating point
	 * values will be converted to {@link BigDecimal} instances.
	 * </p>
	 *
	 * @param json
	 *        the JSON to set
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setSampleJson(String json) {
		sampleJson = json;
		samples = null;
	}

	@SerializeIgnore
	@JsonIgnore
	public Instant getPosted() {
		return posted;
	}

	public void setPosted(Instant posted) {
		this.posted = posted;
	}

	@Override
	@SerializeIgnore
	@JsonIgnore
	public DatumSamples getSamples() {
		if ( samples == null && sampleJson != null ) {
			samples = DatumUtils.getObjectFromJSON(sampleJson, DatumSamples.class);
		}
		return samples;
	}

	/**
	 * Set the {@link DatumSamples} instance to use.
	 *
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setSampleJson(String)} as well.
	 * </p>
	 *
	 * @param samples
	 *        the samples instance to set
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setSamples(DatumSamples samples) {
		this.samples = samples;
		sampleJson = null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GeneralNodeDatum{id=");
		builder.append(id);
		builder.append(", samples=");
		builder.append(samples == null ? "null" : samples.getSampleData());
		builder.append("}");
		return builder.toString();
	}

}
