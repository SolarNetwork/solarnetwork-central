/* ==================================================================
 * CloudIntegrationsConfigurationInput.java - 4/10/2024 11:18:16 am
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

import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.dao.UserRelatedStdInput;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;

/**
 * API for cloud integrations configuration input.
 *
 * @param <C>
 *        the entity type
 * @param <K>
 *        the key type
 * @author matt
 * @version 1.0
 */
public interface CloudIntegrationsConfigurationInput<C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>>
		extends UserRelatedStdInput<C, K> {
	// empty
}
