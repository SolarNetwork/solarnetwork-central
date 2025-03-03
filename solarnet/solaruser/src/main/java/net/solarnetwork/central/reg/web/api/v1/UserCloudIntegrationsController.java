/* ==================================================================
 * UserCloudIntegrationsController.java - 30/09/2024 11:46:24 am
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.central.security.SecurityUtils.getCurrentActorUserId;
import static net.solarnetwork.central.web.WebUtils.uriWithoutHost;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.service.LocalizedServiceInfoProvider.localizedServiceSettings;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import jakarta.validation.Valid;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettings;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettingsEntity;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.UserSettingsEntity;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamMappingConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamPollTaskEntityInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamPollTaskStateInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamPropertyConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamSettingsEntityInput;
import net.solarnetwork.central.user.c2c.domain.CloudIntegrationConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.UserSettingsEntityInput;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.Datum;

/**
 * Web service API for cloud integrations management.
 *
 * @author matt
 * @version 1.5
 */
@Profile(SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS)
@GlobalExceptionRestController
@RestController("v1CloudIntegrationsController")
@RequestMapping(value = { "/api/v1/sec/user/c2c", "/u/sec/c2c" })
public class UserCloudIntegrationsController {

	private final UserCloudIntegrationsBiz userCloudIntegrationsBiz;

	/**
	 * Constructor.
	 *
	 * @param userCloudIntegrationsBiz
	 *        the user cloud integrations service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserCloudIntegrationsController(UserCloudIntegrationsBiz userCloudIntegrationsBiz) {
		super();
		this.userCloudIntegrationsBiz = requireNonNullArgument(userCloudIntegrationsBiz,
				"userCloudIntegrationsBiz");
	}

	/**
	 * List the available integration services.
	 *
	 * @param locale
	 *        the desired locale
	 * @return the services
	 */
	@RequestMapping(value = "/services/integrations", method = RequestMethod.GET)
	public Result<Iterable<LocalizedServiceInfo>> availableCloudIntegrationServices(Locale locale) {
		var services = userCloudIntegrationsBiz.availableIntegrationServices();
		var result = localizedServiceSettings(services, locale);
		return success(result);
	}

	/**
	 * List the available datum stream services for a given integration service.
	 *
	 * @param identifier
	 *        the {@link CloudIntegrationService} identifier to list the
	 *        available datum stream services
	 * @param locale
	 *        the desired locale
	 * @return the services
	 */
	@RequestMapping(value = "/services/datum-streams", method = RequestMethod.GET)
	public Result<Iterable<LocalizedServiceInfo>> availableCloudDatumStreamServicesForIntegration(
			@RequestParam("identifier") String identifier, Locale locale) {
		final CloudIntegrationService service = userCloudIntegrationsBiz.integrationService(identifier);
		if ( service == null ) {
			return success();
		}
		final var services = service.datumStreamServices();
		var result = localizedServiceSettings(services, locale);
		return success(result);
	}

	/**
	 * List the available datum stream data filters for a given datum stream
	 * service.
	 *
	 * @param identifier
	 *        the {@link CloudDatumStreamService} identifier to list the
	 *        available value filters
	 * @param locale
	 *        the desired locale
	 * @return the services
	 */
	@RequestMapping(value = "/services/datum-streams/data-filters", method = RequestMethod.GET)
	public Result<Iterable<LocalizedServiceInfo>> availableCloudDatumStreamFilters(
			@RequestParam("identifier") String identifier, Locale locale) {
		final CloudDatumStreamService service = userCloudIntegrationsBiz.datumStreamService(identifier);
		if ( service == null ) {
			return success();
		}
		final var result = service.dataValueFilters(locale);
		return success(result);
	}

	/*-=======================
	 * User Settings
	 *-======================= */

	@RequestMapping(value = "/settings", method = RequestMethod.GET)
	public Result<UserSettingsEntity> viewUserSettings() {
		var result = userCloudIntegrationsBiz.settingsForUser(getCurrentActorUserId());
		return success(result);
	}

	@RequestMapping(value = "/settings", method = RequestMethod.PUT)
	public Result<UserSettingsEntity> saveUserSettings(
			@Valid @RequestBody UserSettingsEntityInput input) {
		return success(userCloudIntegrationsBiz.saveSettings(getCurrentActorUserId(), input));
	}

	@RequestMapping(value = "/settings", method = RequestMethod.DELETE)
	public Result<UserSettingsEntity> deleteUserSettings() {
		userCloudIntegrationsBiz.deleteSettings(getCurrentActorUserId());
		return success();
	}

	/*-=======================
	 * Integrations
	 *-======================= */

	@RequestMapping(value = "/integrations", method = RequestMethod.GET)
	public Result<FilterResults<CloudIntegrationConfiguration, UserLongCompositePK>> listCloudIntegrationConfigurations(
			BasicFilter filter) {
		var result = userCloudIntegrationsBiz.listConfigurationsForUser(getCurrentActorUserId(), filter,
				CloudIntegrationConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/integrations", method = RequestMethod.POST)
	public ResponseEntity<Result<CloudIntegrationConfiguration>> createCloudIntegrationConfiguration(
			@Valid @RequestBody CloudIntegrationConfigurationInput input) {
		var id = UserLongCompositePK.unassignedEntityIdKey(getCurrentActorUserId());
		var result = userCloudIntegrationsBiz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(on(UserCloudIntegrationsController.class)
				.getCloudIntegrationConfiguration(result.getConfigId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/integrations/{integrationId}", method = RequestMethod.GET)
	public Result<CloudIntegrationConfiguration> getCloudIntegrationConfiguration(
			@PathVariable("integrationId") Long integrationId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
		return success(
				userCloudIntegrationsBiz.configurationForId(id, CloudIntegrationConfiguration.class));
	}

	@RequestMapping(value = "/integrations/{integrationId}", method = RequestMethod.PUT)
	public Result<CloudIntegrationConfiguration> updateCloudIntegrationConfiguration(
			@PathVariable("integrationId") Long integrationId,
			@Valid @RequestBody CloudIntegrationConfigurationInput input) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
		return success(userCloudIntegrationsBiz.saveConfiguration(id, input));
	}

	@RequestMapping(value = "/integrations/{integrationId}/enabled/{enabled}",
			method = RequestMethod.POST)
	public Result<CloudIntegrationConfiguration> enableCloudIntegrationConfiguration(
			@PathVariable("integrationId") Long integrationId,
			@PathVariable("enabled") boolean enabled) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
		userCloudIntegrationsBiz.updateConfigurationEnabled(id, enabled,
				CloudIntegrationConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/integrations/{integrationId}", method = RequestMethod.DELETE)
	public Result<Void> deleteCloudIntegrationConfiguration(
			@PathVariable("integrationId") Long integrationId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
		userCloudIntegrationsBiz.deleteConfiguration(id, CloudIntegrationConfiguration.class);
		return success();
	}

	/**
	 * Validate an integration service configuration.
	 *
	 * @param integrationId
	 *        the ID of the integration configuration to validate
	 * @param locale
	 *        the desired locale
	 * @return the services
	 */
	@RequestMapping(value = "/integrations/{integrationId}/validate", method = RequestMethod.GET)
	public Result<Void> validateIntegrationConfiguration(
			@PathVariable("integrationId") Long integrationId, Locale locale) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
		return userCloudIntegrationsBiz.validateIntegrationConfigurationForId(id, locale);
	}

	/*-=======================
	 * Data Values
	 *-======================= */

	/**
	 * List the data values for a datum stream service and an optional filter.
	 *
	 * @param integrationId
	 *        the integration ID
	 * @param datumStreamServiceIdentifier
	 *        the service identifier of the {@link CloudDatumStreamService} to
	 *        use; if not provided then the first service available for the
	 *        {@link CloudIntegrationService} specified in the specified
	 *        {@link CloudIntegrationConfiguration} will be used
	 * @param req
	 *        the HTTP request to obtain filter parameters from
	 * @return the values
	 */
	@RequestMapping(value = "/integrations/{integrationId}/data-values", method = RequestMethod.GET)
	public Result<Iterable<CloudDataValue>> listCloudDatumStreamDataValues(
			@PathVariable("integrationId") Long integrationId,
			@RequestParam(value = "datumStreamServiceIdentifier",
					required = false) String datumStreamServiceIdentifier,
			WebRequest req) {
		var filter = new LinkedHashMap<String, Object>(4);
		for ( Iterator<String> itr = req.getParameterNames(); itr.hasNext(); ) {
			String param = itr.next();
			if ( "datumStreamServiceIdentifier".equals(param) ) {
				continue;
			}
			Object val = req.getParameter(param);
			if ( val != null ) {
				filter.put(param, val);
			}
		}
		var result = userCloudIntegrationsBiz.listDatumStreamDataValues(
				new UserLongCompositePK(getCurrentActorUserId(), integrationId),
				datumStreamServiceIdentifier, filter);
		return success(result);
	}

	/*-=======================
	 * Datum Stream Mapping
	 *-======================= */

	@RequestMapping(value = "/datum-stream-mappings", method = RequestMethod.GET)
	public Result<FilterResults<CloudDatumStreamMappingConfiguration, UserLongCompositePK>> listCloudDatumStreamMappingConfigurations(
			BasicFilter filter) {
		var result = userCloudIntegrationsBiz.listConfigurationsForUser(getCurrentActorUserId(), filter,
				CloudDatumStreamMappingConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/datum-stream-mappings", method = RequestMethod.POST)
	public ResponseEntity<Result<CloudDatumStreamMappingConfiguration>> createCloudDatumStreamMappingConfiguration(
			@Valid @RequestBody CloudDatumStreamMappingConfigurationInput input) {
		var id = UserLongCompositePK.unassignedEntityIdKey(getCurrentActorUserId());
		var result = userCloudIntegrationsBiz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(on(UserCloudIntegrationsController.class)
				.getCloudDatumStreamMappingConfiguration(result.getConfigId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/datum-stream-mappings/{datumStreamMappingId}", method = RequestMethod.GET)
	public Result<CloudDatumStreamMappingConfiguration> getCloudDatumStreamMappingConfiguration(
			@PathVariable("datumStreamMappingId") Long datumStreamMappingId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamMappingId);
		return success(userCloudIntegrationsBiz.configurationForId(id,
				CloudDatumStreamMappingConfiguration.class));
	}

	@RequestMapping(value = "/datum-stream-mappings/{datumStreamMappingId}", method = RequestMethod.PUT)
	public Result<CloudDatumStreamMappingConfiguration> updateCloudDatumStreamMappingConfiguration(
			@PathVariable("datumStreamMappingId") Long datumStreamMappingId,
			@Valid @RequestBody CloudDatumStreamMappingConfigurationInput input) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamMappingId);
		return success(userCloudIntegrationsBiz.saveConfiguration(id, input));
	}

	@RequestMapping(value = "/datum-stream-mappings/{datumStreamMappingId}",
			method = RequestMethod.DELETE)
	public Result<Void> deleteCloudDatumStreamMappingConfiguration(
			@PathVariable("datumStreamMappingId") Long datumStreamMappingId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamMappingId);
		userCloudIntegrationsBiz.deleteConfiguration(id, CloudDatumStreamMappingConfiguration.class);
		return success();
	}

	/*-=================================
	 * Datum Stream Mapping / Properties
	 *-================================= */

	@RequestMapping(value = "/datum-stream-mappings/{datumStreamMappingId}/properties",
			method = RequestMethod.GET)
	public Result<FilterResults<CloudDatumStreamPropertyConfiguration, UserLongIntegerCompositePK>> listCloudDatumStreamPropertyConfigurations(
			@PathVariable("datumStreamMappingId") Long datumStreamMappingId, BasicFilter filter) {
		filter.setDatumStreamMappingId(datumStreamMappingId);
		var result = userCloudIntegrationsBiz.listConfigurationsForUser(getCurrentActorUserId(), filter,
				CloudDatumStreamPropertyConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/datum-stream-mappings/{datumStreamMappingId}/properties",
			method = RequestMethod.POST)
	public Result<List<CloudDatumStreamPropertyConfiguration>> replaceCloudDatumStreamPropertyConfigurations(
			@PathVariable("datumStreamMappingId") Long datumStreamMappingId,
			@Valid @RequestBody List<CloudDatumStreamPropertyConfigurationInput> inputs) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamMappingId);
		var result = userCloudIntegrationsBiz.replaceDatumStreamPropertyConfiguration(id, inputs);
		return success(result);
	}

	@RequestMapping(value = "/datum-stream-mappings/{datumStreamMappingId}/properties/{index}",
			method = RequestMethod.GET)
	public Result<CloudDatumStreamPropertyConfiguration> getCloudDatumStreamPropertyConfiguration(
			@PathVariable("datumStreamMappingId") Long datumStreamMappingId,
			@PathVariable("index") Integer index) {
		var id = new UserLongIntegerCompositePK(getCurrentActorUserId(), datumStreamMappingId, index);
		return success(userCloudIntegrationsBiz.configurationForId(id,
				CloudDatumStreamPropertyConfiguration.class));
	}

	@RequestMapping(value = "/datum-stream-mappings/{datumStreamMappingId}/properties/{index}",
			method = RequestMethod.PUT)
	public Result<CloudDatumStreamPropertyConfiguration> updateCloudDatumStreamPropertyConfiguration(
			@PathVariable("datumStreamMappingId") Long datumStreamMappingId,
			@PathVariable("index") Integer index,
			@Valid @RequestBody CloudDatumStreamPropertyConfigurationInput input) {
		var id = new UserLongIntegerCompositePK(getCurrentActorUserId(), datumStreamMappingId, index);
		return success(userCloudIntegrationsBiz.saveConfiguration(id, input));
	}

	@RequestMapping(
			value = "/datum-stream-mappings/{datumStreamMappingId}/properties/{index}/enabled/{enabled}",
			method = RequestMethod.POST)
	public Result<CloudDatumStreamPropertyConfiguration> enableCloudDatumStreamPropertyConfiguration(
			@PathVariable("datumStreamMappingId") Long datumStreamMappingId,
			@PathVariable("index") Integer index, @PathVariable("enabled") boolean enabled) {
		var id = new UserLongIntegerCompositePK(getCurrentActorUserId(), datumStreamMappingId, index);
		userCloudIntegrationsBiz.updateConfigurationEnabled(id, enabled,
				CloudDatumStreamPropertyConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/datum-stream-mappings/{datumStreamMappingId}/properties/{index}",
			method = RequestMethod.DELETE)
	public Result<Void> deleteCloudDatumStreamPropertyConfiguration(
			@PathVariable("datumStreamMappingId") Long datumStreamMappingId,
			@PathVariable("index") Integer index) {
		var id = new UserLongIntegerCompositePK(getCurrentActorUserId(), datumStreamMappingId, index);
		userCloudIntegrationsBiz.deleteConfiguration(id, CloudDatumStreamPropertyConfiguration.class);
		return success();
	}

	/*-=======================
	 * Datum Stream
	 *-======================= */

	@RequestMapping(value = "/datum-streams", method = RequestMethod.GET)
	public Result<FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK>> listCloudDatumStreamConfigurations(
			BasicFilter filter) {
		var result = userCloudIntegrationsBiz.listConfigurationsForUser(getCurrentActorUserId(), filter,
				CloudDatumStreamConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/datum-streams", method = RequestMethod.POST)
	public ResponseEntity<Result<CloudDatumStreamConfiguration>> createCloudDatumStreamConfiguration(
			@Valid @RequestBody CloudDatumStreamConfigurationInput input) {
		var id = UserLongCompositePK.unassignedEntityIdKey(getCurrentActorUserId());
		var result = userCloudIntegrationsBiz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(on(UserCloudIntegrationsController.class)
				.getCloudDatumStreamConfiguration(result.getConfigId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/datum-streams/{datumStreamId}", method = RequestMethod.GET)
	public Result<CloudDatumStreamConfiguration> getCloudDatumStreamConfiguration(
			@PathVariable("datumStreamId") Long datumStreamId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamId);
		return success(
				userCloudIntegrationsBiz.configurationForId(id, CloudDatumStreamConfiguration.class));
	}

	@RequestMapping(value = "/datum-streams/{datumStreamId}", method = RequestMethod.PUT)
	public Result<CloudDatumStreamConfiguration> updateCloudDatumStreamConfiguration(
			@PathVariable("datumStreamId") Long datumStreamId,
			@Valid @RequestBody CloudDatumStreamConfigurationInput input) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamId);
		return success(userCloudIntegrationsBiz.saveConfiguration(id, input));
	}

	@RequestMapping(value = "/datum-streams/{datumStreamId}/enabled/{enabled}",
			method = RequestMethod.POST)
	public Result<CloudDatumStreamConfiguration> enableCloudDatumStreamConfiguration(
			@PathVariable("datumStreamId") Long datumStreamId,
			@PathVariable("enabled") boolean enabled) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamId);
		userCloudIntegrationsBiz.updateConfigurationEnabled(id, enabled,
				CloudDatumStreamConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/datum-streams/{datumStreamId}", method = RequestMethod.DELETE)
	public Result<Void> deleteCloudDatumStreamConfiguration(
			@PathVariable("datumStreamId") Long datumStreamId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamId);
		userCloudIntegrationsBiz.deleteConfiguration(id, CloudDatumStreamConfiguration.class);
		return success();
	}

	/*-=======================
	 * Datum Stream Settings
	 *-======================= */

	@RequestMapping(value = "/datum-streams/default-settings", method = RequestMethod.GET)
	public Result<CloudDatumStreamSettings> getDefaultCloudDatumStreamSettings() {
		return success(userCloudIntegrationsBiz.defaultDatumStreamSettings());
	}

	@RequestMapping(value = "/datum-streams/{datumStreamId}/settings", method = RequestMethod.GET)
	public Result<CloudDatumStreamSettingsEntity> getCloudDatumStreamSettings(
			@PathVariable("datumStreamId") Long datumStreamId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamId);
		return success(
				userCloudIntegrationsBiz.configurationForId(id, CloudDatumStreamSettingsEntity.class));
	}

	@RequestMapping(value = "/datum-streams/settings", method = RequestMethod.GET)
	public Result<FilterResults<CloudDatumStreamSettingsEntity, UserLongCompositePK>> listCloudDatumStreamSettings(
			BasicFilter filter) {
		var result = userCloudIntegrationsBiz.listConfigurationsForUser(getCurrentActorUserId(), filter,
				CloudDatumStreamSettingsEntity.class);
		return success(result);
	}

	@RequestMapping(value = "/datum-streams/{datumStreamId}/settings", method = RequestMethod.PUT)
	public Result<CloudDatumStreamSettingsEntity> saveCloudDatumStreamSettings(
			@PathVariable("datumStreamId") Long datumStreamId,
			@Valid @RequestBody CloudDatumStreamSettingsEntityInput input) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamId);
		return success(userCloudIntegrationsBiz.saveConfiguration(id, input));
	}

	@RequestMapping(value = "/datum-streams/{datumStreamId}/settings", method = RequestMethod.DELETE)
	public Result<Void> deleteCloudDatumStreamSettings(
			@PathVariable("datumStreamId") Long datumStreamId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamId);
		userCloudIntegrationsBiz.deleteConfiguration(id, CloudDatumStreamSettingsEntity.class);
		return success();
	}

	/*-=======================
	 * Datum Stream datum
	 *-======================= */

	/**
	 * List the data values for a datum stream service and an optional filter.
	 *
	 * @param datumStreamId
	 *        the datum stream ID
	 * @return the result
	 */
	@RequestMapping(value = "/datum-streams/{datumStreamId}/latest-datum", method = RequestMethod.GET)
	public Result<Iterable<Datum>> cloudDatumStreamLatestDatum(
			@PathVariable("datumStreamId") Long datumStreamId) {
		return success(userCloudIntegrationsBiz.latestDatumStreamDatumForId(
				new UserLongCompositePK(getCurrentActorUserId(), datumStreamId)));
	}

	/**
	 * List the data values for a datum stream service and an optional filter.
	 *
	 * @param datumStreamId
	 *        the datum stream ID
	 * @return the result
	 */
	@RequestMapping(value = "/datum-streams/{datumStreamId}/datum", method = RequestMethod.GET)
	public Result<CloudDatumStreamQueryResult> cloudDatumStreamListDatum(
			@PathVariable("datumStreamId") Long datumStreamId, BasicQueryFilter filter) {
		return success(userCloudIntegrationsBiz.listDatumStreamDatum(
				new UserLongCompositePK(getCurrentActorUserId(), datumStreamId), filter));
	}

	/*-=======================
	 * Datum Stream Poll Tasks
	 *-======================= */

	@RequestMapping(value = "/datum-stream-poll-tasks", method = RequestMethod.GET)
	public Result<FilterResults<CloudDatumStreamPollTaskEntity, UserLongCompositePK>> listCloudDatumStreamPollTasks(
			BasicFilter filter) {
		var result = userCloudIntegrationsBiz.listDatumStreamPollTasksForUser(getCurrentActorUserId(),
				filter);
		return success(result);
	}

	@RequestMapping(value = "/datum-stream-poll-tasks/{datumStreamId}", method = RequestMethod.PUT)
	public Result<CloudDatumStreamPollTaskEntity> updateCloudDatumStreamPollTask(
			@PathVariable("datumStreamId") Long datumStreamId,
			@Valid @RequestBody CloudDatumStreamPollTaskEntityInput input) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamId);
		BasicClaimableJobState[] requiredStates = null;
		if ( input.getRequiredStates() != null && !input.getRequiredStates().isEmpty() ) {
			requiredStates = input.getRequiredStates().toArray(BasicClaimableJobState[]::new);
		}
		return success(userCloudIntegrationsBiz.saveDatumStreamPollTask(id, input, requiredStates));
	}

	@RequestMapping(value = "/datum-stream-poll-tasks/{datumStreamId}", method = RequestMethod.DELETE)
	public Result<Void> deleteCloudDatumStreamPollTask(
			@PathVariable("datumStreamId") Long datumStreamId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamId);
		userCloudIntegrationsBiz.deleteDatumStreamPollTask(id);
		return success();
	}

	@RequestMapping(value = "/datum-stream-poll-tasks/{datumStreamId}/state",
			method = RequestMethod.POST)
	public Result<CloudDatumStreamPollTaskEntity> updateCloudDatumStreamPollTaskState(
			@PathVariable("datumStreamId") Long datumStreamId,
			@Valid @RequestBody CloudDatumStreamPollTaskStateInput input) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamId);
		BasicClaimableJobState[] requiredStates = null;
		if ( input.getRequiredStates() != null && !input.getRequiredStates().isEmpty() ) {
			requiredStates = input.getRequiredStates().toArray(BasicClaimableJobState[]::new);
		}
		return success(userCloudIntegrationsBiz.updateDatumStreamPollTaskState(id, input.getState(),
				requiredStates));
	}
}
