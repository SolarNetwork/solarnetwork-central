/* ==================================================================
 * EndpointAuthConfiguration.java - 28/03/2024 11:32:47 am
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

package net.solarnetwork.central.inin.domain;

import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserUuidLongCompositePK;

/**
 * Endpoint authorization configuration.
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "endpointId", "credentialId", "created", "modified", "enabled" })
public class EndpointAuthConfiguration
		extends BaseUserModifiableEntity<EndpointAuthConfiguration, UserUuidLongCompositePK> implements
		InstructionInputConfigurationEntity<EndpointAuthConfiguration, UserUuidLongCompositePK> {

	private static final long serialVersionUID = -3093710975249328607L;

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
	public EndpointAuthConfiguration(UserUuidLongCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param endpointId
	 *        the endpoint ID
	 * @param credentialId
	 *        the credential ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public EndpointAuthConfiguration(Long userId, UUID endpointId, Long credentialId, Instant created) {
		this(new UserUuidLongCompositePK(userId, endpointId, credentialId), created);
	}

	@Override
	public EndpointAuthConfiguration copyWithId(UserUuidLongCompositePK id) {
		var copy = new EndpointAuthConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EndpointAuth{");
		if ( getUserId() != null ) {
			builder.append("userId=");
			builder.append(getUserId());
			builder.append(", ");
		}
		if ( getEndpointId() != null ) {
			builder.append("endpointId=");
			builder.append(getEndpointId());
			builder.append(", ");
		}
		if ( getCredentialId() != null ) {
			builder.append("credentialId=");
			builder.append(getCredentialId());
			builder.append(", ");
		}
		builder.append("enabled=");
		builder.append(isEnabled());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the endpoint ID.
	 *
	 * @return the endpoint ID
	 */
	public UUID getEndpointId() {
		UserUuidLongCompositePK id = getId();
		return (id != null ? id.getGroupId() : null);
	}

	/**
	 * Get the credential ID.
	 *
	 * @return the credential ID
	 */
	public Long getCredentialId() {
		UserUuidLongCompositePK id = getId();
		return (id != null ? id.getEntityId() : null);
	}

}
