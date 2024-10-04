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
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
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
		Iterable<LocalizedServiceInfo> result = userCloudIntegrationsBiz
				.availableIntegrationServices(locale);
		return success(result);
	}

	@RequestMapping(value = "/integrations", method = RequestMethod.GET)
	public Result<FilterResults<CloudIntegrationConfiguration, UserLongCompositePK>> listCloudIntegrationConfigurations(
			BasicFilter filter) {
		FilterResults<CloudIntegrationConfiguration, UserLongCompositePK> result = userCloudIntegrationsBiz
				.configurationsForUser(getCurrentActorUserId(), filter,
						CloudIntegrationConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/integrations", method = RequestMethod.POST)
	public ResponseEntity<Result<CloudIntegrationConfiguration>> createCloudIntegrationConfiguration(
			@Valid @RequestBody CloudIntegrationConfigurationInput input) {
		UserLongCompositePK id = UserLongCompositePK.unassignedEntityIdKey(getCurrentActorUserId());
		CloudIntegrationConfiguration result = userCloudIntegrationsBiz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(on(UserCloudIntegrationsController.class)
				.getCloudIntegrationConfiguration(result.getConfigId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/integrations/{integrationId}", method = RequestMethod.GET)
	public Result<CloudIntegrationConfiguration> getCloudIntegrationConfiguration(
			@PathVariable("integrationId") Long integrationId) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
		return success(
				userCloudIntegrationsBiz.configurationForId(id, CloudIntegrationConfiguration.class));
	}

	@RequestMapping(value = "/integrations/{integrationId}", method = RequestMethod.PUT)
	public Result<CloudIntegrationConfiguration> updateCloudIntegrationConfiguration(
			@PathVariable("integrationId") Long integrationId,
			@Valid @RequestBody CloudIntegrationConfigurationInput input) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
		CloudIntegrationConfiguration result = userCloudIntegrationsBiz.saveConfiguration(id, input);
		return success(result);
	}

	@RequestMapping(value = "/integrations/{integrationId}/enabled/{enabled}", method = RequestMethod.POST)
	public Result<CloudIntegrationConfiguration> enableCloudIntegrationConfiguration(
			@PathVariable("integrationId") Long integrationId,
			@PathVariable("enabled") boolean enabled) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
		userCloudIntegrationsBiz.enableConfiguration(id, enabled, CloudIntegrationConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/integrations/{integrationId}", method = RequestMethod.DELETE)
	public Result<Void> deleteCloudIntegrationConfiguration(
			@PathVariable("integrationId") Long integrationId) {
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
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
		UserLongCompositePK id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
		return userCloudIntegrationsBiz.validateIntegrationConfigurationForId(id);
	}

}
