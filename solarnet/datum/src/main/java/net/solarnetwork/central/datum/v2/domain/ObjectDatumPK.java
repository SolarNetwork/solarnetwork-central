/* ==================================================================
 * ObjectDatumPK.java - 26/02/2024 2:47:37 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import net.solarnetwork.central.datum.domain.GeneralObjectDatumKey;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Extension of {@link DatumPK} to act also as a {@link GeneralObjectDatumKey}.
 *
 * @author matt
 * @version 1.0
 */
public class ObjectDatumPK extends DatumPK implements GeneralObjectDatumKey {

	private static final long serialVersionUID = -6975117953635161471L;

	private final ObjectDatumKind kind;
	private final Long objectId;
	private final String sourceId;

	/**
	 * Constructor.
	 *
	 * @param kind
	 *        the object kind
	 * @param objectId
	 *        the object ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the timestamp
	 * @param streamId
	 *        the stream ID
	 */
	public ObjectDatumPK(ObjectDatumKind kind, Long objectId, String sourceId, Instant timestamp,
			UUID streamId) {
		super(streamId, timestamp);
		this.kind = kind;
		this.objectId = objectId;
		this.sourceId = sourceId;
	}

	@Override
	protected ObjectDatumPK clone() {
		return (ObjectDatumPK) super.clone();
	}

	/**
	 * Compare two key objects.
	 *
	 * <p>
	 * If {@code objectId} is populated, then this compares {@code objectId},
	 * {@code sourceId}, and then {@code timestamp} values, all in ascending
	 * order with {@literal null} values ordered first. Otherwise the super
	 * implementation is invoked.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(DatumPK o) {
		if ( o instanceof ObjectDatumPK id && objectId != null ) {
			int result = 0;
			if ( objectId != id.objectId ) {
				if ( objectId == null ) {
					return 1;
				} else if ( id.objectId == null ) {
					return -1;
				}
				result = objectId.compareTo(id.objectId);
				if ( result != 0 ) {
					return result;
				}
			}
			if ( sourceId != id.sourceId ) {
				if ( sourceId == null ) {
					return 1;
				} else if ( id.sourceId == null ) {
					return -1;
				}
				result = sourceId.compareTo(id.sourceId);
				if ( result != 0 ) {
					return result;
				}
			}
			if ( getTimestamp() == id.getTimestamp() ) {
				return 0;
			} else if ( getTimestamp() == null ) {
				return 1;
			} else if ( id.getTimestamp() == null ) {
				return -1;
			}
			return getTimestamp().compareTo(id.getTimestamp());
		}
		return super.compareTo(o);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(kind, objectId, sourceId);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof ObjectDatumPK) ) {
			return false;
		}
		ObjectDatumPK other = (ObjectDatumPK) obj;
		return kind == other.kind && Objects.equals(objectId, other.objectId)
				&& Objects.equals(sourceId, other.sourceId);
	}

	@Override
	public ObjectDatumKind getKind() {
		return kind;
	}

	@Override
	public Long getObjectId() {
		return objectId;
	}

	@Override
	public String getSourceId() {
		return sourceId;
	}

}
