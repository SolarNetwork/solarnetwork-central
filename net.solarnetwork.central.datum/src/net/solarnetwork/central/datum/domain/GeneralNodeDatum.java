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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.util.SerializeIgnore;
import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.annotate.JsonUnwrapped;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generalized node-based datum.
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
@JsonPropertyOrder({ "created", "nodeId", "sourceId" })
public class GeneralNodeDatum implements Entity<GeneralNodeDatumPK>, Cloneable, Serializable {

	private static final long serialVersionUID = 9030100020202210123L;

	private static final Logger LOG = LoggerFactory.getLogger(GeneralNodeDatum.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	static {
		OBJECT_MAPPER.setSerializationInclusion(Inclusion.NON_NULL);
		OBJECT_MAPPER.setDeserializationConfig(OBJECT_MAPPER.getDeserializationConfig().with(
				DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS));
	}

	private GeneralNodeDatumPK id = new GeneralNodeDatumPK();
	private GeneralNodeDatumSamples samples;
	private DateTime posted;
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
	 * Convenience method for {@link GeneralNodeDatumSamples#getSampleData()}.
	 * 
	 * @return the sample data, or <em>null</em> if none available
	 */
	@JsonUnwrapped
	@JsonAnyGetter
	public Map<String, ?> getSampleData() {
		GeneralNodeDatumSamples s = getSamples();
		return (s == null ? null : s.getSampleData());
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
		GeneralNodeDatum other = (GeneralNodeDatum) obj;
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
	public String getSampleJson() {
		if ( sampleJson == null ) {
			try {
				sampleJson = OBJECT_MAPPER.writeValueAsString(samples);
			} catch ( Exception e ) {
				LOG.error("Exception marshalling sample {} to JSON", this, e);
				sampleJson = "{}";
			}
		}
		return sampleJson;
	}

	/**
	 * Set the {@link GeneralNodeDatumSamples} object via a JSON string.
	 * 
	 * <p>
	 * This method will remove any previously created GeneralNodeDatumSamples
	 * and replace it with the values parsed from the JSON. All floating point
	 * values will be converted to {@link BigDecimal} instances.
	 * </p>
	 * 
	 * @param json
	 */
	public void setSampleJson(String json) {
		sampleJson = json;
		samples = null;
	}

	@SerializeIgnore
	@JsonIgnore
	public DateTime getPosted() {
		return posted;
	}

	public void setPosted(DateTime posted) {
		this.posted = posted;
	}

	@SerializeIgnore
	@JsonIgnore
	public GeneralNodeDatumSamples getSamples() {
		if ( samples == null && sampleJson != null ) {
			try {
				samples = OBJECT_MAPPER.readValue(sampleJson, GeneralNodeDatumSamples.class);
			} catch ( Exception e ) {
				LOG.error("Exception unmarshalling sampleJson {}", sampleJson, e);
			}
		}
		return samples;
	}

	/**
	 * Set the {@link GeneralNodeDatumSamples} instance to use.
	 * 
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setSampleJson(String)} as well.
	 * </p>
	 * 
	 * @param samples
	 *        the samples instance to set
	 */
	public void setSamples(GeneralNodeDatumSamples samples) {
		this.samples = samples;
		sampleJson = null;
	}

}
