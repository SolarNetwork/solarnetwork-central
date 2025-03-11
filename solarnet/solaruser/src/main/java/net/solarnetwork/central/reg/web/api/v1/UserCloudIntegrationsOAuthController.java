/* ==================================================================
 * UserCloudIntegrationsOAuthController.java - 10/03/2025 10:37:59 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static java.util.Collections.emptyList;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.central.security.SecurityUtils.getCurrentActorUserId;
import static net.solarnetwork.domain.Result.success;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.HttpRequestInfo;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;
import net.solarnetwork.settings.KeyedSettingSpecifier;

/**
 * Web service API for cloud integrations OAuth management.
 *
 * @author matt
 * @version 1.0
 */
@Profile(SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS)
@GlobalExceptionRestController
@RestController("v1CloudIntegrationsOAuthController")
@RequestMapping(value = { "/api/v1/sec/user/c2c/oauth", "/u/sec/c2c/oauth" })
public class UserCloudIntegrationsOAuthController {

	private final UserCloudIntegrationsBiz userCloudIntegrationsBiz;

	/**
	 * Constructor.
	 *
	 * @param userCloudIntegrationsBiz
	 *        the user cloud integrations service
	 */
	public UserCloudIntegrationsOAuthController(
			@Autowired(required = false) UserCloudIntegrationsBiz userCloudIntegrationsBiz) {
		super();
		this.userCloudIntegrationsBiz = userCloudIntegrationsBiz;
	}

	/**
	 * Get the {@link UserCloudIntegrationsBiz}.
	 *
	 * @return the service; never {@literal null}
	 * @throws UnsupportedOperationException
	 *         if the service is not available
	 */
	private UserCloudIntegrationsBiz biz() {
		if ( userCloudIntegrationsBiz == null ) {
			throw new UnsupportedOperationException("UserCloudIntegrationsBiz service not available.");
		}
		return userCloudIntegrationsBiz;
	}

	/**
	 * List the actor's configured integrations that require OAuth access token
	 * configuration.
	 *
	 * @param filter
	 *        the pagination criteria
	 * @return the results
	 */
	@RequestMapping(value = "/integrations", method = RequestMethod.GET)
	public Result<FilterResults<CloudIntegrationConfiguration, UserLongCompositePK>> listOauthCloudIntegrationConfigurations(
			BasicFilter filter) {
		final UserCloudIntegrationsBiz biz = biz();

		// force filter to just integrations that require OAuth access token configuration
		var services = biz.availableIntegrationServices();
		var oauthServiceIdents = StreamSupport.stream(services.spliterator(), false)
				.filter(s -> s.getSettingSpecifiers().stream()
						.filter(ss -> ss instanceof KeyedSettingSpecifier<?> k
								&& CloudIntegrationService.OAUTH_ACCESS_TOKEN_SETTING.equals(k.getKey()))
						.findAny().isPresent())
				.map(s -> s.getId()).toArray(String[]::new);

		if ( oauthServiceIdents.length < 1 ) {
			return success(new BasicFilterResults<>(emptyList()));
		}

		filter.setServiceIdentifiers(oauthServiceIdents);

		var result = biz.listConfigurationsForUser(getCurrentActorUserId(), filter,
				CloudIntegrationConfiguration.class);
		return success(result);
	}

	/**
	 * Generate authorization information for an integration.
	 *
	 * @param integrationId
	 *        the integration ID
	 * @param redirectUri
	 *        an optional redirect URL to use; if not provided an internal URI
	 *        will be used
	 * @param locale
	 *        the desired locale for messages
	 * @return the information
	 */
	@RequestMapping(value = "/integrations/{integrationId}/auth-info", method = RequestMethod.GET)
	public Result<HttpRequestInfo> getOAuthCloudIntegrationAuthorizationInfo(
			@PathVariable("integrationId") Long integrationId,
			@RequestParam(value = "redirect_uri", required = false) String redirectUri, Locale locale) {
		final UserCloudIntegrationsBiz biz = biz();

		var id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
		var integration = biz.configurationForId(id, CloudIntegrationConfiguration.class);
		var service = requireNonNullObject(biz.integrationService(integration.getServiceIdentifier()),
				integration.getServiceIdentifier());

		URI redirect = (redirectUri != null ? URI.create(redirectUri)
				: fromMethodCall(
						on(net.solarnetwork.central.reg.web.UserCloudIntegrationsOAuthController.class)
								.handleOAuthAuthCode(null, Locale.getDefault()))
										.buildAndExpand(integrationId).toUri());

		var result = service.authorizationRequestInfo(integration, redirect, locale);
		return success(result);
	}

	/**
	 * Get an OAuth access token for a given authorization code.
	 *
	 * <p>
	 * The
	 * {@link #getOAuthCloudIntegrationAuthorizationInfo(Long, String, Locale)}
	 * method must have been invoked before this method, to obtain the necessary
	 * {@code state} value. The token details will also be automatically saved
	 * on the
	 * </p>
	 *
	 * @param integrationId
	 *        the ID of the integration to get
	 * @param code
	 *        the code value returned from the OAuth authorization provider
	 * @param state
	 *        the state value returned from a previous call to
	 *        {@link #getOAuthCloudIntegrationAuthorizationInfo(Long, String, Locale)}
	 * @param redirectUri
	 *        the same value passed to the previous call to
	 *        {@link #getOAuthCloudIntegrationAuthorizationInfo(Long, String, Locale)}
	 * @param locale
	 *        the desired locale for messages
	 * @return the token details
	 */
	@RequestMapping(value = "/integrations/{integrationId}/auth-token", method = RequestMethod.POST)
	public Result<Map<String, ?>> getOAuthTokenForAuthCode(
			@PathVariable("integrationId") Long integrationId, @RequestParam("code") String code,
			@RequestParam("state") String state,
			@RequestParam(name = "redirect_uri", required = false) String redirectUri, Locale locale) {
		final UserCloudIntegrationsBiz biz = biz();

		var id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
		var integration = biz.configurationForId(id, CloudIntegrationConfiguration.class);
		var service = requireNonNullObject(biz.integrationService(integration.getServiceIdentifier()),
				integration.getServiceIdentifier());

		var uri = (redirectUri != null ? URI.create(redirectUri)
				: fromMethodCall(
						on(net.solarnetwork.central.reg.web.UserCloudIntegrationsOAuthController.class)
								.handleOAuthAuthCode(null, Locale.getDefault()))
										.buildAndExpand(integrationId).toUri());

		var params = Map.of(CloudIntegrationService.AUTHORIZATION_CODE_PARAM, code,
				CloudIntegrationService.AUTHORIZATION_STATE_PARAM, state,
				CloudIntegrationService.REDIRECT_URI_PARAM, uri);

		Map<String, ?> token = service.fetchAccessToken(integration, params, locale);

		biz.mergeConfigurationServiceProperties(id, token, CloudIntegrationConfiguration.class);

		return success(token);
	}

}
