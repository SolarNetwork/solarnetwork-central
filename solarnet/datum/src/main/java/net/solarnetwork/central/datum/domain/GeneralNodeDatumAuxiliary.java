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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.SerializeIgnore;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * An "auxiliary" node datum entity, which contains "final" and "starting"
 * before/after samples at a specific point in time for a node data stream.
 *
 * @author matt
 * @version 2.0
 * @since 1.35
 */
@JsonPropertyOrder({ "created", "nodeId", "sourceId", "type", "updated", "notes", "final", "start",
		"meta" })
public class GeneralNodeDatumAuxiliary
		implements Entity<GeneralNodeDatumAuxiliaryPK>, Cloneable, Serializable {

	@Serial
	private static final long serialVersionUID = -1132588952509774037L;

	private final GeneralNodeDatumAuxiliaryPK id;
	private @Nullable Instant updated;
	private @Nullable DatumSamples samplesFinal;
	private @Nullable String sampleJsonFinal;
	private @Nullable DatumSamples samplesStart;
	private @Nullable String sampleJsonStart;
	private @Nullable String notes;
	private @Nullable GeneralDatumMetadata meta;
	private @Nullable String metaJson;

	/**
	 * Default constructor.
	 */
	@JsonCreator
	public GeneralNodeDatumAuxiliary() {
		this(new GeneralNodeDatumAuxiliaryPK(), null, null);
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
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@code null}
	 */
	public GeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliaryPK id, @Nullable DatumSamples samplesFinal,
			@Nullable DatumSamples samplesStart) {
		super();
		this.id = requireNonNullArgument(id, "id");
		setSamplesFinal(samplesFinal);
		setSamplesStart(samplesStart);
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
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( (obj == null) || !(obj instanceof GeneralNodeDatumAuxiliary other) ) {
			return false;
		}
		return Objects.equals(id, other.id);
	}

	/**
	 * Convenience getter for {@link GeneralNodeDatumPK#getNodeId()}.
	 *
	 * @return the nodeId
	 */
	public final @Nullable Long getNodeId() {
		return id.getNodeId();
	}

	/**
	 * Convenience setter for
	 * {@link GeneralNodeDatumAuxiliaryPK#setNodeId(Long)}.
	 *
	 * @param nodeId
	 *        the nodeId to set
	 */
	public final void setNodeId(@Nullable Long nodeId) {
		id.setNodeId(nodeId);
	}

	/**
	 * Convenience getter for {@link GeneralNodeDatumAuxiliaryPK#getSourceId()}.
	 *
	 * @return the sourceId
	 */
	public final @Nullable String getSourceId() {
		return id.getSourceId();
	}

	/**
	 * Convenience setter for
	 * {@link GeneralNodeDatumAuxiliaryPK#setSourceId(String)}.
	 *
	 * @param sourceId
	 *        the sourceId to set
	 */
	public final void setSourceId(@Nullable String sourceId) {
		id.setSourceId(sourceId);
	}

	/**
	 * Convenience setter for
	 * {@link GeneralNodeDatumAuxiliaryPK#setCreated(Instant)}.
	 *
	 * @param created
	 *        the created to set
	 */
	public final void setCreated(@Nullable Instant created) {
		id.setCreated(created);
	}

	@Override
	public final @Nullable Instant getCreated() {
		return id.getCreated();
	}

	/**
	 * Convenience getter for {@link GeneralNodeDatumAuxiliaryPK#getType()}.
	 *
	 * @return the type
	 */
	public final @Nullable DatumAuxiliaryType getType() {
		return id.getType();
	}

	/**
	 * Convenience setter for
	 * {@link GeneralNodeDatumAuxiliaryPK#setType(DatumAuxiliaryType)}.
	 *
	 * @param type
	 *        the type to set
	 */
	public final void setType(@Nullable DatumAuxiliaryType type) {
		id.setType(type);
	}

	public final @Nullable Instant getUpdated() {
		return updated;
	}

	public final void setUpdated(@Nullable Instant updated) {
		this.updated = updated;
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public final GeneralNodeDatumAuxiliaryPK getId() {
		return id;
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
	public final @Nullable String getSampleJsonFinal() {
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
	public final void setSampleJsonFinal(@Nullable String json) {
		sampleJsonFinal = json;
		samplesFinal = null;
	}

	@JsonProperty("final")
	public final @Nullable DatumSamples getSamplesFinal() {
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
	public final void setSamplesFinal(@Nullable DatumSamples samples) {
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
	public final @Nullable Map<String, ?> getSampleDataFinal() {
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
	public final @Nullable String getSampleJsonStart() {
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
	public final void setSampleJsonStart(@Nullable String json) {
		sampleJsonStart = json;
		samplesStart = null;
	}

	@JsonProperty("start")
	public final @Nullable DatumSamples getSamplesStart() {
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
	public final void setSamplesStart(@Nullable DatumSamples samples) {
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
	public final @Nullable Map<String, ?> getSampleDataStart() {
		DatumSamples s = getSamplesStart();
		return (s == null ? null : s.getSampleData());
	}

	/**
	 * Get the notes.
	 *
	 * @return the notes
	 */
	public final @Nullable String getNotes() {
		return notes;
	}

	/**
	 * Set the notes.
	 *
	 * @param notes
	 *        the notes
	 */
	public final void setNotes(@Nullable String notes) {
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
	public final @Nullable GeneralDatumMetadata getMeta() {
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
	public final void setMeta(@Nullable GeneralDatumMetadata meta) {
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
	public final @Nullable String getMetaJson() {
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
	public final void setMetaJson(@Nullable String metaJson) {
		this.metaJson = metaJson;
		this.meta = null;
	}
}
