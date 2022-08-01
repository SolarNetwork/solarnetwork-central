/* ==================================================================
 * UserEventPK.java - 1/08/2022 10:20:13 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.fasterxml.uuid.UUIDComparator;

/**
 * Primary key for user events.
 * 
 * @author matt
 * @version 1.0
 */
public class UserEventPK extends BasePK implements Serializable, Cloneable, Comparable<UserEventPK> {

	private static final long serialVersionUID = -3705339650905636650L;

	private final Long userId;
	private final Instant created;
	private final UUID eventId;
	private final String kind;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the creation date
	 * @param eventId
	 *        the event ID
	 * @param kind
	 *        the event kind
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserEventPK(Long userId, Instant created, UUID eventId, String kind) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.created = requireNonNullArgument(created, "created");
		this.eventId = requireNonNullArgument(eventId, "eventId");
		this.kind = requireNonNullArgument(kind, "kind");
	}

	@Override
	public int compareTo(UserEventPK o) {
		if ( o == null ) {
			return 1;
		}

		int comparison = userId.compareTo(o.userId);
		if ( comparison != 0 ) {
			return comparison;
		}

		comparison = created.compareTo(o.created);
		if ( comparison != 0 ) {
			return comparison;
		}

		// NOTE: JDK UUID ordering not suitable here, see UUIDComparator for more info
		comparison = UUIDComparator.staticCompare(eventId, o.eventId);
		if ( comparison != 0 ) {
			return comparison;
		}

		return kind.compareTo(o.kind);
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("u=").append(userId);
		buf.append(";c=").append(created);
		buf.append(";e=").append(eventId);
		buf.append(";k=").append(kind);
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		buf.append("userId=").append(userId);
		buf.append(", created=").append(created);
		buf.append(", eventId=").append(eventId);
		buf.append(", kind=").append(kind);
	}

	@Override
	protected UserEventPK clone() {
		return (UserEventPK) super.clone();
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, created, eventId, kind);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof UserEventPK) ) {
			return false;
		}
		UserEventPK other = (UserEventPK) obj;
		return Objects.equals(userId, other.userId) && Objects.equals(created, other.created)
				&& Objects.equals(eventId, other.eventId) && Objects.equals(kind, other.kind);
	}

	/**
	 * Get the event ID.
	 * 
	 * @return the event ID
	 */
	public UUID getEventId() {
		return eventId;
	}

	/**
	 * Get the user ID.
	 * 
	 * @return the user ID
	 */
	public Long getUserId() {
		return userId;
	}

	/**
	 * Get the event creation date.
	 * 
	 * @return the created
	 */
	public Instant getCreated() {
		return created;
	}

	/**
	 * Get the event kind.
	 * 
	 * @return the kind
	 */
	public String getKind() {
		return kind;
	}

}
