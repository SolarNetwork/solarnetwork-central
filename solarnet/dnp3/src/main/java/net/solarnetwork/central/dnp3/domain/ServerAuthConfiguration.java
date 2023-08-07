/* ==================================================================
 * ServerAuthConfiguration.java - 6/08/2023 10:26:04 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.domain;

import java.time.Instant;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserLongStringCompositePK;

/**
 * DNP3 server authorization configuration.
 * 
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "serverId", "identifier", "created", "modified", "enabled", "name" })
public class ServerAuthConfiguration
		extends BaseUserModifiableEntity<ServerAuthConfiguration, UserLongStringCompositePK> {

	private static final long serialVersionUID = -2954822894884485056L;

	private String name;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ServerAuthConfiguration(UserLongStringCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param serverId
	 *        the server ID
	 * @param identity
	 *        the identity
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ServerAuthConfiguration(Long userId, Long serverId, String identity, Instant created) {
		this(new UserLongStringCompositePK(userId, serverId, identity), created);
	}

	@Override
	public ServerAuthConfiguration copyWithId(UserLongStringCompositePK id) {
		var copy = new ServerAuthConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(ServerAuthConfiguration entity) {
		super.copyTo(entity);
		entity.setName(name);
	}

	@Override
	public boolean isSameAs(ServerAuthConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		return Objects.equals(this.name, other.getName());
	}

	/**
	 * Get the server ID.
	 * 
	 * @return the server ID
	 */
	public Long getServerId() {
		UserLongStringCompositePK id = getId();
		return (id != null ? id.getGroupId() : null);
	}

	/**
	 * Get the identifier.
	 * 
	 * @return the identifier
	 */
	public String getIdentifier() {
		UserLongStringCompositePK id = getId();
		return (id != null ? id.getEntityId() : null);
	}

	/**
	 * Get the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * 
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

}
