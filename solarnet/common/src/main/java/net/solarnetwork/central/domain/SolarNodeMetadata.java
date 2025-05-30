/* ==================================================================
 * SolarNodeMetadata.java - 11/11/2016 10:43:37 AM
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

package net.solarnetwork.central.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import net.solarnetwork.central.dao.BaseEntity;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.SerializeIgnore;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Metadata about a {@link SolarNode}.
 *
 * <p>
 * </p>
 * <b>Note</b> that {@link JsonUtils#getObjectFromJSON(String, Class)} is used
 * to manage the JSON value passed to {@link #setMetaJson(String)}.
 * </p>
 *
 * @author matt
 * @version 3.0
 * @since 1.32
 */
@JsonIgnoreProperties("id")
@JsonPropertyOrder({ "nodeId", "created", "updated" })
public class SolarNodeMetadata extends BaseEntity<SolarNodeMetadata>
		implements NodeMetadata, Cloneable, Serializable {

	@Serial
	private static final long serialVersionUID = 7366747359583724835L;

	private Instant updated;
	private GeneralDatumMetadata meta;

	/**
	 * Constructor.
	 */
	public SolarNodeMetadata() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the node ID
	 * @since 2.1
	 */
	public SolarNodeMetadata(Long id) {
		super();
		setId(id);
	}

	/**
	 * Convenience getter for {@link #getId()}.
	 *
	 * @return the nodeId
	 */
	@Override
	public Long getNodeId() {
		return getId();
	}

	/**
	 * Convenience setter for {@link #setId(Long)}.
	 *
	 * @param nodeId
	 *        the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		setId(nodeId);
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
		return meta;
	}

	@JsonProperty
	public void setMeta(GeneralDatumMetadata meta) {
		this.meta = meta;
	}

	@JsonIgnore
	@SerializeIgnore
	public String getMetaJson() {
		return JsonUtils.getJSONString(meta, "{}");
	}

	/**
	 * Set the metadata as JSON.
	 *
	 * @param metaJson
	 *        the JSON metadata to set
	 */
	public void setMetaJson(String metaJson) {
		this.meta = JsonUtils.getObjectFromJSON(metaJson, GeneralDatumMetadata.class);
	}

	@Override
	public Instant getUpdated() {
		return updated;
	}

	/**
	 * Set the updated date.
	 *
	 * @param updated
	 *        the date to set
	 */
	public void setUpdated(Instant updated) {
		this.updated = updated;
	}

}
