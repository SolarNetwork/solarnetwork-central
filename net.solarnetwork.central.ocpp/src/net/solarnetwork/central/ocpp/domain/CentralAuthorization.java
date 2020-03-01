/* ==================================================================
 * CentralAuthorization.java - 25/02/2020 2:10:08 pm
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

package net.solarnetwork.central.ocpp.domain;

import java.time.Instant;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.user.dao.UserRelatedEntity;
import net.solarnetwork.ocpp.domain.Authorization;

/**
 * An authorization entity.
 * 
 * @author matt
 * @version 1.0
 */
@JsonPropertyOrder({ "id", "created", "userId", "token", "enabled", "expired", "expiryDate",
		"parentId" })
public class CentralAuthorization extends Authorization implements UserRelatedEntity<Long> {

	private final Long userId;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the owner user ID
	 */
	public CentralAuthorization(Long userId) {
		super();
		this.userId = userId;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param userId
	 *        the owner user ID
	 */
	public CentralAuthorization(Long id, Long userId) {
		super(id);
		this.userId = userId;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param userId
	 *        the owner user ID
	 * @param created
	 *        the creation date
	 */
	public CentralAuthorization(Long id, Long userId, Instant created) {
		super(id, created);
		this.userId = userId;
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the owner user ID
	 * @param created
	 *        the created date
	 * @param token
	 *        the token
	 */
	public CentralAuthorization(Long userId, Instant created, String token) {
		super(created, token);
		this.userId = userId;
	}

	/**
	 * Copy constructor.
	 * 
	 * Copy constructor.
	 * 
	 * @param other
	 *        the authorization to copy
	 */
	public CentralAuthorization(Authorization other) {
		super(other);
		this.userId = (other instanceof CentralAuthorization ? ((CentralAuthorization) other).userId
				: null);
	}

	@Override
	public boolean isSameAs(Authorization other) {
		if ( !(other instanceof CentralAuthorization) ) {
			return false;
		}
		boolean result = super.isSameAs(other);
		if ( result ) {
			result = Objects.equals(this.userId, ((CentralAuthorization) other).userId);
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Authorization{id=");
		builder.append(getId());
		builder.append(", ");
		builder.append(", userId=").append(userId).append(", ");
		if ( getToken() != null ) {
			builder.append("token=");
			builder.append(getToken());
			builder.append(", ");
		}
		if ( getExpiryDate() != null ) {
			builder.append("expiryDate=");
			builder.append(getExpiryDate());
			builder.append(", ");
		}
		if ( getParentId() != null ) {
			builder.append("parentId=");
			builder.append(getParentId());
			builder.append(", ");
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the owner user ID.
	 * 
	 * @return the owner user ID
	 */
	@Override
	public Long getUserId() {
		return userId;
	}

}
