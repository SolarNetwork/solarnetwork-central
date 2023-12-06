/* ==================================================================
 * UserOscpController.java - 15/08/2022 3:01:10 pm
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.central.oscp.config.SolarNetOscpConfiguration.OSCP_V20;
import static net.solarnetwork.central.web.WebUtils.uriWithoutHost;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.web.domain.Response.response;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import java.net.URI;
import java.util.Collection;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupSettings;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.UserSettings;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.oscp.biz.UserOscpBiz;
import net.solarnetwork.central.user.oscp.domain.AssetConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.CapacityGroupConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.CapacityGroupSettingsInput;
import net.solarnetwork.central.user.oscp.domain.CapacityOptimizerConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.CapacityProviderConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.UserSettingsInput;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.Result;
import net.solarnetwork.web.domain.Response;

/**
 * Web service API for OSCP management.
 * 
 * @author matt
 * @version 1.0
 */
@Profile(OSCP_V20)
@GlobalExceptionRestController
@RestController("v1OscpController")
@RequestMapping(value = { "/u/sec/oscp", "/api/v1/sec/user/oscp" })
public class UserOscpController {

	private final UserOscpBiz userOscpBiz;

	/**
	 * Constructor.
	 * 
	 * @param userOcppBiz
	 *        the user OCPP service
	 */

	public UserOscpController(@Autowired(required = false) UserOscpBiz userOscpBiz) {
		super();
		this.userOscpBiz = userOscpBiz;
	}

	/**
	 * Get the {@link UserOscpBiz}.
	 * 
	 * @return the service; never {@literal null}
	 * @throws UnsupportedOperationException
	 *         if the service is not available
	 */
	private UserOscpBiz userOscpBiz() {
		if ( userOscpBiz == null ) {
			throw new UnsupportedOperationException("OSCP service not available.");
		}
		return userOscpBiz;
	}

	/**
	 * Update a user settings for the current user.
	 * 
	 * @param input
	 *        the input
	 * @return the configuration
	 */
	@RequestMapping(method = PUT, value = "/settings", consumes = APPLICATION_JSON_VALUE)
	public Result<UserSettings> updateUserSettings(@Valid @RequestBody UserSettingsInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		UserSettings result = userOscpBiz().updateUserSettings(userId, input);
		return success(result);
	}

	/**
	 * View a specific user settings.
	 * 
	 * @param id
	 *        the ID of the user settings to view
	 * @return the settings
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/settings")
	public Response<UserSettings> viewUserSettings() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return response(userOscpBiz().settingsForUser(userId));
	}

	/**
	 * Delete a specific user settings.
	 * 
	 * @param id
	 *        the ID of the user settings to delete
	 * @return the result
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/settings")
	public Response<Void> deleteUserSettings() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOscpBiz().deleteUserSettings(userId);
		return response(null);
	}

	/**
	 * Get all available capacity provider configurations for the current user.
	 * 
	 * @return the configurations
	 */
	@RequestMapping(method = GET, value = "/capacity-providers")
	public Result<Collection<CapacityProviderConfiguration>> availableCapacityProviders() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CapacityProviderConfiguration> list = userOscpBiz().capacityProvidersForUser(userId);
		return success(list);
	}

	/**
	 * Create a new capacity provider configuration for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = POST, value = "/capacity-providers", consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Result<CapacityProviderConfiguration>> createCapacityProvider(
			@Valid @RequestBody CapacityProviderConfigurationInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		CapacityProviderConfiguration result = userOscpBiz().createCapacityProvider(userId, input);
		URI loc = uriWithoutHost(
				fromMethodCall(on(UserOscpController.class).getCapacityProvider(result.getEntityId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	/**
	 * Get a capacity provider configuration for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = GET, value = "/capacity-providers/{providerId}")
	public Result<CapacityProviderConfiguration> getCapacityProvider(
			@PathVariable("providerId") Long providerId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		CapacityProviderConfiguration result = userOscpBiz().capacityProviderForUser(userId, providerId);
		return success(result);
	}

	/**
	 * Update a capacity provider configuration for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = PUT, value = "/capacity-providers/{providerId}", consumes = APPLICATION_JSON_VALUE)
	public Result<CapacityProviderConfiguration> updateCapacityProvider(
			@PathVariable("providerId") Long providerId,
			@Valid @RequestBody CapacityProviderConfigurationInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		CapacityProviderConfiguration result = userOscpBiz().updateCapacityProvider(userId, providerId,
				input);
		return success(result);
	}

	/**
	 * Delete a capacity provider configuration for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = DELETE, value = "/capacity-providers/{providerId}")
	public Result<Void> deleteCapacityProvider(@PathVariable("providerId") Long providerId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOscpBiz().deleteCapacityProvider(userId, providerId);
		return success();
	}

	/**
	 * Get all available capacity optimizer configurations for the current user.
	 * 
	 * @return the configurations
	 */
	@RequestMapping(method = GET, value = "/capacity-optimizers")
	public Result<Collection<CapacityOptimizerConfiguration>> availableCapacityOptimizers() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CapacityOptimizerConfiguration> list = userOscpBiz()
				.capacityOptimizersForUser(userId);
		return success(list);
	}

	/**
	 * Create a new capacity optimizer configuration for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = POST, value = "/capacity-optimizers", consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Result<CapacityOptimizerConfiguration>> createCapacityOptimizer(
			@Valid @RequestBody CapacityOptimizerConfigurationInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		CapacityOptimizerConfiguration result = userOscpBiz().createCapacityOptimizer(userId, input);
		URI loc = uriWithoutHost(
				fromMethodCall(on(UserOscpController.class).getCapacityOptimizer(result.getEntityId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	/**
	 * Get a capacity optimizer configuration for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = GET, value = "/capacity-optimizers/{optimizerId}")
	public Result<CapacityOptimizerConfiguration> getCapacityOptimizer(
			@PathVariable("optimizerId") Long optimizerId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		CapacityOptimizerConfiguration result = userOscpBiz().capacityOptimizerForUser(userId,
				optimizerId);
		return success(result);
	}

	/**
	 * Update a capacity optimizer configuration for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = PUT, value = "/capacity-optimizers/{optimizerId}", consumes = APPLICATION_JSON_VALUE)
	public Result<CapacityOptimizerConfiguration> updateCapacityOptimizer(
			@PathVariable("optimizerId") Long optimizerId,
			@Valid @RequestBody CapacityOptimizerConfigurationInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		CapacityOptimizerConfiguration result = userOscpBiz().updateCapacityOptimizer(userId,
				optimizerId, input);
		return success(result);
	}

	/**
	 * Delete a capacity optimizer configuration for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = DELETE, value = "/capacity-optimizers/{optimizerId}")
	public Result<Void> deleteCapacityOptimizer(@PathVariable("optimizerId") Long optimizerId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOscpBiz().deleteCapacityOptimizer(userId, optimizerId);
		return success();
	}

	/**
	 * Get all available capacity group configurations for the current user.
	 * 
	 * @return the configurations
	 */
	@RequestMapping(method = GET, value = "/capacity-groups")
	public Result<Collection<CapacityGroupConfiguration>> availableCapacityGroups() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CapacityGroupConfiguration> list = userOscpBiz().capacityGroupsForUser(userId);
		return success(list);
	}

	/**
	 * Create a new capacity group configuration for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = POST, value = "/capacity-groups", consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Result<CapacityGroupConfiguration>> createCapacityGroup(
			@Valid @RequestBody CapacityGroupConfigurationInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		CapacityGroupConfiguration result = userOscpBiz().createCapacityGroup(userId, input);
		URI loc = uriWithoutHost(
				fromMethodCall(on(UserOscpController.class).getCapacityGroup(result.getEntityId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	/**
	 * Get a capacity group for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = GET, value = "/capacity-groups/{groupId}")
	public Result<CapacityGroupConfiguration> getCapacityGroup(@PathVariable("groupId") Long groupId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		CapacityGroupConfiguration result = userOscpBiz().capacityGroupForUser(userId, groupId);
		return success(result);
	}

	/**
	 * Update a capacity group for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = PUT, value = "/capacity-groups/{groupId}", consumes = APPLICATION_JSON_VALUE)
	public Result<CapacityGroupConfiguration> updateCapacityGroup(@PathVariable("groupId") Long groupId,
			@Valid @RequestBody CapacityGroupConfigurationInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		CapacityGroupConfiguration result = userOscpBiz().updateCapacityGroup(userId, groupId, input);
		return success(result);
	}

	/**
	 * Delete a capacity group configuration for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = DELETE, value = "/capacity-groups/{groupId}")
	public Result<Void> deleteCapacityGroup(@PathVariable("groupId") Long groupId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOscpBiz().deleteCapacityGroup(userId, groupId);
		return success();
	}

	/**
	 * Get all available capacity group settings for the current user.
	 * 
	 * @return the capacity group settings
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/capacity-groups/settings")
	public Response<Collection<CapacityGroupSettings>> availableCapacityGroupSettings() {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<CapacityGroupSettings> list = userOscpBiz().capacityGroupSettingsForUser(userId);
		return response(list);
	}

	/**
	 * Update a capacity group settings for the current user.
	 * 
	 * @param id
	 *        the capacity group ID of the settings to update
	 * @param input
	 *        the input
	 * @return the configuration
	 */
	@RequestMapping(method = PUT, value = "/capacity-groups/{groupId}/settings", consumes = APPLICATION_JSON_VALUE)
	public Result<CapacityGroupSettings> updateCapacityGroupSettings(@PathVariable("groupId") Long id,
			@Valid @RequestBody CapacityGroupSettingsInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		CapacityGroupSettings result = userOscpBiz().updateCapacityGroupSettings(userId, id, input);
		return success(result);
	}

	/**
	 * View a specific capacity group settings.
	 * 
	 * @param id
	 *        the ID of the capacity group settings to view
	 * @return the settings
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/capacity-groups/{groupId}/settings")
	public Response<CapacityGroupSettings> viewCapacityGroupSettings(@PathVariable("groupId") Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return response(userOscpBiz().capacityGroupSettingsForUser(userId, id));
	}

	/**
	 * Delete a specific capacity group settings.
	 * 
	 * @param id
	 *        the ID of the capacity group settings to delete
	 * @return the result
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/capacity-groups/{groupId}/settings")
	public Response<Void> deleteCapacityGroupSettings(@PathVariable("groupId") Long id) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOscpBiz().deleteCapacityGroupSettings(userId, id);
		return response(null);
	}

	/**
	 * Get all available asset configurations within a given capacity group for
	 * the current user.
	 * 
	 * @return the configurations
	 */
	@RequestMapping(method = GET, value = "/capacity-groups/{groupId}/assets")
	public Result<Collection<AssetConfiguration>> availableCapacityGroupAssets(
			@PathVariable("groupId") Long groupId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<AssetConfiguration> list = userOscpBiz().assetsForUserCapacityGroup(userId, groupId);
		return success(list);
	}

	/**
	 * Create a new asset configuration for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = POST, value = "/assets", consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Result<AssetConfiguration>> createAsset(
			@Valid @RequestBody AssetConfigurationInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		AssetConfiguration result = userOscpBiz().createAsset(userId, input);
		URI loc = uriWithoutHost(
				fromMethodCall(on(UserOscpController.class).getAsset(result.getEntityId())));
		return ResponseEntity.created(loc).body(success(result));
	}

	/**
	 * Get an asset configuration for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = GET, value = "/assets/{assetId}")
	public Result<AssetConfiguration> getAsset(@PathVariable("assetId") Long assetId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		AssetConfiguration result = userOscpBiz().assetForUser(userId, assetId);
		return success(result);
	}

	/**
	 * Update an asset configuration for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = PUT, value = "/assets/{assetId}", consumes = APPLICATION_JSON_VALUE)
	public Result<AssetConfiguration> updateAsset(@PathVariable("assetId") Long assetId,
			@Valid @RequestBody AssetConfigurationInput input) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		AssetConfiguration result = userOscpBiz().updateAsset(userId, assetId, input);
		return success(result);
	}

	/**
	 * Delete an asset configuration for the current user.
	 * 
	 * @return the configuration
	 */
	@RequestMapping(method = DELETE, value = "/assets/{assetId}")
	public Result<Void> deleteAsset(@PathVariable("assetId") Long assetId) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		userOscpBiz().deleteAsset(userId, assetId);
		return success();
	}

}
