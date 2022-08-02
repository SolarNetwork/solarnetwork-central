/* ==================================================================
 * UserEvent.java - 1/08/2022 10:42:46 am
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
import java.util.UUID;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.dao.BasicIdentity;
import net.solarnetwork.dao.Entity;

/**
 * An application event related to a user.
 * 
 * @author matt
 * @version 1.0
 */
public class UserEvent extends BasicIdentity<UserEventPK>
		implements Entity<UserEventPK>, UserRelatedEntity<UserEventPK>, Serializable, Cloneable {

	private static final long serialVersionUID = 1867734608363358361L;

	private final String message;
	private final String data;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param message
	 *        the message
	 * @param data
	 *        the JSON data
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@literal null}
	 */
	public UserEvent(UserEventPK id, String message, String data) {
		super(requireNonNullArgument(id, "id"));
		this.message = message;
		this.data = data;
	}

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
	 * @param message
	 *        the message
	 * @param data
	 *        the JSON data
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null} other than {@code message} or
	 *         {@code data}
	 */
	public UserEvent(Long userId, Instant created, UUID eventId, String kind, String message,
			String data) {
		this(new UserEventPK(userId, created, eventId, kind), message, data);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserEvent{");
		if ( getUserId() != null ) {
			builder.append("userId=");
			builder.append(getUserId());
			builder.append(", ");
		}
		if ( getCreated() != null ) {
			builder.append("created=");
			builder.append(getCreated());
			builder.append(", ");
		}
		if ( getEventId() != null ) {
			builder.append("eventId=");
			builder.append(getEventId());
			builder.append(", ");
		}
		if ( getKind() != null ) {
			builder.append("kind=");
			builder.append(getKind());
			builder.append(", ");
		}
		if ( message != null ) {
			builder.append("message=");
			builder.append(message);
			builder.append(", ");
		}
		if ( data != null ) {
			builder.append("data=");
			builder.append(data);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public Long getUserId() {
		return getId().getUserId();
	}

	@Override
	public Instant getCreated() {
		return getId().getCreated();
	}

	/**
	 * Get the event ID.
	 * 
	 * @return the event ID
	 */
	public UUID getEventId() {
		return getId().getEventId();
	}

	/**
	 * Get the event kind.
	 * 
	 * @return the kind
	 */
	public String getKind() {
		return getId().getKind();
	}

	@Override
	public UserEvent clone() {
		return (UserEvent) super.clone();
	}

	/**
	 * Get the message.
	 * 
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Get the JSON data.
	 * 
	 * @return the data
	 */
	public String getData() {
		return data;
	}

}
