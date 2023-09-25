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

import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.dao.BasicIdentity;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.util.UuidUtils;

/**
 * An application event related to a user.
 * 
 * <p>
 * Note that time-based UUIDs are assumed for {@link #getCreated()} to work, and
 * microsecond precision time is assumed for V7 UUIDs. See
 * {@link UuidUtils#extractTimestamp(UUID, boolean)}.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class UserEvent extends BasicIdentity<UserUuidPK>
		implements Entity<UserUuidPK>, UserRelatedEntity<UserUuidPK>, Serializable, Cloneable {

	private static final long serialVersionUID = -2418940464038903514L;

	private final String[] tags;
	private final String message;
	private final String data;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param tags
	 *        the tags
	 * @param message
	 *        the message
	 * @param data
	 *        the JSON data
	 * @throws IllegalArgumentException
	 *         if {@code id} or {@code tags} are {@literal null}
	 */
	public UserEvent(UserUuidPK id, String[] tags, String message, String data) {
		super(requireNonNullArgument(id, "id"));
		this.tags = requireNonEmptyArgument(tags, "tags");
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
	public UserEvent(Long userId, UUID eventId, String[] tags, String message, String data) {
		this(new UserUuidPK(userId, eventId), tags, message, data);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserEvent{");
		builder.append("userId=");
		builder.append(getUserId());
		builder.append(", ");

		builder.append("eventId=");
		builder.append(getEventId());
		builder.append(", ");

		if ( tags != null ) {
			builder.append("tags=");
			builder.append(Arrays.toString(tags));
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
	public UserEvent clone() {
		return (UserEvent) super.clone();
	}

	@Override
	public Long getUserId() {
		return getId().getUserId();
	}

	@Override
	public Instant getCreated() {
		return UuidUtils.extractTimestamp(getEventId(), UuidUtils.V7_MICRO_COUNT_PRECISION);
	}

	/**
	 * Get the event ID.
	 * 
	 * @return the event ID
	 */
	public UUID getEventId() {
		return getId().getUuid();
	}

	/**
	 * Get the event tags.
	 * 
	 * @return the tags
	 */
	public String[] getTags() {
		return tags;
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
