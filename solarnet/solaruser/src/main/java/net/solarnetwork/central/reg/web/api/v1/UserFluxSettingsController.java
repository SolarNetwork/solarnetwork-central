/* ==================================================================
 * UserFluxSettingsController.java - 25/06/2024 5:25:39 pm
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

import static net.solarnetwork.central.datum.v2.support.DatumUtils.criteriaFromFilter;
import static net.solarnetwork.central.security.SecurityUtils.getCurrentActorUserId;
import static net.solarnetwork.central.web.WebUtils.uriWithoutHost;
import static net.solarnetwork.domain.Result.success;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.flux.biz.UserFluxBiz;
import net.solarnetwork.central.user.flux.dao.BasicFluxConfigurationFilter;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfigurationInput;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfigurationInput;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;
import net.solarnetwork.util.ObjectUtils;

/**
 * Web service API for SolarFlux settings management.
 *
 * @author matt
 * @version 1.0
 */
@GlobalExceptionRestController
@RestController("v1FluxSettingsController")
@RequestMapping(value = { "/api/v1/sec/user/flux", "/u/sec/flux" })
public class UserFluxSettingsController {

	private final UserFluxBiz userFluxBiz;

	/**
	 * Constructor.
	 *
	 * @param userFluxBiz
	 *        the user SolarFlux service to use
	 */
	public UserFluxSettingsController(UserFluxBiz userFluxBiz) {
		super();
		this.userFluxBiz = ObjectUtils.requireNonNullArgument(userFluxBiz, "userFluxBiz");
	}

	/**
	 * Get the default aggregate publish settings for the current actor.
	 *
	 * @return the settings
	 */
	@RequestMapping(value = "/agg/pub/default-settings", method = RequestMethod.GET)
	public Result<UserFluxDefaultAggregatePublishConfiguration> viewDefaultAggregatePublishConfiguration() {
		var result = userFluxBiz.defaultAggregatePublishConfigurationForUser(getCurrentActorUserId());
		return success(result);
	}

	/**
	 * Save the default aggregate publish settings for the current user.
	 *
	 * @param input
	 *        the settings to save
	 * @return the persisted settings
	 */
	@RequestMapping(value = "/agg/pub/default-settings", method = RequestMethod.PUT)
	public Result<UserFluxDefaultAggregatePublishConfiguration> updateDefaultAggregatePublishConfiguration(
			@Valid @RequestBody UserFluxDefaultAggregatePublishConfigurationInput input) {
		var result = userFluxBiz.saveDefaultAggregatePublishConfiguration(getCurrentActorUserId(),
				input);
		return success(result);
	}

	/**
	 * Save the default aggregate publish settings for the current user.
	 *
	 * @return an empty result
	 */
	@RequestMapping(value = "/agg/pub/default-settings", method = RequestMethod.DELETE)
	public Result<Void> deleteDefaultAggregatePublishConfiguration() {
		userFluxBiz.deleteDefaultAggregatePublishConfiguration(getCurrentActorUserId());
		return success();
	}

	/**
	 * List aggregate publish settings for the current user.
	 *
	 * @param filter
	 *        the filter
	 * @return the matching settings
	 */
	@RequestMapping(value = "/agg/pub/settings", method = RequestMethod.GET)
	public Result<FilterResults<UserFluxAggregatePublishConfiguration, UserLongCompositePK>> listAggregatePublishConfigurations(
			DatumFilterCommand filter) {
		var f = new BasicFluxConfigurationFilter();
		f.copyFrom(criteriaFromFilter(filter));
		var result = userFluxBiz.aggregatePublishConfigurationsForUser(getCurrentActorUserId(), f);
		return success(result);
	}

	/**
	 * Create a new aggregate publish settings for the current user.
	 *
	 * @param input
	 *        the settings to create
	 * @return the persisted settings
	 */
	@RequestMapping(value = "/agg/pub/settings", method = RequestMethod.POST)
	public ResponseEntity<Result<UserFluxAggregatePublishConfiguration>> createAggregatePublishConfiguration(
			@Valid @RequestBody UserFluxAggregatePublishConfigurationInput input) {
		UserLongCompositePK id = UserLongCompositePK.unassignedEntityIdKey(getCurrentActorUserId());
		UserFluxAggregatePublishConfiguration result = userFluxBiz.saveAggregatePublishConfiguration(id,
				input);
		URI loc = uriWithoutHost(fromMethodCall(on(UserFluxSettingsController.class)
				.viewAggregatePublishConfiguration(result.getConfigurationId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	/**
	 * Get aggregate publish settings for the current actor.
	 *
	 * @param configurationId
	 *        the ID of the configuration to get
	 * @return the settings
	 */
	@RequestMapping(value = "/agg/pub/settings/{configurationId}", method = RequestMethod.GET)
	public Result<UserFluxAggregatePublishConfiguration> viewAggregatePublishConfiguration(
			@PathVariable Long configurationId) {
		var result = userFluxBiz.aggregatePublishConfigurationForUser(getCurrentActorUserId(),
				configurationId);
		return success(result);
	}

	/**
	 * Update aggregate publish settings for the current actor.
	 *
	 * @param configurationId
	 *        the ID of the configuration to get
	 * @param input
	 *        the settings to save
	 * @return the persisted settings
	 */
	@RequestMapping(value = "/agg/pub/settings/{configurationId}", method = RequestMethod.PUT)
	public Result<UserFluxAggregatePublishConfiguration> updateAggregatePublishConfiguration(
			@PathVariable Long configurationId,
			@Valid @RequestBody UserFluxAggregatePublishConfigurationInput input) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), configurationId);
		var result = userFluxBiz.saveAggregatePublishConfiguration(id, input);
		return success(result);
	}

	/**
	 * Delete aggregate publish settings for the current actor.
	 *
	 * @param configurationId
	 *        the ID of the configuration to delete
	 * @return an empty result
	 */
	@RequestMapping(value = "/agg/pub/settings/{configurationId}", method = RequestMethod.DELETE)
	public Result<UserFluxAggregatePublishConfiguration> deleteAggregatePublishConfiguration(
			@PathVariable Long configurationId) {
		var id = new UserLongCompositePK(getCurrentActorUserId(), configurationId);
		userFluxBiz.deleteAggregatePublishConfiguration(id);
		return success();
	}

}
