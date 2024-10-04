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
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamPropertyConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.CloudIntegrationConfigurationInput;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.Result;

/**
 * Web service API for cloud integrations management.
 *
 * @author matt
 * @version 1.0
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

	@RequestMapping(value = "/integrations", method = RequestMethod.GET)
	public Result<FilterResults<CloudIntegrationConfiguration, UserLongCompositePK>> listCloudIntegrationConfigurations(
			BasicFilter filter) {
		var result = userCloudIntegrationsBiz.configurationsForUser(getCurrentActorUserId(), filter,
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

	@RequestMapping(value = "/integrations/{integrationId}/enabled/{enabled}", method = RequestMethod.POST)
	public Result<CloudIntegrationConfiguration> enableCloudIntegrationConfiguration(
			@PathVariable("integrationId") Long integrationId,
			@PathVariable("enabled") boolean enabled) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
		userCloudIntegrationsBiz.enableConfiguration(id, enabled, CloudIntegrationConfiguration.class);
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
	 * @return the services
	 */
	@RequestMapping(value = "/integrations/{integrationId}/validate", method = RequestMethod.GET)
	public Result<Void> validateIntegrationConfiguration(
			@PathVariable("integrationId") Long integrationId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
		return userCloudIntegrationsBiz.validateIntegrationConfigurationForId(id);
	}

	@RequestMapping(value = "/datum-streams", method = RequestMethod.GET)
	public Result<FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK>> listCloudDatumStreamConfigurations(
			BasicFilter filter) {
		var result = userCloudIntegrationsBiz.configurationsForUser(getCurrentActorUserId(), filter,
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

	@RequestMapping(value = "/datum-streams/{datumStreamId}/enabled/{enabled}", method = RequestMethod.POST)
	public Result<CloudDatumStreamConfiguration> enableCloudDatumStreamConfiguration(
			@PathVariable("datumStreamId") Long datumStreamId,
			@PathVariable("enabled") boolean enabled) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamId);
		userCloudIntegrationsBiz.enableConfiguration(id, enabled, CloudDatumStreamConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/datum-streams/{datumStreamId}", method = RequestMethod.DELETE)
	public Result<Void> deleteCloudDatumStreamConfiguration(
			@PathVariable("datumStreamId") Long datumStreamId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), datumStreamId);
		userCloudIntegrationsBiz.deleteConfiguration(id, CloudDatumStreamConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/datum-streams/{datumStreamId}/properties", method = RequestMethod.GET)
	public Result<FilterResults<CloudDatumStreamPropertyConfiguration, UserLongIntegerCompositePK>> listCloudDatumStreamPropertyConfigurations(
			@PathVariable("datumStreamId") Long datumStreamId, BasicFilter filter) {
		filter.setDatumStreamId(datumStreamId);
		var result = userCloudIntegrationsBiz.configurationsForUser(getCurrentActorUserId(), filter,
				CloudDatumStreamPropertyConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/datum-streams/{datumStreamId}/properties", method = RequestMethod.POST)
	public ResponseEntity<Result<CloudDatumStreamPropertyConfiguration>> createCloudDatumStreamPropertyConfiguration(
			@PathVariable("datumStreamId") Long datumStreamId,
			@Valid @RequestBody CloudDatumStreamPropertyConfigurationInput input) {
		var id = UserLongIntegerCompositePK.unassignedEntityIdKey(getCurrentActorUserId(),
				datumStreamId);
		var result = userCloudIntegrationsBiz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(
				on(UserCloudIntegrationsController.class).getCloudDatumStreamPropertyConfiguration(
						result.getDatumStreamId(), result.getIndex())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/datum-streams/{datumStreamId}/properties/{index}", method = RequestMethod.GET)
	public Result<CloudDatumStreamPropertyConfiguration> getCloudDatumStreamPropertyConfiguration(
			@PathVariable("datumStreamId") Long datumStreamId, @PathVariable("index") Integer index) {
		var id = new UserLongIntegerCompositePK(getCurrentActorUserId(), datumStreamId, index);
		return success(userCloudIntegrationsBiz.configurationForId(id,
				CloudDatumStreamPropertyConfiguration.class));
	}

	@RequestMapping(value = "/datum-streams/{datumStreamId}/properties/{index}", method = RequestMethod.PUT)
	public Result<CloudDatumStreamPropertyConfiguration> updateCloudDatumStreamPropertyConfiguration(
			@PathVariable("datumStreamId") Long datumStreamId, @PathVariable("index") Integer index,
			@Valid @RequestBody CloudDatumStreamPropertyConfigurationInput input) {
		var id = new UserLongIntegerCompositePK(getCurrentActorUserId(), datumStreamId, index);
		return success(userCloudIntegrationsBiz.saveConfiguration(id, input));
	}

	@RequestMapping(value = "/datum-streams/{datumStreamId}/properties/{index}/enabled/{enabled}", method = RequestMethod.POST)
	public Result<CloudDatumStreamPropertyConfiguration> enableCloudDatumStreamPropertyConfiguration(
			@PathVariable("datumStreamId") Long datumStreamId, @PathVariable("index") Integer index,
			@PathVariable("enabled") boolean enabled) {
		var id = new UserLongIntegerCompositePK(getCurrentActorUserId(), datumStreamId, index);
		userCloudIntegrationsBiz.enableConfiguration(id, enabled,
				CloudDatumStreamPropertyConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/datum-streams/{datumStreamId}/properties/{index}", method = RequestMethod.DELETE)
	public Result<Void> deleteCloudDatumStreamPropertyConfiguration(
			@PathVariable("datumStreamId") Long datumStreamId, @PathVariable("index") Integer index) {
		var id = new UserLongIntegerCompositePK(getCurrentActorUserId(), datumStreamId, index);
		userCloudIntegrationsBiz.deleteConfiguration(id, CloudDatumStreamPropertyConfiguration.class);
		return success();
	}

}
