/* ==================================================================
 * GeneralLocationDatumMetadata.java - Oct 17, 2014 3:02:19 PM
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
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.SerializeIgnore;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Metadata about the {@link GeneralLocationDatum} associated with a specific
 * location and source.
 *
 * <p>
 * <b>Note</b> that {@link DatumUtils#getObjectFromJSON(String, Class)} is used
 * to manage the JSON value passed to {@link #setMetaJson(String)}.
 * </p>
 *
 * @author matt
 * @version 3.0
 */
@JsonPropertyOrder({ "created", "updated", "locationId", "sourceId", "m", "t" })
public class GeneralLocationDatumMetadata implements Entity<LocationSourcePK>, Cloneable, Serializable {

	@Serial
	private static final long serialVersionUID = 7692101091820630679L;

	private final LocationSourcePK id = new LocationSourcePK();
	private @Nullable Instant created;
	private @Nullable Instant updated;
	private @Nullable GeneralDatumMetadata meta;
	private @Nullable String metaJson;

	private @Nullable SolarLocation location;

	@Override
	public GeneralLocationDatumMetadata clone() {
		try {
			return (GeneralLocationDatumMetadata) super.clone();
		} catch ( CloneNotSupportedException e ) {
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

	@Override
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof GeneralLocationDatumMetadata other) ) {
			return false;
		}
		return id.equals(other.id);
	}

	/**
	 * Convenience getter for {@link LocationSourcePK#getLocationId()}.
	 *
	 * @return the locationId
	 */
	public final @Nullable Long getLocationId() {
		return id.getLocationId();
	}

	/**
	 * Convenience setter for {@link LocationSourcePK#setLocationId(Long)}.
	 *
	 * @param locationId
	 *        the locationId to set
	 */
	public final void setLocationId(@Nullable Long locationId) {
		id.setLocationId(locationId);
	}

	/**
	 * Convenience getter for {@link LocationSourcePK#getSourceId()}.
	 *
	 * @return the sourceId
	 */
	public final @Nullable String getSourceId() {
		return id.getSourceId();
	}

	/**
	 * Convenience setter for {@link LocationSourcePK#setSourceId(String)}.
	 *
	 * @param sourceId
	 *        the sourceId to set
	 */
	public final void setSourceId(@Nullable String sourceId) {
		id.setSourceId(sourceId);
	}

	/**
	 * Alternative for {@link #getMeta()}. This method exists so that we can
	 * configure {@code @JsonUnwrapped} on our {@link GeneralDatumMetadata} but
	 * still support setting it in a normal, wrapped fashion via
	 * {@link #setMeta(GeneralDatumMetadata)}.
	 *
	 * @return GeneralDatumMetadata
	 */
	@JsonUnwrapped
	public final @Nullable GeneralDatumMetadata getMetadata() {
		return getMeta();
	}

	@JsonIgnore
	@SerializeIgnore
	public final @Nullable GeneralDatumMetadata getMeta() {
		if ( meta == null && metaJson != null ) {
			meta = DatumUtils.getObjectFromJSON(metaJson, GeneralDatumMetadata.class);
			metaJson = null; // clear this out, because we might mutate meta and invalidate our cached JSON value
		}
		return meta;
	}

	@JsonProperty
	public final void setMeta(@Nullable GeneralDatumMetadata meta) {
		this.meta = meta;
		this.metaJson = null;
	}

	@JsonIgnore
	@SerializeIgnore
	public final @Nullable String getMetaJson() {
		if ( metaJson == null ) {
			metaJson = DatumUtils.getJSONString(meta, "{}");
			meta = null; // clear this out, because we might otherwise mutate it and invalidate our cached JSON value
		}
		return metaJson;
	}

	public final void setMetaJson(@Nullable String infoJson) {
		this.metaJson = infoJson;
		this.meta = null;
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public final @Nullable LocationSourcePK getId() {
		return id;
	}

	@Override
	public final @Nullable Instant getCreated() {
		return created;
	}

	public final void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	public final @Nullable Instant getUpdated() {
		return updated;
	}

	public final void setUpdated(@Nullable Instant updated) {
		this.updated = updated;
	}

	public final @Nullable SolarLocation getLocation() {
		return location;
	}

	public final void setLocation(@Nullable SolarLocation location) {
		this.location = location;
	}

}
