/* ==================================================================
 * SystemTaskContext.java - 22/08/2022 9:11:18 am
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

package net.solarnetwork.central.oscp.util;

import java.util.Map;
import net.solarnetwork.central.oscp.dao.ExternalSystemConfigurationDao;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * Contextual information for external system related tasks related to a
 * capacity group.
 * 
 * @param <C>
 *        the configuration type
 * @author matt
 * @version 1.0
 */
public record CapacityGroupSystemTaskContext<C extends BaseOscpExternalSystemConfiguration<C>> (
		String name, OscpRole role, C config, CapacityGroupConfiguration group, String[] errorEventTags,
		String[] successEventTags, ExternalSystemConfigurationDao<C> dao, Map<String, ?> parameters)
		implements CapacityGroupTaskContext<C> {

	@Override
	public String groupIdentifier() {
		CapacityGroupConfiguration group = group();
		return (group != null ? group.getIdentifier() : null);
	}

}
