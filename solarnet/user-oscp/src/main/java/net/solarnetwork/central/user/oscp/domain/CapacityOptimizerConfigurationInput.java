/* ==================================================================
 * CapacityOptimizerConfigurationInput.java - 15/08/2022 1:17:17 pm
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

package net.solarnetwork.central.user.oscp.domain;

import java.time.Instant;
import net.solarnetwork.central.domain.UserLongPK;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;

/**
 * DTO for capacity optimizer configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class CapacityOptimizerConfigurationInput
		extends BaseOscpExternalSystemConfigurationInput<CapacityOptimizerConfiguration> {

	@Override
	public CapacityOptimizerConfiguration toEntity(UserLongPK id) {
		CapacityOptimizerConfiguration conf = new CapacityOptimizerConfiguration(id, Instant.now());
		populateConfiguration(conf);
		return conf;
	}

}
