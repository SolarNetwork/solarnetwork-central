/* ==================================================================
 * UserCloudIntegrationsControlsController.java - 12/11/2025 11:46:24 am
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
import net.solarnetwork.central.c2c.biz.CloudControlService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.central.user.c2c.domain.CloudControlConfigurationInput;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.Result;

/**
 * Web service API for cloud integrations controls management.
 *
 * @author matt
 * @version 1.0
 */
@Profile(SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS)
@GlobalExceptionRestController
@RestController("v1CloudIntegrationsControlsController")
@RequestMapping(value = { "/api/v1/sec/user/c2c", "/u/sec/c2c" })
public class UserCloudIntegrationsControlsController {

	private final UserCloudIntegrationsBiz biz;

	/**
	 * Constructor.
	 *
	 * @param userCloudIntegrationsBiz
	 *        the user cloud integrations service
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserCloudIntegrationsControlsController(UserCloudIntegrationsBiz userCloudIntegrationsBiz) {
		super();
		this.biz = requireNonNullArgument(userCloudIntegrationsBiz, "userCloudIntegrationsBiz");
	}

	/**
	 * List the available control services for a given integration service.
	 *
	 * @param identifier
	 *        the {@link CloudIntegrationService} identifier to list the
	 *        available control services
	 * @param locale
	 *        the desired locale
	 * @return the services
	 */
	@RequestMapping(value = "/services/controls", method = RequestMethod.GET)
	public Result<Iterable<LocalizedServiceInfo>> availableCloudControlServicesForIntegration(
			@RequestParam("identifier") String identifier, Locale locale) {
		final CloudIntegrationService service = biz.integrationService(identifier);
		if ( service == null ) {
			return success();
		}
		final var services = service.controlServices();
		var result = localizedServiceSettings(services, locale);
		return success(result);
	}

	/**
	 * List the available control data filters for a given control service.
	 *
	 * @param identifier
	 *        the {@link CloudControlService} identifier to list the available
	 *        value filters
	 * @param locale
	 *        the desired locale
	 * @return the services
	 */
	@RequestMapping(value = "/services/controls/data-filters", method = RequestMethod.GET)
	public Result<Iterable<LocalizedServiceInfo>> availableCloudControlFilters(
			@RequestParam("identifier") String identifier, Locale locale) {
		final CloudControlService service = biz.controlService(identifier);
		if ( service == null ) {
			return success();
		}
		final var result = service.dataValueFilters(locale);
		return success(result);
	}

	/*-=======================
	 * Control Data Values
	 *-======================= */

	/**
	 * List the data values for a control service and an optional filter.
	 *
	 * @param integrationId
	 *        the integration ID
	 * @param controlServiceIdentifier
	 *        the service identifier of the {@link CloudControlService} to use;
	 *        if not provided then the first service available for the
	 *        {@link CloudIntegrationService} specified in the specified
	 *        {@link CloudIntegrationConfiguration} will be used
	 * @param req
	 *        the HTTP request to obtain filter parameters from
	 * @return the values
	 */
	@RequestMapping(value = "/integrations/{integrationId}/control-data-values",
			method = RequestMethod.GET)
	public Result<Iterable<CloudDataValue>> listCloudControlDataValues(@PathVariable Long integrationId,
			@RequestParam(value = "controlServiceIdentifier",
					required = false) String controlServiceIdentifier,
			WebRequest req) {
		var filter = new LinkedHashMap<String, Object>(4);
		for ( Iterator<String> itr = req.getParameterNames(); itr.hasNext(); ) {
			String param = itr.next();
			if ( "controlServiceIdentifier".equals(param) ) {
				continue;
			}
			Object val = req.getParameter(param);
			if ( val != null ) {
				filter.put(param, val);
			}
		}
		var result = biz.listControlDataValues(
				new UserLongCompositePK(getCurrentActorUserId(), integrationId),
				controlServiceIdentifier, filter);
		return success(result);
	}

	/*-=======================
	 * Controls
	 *-======================= */

	@RequestMapping(value = "/controls", method = RequestMethod.GET)
	public Result<FilterResults<CloudControlConfiguration, UserLongCompositePK>> listCloudControlConfigurations(
			BasicFilter filter) {
		var result = biz.listConfigurationsForUser(getCurrentActorUserId(), filter,
				CloudControlConfiguration.class);
		return success(result);
	}

	@RequestMapping(value = "/controls", method = RequestMethod.POST)
	public ResponseEntity<Result<CloudControlConfiguration>> createCloudControlConfiguration(
			@Valid @RequestBody CloudControlConfigurationInput input) {
		var id = UserLongCompositePK.unassignedEntityIdKey(getCurrentActorUserId());
		var result = biz.saveConfiguration(id, input);
		URI loc = uriWithoutHost(fromMethodCall(on(UserCloudIntegrationsControlsController.class)
				.getCloudControlConfiguration(result.getConfigId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	@RequestMapping(value = "/controls/{cloudControlId}", method = RequestMethod.GET)
	public Result<CloudControlConfiguration> getCloudControlConfiguration(
			@PathVariable Long cloudControlId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), cloudControlId);
		return success(biz.configurationForId(id, CloudControlConfiguration.class));
	}

	@RequestMapping(value = "/controls/{cloudControlId}", method = RequestMethod.PUT)
	public Result<CloudControlConfiguration> updateCloudControlConfiguration(
			@PathVariable Long cloudControlId,
			@Valid @RequestBody CloudControlConfigurationInput input) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), cloudControlId);
		return success(biz.saveConfiguration(id, input));
	}

	@RequestMapping(value = "/controls/{cloudControlId}/enabled/{enabled}", method = RequestMethod.POST)
	public Result<CloudControlConfiguration> enableCloudControlConfiguration(
			@PathVariable Long cloudControlId, @PathVariable boolean enabled) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), cloudControlId);
		biz.updateConfigurationEnabled(id, enabled, CloudControlConfiguration.class);
		return success();
	}

	@RequestMapping(value = "/controls/{cloudControlId}", method = RequestMethod.DELETE)
	public Result<Void> deleteCloudControlConfiguration(@PathVariable Long cloudControlId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), cloudControlId);
		biz.deleteConfiguration(id, CloudControlConfiguration.class);
		return success();
	}

}
