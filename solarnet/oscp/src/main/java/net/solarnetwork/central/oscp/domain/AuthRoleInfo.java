/* ==================================================================
 * AuthRoleInfo.java - 17/08/2022 11:06:41 am
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

package net.solarnetwork.central.oscp.domain;

import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.util.ObjectUtils;

/**
 * Authenticated role information.
 * 
 * @param actorId
 *        the actor key
 * @param role
 *        the role
 * @author matt
 * @version 1.0
 */
public record AuthRoleInfo(UserLongCompositePK id, OscpRole role) {

	/**
	 * Get the user ID.
	 * 
	 * @return the uesr ID
	 */
	public Long userId() {
		return id().getUserId();
	}

	/**
	 * Get the entity ID.
	 * 
	 * @return the entity ID
	 */
	public Long entityId() {
		return id().getEntityId();
	}

	/**
	 * Get a unique identifier for this instance.
	 * 
	 * @return the identifier
	 */
	public String asIdentifier() {
		return "%s:%d:%d".formatted(role.getAlias(), userId(), entityId());
	}

	/**
	 * Get an instance for an identifier.
	 * 
	 * @param identifier
	 *        the identifier, as in the form returned from
	 *        {@link #asIdentifier()}
	 * @return the instance
	 * @throws IllegalArgumentException
	 *         if the identifier cannot be parsed
	 */
	public static AuthRoleInfo forIdentifier(String identifier) {
		String[] components = ObjectUtils.requireNonNullArgument(identifier, "identifier").split(":", 3);
		if ( components.length < 3 ) {
			throw new IllegalArgumentException("Invalid identifier [%s]".formatted(identifier));
		}
		try {
			return new AuthRoleInfo(
					new UserLongCompositePK(Long.valueOf(components[1]), Long.valueOf(components[2])),
					OscpRole.forAlias(components[0]));
		} catch ( NumberFormatException e ) {
			throw new IllegalArgumentException("Invalid identifier [%s]".formatted(identifier));
		}
	}

}
