/* ==================================================================
 * UserOcppController.java - 1/03/2020 7:36:49 am
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.web.domain.Response.response;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.ocpp.biz.UserOcppBiz;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.web.domain.Response;

/**
 * Web service API for OCPP management.
 * 
 * @author matt
 * @version 2.0
 */
@GlobalExceptionRestController
@RestController("v1OcppController")
@RequestMapping(value = { "/sec/ocpp", "/v1/sec/user/ocpp" })
public class UserOcppController {

	private final UserOcppBiz userOcppBiz;

	/**
	 * Constructor.
	 * 
	 * @param userOcppBiz
	 *        the user OCPP service
	 */

	public UserOcppController(@Autowired(required = false) UserOcppBiz userOcppBiz) {
		super();
		this.userOcppBiz = userOcppBiz;
	}

	/**
	 * Get the {@link UserOcppBiz}.
	 * 
	 * @return the service; never {@literal null}
	 * @throws UnsupportedOperationException
	 *         if the service is not available
	 */
	private UserOcppBiz userOcppBiz() {
		if ( userOcppBiz == null ) {
			throw new UnsupportedOperationException("OCPP service not available.");
		}
		return userOcppBiz;
	}

	/**
	 * Create a response entity out of a save operation on an entity.
	 * 
	 * @param <T>
	 *        the entity type
	 * @param id
	 *        the entity ID that was provided on the request to be saved
	 * @param out
	 *        the entity that was saved and should be returned in the response
	 * @return a response entity, with {@link HttpStatus#CREATED} if the
	 *         {@code in} has a {@literal null} primary key or
	 *         {@link HttpStatus#OK} otherwise
	 */
	private <T extends Entity<?>> ResponseEntity<Response<T>> responseForSave(Object id, T out) {
		HttpStatus status = id == null ? HttpStatus.CREATED : HttpStatus.OK;
		return new ResponseEntity<Response<T>>(response(out), status);
	}

	/**
	 * Get all available charge points for the current user.
	 * 
	 * @return the charge points
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/authorizations")
	public Response<Collection<CentralAuthorization>> availableAuthorizations() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CentralAuthorization> list = userOcppBiz().authorizationsForUser(userId);
		return response(list);
	}

	/**
	 * Save an authorization.
	 * 
	 * @param authorization
	 *        the authorization to save
	 * @return the saved authorization
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/authorizations")
	public ResponseEntity<Response<CentralAuthorization>> saveAuthorization(
			@RequestBody CentralAuthorization authorization) {
		return responseForSave(authorization.getId(), userOcppBiz().saveAuthorization(authorization));
	}

	/**
	 * View an authorization.
	 * 
	 * @param id
	 *        the ID of the authorization to view
	 * @return the authorization
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/authorizations/{id}")
	public Response<CentralAuthorization> viewAuthorization(@PathVariable("id") Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return response(userOcppBiz().authorizationForUser(userId, id));
	}

	/**
	 * Delete an authorization.
	 * 
	 * @param id
	 *        the ID of the authorization to delete
	 * @return the result
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/authorizations/{id}")
	public Response<Void> deleteAuthorization(@PathVariable("id") Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOcppBiz().deleteUserAuthorization(userId, id);
		return response(null);
	}

	/**
	 * Get all available charge points for the current user.
	 * 
	 * @return the charge points
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/chargers")
	public Response<Collection<CentralChargePoint>> availableChargePoints() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CentralChargePoint> list = userOcppBiz().chargePointsForUser(userId);
		return response(list);
	}

	/**
	 * Save a charge point.
	 * 
	 * @param chargePoint
	 *        the charge point to save
	 * @return the saved charge point
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/chargers")
	public ResponseEntity<Response<CentralChargePoint>> saveChargePoint(
			@RequestBody CentralChargePoint chargePoint) {
		return responseForSave(chargePoint.getId(), userOcppBiz().saveChargePoint(chargePoint));
	}

	/**
	 * View a specific charge point.
	 * 
	 * @param id
	 *        the ID of the charge point to view
	 * @return the system user
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/chargers/{id}")
	public Response<CentralChargePoint> viewChargePoint(@PathVariable("id") Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return response(userOcppBiz().chargePointForUser(userId, id));
	}

	/**
	 * Delete a specific charge point.
	 * 
	 * @param id
	 *        the ID of the charge point to delete
	 * @return the result
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/chargers/{id}")
	public Response<Void> deleteChargePoint(@PathVariable("id") Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOcppBiz().deleteUserChargePoint(userId, id);
		return response(null);
	}

	/**
	 * Get all available charge point settings for the current user.
	 * 
	 * @return the charge point settings
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/chargers/settings")
	public Response<Collection<ChargePointSettings>> availableChargePointSettings() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<ChargePointSettings> list = userOcppBiz().chargePointSettingsForUser(userId);
		return response(list);
	}

	/**
	 * Save a charge point settings.
	 * 
	 * @param chargePointSettings
	 *        the charge point settings to save
	 * @return the saved charge point settings
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/chargers/settings")
	public ResponseEntity<Response<ChargePointSettings>> saveChargePointSettings(
			@RequestBody ChargePointSettings chargePointSettings) {
		return responseForSave(chargePointSettings.getId(),
				userOcppBiz().saveChargePointSettings(chargePointSettings));
	}

	/**
	 * View a specific charge point settings.
	 * 
	 * @param id
	 *        the ID of the charge point settings to view
	 * @return the settings
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/chargers/{id}/settings")
	public Response<ChargePointSettings> viewChargePointSettings(@PathVariable("id") Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return response(userOcppBiz().chargePointSettingsForUser(userId, id));
	}

	/**
	 * Delete a specific charge point settings.
	 * 
	 * @param id
	 *        the ID of the charge point settings to delete
	 * @return the result
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/chargers/{id}/settings")
	public Response<Void> deleteChargePointSettings(@PathVariable("id") Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOcppBiz().deleteUserChargePointSettings(userId, id);
		return response(null);
	}

	/**
	 * Get all available connectors for the current user.
	 * 
	 * @return the connectors
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/connectors")
	public Response<Collection<CentralChargePointConnector>> availableConnectors() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CentralChargePointConnector> list = userOcppBiz()
				.chargePointConnectorsForUser(userId);
		return response(list);
	}

	/**
	 * Save a connector.
	 * 
	 * @param connector
	 *        the connector to save
	 * @return the saved connector
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/connectors")
	public ResponseEntity<Response<CentralChargePointConnector>> saveConnector(
			@RequestBody CentralChargePointConnector connector) {
		ChargePointConnectorKey id = connector.getId();
		if ( id == null ) {
			throw new IllegalArgumentException("The connector ID must be specified.");
		}
		if ( id.getConnectorId() < 0 ) {
			throw new IllegalArgumentException("The connector ID must not be negative.");
		}
		return responseForSave(connector.getId(), userOcppBiz().saveChargePointConnector(connector));
	}

	/**
	 * View a specific credential.
	 * 
	 * @param chargePointId
	 *        the charge point ID
	 * @param connectorId
	 *        the ID of the connector to view
	 * @return the system user
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/connectors/{chargePointId}/{connectorId}")
	public Response<CentralChargePointConnector> viewConnector(
			@PathVariable("chargePointId") long chargePointId,
			@PathVariable("connectorId") int connectorId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return response(userOcppBiz().chargePointConnectorForUser(userId,
				new ChargePointConnectorKey(chargePointId, connectorId)));
	}

	/**
	 * Delete a specific credential.
	 * 
	 * @param chargePointId
	 *        the ID of the charge point
	 * @param connectorId
	 *        the connector ID to delete
	 * @return the result
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/connectors/{chargePointId}/{connectorId}")
	public Response<Void> deleteConnector(@PathVariable("chargePointId") long chargePointId,
			@PathVariable("connectorId") int connectorId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOcppBiz().deleteUserChargePointConnector(userId,
				new ChargePointConnectorKey(chargePointId, connectorId));
		return response(null);
	}

	/**
	 * Get all available OCPP system users for the current user.
	 * 
	 * @return the system users
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/credentials", params = "!username")
	public Response<Collection<CentralSystemUser>> availableSystemUsers() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CentralSystemUser> list = userOcppBiz().systemUsersForUser(userId);
		return response(list);
	}

	/**
	 * View a system user by its username.
	 * 
	 * @param username
	 *        the username of the system user to view
	 * @return the system user
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/credentials", params = "username")
	public Response<CentralSystemUser> viewSystemUser(@RequestParam("username") String username) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return response(userOcppBiz().systemUserForUser(userId, username));
	}

	/**
	 * Save a system user.
	 * 
	 * @param systemUser
	 *        the system user to save
	 * @return the saved system user
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/credentials")
	public ResponseEntity<Response<CentralSystemUser>> saveSystemUser(
			@RequestBody CentralSystemUser systemUser) {
		return responseForSave(systemUser.getId(), userOcppBiz().saveSystemUser(systemUser));
	}

	/**
	 * View a specific credential.
	 * 
	 * @param id
	 *        the ID of the system user to view
	 * @return the system user
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/credentials/{id}")
	public Response<CentralSystemUser> viewSystemUser(@PathVariable("id") Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return response(userOcppBiz().systemUserForUser(userId, id));
	}

	/**
	 * Delete a specific credential.
	 * 
	 * @param id
	 *        the ID of the system user to delete
	 * @return the result
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/credentials/{id}")
	public Response<Void> deleteSystemUser(@PathVariable("id") Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOcppBiz().deleteUserSystemUser(userId, id);
		return response(null);
	}

	/**
	 * Save user settings.
	 * 
	 * @param userSettings
	 *        the settings to save
	 * @return the saved settings
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/settings")
	public ResponseEntity<Response<UserSettings>> saveSettings(@RequestBody UserSettings userSettings) {
		return responseForSave(userSettings.getId(), userOcppBiz().saveSettings(userSettings));
	}

	/**
	 * View the user settings.
	 * 
	 * @return the user settings
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/settings")
	public Response<UserSettings> viewSettings() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return response(userOcppBiz().settingsForUser(userId));
	}

	/**
	 * Delete the user settings.
	 * 
	 * @return the result
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/settings")
	public Response<Void> deleteSettings() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOcppBiz().deleteUserSettings(userId);
		return response(null);
	}

}
