/* ==================================================================
 * UserMetadataEntity.java - 11/11/2016 10:54:43 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

import java.io.Serializable;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import net.solarnetwork.central.domain.BaseEntity;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.domain.SerializeIgnore;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Metadata about a {@link SolarNode}.
 * 
 * <b>Note</b> that {@link JsonUtils#getObjectFromJSON(String, Class)} is used
 * to manage the JSON value passed to {@link #setMetaJson(String)}.
 * 
 * @author matt
 * @version 2.0
 * @since 1.23
 */
@JsonIgnoreProperties("id")
@JsonPropertyOrder({ "userId", "created", "updated" })
public class UserMetadataEntity extends BaseEntity implements UserMetadata, Cloneable, Serializable {

	private static final long serialVersionUID = 3344337449760914930L;

	private Instant updated;
	private GeneralDatumMetadata meta;
	private String metaJson;

	/**
	 * Convenience getter for {@link #getId()}.
	 * 
	 * @return the userId
	 */
	@Override
	public Long getUserId() {
		return getId();
	}

	/**
	 * Convenience setter for {@link #setId(Long)}.
	 * 
	 * @param userId
	 *        the user ID to set
	 */
	public void setUserId(Long userId) {
		setId(userId);
	}

	/**
	 * Alternative for {@link #getMeta()}. This method exists so that we can
	 * configure {@code @JsonUnwrapped} on our {@link GeneralDatumMetadata} but
	 * still support setting it in a normal, wrapped fashion via
	 * {@link #setMeta(GeneralDatumMetadata)}.
	 * 
	 * @return GeneralDatumMetadata
	 */
	@Override
	@JsonUnwrapped
	public GeneralDatumMetadata getMetadata() {
		return getMeta();
	}

	@JsonIgnore
	@SerializeIgnore
	public GeneralDatumMetadata getMeta() {
		if ( meta == null && metaJson != null ) {
			meta = JsonUtils.getObjectFromJSON(metaJson, GeneralDatumMetadata.class);
			metaJson = null; // clear this out, because we might mutate meta and invalidate our cached JSON value
		}
		return meta;
	}

	@JsonProperty
	public void setMeta(GeneralDatumMetadata meta) {
		this.meta = meta;
		this.metaJson = null;
	}

	@JsonIgnore
	@SerializeIgnore
	public String getMetaJson() {
		if ( metaJson == null ) {
			metaJson = JsonUtils.getJSONString(meta, "{}");
			meta = null; // clear this out, because we might otherwise mutate it and invalidate our cached JSON value
		}
		return metaJson;
	}

	public void setMetaJson(String infoJson) {
		this.metaJson = infoJson;
		this.meta = null;
	}

	@Override
	public Instant getUpdated() {
		return updated;
	}

	public void setUpdated(Instant updated) {
		this.updated = updated;
	}

}
