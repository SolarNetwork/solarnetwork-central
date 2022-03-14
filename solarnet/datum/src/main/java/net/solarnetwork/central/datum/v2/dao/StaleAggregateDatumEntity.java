/* ==================================================================
 * StaleAggregateDatumEntity.java - 3/11/2020 12:58:01 pm
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
import java.time.Instant;
import java.util.UUID;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StreamKindPK;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.BasicIdentity;

/**
 * Entity for a "stale" aggregation record, which is used to mark specific
 * aggregation time periods as needing to be (re)computed.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class StaleAggregateDatumEntity extends BasicIdentity<StreamKindPK>
		implements StaleAggregateDatum, Entity<StreamKindPK>, Cloneable, Serializable {

	private static final long serialVersionUID = 6192621556532302225L;

	private final Instant created;

	/**
	 * Constructor.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timestamp
	 *        the timestamp
	 * @param kind
	 *        the aggregation kind
	 * @param created
	 *        the creation date
	 */
	public StaleAggregateDatumEntity(UUID streamId, Instant timestamp, Aggregation kind,
			Instant created) {
		super(new StreamKindPK(streamId, timestamp, kind.getKey()));
		this.created = created;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("StaleAggregateDatumEntity{");
		if ( getKind() != null ) {
			builder.append("kind=");
			builder.append(getKind());
			builder.append(", ");
		}
		if ( getStreamId() != null ) {
			builder.append("streamId=");
			builder.append(getStreamId());
			builder.append(", ");
		}
		if ( getTimestamp() != null ) {
			builder.append("timestamp=");
			builder.append(getTimestamp());
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public Aggregation getKind() {
		StreamKindPK id = getId();
		return (id.getKind() != null ? Aggregation.forKey(id.getKind()) : null);
	}

	@Override
	public Instant getTimestamp() {
		StreamKindPK id = getId();
		return (id != null ? id.getTimestamp() : null);
	}

	@Override
	public UUID getStreamId() {
		StreamKindPK id = getId();
		return (id != null ? id.getStreamId() : null);
	}

	@Override
	public Instant getCreated() {
		return created;
	}

}
