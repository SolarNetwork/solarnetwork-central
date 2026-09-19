/* ==================================================================
 * UserNodeConfirmation.java - Sep 7, 2011 11:06:59 AM
 *
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.dao.BaseEntity;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.util.ObjectUtils;

/**
 * The "pending confirmation" entity for after a user generates a node
 * "invitation" to join SolarNet. The user must confirm the invitation before a
 * UserNode entity is created.
 *
 * @author matt
 * @version 2.1
 */
public class UserNodeConfirmation extends BaseEntity implements UserRelatedEntity<Long> {

	@Serial
	private static final long serialVersionUID = -3535047613550046877L;

	private @Nullable User user;
	private @Nullable Long nodeId;
	private @Nullable String confirmationKey;
	private @Nullable Instant confirmationDate;
	private @Nullable String securityPhrase;
	private @Nullable String country;
	private @Nullable String timeZoneId;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the confirmation ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserNodeConfirmation(Long id) {
		super();
		setId(requireNonNullArgument(id, "id"));
	}

	/**
	 * Constructor.
	 * 
	 * @param user
	 *        the user
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserNodeConfirmation(User user) {
		super();
		this.user = requireNonNullArgument(user, "user");
	}

	/**
	 * Get the user.
	 * 
	 * @return the user
	 */
	public final @Nullable User getUser() {
		return user;
	}

	/**
	 * Set the uesr.
	 * 
	 * @param user
	 *        the user to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setUser(User user) {
		this.user = ObjectUtils.requireNonNullArgument(user, "user");
	}

	@Override
	public final Long getUserId() {
		return nonnull(nonnull(user, "user").getId(), "userId");
	}

	public final @Nullable Long getNodeId() {
		return nodeId;
	}

	public final void setNodeId(@Nullable Long nodeId) {
		this.nodeId = nodeId;
	}

	public final @Nullable String getConfirmationKey() {
		return confirmationKey;
	}

	public final void setConfirmationKey(@Nullable String confirmationKey) {
		this.confirmationKey = confirmationKey;
	}

	public final @Nullable Instant getConfirmationDate() {
		return confirmationDate;
	}

	public final void setConfirmationDate(@Nullable Instant confirmationDate) {
		this.confirmationDate = confirmationDate;
	}

	public final @Nullable String getSecurityPhrase() {
		return securityPhrase;
	}

	public final void setSecurityPhrase(@Nullable String securityPhrase) {
		this.securityPhrase = securityPhrase;
	}

	public final @Nullable String getCountry() {
		return country;
	}

	public final void setCountry(@Nullable String countryCode) {
		this.country = countryCode;
	}

	public final @Nullable String getTimeZoneId() {
		return timeZoneId;
	}

	public final void setTimeZoneId(@Nullable String timeZoneName) {
		this.timeZoneId = timeZoneName;
	}

}
