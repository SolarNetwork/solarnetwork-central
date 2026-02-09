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

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V16;
import static net.solarnetwork.central.user.ocpp.config.UserOcppBizConfig.CHARGE_POINT_ACTION_STATUS_FILTER;
import static net.solarnetwork.central.user.ocpp.config.UserOcppBizConfig.CHARGE_POINT_STATUS_FILTER;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.ocpp.dao.BasicOcppCriteria;
import net.solarnetwork.central.ocpp.dao.ChargePointActionStatusFilter;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusFilter;
import net.solarnetwork.central.ocpp.domain.BasicOcppFilter;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatus;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.central.ocpp.util.ChargePointActionStatusSerializer;
import net.solarnetwork.central.ocpp.util.ChargePointStatusSerializer;
import net.solarnetwork.central.reg.config.JsonConfig;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.central.support.OutputSerializationSupportContext;
import net.solarnetwork.central.user.ocpp.biz.UserOcppBiz;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.central.web.WebUtils;
import net.solarnetwork.codec.PropertySerializerRegistrar;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargeSession;
import net.solarnetwork.ocpp.domain.ChargeSessionEndReason;
import tools.jackson.databind.ObjectMapper;

/**
 * Web service API for OCPP management.
 *
 * @author matt
 * @version 3.1
 */
@Profile(OCPP_V16)
@GlobalExceptionRestController
@RestController("v1OcppController")
@RequestMapping(value = { "/u/sec/ocpp", "/api/v1/sec/user/ocpp" })
public class UserOcppController {

	private final UserOcppBiz userOcppBiz;
	private final ObjectMapper objectMapper;
	private final ObjectMapper cborObjectMapper;
	private final PropertySerializerRegistrar propertySerializerRegistrar;
	private Validator chargePointStatusFilterValidator;
	private Validator chargePointActionStatusFilterValidator;

	/**
	 * Constructor.
	 *
	 * @param userOcppBiz
	 *        the user OCPP service
	 * @param objectMapper
	 *        the object mapper to use for JSON
	 * @param cborObjectMapper
	 *        the mapper to use for CBOR
	 * @param propertySerializerRegistrar
	 *        the registrar to use (may be {@literal null}
	 */
	public UserOcppController(UserOcppBiz userOcppBiz,
			@Qualifier(JsonConfig.JSON_STREAMING_MAPPER) ObjectMapper objectMapper,
			@Qualifier(JsonConfig.CBOR_STREAMING_MAPPER) ObjectMapper cborObjectMapper,
			PropertySerializerRegistrar propertySerializerRegistrar) {
		super();
		this.userOcppBiz = requireNonNullArgument(userOcppBiz, "userOcppBiz");
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.cborObjectMapper = requireNonNullArgument(cborObjectMapper, "cborObjectMapper");
		this.propertySerializerRegistrar = propertySerializerRegistrar;
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
	private <T extends Entity<?>> ResponseEntity<Result<T>> responseForSave(Object id, T out) {
		HttpStatus status = id == null ? HttpStatus.CREATED : HttpStatus.OK;
		return new ResponseEntity<>(success(out), status);
	}

	/**
	 * Get all available charge points for the current user.
	 *
	 * @return the charge points
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/authorizations")
	public Result<Collection<CentralAuthorization>> availableAuthorizations() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CentralAuthorization> list = userOcppBiz().authorizationsForUser(userId);
		return success(list);
	}

	/**
	 * Save an authorization.
	 *
	 * @param authorization
	 *        the authorization to save
	 * @return the saved authorization
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/authorizations")
	public ResponseEntity<Result<CentralAuthorization>> saveAuthorization(
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
	public Result<CentralAuthorization> viewAuthorization(@PathVariable Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userOcppBiz().authorizationForUser(userId, id));
	}

	/**
	 * Delete an authorization.
	 *
	 * @param id
	 *        the ID of the authorization to delete
	 * @return the result
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/authorizations/{id}")
	public Result<Void> deleteAuthorization(@PathVariable Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOcppBiz().deleteUserAuthorization(userId, id);
		return success();
	}

	/**
	 * Get all available charge points for the current user.
	 *
	 * @return the charge points
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/chargers")
	public Result<Collection<CentralChargePoint>> availableChargePoints() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CentralChargePoint> list = userOcppBiz().chargePointsForUser(userId);
		return success(list);
	}

	/**
	 * Find available charge points for the current user and an optional filter
	 *
	 * @return the charge points
	 * @since 3.1
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/chargers/find")
	public Result<FilterResults<CentralChargePoint, Long>> availableChargePoints(
			BasicOcppFilter filter) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userOcppBiz().listChargePointsForUser(userId, filter));
	}

	/**
	 * Save a charge point.
	 *
	 * @param chargePoint
	 *        the charge point to save
	 * @return the saved charge point
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/chargers")
	public ResponseEntity<Result<CentralChargePoint>> saveChargePoint(
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
	public Result<CentralChargePoint> viewChargePoint(@PathVariable Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userOcppBiz().chargePointForUser(userId, id));
	}

	/**
	 * Delete a specific charge point.
	 *
	 * @param id
	 *        the ID of the charge point to delete
	 * @return the result
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/chargers/{id}")
	public Result<Void> deleteChargePoint(@PathVariable Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOcppBiz().deleteUserChargePoint(userId, id);
		return success();
	}

	/**
	 * Get all available charge point settings for the current user.
	 *
	 * @return the charge point settings
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/chargers/settings")
	public Result<Collection<ChargePointSettings>> availableChargePointSettings() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<ChargePointSettings> list = userOcppBiz().chargePointSettingsForUser(userId);
		return success(list);
	}

	/**
	 * Save a charge point settings.
	 *
	 * @param chargePointSettings
	 *        the charge point settings to save
	 * @return the saved charge point settings
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/chargers/settings")
	public ResponseEntity<Result<ChargePointSettings>> saveChargePointSettings(
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
	public Result<ChargePointSettings> viewChargePointSettings(@PathVariable Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userOcppBiz().chargePointSettingsForUser(userId, id));
	}

	/**
	 * Delete a specific charge point settings.
	 *
	 * @param id
	 *        the ID of the charge point settings to delete
	 * @return the result
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/chargers/{id}/settings")
	public Result<Void> deleteChargePointSettings(@PathVariable Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOcppBiz().deleteUserChargePointSettings(userId, id);
		return success();
	}

	/**
	 * Query for charger status.
	 *
	 * @param filter
	 *        the query filter
	 * @param validationResult
	 *        the binding result
	 * @param accept
	 *        the desired content type
	 * @param response
	 *        the HTTP response
	 * @throws IOException
	 *         if an IO error occurs
	 * @since 2.1
	 */
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, value = "/chargers/status")
	public void listChargePointStatus(final BasicOcppCriteria filter,
			final BindingResult validationResult, @RequestHeader(HttpHeaders.ACCEPT) final String accept,
			final HttpServletResponse response) throws IOException {
		if ( chargePointStatusFilterValidator != null ) {
			chargePointStatusFilterValidator.validate(filter, validationResult);
			if ( validationResult.hasErrors() ) {
				throw new ValidationException(validationResult);
			}
		}
		final Long userId = SecurityUtils.getCurrentActorUserId();
		filter.setUserId(userId);
		final List<MediaType> acceptTypes = MediaType.parseMediaTypes(accept);
		try (FilteredResultsProcessor<ChargePointStatus> processor = WebUtils
				.filteredResultsProcessorForType(acceptTypes, response,
						new OutputSerializationSupportContext<>(objectMapper, cborObjectMapper,
								ChargePointStatusSerializer.INSTANCE, propertySerializerRegistrar))) {
			userOcppBiz().findFilteredChargePointStatus(filter, processor, null, null, null);
		}
	}

	/**
	 * Query for charger action status.
	 *
	 * @param filter
	 *        the query filter
	 * @param validationResult
	 *        the binding result
	 * @param accept
	 *        the desired content type
	 * @param response
	 *        the HTTP response
	 * @throws IOException
	 *         if an IO error occurs
	 * @since 2.1
	 */
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, value = "/chargers/action-status")
	public void listChargePointActionStatus(final BasicOcppCriteria filter,
			final BindingResult validationResult, @RequestHeader(HttpHeaders.ACCEPT) final String accept,
			final HttpServletResponse response) throws IOException {
		if ( chargePointActionStatusFilterValidator != null ) {
			chargePointActionStatusFilterValidator.validate(filter, validationResult);
			if ( validationResult.hasErrors() ) {
				throw new ValidationException(validationResult);
			}
		}
		final Long userId = SecurityUtils.getCurrentActorUserId();
		filter.setUserId(userId);
		final List<MediaType> acceptTypes = MediaType.parseMediaTypes(accept);
		try (FilteredResultsProcessor<ChargePointActionStatus> processor = WebUtils
				.filteredResultsProcessorForType(acceptTypes, response,
						new OutputSerializationSupportContext<>(objectMapper, cborObjectMapper,
								ChargePointActionStatusSerializer.INSTANCE,
								propertySerializerRegistrar))) {
			userOcppBiz().findFilteredChargePointActionStatus(filter, processor, null, null, null);
		}
	}

	/**
	 * Get all available connectors for the current user.
	 *
	 * @return the connectors
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/connectors")
	public Result<Collection<CentralChargePointConnector>> availableConnectors() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CentralChargePointConnector> list = userOcppBiz()
				.chargePointConnectorsForUser(userId);
		return success(list);
	}

	/**
	 * Save a connector.
	 *
	 * @param connector
	 *        the connector to save
	 * @return the saved connector
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/connectors")
	public ResponseEntity<Result<CentralChargePointConnector>> saveConnector(
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
	 * View a specific connector.
	 *
	 * @param chargePointId
	 *        the charge point ID
	 * @return the connectors
	 * @since 3.1
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/connectors/{chargePointId}")
	public Result<Collection<CentralChargePointConnector>> viewConnectorsForCharger(
			@PathVariable long chargePointId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userOcppBiz().chargePointConnectorsForUser(userId, chargePointId));
	}

	/**
	 * View a specific connector.
	 *
	 * @param chargePointId
	 *        the charge point ID
	 * @param connectorId
	 *        the ID of the connector to view
	 * @return the connector
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/connectors/{chargePointId}/{connectorId}")
	public Result<CentralChargePointConnector> viewConnector(@PathVariable long chargePointId,
			@PathVariable int connectorId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userOcppBiz().chargePointConnectorForUser(userId,
				new ChargePointConnectorKey(chargePointId, connectorId)));
	}

	/**
	 * Delete a specific connector.
	 *
	 * @param chargePointId
	 *        the ID of the charge point
	 * @param connectorId
	 *        the connector ID to delete
	 * @return the result
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/connectors/{chargePointId}/{connectorId}")
	public Result<Void> deleteConnector(@PathVariable long chargePointId,
			@PathVariable int connectorId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOcppBiz().deleteUserChargePointConnector(userId,
				new ChargePointConnectorKey(chargePointId, connectorId));
		return success();
	}

	/**
	 * View a specific connector.
	 *
	 * @param chargePointId
	 *        the charge point ID
	 * @param evseId
	 *        the EVSE ID
	 * @param connectorId
	 *        the ID of the connector to view
	 * @return the system user
	 * @since 2.4
	 */
	@RequestMapping(method = RequestMethod.GET,
			value = "/connectors/{chargePointId}/{evseId}/{connectorId}")
	public Result<CentralChargePointConnector> viewConnector(@PathVariable long chargePointId,
			@PathVariable int evseId, @PathVariable int connectorId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userOcppBiz().chargePointConnectorForUser(userId,
				new ChargePointConnectorKey(chargePointId, evseId, connectorId)));
	}

	/**
	 * Delete a specific connector.
	 *
	 * @param chargePointId
	 *        the ID of the charge point
	 * @param evseId
	 *        the EVSE ID
	 * @param connectorId
	 *        the connector ID to delete
	 * @return the result
	 * @since 2.4
	 */
	@RequestMapping(method = RequestMethod.DELETE,
			value = "/connectors/{chargePointId}/{evseId}/{connectorId}")
	public Result<Void> deleteConnector(@PathVariable long chargePointId, @PathVariable int evseId,
			@PathVariable int connectorId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOcppBiz().deleteUserChargePointConnector(userId,
				new ChargePointConnectorKey(chargePointId, evseId, connectorId));
		return success();
	}

	/**
	 * Get all available OCPP system users for the current user.
	 *
	 * @return the system users
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/credentials", params = "!username")
	public Result<Collection<CentralSystemUser>> availableSystemUsers() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CentralSystemUser> list = userOcppBiz().systemUsersForUser(userId);
		return success(list);
	}

	/**
	 * View a system user by its username.
	 *
	 * @param username
	 *        the username of the system user to view
	 * @return the system user
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/credentials", params = "username")
	public Result<CentralSystemUser> viewSystemUser(@RequestParam("username") String username) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userOcppBiz().systemUserForUser(userId, username));
	}

	/**
	 * Save a system user.
	 *
	 * @param systemUser
	 *        the system user to save
	 * @return the saved system user
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/credentials")
	public ResponseEntity<Result<CentralSystemUser>> saveSystemUser(
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
	public Result<CentralSystemUser> viewSystemUser(@PathVariable Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userOcppBiz().systemUserForUser(userId, id));
	}

	/**
	 * Delete a specific credential.
	 *
	 * @param id
	 *        the ID of the system user to delete
	 * @return the result
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/credentials/{id}")
	public Result<Void> deleteSystemUser(@PathVariable Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOcppBiz().deleteUserSystemUser(userId, id);
		return success();
	}

	/**
	 * Save user settings.
	 *
	 * @param userSettings
	 *        the settings to save
	 * @return the saved settings
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/settings")
	public ResponseEntity<Result<UserSettings>> saveSettings(@RequestBody UserSettings userSettings) {
		return responseForSave(userSettings.getId(), userOcppBiz().saveSettings(userSettings));
	}

	/**
	 * View the user settings.
	 *
	 * @return the user settings
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/settings")
	public Result<UserSettings> viewSettings() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userOcppBiz().settingsForUser(userId));
	}

	/**
	 * Delete the user settings.
	 *
	 * @return the result
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/settings")
	public Result<Void> deleteSettings() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOcppBiz().deleteUserSettings(userId);
		return success();
	}

	/**
	 * Find filtered charge sessions.
	 *
	 * @param filter
	 *        the filter
	 * @return the charge sessions
	 * @since 2.2
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/sessions")
	public Result<FilterResults<ChargeSession, UUID>> findFilteredChargeSessions(
			BasicOcppCriteria filter) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		filter.setUserId(userId);
		return success(userOcppBiz().findFilteredChargeSessions(filter));
	}

	/**
	 * End a charge session.
	 *
	 * @param sessionId
	 *        the ID of the charge session to end
	 * @param endReason
	 *        the end reason
	 * @param endAuthId
	 *        the optional end authorzation ID
	 * @return {@literal true} if the session is ended, {@literal false} if it
	 *         was already ended or does not exist
	 * @since 2.2
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/sessions/end")
	public Result<Boolean> endChargeSession(@RequestParam("id") UUID sessionId,
			@RequestParam("endReason") ChargeSessionEndReason endReason,
			@RequestParam(name = "endAuthId", required = false) String endAuthId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userOcppBiz().endChargeSession(userId, sessionId, endReason, endAuthId));
	}

	/**
	 * View a specific charge session.
	 *
	 * @param sessionId
	 *        the ID of the charge session to view
	 * @return the session
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/sessions/{sessionId}")
	public Result<ChargeSession> viewSession(@PathVariable UUID sessionId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		ChargeSession sess = userOcppBiz().chargeSessionForUser(userId, sessionId);
		return success(sess);
	}

	/**
	 * Get all incomplete charge sessions for the current user and charge point.
	 *
	 * @param chargePointId
	 *        the charge point ID
	 * @return the charge points
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/sessions/incomplete/{chargePointId}")
	public Result<Collection<ChargeSession>> incompleteSessionsForChargePoint(
			@PathVariable long chargePointId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<ChargeSession> list = userOcppBiz().incompleteChargeSessionsForChargePoint(userId,
				chargePointId);
		return success(list);
	}

	/**
	 * Get the charge point status filter validator to use.
	 *
	 * @return the validator
	 */
	public Validator getChargePointStatusFilterValidator() {
		return chargePointStatusFilterValidator;
	}

	/**
	 * Set the charge point status filter validator to use.
	 *
	 * @param validator
	 *        the validator to set
	 * @throws IllegalArgumentException
	 *         if {@code validator} does not support the
	 *         {@link ChargePointStatusFilter} class
	 */
	@Autowired
	@Qualifier(CHARGE_POINT_STATUS_FILTER)
	public void setChargePointStatusFilterValidator(Validator validator) {
		if ( validator != null && !validator.supports(ChargePointStatusFilter.class) ) {
			throw new IllegalArgumentException(
					"The Validator must support the ChargePointStatusFilter class.");
		}
		this.chargePointStatusFilterValidator = validator;
	}

	/**
	 * Get the charge point action status filter validator to use.
	 *
	 * @return the validator
	 */
	public Validator getChargePointActionStatusFilterValidator() {
		return chargePointActionStatusFilterValidator;
	}

	/**
	 * Set the charge point action status filter validator to use.
	 *
	 * @param validator
	 *        the validator to set
	 * @throws IllegalArgumentException
	 *         if {@code validator} does not support the
	 *         {@link ChargePointStatusFilter} class
	 */
	@Autowired
	@Qualifier(CHARGE_POINT_ACTION_STATUS_FILTER)
	public void setChargePointActionStatusFilterValidator(Validator validator) {
		if ( validator != null && !validator.supports(ChargePointActionStatusFilter.class) ) {
			throw new IllegalArgumentException(
					"The Validator must support the ChargePointActionStatusFilter class.");
		}
		this.chargePointActionStatusFilterValidator = validator;
	}

}
