/* ==================================================================
 * Datum.java - 22/10/2020 10:02:38 am
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.BasicSerializableIdentity;
import net.solarnetwork.domain.datum.DatumProperties;

/**
 * Basic unit of a datum stream.
 *
 * <p>
 * A datum is the smallest unit of data within a datum stream, identified by a
 * stream ID and timestamp. A {@link DatumProperties} instance holds the
 * property sample values associated with the datum.
 * </p>
 *
 * @author matt
 * @version 2.0
 * @since 2.8
 */
public class DatumEntity extends BasicSerializableIdentity<DatumPK>
		implements Datum, Entity<DatumPK>, Cloneable, Serializable {

	@Serial
	private static final long serialVersionUID = -6655090793049766389L;

	private final @Nullable Instant received;
	private final DatumProperties properties;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param received
	 *        the date the datum was received by SolarNetwork
	 * @param properties
	 *        the properties
	 * @throws IllegalArgumentException
	 *         if any argument except {@code received} is {@code null}
	 */
	public DatumEntity(DatumPK id, Instant received, DatumProperties properties) {
		super(requireNonNullArgument(id, "id"));
		this.received = received;
		this.properties = requireNonNullArgument(properties, "properties");
	}

	/**
	 * Constructor.
	 *
	 * @param streamId
	 *        the stream ID
	 * @param timestamp
	 *        the timestamp
	 * @param received
	 *        the date the datum was received by SolarNetwork
	 * @param properties
	 *        the samples
	 * @throws IllegalArgumentException
	 *         if any argument except {@code received} is {@code null}
	 */
	public DatumEntity(UUID streamId, Instant timestamp, Instant received, DatumProperties properties) {
		this(new DatumPK(streamId, timestamp), received, properties);
	}

	@Override
	public DatumEntity clone() {
		return (DatumEntity) super.clone();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DatumEntity{");
		if ( getId() != null ) {
			builder.append("streamId=");
			builder.append(getId().getStreamId());
			builder.append(", ts=");
			builder.append(getId().getTimestamp());
		}
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
		return getId().streamIdIsAssigned();
	}

	@Override
	public Instant getCreated() {
		return getTimestamp();
	}

	/**
	 * Get the received date.
	 *
	 * @return the received date
	 */
	public @Nullable Instant getReceived() {
		return received;
	}

	/**
	 * Get the property values.
	 *
	 * @return the property values
	 */
	@Override
	public DatumProperties getProperties() {
		return properties;
	}

}
