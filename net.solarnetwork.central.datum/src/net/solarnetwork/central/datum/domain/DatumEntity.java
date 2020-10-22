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

package net.solarnetwork.central.datum.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.BasicIdentity;

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
 * @version 1.0
 * @since 2.8
 */
public class DatumEntity extends BasicIdentity<DatumStreamPK>
		implements Datum, Entity<DatumStreamPK>, Cloneable, Serializable {

	private static final long serialVersionUID = -6655090793049766389L;

	private final Instant received;
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
	 */
	public DatumEntity(DatumStreamPK id, Instant received, DatumProperties properties) {
		super(id);
		this.received = received;
		this.properties = properties;
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param timestamp
	 *        the timestamp
	 * @param received
	 *        the date the datum was received by SolarNetwork
	 * @param properties
	 *        the samples
	 */
	public DatumEntity(UUID streamId, Instant timestamp, Instant received, DatumProperties properties) {
		this(new DatumStreamPK(streamId, timestamp), received, properties);
	}

	@Override
	public boolean hasId() {
		DatumStreamPK id = getId();
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
	@Override
	public Instant getTimestamp() {
		DatumStreamPK id = getId();
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
	@Override
	public UUID getStreamId() {
		DatumStreamPK id = getId();
		return (id != null ? id.getStreamId() : null);
	}

	/**
	 * Get the received date.
	 * 
	 * @return the received date
	 */
	public Instant getReceived() {
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
