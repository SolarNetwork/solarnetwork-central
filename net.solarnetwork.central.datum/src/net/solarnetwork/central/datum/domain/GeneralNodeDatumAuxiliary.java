/* ==================================================================
 * GeneralNodeDatumAuxiliary.java - 1/02/2019 5:08:04 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import org.joda.time.DateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.util.SerializeIgnore;

/**
 * An "auxiliary" node datum entity, which contains "final" and "starting"
 * before/after samples at a specific point in time for a node data stream.
 * 
 * @author matt
 * @version 1.0
 * @since 1.35
 */
@JsonPropertyOrder({ "created", "nodeId", "sourceId", "type" })
public class GeneralNodeDatumAuxiliary
		implements Entity<GeneralNodeDatumAuxiliaryPK>, Cloneable, Serializable {

	private static final long serialVersionUID = 4458346623126424619L;

	private GeneralNodeDatumAuxiliaryPK id = new GeneralNodeDatumAuxiliaryPK();
	private GeneralNodeDatumSamples samplesFinal;
	private String sampleJsonFinal;
	private GeneralNodeDatumSamples samplesStart;
	private String sampleJsonStart;

	/**
	 * Convenience getter for {@link GeneralNodeDatumPK#getNodeId()}.
	 * 
	 * @return the nodeId
	 */
	public Long getNodeId() {
		return (id == null ? null : id.getNodeId());
	}

	/**
	 * Convenience setter for
	 * {@link GeneralNodeDatumAuxiliaryPK#setNodeId(Long)}.
	 * 
	 * @param nodeId
	 *        the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		if ( id == null ) {
			id = new GeneralNodeDatumAuxiliaryPK();
		}
		id.setNodeId(nodeId);
	}

	/**
	 * Convenience getter for {@link GeneralNodeDatumAuxiliaryPK#getSourceId()}.
	 * 
	 * @return the sourceId
	 */
	public String getSourceId() {
		return (id == null ? null : id.getSourceId());
	}

	/**
	 * Convenience setter for
	 * {@link GeneralNodeDatumAuxiliaryPK#setSourceId(String)}.
	 * 
	 * @param sourceId
	 *        the sourceId to set
	 */
	public void setSourceId(String sourceId) {
		if ( id == null ) {
			id = new GeneralNodeDatumAuxiliaryPK();
		}
		id.setSourceId(sourceId);
	}

	/**
	 * Convenience setter for
	 * {@link GeneralNodeDatumAuxiliaryPK#setCreated(DateTime)}.
	 * 
	 * @param created
	 *        the created to set
	 */
	public void setCreated(DateTime created) {
		if ( id == null ) {
			id = new GeneralNodeDatumAuxiliaryPK();
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
	public GeneralNodeDatumAuxiliaryPK getId() {
		return id;
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
	public int compareTo(GeneralNodeDatumAuxiliaryPK o) {
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
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof GeneralNodeDatumAuxiliary) ) {
			return false;
		}
		GeneralNodeDatumAuxiliary other = (GeneralNodeDatumAuxiliary) obj;
		return Objects.equals(id, other.id);
	}

	/**
	 * Get the final {@link GeneralNodeDatumSamples} object as a JSON string.
	 * 
	 * <p>
	 * This method will ignore {@literal null} values.
	 * </p>
	 * 
	 * @return a JSON encoded string, never {@literal null}
	 */
	@SerializeIgnore
	@JsonIgnore
	public String getSampleJsonFinal() {
		if ( sampleJsonFinal == null ) {
			sampleJsonFinal = DatumUtils.getJSONString(samplesFinal, "{}");
		}
		return sampleJsonFinal;
	}

	/**
	 * Set the final {@link GeneralNodeDatumSamples} object via a JSON string.
	 * 
	 * <p>
	 * This method will remove any previously created final
	 * GeneralNodeDatumSamples and replace it with the values parsed from the
	 * JSON. All floating point values will be converted to {@link BigDecimal}
	 * instances.
	 * </p>
	 * 
	 * @param json
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setSampleJsonFinal(String json) {
		sampleJsonFinal = json;
		samplesFinal = null;
	}

	@SerializeIgnore
	@JsonIgnore
	public GeneralNodeDatumSamples getSamplesFinal() {
		if ( samplesFinal == null && sampleJsonFinal != null ) {
			samplesFinal = DatumUtils.getObjectFromJSON(sampleJsonFinal, GeneralNodeDatumSamples.class);
		}
		return samplesFinal;
	}

	/**
	 * Set the final {@link GeneralNodeDatumSamples} instance to use.
	 * 
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setSampleJsonFinal(String)} as well.
	 * </p>
	 * 
	 * @param samples
	 *        the samples instance to set
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setSamplesFinal(GeneralNodeDatumSamples samples) {
		samplesFinal = samples;
		sampleJsonFinal = null;
	}

	/**
	 * Convenience method for final
	 * {@link GeneralNodeDatumSamples#getSampleData()}.
	 * 
	 * @return the sample data, or {@literal null} if none available
	 */
	@JsonProperty("final")
	public Map<String, ?> getSampleDataFinal() {
		GeneralNodeDatumSamples s = getSamplesFinal();
		return (s == null ? null : s.getSampleData());
	}

	/**
	 * Get the start {@link GeneralNodeDatumSamples} object as a JSON string.
	 * 
	 * <p>
	 * This method will ignore {@literal null} values.
	 * </p>
	 * 
	 * @return a JSON encoded string, never {@literal null}
	 */
	@SerializeIgnore
	@JsonIgnore
	public String getSampleJsonStart() {
		if ( sampleJsonStart == null ) {
			sampleJsonStart = DatumUtils.getJSONString(samplesStart, "{}");
		}
		return sampleJsonStart;
	}

	/**
	 * Set the start {@link GeneralNodeDatumSamples} object via a JSON string.
	 * 
	 * <p>
	 * This method will remove any previously created start
	 * GeneralNodeDatumSamples and replace it with the values parsed from the
	 * JSON. All floating point values will be converted to {@link BigDecimal}
	 * instances.
	 * </p>
	 * 
	 * @param json
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setSampleJsonStart(String json) {
		sampleJsonStart = json;
		samplesStart = null;
	}

	@SerializeIgnore
	@JsonIgnore
	public GeneralNodeDatumSamples getSamplesStart() {
		if ( samplesStart == null && sampleJsonStart != null ) {
			samplesStart = DatumUtils.getObjectFromJSON(sampleJsonStart, GeneralNodeDatumSamples.class);
		}
		return samplesStart;
	}

	/**
	 * Set the start {@link GeneralNodeDatumSamples} instance to use.
	 * 
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setSampleJsonStart(String)} as well.
	 * </p>
	 * 
	 * @param samples
	 *        the samples instance to set
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setSamplesStart(GeneralNodeDatumSamples samples) {
		samplesStart = samples;
		sampleJsonStart = null;
	}

	/**
	 * Convenience method for final
	 * {@link GeneralNodeDatumSamples#getSampleData()}.
	 * 
	 * @return the sample data, or {@literal null} if none available
	 */
	@JsonProperty("start")
	public Map<String, ?> getSampleDataStart() {
		GeneralNodeDatumSamples s = getSamplesStart();
		return (s == null ? null : s.getSampleData());
	}

}
