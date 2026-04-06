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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
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

	@Serial
	private static final long serialVersionUID = -2954822894884485056L;

	private String name;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param name
	 *        the name
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ServerAuthConfiguration(UserLongStringCompositePK id, Instant created, String name) {
		super(id, created);
		this.name = requireNonNullArgument(name, "name");
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
	 * @param name
	 *        the name
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ServerAuthConfiguration(Long userId, Long serverId, String identity, Instant created,
			String name) {
		this(new UserLongStringCompositePK(userId, serverId, identity), created, name);
	}

	@Override
	public ServerAuthConfiguration copyWithId(UserLongStringCompositePK id) {
		var copy = new ServerAuthConfiguration(id, created(), name);
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(ServerAuthConfiguration entity) {
		super.copyTo(entity);
		entity.setName(name);
	}

	@Override
	public boolean isSameAs(@Nullable ServerAuthConfiguration other) {
		if ( !super.isSameAs(other) ) {
			return false;
		}
		final var o = nonnull(other, "other");
		return Objects.equals(this.name, o.getName());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ServerAuth{");
		if ( getUserId() != null ) {
			builder.append("userId=");
			builder.append(getUserId());
			builder.append(", ");
		}
		if ( getServerId() != null ) {
			builder.append("serverId=");
			builder.append(getServerId());
			builder.append(", ");
		}
		if ( getIdentifier() != null ) {
			builder.append("identifier=");
			builder.append(getIdentifier());
			builder.append(", ");
		}
		if ( name != null ) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		builder.append("enabled=");
		builder.append(isEnabled());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the server ID.
	 *
	 * @return the server ID
	 */
	public final Long getServerId() {
		return id().getGroupId();
	}

	/**
	 * Get the identifier.
	 *
	 * @return the identifier
	 */
	public final String getIdentifier() {
		return id().getEntityId();
	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Set the name
	 *
	 * @param name
	 *        the name to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setName(String name) {
		this.name = requireNonNullArgument(name, "name");
	}

}
