/* ==================================================================
 * TypedDatumEntity.java - 17/11/2020 7:41:30 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.BasicIdentity;

/**
 * A datum with a type flag, typically used for intermediate calculation steps.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class TypedDatumEntity extends BasicIdentity<DatumPK>
		implements Entity<DatumPK>, Cloneable, Serializable {

	private static final long serialVersionUID = 8626921281040436299L;

	private final int type;
	private final DatumProperties properties;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param type
	 *        the type
	 * @param properties
	 *        the properties
	 */
	public TypedDatumEntity(DatumPK id, int type, DatumProperties properties) {
		super(id);
		this.type = type;
		this.properties = properties;
	}

	/**
	 * Constructor.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timestamp
	 *        the timestamp
	 * @param type
	 *        the type
	 * @param properties
	 *        the samples
	 */
	public TypedDatumEntity(UUID streamId, Instant timestamp, int type, DatumProperties properties) {
		this(new DatumPK(streamId, timestamp), type, properties);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * Note this constructor has {@code Object} arguments to work around MyBatis
	 * mapping issues.
	 * </p>
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timestamp
	 *        the timestamp
	 * @param type
	 *        the type
	 * @param instantaneous
	 *        the instantaneous values; must be {@code BigDecimal[]}
	 * @param accumulating
	 *        the accumulating values; must be {@code BigDecimal[]}
	 * @param status
	 *        the status values; must be {@code String[]}
	 * @param tags
	 *        the tag values; must be {@code String[]}
	 */
	public TypedDatumEntity(UUID streamId, Instant timestamp, int type, Object instantaneous,
			Object accumulating, Object status, Object tags) {
		this(new DatumPK(streamId, timestamp), type,
				DatumProperties.propertiesOf((BigDecimal[]) instantaneous, (BigDecimal[]) accumulating,
						(String[]) status, (String[]) tags));
	}

	@Override
	public TypedDatumEntity clone() {
		return (TypedDatumEntity) super.clone();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TypedDatumEntity{");
		if ( getId() != null ) {
			builder.append("streamId=");
			builder.append(getId().getStreamId());
			builder.append(", ts=");
			builder.append(getId().getTimestamp());
		}
		builder.append(", type=").append(type);
		if ( properties != null ) {
			if ( properties.getInstantaneous() != null ) {
				builder.append(", i=");
				builder.append(Arrays.toString(properties.getInstantaneous()));
			}
			if ( properties.getAccumulating() != null ) {
				builder.append(", a=");
				builder.append(Arrays.toString(properties.getAccumulating()));
			}
			if ( properties.getStatus() != null ) {
				builder.append(", s=");
				builder.append(Arrays.toString(properties.getStatus()));
			}
			if ( properties.getTags() != null ) {
				builder.append(", t=");
				builder.append(Arrays.toString(properties.getTags()));
			}
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public boolean hasId() {
		DatumPK id = getId();
		return (id != null && id.getStreamId() != null && id.getTimestamp() != null);
	}

	@Override
	public Instant getCreated() {
		return getTimestamp();
	}

	/**
	 * Get the datum timestamp.
	 * 
	 * <p>
	 * The {@link #getCreated()} method is an alias for this method. This method
	 * is a shortcut for {@code getId().getTimestamp()}.
	 * </p>
	 * 
	 * @return the datum timestamp
	 */
	public Instant getTimestamp() {
		DatumPK id = getId();
		return (id != null ? id.getTimestamp() : null);
	}

	/**
	 * Get the datum stream ID.
	 * 
	 * <p>
	 * This method is a shortcut for {@code getId().getStreamId()}.
	 * </p>
	 * 
	 * @return the stream ID
	 */
	public UUID getStreamId() {
		DatumPK id = getId();
		return (id != null ? id.getStreamId() : null);
	}

	/**
	 * Get the property values.
	 * 
	 * @return the property values
	 */
	public DatumProperties getProperties() {
		return properties;
	}

	/**
	 * Get the type.
	 * 
	 * <p>
	 * The type is a context-specific value used during datum calculations. For
	 * example it might indicate if the datum represents a real vs. auxiliary
	 * datum.
	 * </p>
	 * 
	 * @return the type
	 */
	public int getType() {
		return type;
	}

}
