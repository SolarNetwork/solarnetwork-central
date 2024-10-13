/* ==================================================================
 * CloudIntegrationConfigurationInput.java - 4/10/2024 1:23:10 pm
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

package net.solarnetwork.central.user.c2c.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.dao.BaseUserRelatedStdIdentifiableConfigurationInput;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * DTO for cloud integration configuration.
 *
 * @author matt
 * @version 1.0
 */
public class CloudIntegrationConfigurationInput extends
		BaseUserRelatedStdIdentifiableConfigurationInput<CloudIntegrationConfiguration, UserLongCompositePK>
		implements
		CloudIntegrationsConfigurationInput<CloudIntegrationConfiguration, UserLongCompositePK> {

	/**
	 * Constructor.
	 */
	public CloudIntegrationConfigurationInput() {
		super();
	}

	@Override
	public CloudIntegrationConfiguration toEntity(UserLongCompositePK id, Instant date) {
		CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(
				requireNonNullArgument(id, "id"), date);
		populateConfiguration(conf);
		return conf;
	}

}
