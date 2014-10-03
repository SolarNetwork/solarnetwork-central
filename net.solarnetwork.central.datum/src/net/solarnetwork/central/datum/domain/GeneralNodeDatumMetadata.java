/* ==================================================================
 * GeneralDatumMetadata.java - Oct 3, 2014 6:52:09 AM
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

import java.io.Serializable;
import java.math.BigDecimal;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.util.SerializeIgnore;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.annotate.JsonUnwrapped;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Metadata about the {@link GeneralNodeDatum} associated with a specific node
 * and source.
 * 
 * <p>
 * <b>Note</b> that an internal {@link ObjectMapper} is used to manage the JSON
 * value passed to {@link #setSampleJson(String)}. All floating point values
 * will be converted to {@link BigDecimal} when parsed, to faithfully represent
 * the data.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@JsonPropertyOrder({ "created", "updated", "nodeId", "sourceId", "m", "t" })
public class GeneralNodeDatumMetadata implements Entity<NodeSourcePK>, Cloneable, Serializable {

	private static final long serialVersionUID = 9219762451921441252L;

	protected static final Logger LOG = LoggerFactory.getLogger(GeneralNodeDatumMetadata.class);
	protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	static {
		OBJECT_MAPPER.setSerializationInclusion(Inclusion.NON_NULL);
		OBJECT_MAPPER.setDeserializationConfig(OBJECT_MAPPER.getDeserializationConfig().with(
				DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS));
	}

	private NodeSourcePK id = new NodeSourcePK();
	private DateTime created;
	private DateTime updated;
	private GeneralDatumMetadata meta;
	private String metaJson;

	@Override
	public int compareTo(NodeSourcePK o) {
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
		GeneralNodeDatumMetadata other = (GeneralNodeDatumMetadata) obj;
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
	 * Convenience getter for {@link NodeSourcePK#getNodeId()}.
	 * 
	 * @return the nodeId
	 */
	public Long getNodeId() {
		return (id == null ? null : id.getNodeId());
	}

	/**
	 * Convenience setter for {@link NodeSourcePK#setNodeId(Long)}.
	 * 
	 * @param nodeId
	 *        the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		if ( id == null ) {
			id = new NodeSourcePK();
		}
		id.setNodeId(nodeId);
	}

	/**
	 * Convenience getter for {@link NodeSourcePK#getSourceId()}.
	 * 
	 * @return the sourceId
	 */
	public String getSourceId() {
		return (id == null ? null : id.getSourceId());
	}

	/**
	 * Convenience setter for {@link NodeSourcePK#setSourceId(String)}.
	 * 
	 * @param sourceId
	 *        the sourceId to set
	 */
	public void setSourceId(String sourceId) {
		if ( id == null ) {
			id = new NodeSourcePK();
		}
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
	public GeneralDatumMetadata getMetadata() {
		return getMeta();
	}

	@JsonIgnore
	@SerializeIgnore
	public GeneralDatumMetadata getMeta() {
		if ( meta == null && metaJson != null ) {
			try {
				meta = OBJECT_MAPPER.readValue(metaJson, GeneralDatumMetadata.class);
			} catch ( Exception e ) {
				LOG.error("Exception unmarshalling meta JSON {}", metaJson, e);
			}
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
			try {
				metaJson = OBJECT_MAPPER.writeValueAsString(meta);
			} catch ( Exception e ) {
				LOG.error("Exception marshalling meta {} to JSON", this, e);
				metaJson = "{}";
			}
			meta = null; // clear this out, because we might otherwise mutate it and invalidate our cached JSON value
		}
		return metaJson;
	}

	public void setMetaJson(String infoJson) {
		this.metaJson = infoJson;
		this.meta = null;
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public NodeSourcePK getId() {
		return id;
	}

	public void setId(NodeSourcePK id) {
		this.id = id;
	}

	@Override
	public DateTime getCreated() {
		return created;
	}

	public void setCreated(DateTime created) {
		this.created = created;
	}

	public DateTime getUpdated() {
		return updated;
	}

	public void setUpdated(DateTime updated) {
		this.updated = updated;
	}

}
