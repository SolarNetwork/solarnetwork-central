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
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.domain.SerializeIgnore;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * An "auxiliary" node datum entity, which contains "final" and "starting"
 * before/after samples at a specific point in time for a node data stream.
 * 
 * @author matt
 * @version 1.2
 * @since 1.35
 */
@JsonPropertyOrder({ "created", "nodeId", "sourceId", "type", "updated", "notes", "final", "start",
		"meta" })
public class GeneralNodeDatumAuxiliary
		implements Entity<GeneralNodeDatumAuxiliaryPK>, Cloneable, Serializable {

	private static final long serialVersionUID = -1132588952509774037L;

	private GeneralNodeDatumAuxiliaryPK id = new GeneralNodeDatumAuxiliaryPK();
	private Instant updated;
	private DatumSamples samplesFinal;
	private String sampleJsonFinal;
	private DatumSamples samplesStart;
	private String sampleJsonStart;
	private String notes;
	private GeneralDatumMetadata meta;
	private String metaJson;

	/**
	 * Default constructor.
	 */
	public GeneralNodeDatumAuxiliary() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param samplesFinal
	 *        the final samples
	 * @param samplesStart
	 *        the starting samples
	 */
	public GeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliaryPK id, DatumSamples samplesFinal,
			DatumSamples samplesStart) {
		super();
		setId(id);
		setSamplesFinal(samplesFinal);
		setSamplesStart(samplesStart);
	}

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
	 * {@link GeneralNodeDatumAuxiliaryPK#setCreated(Instant)}.
	 * 
	 * @param created
	 *        the created to set
	 */
	public void setCreated(Instant created) {
		if ( id == null ) {
			id = new GeneralNodeDatumAuxiliaryPK();
		}
		id.setCreated(created);
	}

	@Override
	public Instant getCreated() {
		return (id == null ? null : id.getCreated());
	}

	/**
	 * Convenience getter for {@link GeneralNodeDatumAuxiliaryPK#getType()}.
	 * 
	 * @return the type
	 */
	public DatumAuxiliaryType getType() {
		return (id == null ? null : id.getType());
	}

	/**
	 * Convenience setter for
	 * {@link GeneralNodeDatumAuxiliaryPK#setType(DatumAuxiliaryType)}.
	 * 
	 * @param type
	 *        the type to set
	 */
	public void setType(DatumAuxiliaryType type) {
		if ( id == null ) {
			id = new GeneralNodeDatumAuxiliaryPK();
		}
		id.setType(type);
	}

	public Instant getUpdated() {
		return updated;
	}

	public void setUpdated(Instant updated) {
		this.updated = updated;
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public GeneralNodeDatumAuxiliaryPK getId() {
		return id;
	}

	public void setId(GeneralNodeDatumAuxiliaryPK id) {
		this.id = id;
	}

	@Override
	public GeneralNodeDatumAuxiliary clone() {
		try {
			return (GeneralNodeDatumAuxiliary) super.clone();
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
	 * Get the final {@link DatumSamples} object as a JSON string.
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
	 * Set the final {@link DatumSamples} object via a JSON string.
	 * 
	 * <p>
	 * This method will remove any previously created final DatumSamples and
	 * replace it with the values parsed from the JSON. All floating point
	 * values will be converted to {@link BigDecimal} instances.
	 * </p>
	 * 
	 * @param json
	 *        the JSON to set
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setSampleJsonFinal(String json) {
		sampleJsonFinal = json;
		samplesFinal = null;
	}

	@JsonProperty("final")
	public DatumSamples getSamplesFinal() {
		if ( samplesFinal == null && sampleJsonFinal != null ) {
			samplesFinal = DatumUtils.getObjectFromJSON(sampleJsonFinal, DatumSamples.class);
		}
		return samplesFinal;
	}

	/**
	 * Set the final {@link DatumSamples} instance to use.
	 * 
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setSampleJsonFinal(String)} as well.
	 * </p>
	 * 
	 * @param samples
	 *        the samples instance to set
	 */
	@JsonProperty("final")
	public void setSamplesFinal(DatumSamples samples) {
		samplesFinal = samples;
		sampleJsonFinal = null;
	}

	/**
	 * Convenience method for final {@link DatumSamples#getSampleData()}.
	 * 
	 * @return the sample data, or {@literal null} if none available
	 */
	@SerializeIgnore
	@JsonIgnore
	public Map<String, ?> getSampleDataFinal() {
		DatumSamples s = getSamplesFinal();
		return (s == null ? null : s.getSampleData());
	}

	/**
	 * Get the start {@link DatumSamples} object as a JSON string.
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
	 * Set the start {@link DatumSamples} object via a JSON string.
	 * 
	 * <p>
	 * This method will remove any previously created start DatumSamples and
	 * replace it with the values parsed from the JSON. All floating point
	 * values will be converted to {@link BigDecimal} instances.
	 * </p>
	 * 
	 * @param json
	 *        the JSON to set
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setSampleJsonStart(String json) {
		sampleJsonStart = json;
		samplesStart = null;
	}

	@JsonProperty("start")
	public DatumSamples getSamplesStart() {
		if ( samplesStart == null && sampleJsonStart != null ) {
			samplesStart = DatumUtils.getObjectFromJSON(sampleJsonStart, DatumSamples.class);
		}
		return samplesStart;
	}

	/**
	 * Set the start {@link DatumSamples} instance to use.
	 * 
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setSampleJsonStart(String)} as well.
	 * </p>
	 * 
	 * @param samples
	 *        the samples instance to set
	 */
	@JsonProperty("start")
	public void setSamplesStart(DatumSamples samples) {
		samplesStart = samples;
		sampleJsonStart = null;
	}

	/**
	 * Convenience method for final {@link DatumSamples#getSampleData()}.
	 * 
	 * @return the sample data, or {@literal null} if none available
	 */
	@JsonIgnore
	@SerializeIgnore
	public Map<String, ?> getSampleDataStart() {
		DatumSamples s = getSamplesStart();
		return (s == null ? null : s.getSampleData());
	}

	/**
	 * Get the notes.
	 * 
	 * @return the notes
	 */
	public String getNotes() {
		return notes;
	}

	/**
	 * Set the notes.
	 * 
	 * @param notes
	 *        the notes
	 */
	public void setNotes(String notes) {
		this.notes = notes;
	}

	/**
	 * Get the metadata.
	 * 
	 * <p>
	 * This will parse the {code metaJson} property if that is available.
	 * </p>
	 * 
	 * @return the metadata
	 * @since 1.1
	 */
	public GeneralDatumMetadata getMeta() {
		if ( meta == null && metaJson != null ) {
			meta = DatumUtils.getObjectFromJSON(metaJson, GeneralDatumMetadata.class);
			metaJson = null; // clear this out, because we might mutate meta and invalidate our cached JSON value
		}
		return meta;
	}

	/**
	 * Set the metadata.
	 * 
	 * <p>
	 * This will clear the {code metaJson} property.
	 * </p>
	 * 
	 * @param meta
	 *        the metadata to set
	 * @since 1.1
	 */
	public void setMeta(GeneralDatumMetadata meta) {
		this.meta = meta;
		this.metaJson = null;
	}

	/**
	 * Get the metadata as JSON.
	 * 
	 * <p>
	 * This will serialize the {@code meta} property if that is available.
	 * </p>
	 * 
	 * @return the metadata JSON
	 * @since 1.1
	 */
	@JsonIgnore
	@SerializeIgnore
	public String getMetaJson() {
		if ( metaJson == null ) {
			metaJson = DatumUtils.getJSONString(meta, "{}");
			meta = null; // clear this out, because we might otherwise mutate it and invalidate our cached JSON value
		}
		return metaJson;
	}

	/**
	 * Set the metadata as JSON.
	 * 
	 * <p>
	 * This will clear the {@code meta} property if that is available.
	 * </p>
	 * 
	 * @param metaJson
	 *        the metadata
	 */
	public void setMetaJson(String metaJson) {
		this.metaJson = metaJson;
		this.meta = null;
	}
}
