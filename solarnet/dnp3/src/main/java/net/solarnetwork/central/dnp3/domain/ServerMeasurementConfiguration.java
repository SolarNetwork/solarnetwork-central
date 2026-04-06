/* ==================================================================
 * ServerMeasurementConfiguration.java - 6/08/2023 12:20:33 pm
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;

/**
 * DNP3 server measurement configuration.
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id", "valid" })
@JsonPropertyOrder({ "userId", "serverId", "index", "created", "modified", "enabled", "nodeId",
		"sourceId", "property", "type", "multiplier", "offset", "scale" })
public final class ServerMeasurementConfiguration
		extends BaseServerDatumStreamConfiguration<ServerMeasurementConfiguration, MeasurementType> {

	@Serial
	private static final long serialVersionUID = 6699420889385795704L;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the sourceId
	 * @param type
	 *        the type
	 * @param property
	 *        the property
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ServerMeasurementConfiguration(UserLongIntegerCompositePK id, Instant created, Long nodeId,
			String sourceId, MeasurementType type, String property) {
		super(id, created, nodeId, sourceId, type);
		setProperty(requireNonNullArgument(property, "property"));
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param serverId
	 *        the server ID
	 * @param index
	 *        the index
	 * @param created
	 *        the creation date
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the sourceId
	 * @param type
	 *        the type
	 * @param property
	 *        the property
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ServerMeasurementConfiguration(Long userId, Long serverId, Integer index, Instant created,
			Long nodeId, String sourceId, MeasurementType type, String property) {
		this(new UserLongIntegerCompositePK(userId, serverId, index), created, nodeId, sourceId, type,
				property);
	}

	@Override
	public ServerMeasurementConfiguration copyWithId(UserLongIntegerCompositePK id) {
		var copy = new ServerMeasurementConfiguration(id, created(), getNodeId(), getSourceId(),
				getType(), property());
		copyTo(copy);
		return copy;
	}

	/**
	 * Get the property.
	 *
	 * @return the property
	 */
	@SuppressWarnings("NullAway")
	public final String property() {
		return getProperty();
	}

}
