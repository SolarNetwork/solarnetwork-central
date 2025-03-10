/* ==================================================================
 * CloudIntegrationConfigurationDao.java - 1/10/2024 7:39:45 am
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

package net.solarnetwork.central.c2c.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Map;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.common.dao.GenericCompositeKey2Dao;
import net.solarnetwork.central.dao.UserModifiableEnabledStatusDao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.FilterableDao;

/**
 * DAO API for {@link CloudIntegrationConfiguration} entities.
 *
 * @author matt
 * @version 1.1
 */
public interface CloudIntegrationConfigurationDao
		extends GenericCompositeKey2Dao<CloudIntegrationConfiguration, UserLongCompositePK, Long, Long>,
		FilterableDao<CloudIntegrationConfiguration, UserLongCompositePK, CloudIntegrationFilter>,
		UserModifiableEnabledStatusDao<CloudIntegrationFilter> {

	/**
	 * Convenient method to find the integration associated with a datum stream.
	 *
	 * @param datumStreamId
	 *        the datum stream ID to find the integration for
	 * @return the integration, or {@literal null} if not found
	 * @since 1.1
	 */
	default CloudIntegrationConfiguration integrationForDatumStream(UserLongCompositePK datumStreamId) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(datumStreamId, "datumStreamId").getUserId());
		filter.setDatumStreamId(datumStreamId.getEntityId());

		var results = findFiltered(filter);
		return (results.getReturnedResultCount() > 0 ? results.iterator().next() : null);
	}

	/**
	 * Save an authorization state value on an integration.
	 *
	 * @param id
	 *        the ID of the integration to update
	 * @param state
	 *        the state to save, or {@code null} to remove the state value
	 * @param expectedState
	 *        an optional expected state; if provided then only update the state
	 *        if a state value already exists with this value
	 * @return {@code true} if a matching record was updated
	 */
	boolean saveOAuthAuthorizationState(UserLongCompositePK id, String state, String expectedState);

	/**
	 * Save service properties, replacing any existing properties with the same
	 * values while preserving any existing properties.
	 *
	 * @param id
	 *        the ID of the integration to update
	 * @param serviceProperties
	 *        the properties to merge onto the integration's service properties
	 * @return {@code true} if a matching record was updated
	 */
	boolean mergeServiceProperties(UserLongCompositePK id, Map<String, ?> serviceProperties);

}
