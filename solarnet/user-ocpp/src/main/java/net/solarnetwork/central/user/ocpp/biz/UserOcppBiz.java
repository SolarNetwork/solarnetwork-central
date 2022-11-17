/* ==================================================================
 * UserOcppBiz.java - 29/02/2020 4:24:46 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.ocpp.biz;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusFilter;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargeSession;

/**
 * Service API for SolarUser OCPP support.
 * 
 * @author matt
 * @version 1.1
 */
public interface UserOcppBiz {

	/**
	 * Get an OCPP system user for a given username.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get OCPP system user for
	 * @param username
	 *        the username to find
	 * @return the system user
	 * @throws RuntimeException
	 *         if not available
	 */
	CentralSystemUser systemUserForUser(Long userId, String username);

	/**
	 * Get an OCPP system user for a given ID.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get OCPP system user for
	 * @param id
	 *        the system user ID to find
	 * @return the system user
	 * @throws RuntimeException
	 *         if not available
	 */
	CentralSystemUser systemUserForUser(Long userId, Long id);

	/**
	 * Delete an OCPP system user for a given ID.
	 * 
	 * @param userId
	 *        the SolarUser user ID to delete the OCPP system user for
	 * @param id
	 *        the system user ID to delete
	 * @throws RuntimeException
	 *         if not available
	 */
	void deleteUserSystemUser(Long userId, Long id);

	/**
	 * List the available OCPP system users for a given user.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get OCPP system users for
	 * @return all available system users; never {@literal null}
	 */
	Collection<CentralSystemUser> systemUsersForUser(Long userId);

	/**
	 * Create a new OCPP system user, or update an existing user.
	 * 
	 * @param systemUser
	 *        the details to save
	 * @return the persisted system user, with any default values populated and
	 *         ID assigned if creating a new entity
	 */
	CentralSystemUser saveSystemUser(CentralSystemUser systemUser);

	/**
	 * Get an OCPP authorization for a given ID.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get OCPP authorization for
	 * @param id
	 *        the authorization ID to find
	 * @return the authorization
	 * @throws RuntimeException
	 *         if not available
	 */
	CentralAuthorization authorizationForUser(Long userId, Long id);

	/**
	 * Delete an OCPP authorization for a given ID.
	 * 
	 * @param userId
	 *        the SolarUser user ID to delete the OCPP authorization for
	 * @param id
	 *        the authorization ID to delete
	 * @throws RuntimeException
	 *         if not available
	 */
	void deleteUserAuthorization(Long userId, Long id);

	/**
	 * Get the available OCPP authorizations for a given user.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get OCPP authorizations for
	 * @return all available authorizations; never {@literal null}
	 */
	Collection<CentralAuthorization> authorizationsForUser(Long userId);

	/**
	 * Create a new OCPP authorization, or update an existing authorization.
	 * 
	 * @param authorization
	 *        the details to save
	 * @return the persisted authorization, with any default values populated
	 *         and ID assigned if creating a new entity
	 */
	CentralAuthorization saveAuthorization(CentralAuthorization authorization);

	/**
	 * List the available charge points for a given user.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get charge points for
	 * @return all available charge points; never {@literal null}
	 */
	Collection<CentralChargePoint> chargePointsForUser(Long userId);

	/**
	 * Create a new charge point registration, or update an existing
	 * registration.
	 * 
	 * @param chargePoint
	 *        the details to save
	 * @return the persisted charge point, with any default values populated and
	 *         ID assigned if creating a new entity
	 */
	CentralChargePoint saveChargePoint(CentralChargePoint chargePoint);

	/**
	 * Get an OCPP charge point for a given ID.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get OCPP charge point for
	 * @param id
	 *        the charge point ID to find
	 * @return the charge point
	 * @throws RuntimeException
	 *         if not available
	 */
	CentralChargePoint chargePointForUser(Long userId, Long id);

	/**
	 * Delete an OCPP charge point for a given ID.
	 * 
	 * @param userId
	 *        the SolarUser user ID to delete the OCPP charge point for
	 * @param id
	 *        the charge point ID to delete
	 * @throws RuntimeException
	 *         if not available
	 */
	void deleteUserChargePoint(Long userId, Long id);

	/**
	 * Get an OCPP connector for a given ID.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get OCPP connector for
	 * @param id
	 *        the connector ID to find
	 * @return the connector
	 * @throws RuntimeException
	 *         if not available
	 */
	CentralChargePointConnector chargePointConnectorForUser(Long userId, ChargePointConnectorKey id);

	/**
	 * Delete an OCPP connector for a given ID.
	 * 
	 * @param userId
	 *        the SolarUser user ID to delete the OCPP connector for
	 * @param id
	 *        the connector ID to delete
	 * @throws RuntimeException
	 *         if not available
	 */
	void deleteUserChargePointConnector(Long userId, ChargePointConnectorKey id);

	/**
	 * Get the available OCPP connectors for a given user.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get OCPP connectors for
	 * @return all available connectors; never {@literal null}
	 */
	Collection<CentralChargePointConnector> chargePointConnectorsForUser(Long userId);

	/**
	 * Create a new OCPP connector, or update an existing connector.
	 * 
	 * @param connector
	 *        the details to save
	 * @return the persisted connector, with any default values populated and ID
	 *         assigned if creating a new entity
	 */
	CentralChargePointConnector saveChargePointConnector(CentralChargePointConnector connector);

	/**
	 * Get a charge point settings for a given ID.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get OCPP connector for
	 * @param chargePointId
	 *        the charge point ID to find settings for
	 * @return the settings
	 * @throws RuntimeException
	 *         if not available
	 */
	ChargePointSettings chargePointSettingsForUser(Long userId, Long chargePointId);

	/**
	 * Delete a charge point settings entity for a given ID.
	 * 
	 * @param userId
	 *        the SolarUser user ID to delete the charge point settings for
	 * @param chargePointId
	 *        the charge point ID to delete the settings for
	 * @throws RuntimeException
	 *         if not available
	 */
	void deleteUserChargePointSettings(Long userId, Long chargePointId);

	/**
	 * Get the available charge point settings entities for a given user.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get settings entities for
	 * @return all available settings entities; never {@literal null}
	 */
	Collection<ChargePointSettings> chargePointSettingsForUser(Long userId);

	/**
	 * Create a new charge point settings entity, or update an existing entity.
	 * 
	 * @param settings
	 *        the settings to save
	 * @return the persisted settings, with any default values populated and ID
	 *         assigned if creating a new entity
	 */
	ChargePointSettings saveChargePointSettings(ChargePointSettings settings);

	/**
	 * API for querying for a filtered set of charger point statuses, streaming
	 * the results.
	 * 
	 * @param filter
	 *        the query filter
	 * @param processor
	 *        the processor for the results
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.1
	 */
	void findFilteredChargePointStatus(ChargePointStatusFilter filter,
			FilteredResultsProcessor<ChargePointStatus> processor, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) throws IOException;

	/**
	 * Get the available settings entities for a given user.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get settings entity for
	 * @return the settings entity, or {@literal null} if not available
	 */
	UserSettings settingsForUser(Long userId);

	/**
	 * Delete a settings entity for a given user.
	 * 
	 * @param userId
	 *        the SolarUser user ID to delete the settings for
	 * @throws RuntimeException
	 *         if not available
	 */
	void deleteUserSettings(Long userId);

	/**
	 * Create a new settings entity, or update an existing entity.
	 * 
	 * @param settings
	 *        the settings to save
	 * @return the persisted settings, with any default values populated and ID
	 *         assigned if creating a new entity
	 */
	UserSettings saveSettings(UserSettings settings);

	/**
	 * Get a charge session for a given user.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get charge session for
	 * @param sessionId
	 *        the charge session ID to get
	 * @return the session entity, or {@literal null} if not available
	 */
	ChargeSession chargeSessionForUser(Long userId, UUID sessionId);

	/**
	 * Get all incomplete charge sessions for a given charge point.
	 * 
	 * @param userId
	 *        the SolarUser user ID to delete the charge point settings for
	 * @param chargePointId
	 *        the charge point ID to get the sessions for
	 * @return all matching incomplete sessions; never {@literal null}
	 */
	Collection<ChargeSession> incompleteChargeSessionsForChargePoint(Long userId, Long chargePointId);

}
