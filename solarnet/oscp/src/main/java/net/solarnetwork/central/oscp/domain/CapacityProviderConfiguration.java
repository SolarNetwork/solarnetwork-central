/* ==================================================================
 * CapacityProviderConfiguration.java - 11/08/2022 10:08:30 am
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

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Configuration for capacity provider integration.
 * 
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties("authRole")
public class CapacityProviderConfiguration
		extends BaseOscpExternalSystemConfiguration<CapacityProviderConfiguration> {

	private static final long serialVersionUID = 6135235926771515625L;

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
	public CapacityProviderConfiguration(UserLongCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param user
	 *        ID the user ID
	 * @param entityId
	 *        the entity ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CapacityProviderConfiguration(Long userId, Long entityId, Instant created) {
		super(userId, entityId, created);
	}

	@Override
	public CapacityProviderConfiguration clone() {
		return (CapacityProviderConfiguration) super.clone();
	}

	@Override
	public CapacityProviderConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new CapacityProviderConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@JsonIgnore
	@Override
	public AuthRoleInfo getAuthRole() {
		return new AuthRoleInfo(getId(), OscpRole.CapacityProvider);
	}

}
