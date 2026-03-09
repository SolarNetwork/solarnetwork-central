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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.CopyingIdentity;
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
		GeneralObjectDatum<GeneralNodeDatumPK>, CopyingIdentity<GeneralNodeDatum, GeneralNodeDatumPK> {

	@Serial
	private static final long serialVersionUID = -3840727299179538235L;

	private final GeneralNodeDatumPK id;
	private @Nullable DatumSamples samples;
	private @Nullable Instant posted;
	private @Nullable String sampleJson;

	/**
	 * Constructor.
	 *
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public GeneralNodeDatum(GeneralNodeDatumPK id) {
		super();
		this.id = requireNonNullArgument(id, "id");
	}

	/**
	 * Constructor.
	 *
	 * @param nodeId
	 *        the node ID
	 * @param created
	 *        the creation date
	 * @param sourceId
	 *        the source ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	@JsonCreator
	public GeneralNodeDatum(@JsonProperty("nodeId") Long nodeId,
			@JsonProperty("created") Instant created, @JsonProperty("sourceId") String sourceId) {
		this(new GeneralNodeDatumPK(nodeId, created, sourceId));
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

	@Override
	public GeneralNodeDatum copyWithId(GeneralNodeDatumPK id) {
		GeneralNodeDatum other = new GeneralNodeDatum(id);
		copyTo(other);
		return other;
	}

	@Override
	public void copyTo(GeneralNodeDatum other) {
		other.posted = posted;
		other.sampleJson = sampleJson;
		other.samples = (samples != null ? new DatumSamples(samples) : new DatumSamples());
	}

	/**
	 * Convenience method for {@link DatumSamples#getSampleData()}.
	 *
	 * @return the sample data, or {@code null} if none available
	 */
	@JsonUnwrapped
	@JsonAnyGetter
	public @Nullable Map<String, ?> getSampleData() {
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
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( (obj == null) || (getClass() != obj.getClass()) ) {
			return false;
		}
		GeneralNodeDatum other = (GeneralNodeDatum) obj;
		return id.equals(other.id);
	}

	/**
	 * Convenience getter for {@link GeneralNodeDatumPK#getNodeId()}.
	 *
	 * @return the nodeId
	 */
	public final Long getNodeId() {
		return id.getNodeId();
	}

	/**
	 * Convenience getter for {@link GeneralNodeDatumPK#getSourceId()}.
	 *
	 * @return the sourceId
	 */
	public final String getSourceId() {
		return id.getSourceId();
	}

	@Override
	public final Instant getCreated() {
		return id.getCreated();
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public final GeneralNodeDatumPK getId() {
		return id;
	}

	/**
	 * Get the {@link DatumSamples} object as a JSON string.
	 *
	 * <p>
	 * This method will ignore {@code null} values.
	 * </p>
	 *
	 * @return a JSON encoded string, never {@code null}
	 */
	@SerializeIgnore
	@JsonIgnore
	public final @Nullable String getSampleJson() {
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
	public final void setSampleJson(@Nullable String json) {
		sampleJson = json;
		samples = null;
	}

	@SerializeIgnore
	@JsonIgnore
	public final @Nullable Instant getPosted() {
		return posted;
	}

	public final void setPosted(@Nullable Instant posted) {
		this.posted = posted;
	}

	@Override
	@SerializeIgnore
	@JsonIgnore
	public final DatumSamples getSamples() {
		if ( samples == null && sampleJson != null ) {
			samples = DatumUtils.getObjectFromJSON(sampleJson, DatumSamples.class);
		}
		if ( samples == null ) {
			samples = new DatumSamples();
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
	public final void setSamples(@Nullable DatumSamples samples) {
		this.samples = samples;
		sampleJson = null;
	}

}
