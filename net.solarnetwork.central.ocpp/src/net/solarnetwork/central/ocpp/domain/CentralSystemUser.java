/* ==================================================================
 * CentralSystemUser.java - 25/02/2020 10:26:26 am
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
import net.solarnetwork.ocpp.domain.SystemUser;

/**
 * An OCPP charge point system user.
 * 
 * @author matt
 * @version 1.0
 */
public class CentralSystemUser extends SystemUser {

	private final Long userId;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the owner user ID
	 */
	public CentralSystemUser(Long userId) {
		super();
		this.userId = userId;
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the owner user ID
	 * @param created
	 *        the creation date
	 * @param username
	 *        the username
	 * @param password
	 *        the password
	 */
	public CentralSystemUser(Long userId, Instant created, String username, String password) {
		super(created, username, password);
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
	public CentralSystemUser(Long id, Long userId, Instant created) {
		super(id, created);
		this.userId = userId;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the other system
	 */
	public CentralSystemUser(SystemUser other) {
		super(other);
		this.userId = (other instanceof CentralSystemUser ? ((CentralSystemUser) other).userId : null);
	}

	@Override
	public boolean isSameAs(SystemUser other) {
		if ( !(other instanceof CentralSystemUser) ) {
			return false;
		}
		boolean result = super.isSameAs(other);
		if ( result ) {
			result = Objects.equals(this.userId, ((CentralSystemUser) other).userId);
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CentralSystemUser{");
		if ( userId != null ) {
			builder.append("userId=");
			builder.append(userId);
			builder.append(", ");
		}
		if ( getUsername() != null ) {
			builder.append("username=");
			builder.append(getUsername());
			builder.append(", ");
		}
		if ( getAllowedChargePoints() != null ) {
			builder.append("allowedChargePoints=");
			builder.append(getAllowedChargePoints());
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the owner user ID.
	 * 
	 * @return the owner user ID
	 */
	public Long getUserId() {
		return userId;
	}

}
