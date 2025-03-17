/* ==================================================================
 * UserDestinationConfiguration.java - 21/03/2018 2:14:08 PM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.export.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.datum.export.domain.DestinationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * User related {@link DestinationConfiguration} entity.
 *
 * @author matt
 * @version 1.1
 */
@JsonPropertyOrder({ "id", "created", "userId", "name", "serviceIdentifier", "serviceProperties" })
@JsonIgnoreProperties("enabled")
public class UserDestinationConfiguration
		extends BaseExportConfigurationEntity<UserDestinationConfiguration>
		implements DestinationConfiguration, Serializable {

	@Serial
	private static final long serialVersionUID = 4860680512331245447L;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserDestinationConfiguration(UserLongCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param configId
	 *        the configuration ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserDestinationConfiguration(Long userId, Long configId, Instant created) {
		this(new UserLongCompositePK(userId, configId), created);
	}

	@Override
	public UserDestinationConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new UserDestinationConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

}
