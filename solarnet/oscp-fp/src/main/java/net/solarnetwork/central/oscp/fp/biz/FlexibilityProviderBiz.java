/* ==================================================================
 * FlexibilityProviderBiz.java - 16/08/2022 5:17:58 pm
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

package net.solarnetwork.central.oscp.fp.biz;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Future;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.CapacityForecast;
import net.solarnetwork.central.oscp.domain.OscpUserEvents;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.central.oscp.domain.TimeBlockAmount;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.domain.KeyValuePair;

/**
 * Business service API for Flexibility Provider.
 * 
 * @author matt
 * @version 1.0
 */
public interface FlexibilityProviderBiz extends OscpUserEvents {

	/** User event tags for Capacity Provider registration events. */
	String[] CAPACITY_PROVIDER_REGISTER_TAGS = new String[] { OSCP_TAG, CAPACITY_PROVIDER_TAG,
			REGISTER_TAG };

	/** User event tags for Capacity Provider registration error events. */
	String[] CAPACITY_PROVIDER_REGISTER_ERROR_TAGS = new String[] { OSCP_TAG, CAPACITY_PROVIDER_TAG,
			REGISTER_TAG, ERROR_TAG };

	/** User event tags for Capacity Provider heartbeat events. */
	String[] CAPACITY_PROVIDER_HEARTBEAT_TAGS = new String[] { OSCP_TAG, CAPACITY_PROVIDER_TAG,
			HEARTBEAT_TAG };

	/** User event tags for Capacity Provider heartbeat error events. */
	String[] CAPACITY_PROVIDER_HEARTBEAT_ERROR_TAGS = new String[] { OSCP_TAG, CAPACITY_PROVIDER_TAG,
			HEARTBEAT_TAG, ERROR_TAG };

	/** User event tags for Capacity Provider handshake events. */
	String[] CAPACITY_PROVIDER_HANDSHAKE_TAGS = new String[] { OSCP_TAG, CAPACITY_PROVIDER_TAG,
			HANDSHAKE_TAG };

	/** User event tags for Capacity Provider handshake error events. */
	String[] CAPACITY_PROVIDER_HANDSHAKE_ERROR_TAGS = new String[] { OSCP_TAG, CAPACITY_PROVIDER_TAG,
			HANDSHAKE_TAG, ERROR_TAG };

	/**
	 * User event tags for Capacity Provider update group capacity forecast
	 * events.
	 */
	String[] CAPACITY_PROVIDER_UPDATE_GROUP_CAPACITY_FORECAST_TAGS = new String[] { OSCP_TAG,
			CAPACITY_PROVIDER_TAG, UPDATE_GROUP_CAPACITY_FORECAST_TAG };

	/**
	 * User event tags for Capacity Provider update group capacity forecast
	 * error events.
	 */
	String[] CAPACITY_PROVIDER_UPDATE_GROUP_CAPACITY_FORECAST_ERROR_TAGS = new String[] { OSCP_TAG,
			CAPACITY_PROVIDER_TAG, UPDATE_GROUP_CAPACITY_FORECAST_TAG, ERROR_TAG };

	/** User event tags for Capacity Optimizer heartbeat events. */
	String[] CAPACITY_OPTIMIZER_HEARTBEAT_TAGS = new String[] { OSCP_TAG, CAPACITY_OPTIMIZER_TAG,
			HEARTBEAT_TAG };

	/** User event tags for Capacity Optimizer heartbeat error events. */
	String[] CAPACITY_OPTIMIZER_HEARTBEAT_ERROR_TAGS = new String[] { OSCP_TAG, CAPACITY_OPTIMIZER_TAG,
			HEARTBEAT_TAG, ERROR_TAG };

	/**
	 * User event tags for Capacity Optimizer adjust group capacity forecast
	 * events.
	 */
	String[] CAPACITY_OPTIMIZER_ADJUST_GROUP_CAPACITY_FORECAST_TAGS = new String[] { OSCP_TAG,
			CAPACITY_OPTIMIZER_TAG, ADJUST_GROUP_CAPACITY_FORECAST_TAG };

	/**
	 * User event tags for Capacity Optimizer adjust group capacity forecast
	 * error events.
	 */
	String[] CAPACITY_OPTIMIZER_ADJUST_GROUP_CAPACITY_FORECAST_ERROR_TAGS = new String[] { OSCP_TAG,
			CAPACITY_OPTIMIZER_TAG, ADJUST_GROUP_CAPACITY_FORECAST_TAG, ERROR_TAG };

	/**
	 * User event tags for Capacity Optimizer group capacity compliance error
	 * events.
	 */
	String[] CAPACITY_OPTIMIZER_GROUP_CAPACITY_COMPLIANCE_TAGS = new String[] { OSCP_TAG,
			CAPACITY_OPTIMIZER_TAG, GROUP_CAPACITY_COMPLIANCE_ERROR_TAG };

	/**
	 * User event tags for Capacity Optimizer group capacity compliance error
	 * error events.
	 */
	String[] CAPACITY_OPTIMIZER_GROUP_CAPACITY_COMPLIANCE_TAGS_ERROR_TAGS = new String[] { OSCP_TAG,
			CAPACITY_OPTIMIZER_TAG, GROUP_CAPACITY_COMPLIANCE_ERROR_TAG, ERROR_TAG };

	/**
	 * Register an external system using an authorization token created in
	 * SolarNetwork and shared with the system through an external process (e.g.
	 * email, phone, etc).
	 * 
	 * <p>
	 * The {@code externalSystemReady} parameter allows the calling code to
	 * signal to the registration process that the external system is ready to
	 * receive the corresponding registration callback to complete the
	 * registration process.
	 * </p>
	 * 
	 * @param authInfo
	 *        the authorization info of the external system
	 * @param externalSystemToken
	 *        the authorization token to use when making requests to the
	 *        external system
	 * @param versionUrl
	 *        the external system's OSCP version and base URL to use
	 * @param externalSystemReady
	 *        a future that will be completed when it is OK to initiate the
	 *        response callback to the external system
	 * @throws AuthorizationException
	 *         with
	 *         {@link AuthorizationException.Reason#REGISTRATION_NOT_CONFIRMED}
	 *         if the system configuration associated with {@code authInfo} does
	 *         not exist
	 */
	void register(AuthRoleInfo authInfo, String externalSystemToken, KeyValuePair versionUrl,
			Future<?> externalSystemReady) throws AuthorizationException;

	/**
	 * Initiate a handshake to provide desired system settings.
	 * 
	 * @param authInfo
	 *        the authorization info of the external system
	 * @param settings
	 *        the desired settings
	 * @param requestIdentifier
	 *        the OSCP request identifier, to correlate the response to
	 * @param externalSystemReady
	 *        a future that will be completed when it is OK to initiate the
	 *        response callback to the external system
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if the
	 *         system configuration associated with {@code authInfo} does not
	 *         exist
	 */
	void handshake(AuthRoleInfo authInfo, SystemSettings settings, String requestIdentifier,
			Future<?> externalSystemReady);

	/**
	 * Handle a heartbeat from an external system.
	 * 
	 * @param authInfo
	 *        the authorization info of the external system
	 * @param expiresDate
	 *        the date after which the external system "liveness" expires (i.e.
	 *        another heartbeat from the system is expected before this date)
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if the
	 *         system configuration associated with {@code authInfo} does not
	 *         exist
	 */
	void heartbeat(AuthRoleInfo authInfo, Instant expiresDate);

	/**
	 * Process an update group capacity forecast from a Capacity Provider, by
	 * forwarding the request to associated Capacity Optimizer.
	 * 
	 * @param authInfo
	 *        the authorization info of the external system
	 * @param groupIdentifier
	 *        the OSCP group identifier
	 * @param forecastIdentifier
	 *        the identifier for the forecast, must be provided for
	 *        {@link #groupCapacityComplianceError(AuthRoleInfo, String, String, String, List)}
	 *        to work properly
	 * @param forecast
	 *        the forecast
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if the
	 *         system configuration associated with {@code authInfo} does not
	 *         exist
	 */
	void updateGroupCapacityForecast(AuthRoleInfo authInfo, String groupIdentifier,
			String forecastIdentifier, CapacityForecast forecast);

	/**
	 * Process an adjust group capacity forecast from a Capacity Optimizer, by
	 * forwarding the message to the associated Capacity Provider.
	 * 
	 * @param authInfo
	 *        the authorization info of the external system
	 * @param groupIdentifier
	 *        the OSCP group identifier
	 * @param forecast
	 *        the forecast
	 * @param requestIdentifier
	 *        the OSCP request identifier
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if the
	 *         system configuration associated with {@code authInfo} does not
	 *         exist
	 */
	void adjustGroupCapacityForecast(AuthRoleInfo authInfo, String groupIdentifier,
			String requestIdentifier, CapacityForecast forecast);

	/**
	 * Process a group capacity compliance error from a Capacity Optimizer, by
	 * forwarding the message to the associated Capacity Provider.
	 * 
	 * @param authInfo
	 *        the authorization info of the external system
	 * @param groupIdentifier
	 *        the OSCP group identifier
	 * @param forecastIdentifier
	 *        the identifier of the forecast the compliance error refers to
	 * @param message
	 *        the compliance message that describes the issue
	 * @param blocks
	 *        the forecast blocks that are non-compliant
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if the
	 *         system configuration associated with {@code authInfo} does not
	 *         exist
	 */
	void groupCapacityComplianceError(AuthRoleInfo authInfo, String groupIdentifier,
			String forecastIdentifier, String message, List<TimeBlockAmount> blocks);

}
