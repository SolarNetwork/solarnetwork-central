/* ==================================================================
 * UserCloudIntegrationsOAuthController.java - 10/03/2025 9:00:29 am
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

package net.solarnetwork.central.reg.web;

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.central.security.SecurityUtils.getCurrentActorUserId;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;

/**
 * Controller for Cloud Integrations OAuth UI.
 *
 * @author matt
 * @version 1.0
 */
@GlobalServiceController
@RequestMapping("/u/sec/c2c/oauth")
public class UserCloudIntegrationsOAuthController {

	/** The model attribute for the actor's user ID. */
	public static final String ACTOR_USER_ID_ATTR = "actorUserId";

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

	@ModelAttribute(name = ACTOR_USER_ID_ATTR)
	public Long actorUserId() {
		return SecurityUtils.getCurrentActorUserId();
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	public ModelAndView oauthHome() {
		return new ModelAndView("sec/c2c/oauth");
	}

	/**
	 * Get an OAuth access token for a given authorization code.
	 *
	 * <p>
	 * The
	 * {@link net.solarnetwork.central.reg.web.api.v1.UserCloudIntegrationsOAuthController#getOAuthCloudIntegrationAuthorizationInfo(Long, String, Locale)}
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
	 *        {@code getOAuthCloudIntegrationAuthorizationInfo()}
	 * @param redirectUri
	 *        the same value passed to the previous call to
	 *        {@code getOAuthCloudIntegrationAuthorizationInfo()}
	 * @param locale
	 *        the desired locale for messages
	 * @return the resulting view
	 */
	@RequestMapping(value = "/integrations/{integrationId}/auth-code", method = RequestMethod.GET)
	public ModelAndView handleOAuthAuthCode(@PathVariable("integrationId") Long integrationId,
			@RequestParam("code") String code, @RequestParam("state") String state,
			@RequestParam(name = "redirect_uri", required = false) String redirectUri, Locale locale) {
		final UserCloudIntegrationsBiz biz = biz();
		var id = new UserLongCompositePK(getCurrentActorUserId(), integrationId);
		var integration = biz.configurationForId(id, CloudIntegrationConfiguration.class);
		var service = requireNonNullObject(biz.integrationService(integration.getServiceIdentifier()),
				integration.getServiceIdentifier());

		var uri = (redirectUri != null ? URI.create(redirectUri)
				: fromMethodCall(on(UserCloudIntegrationsOAuthController.class).handleOAuthAuthCode(0L,
						"", "", null, Locale.getDefault())).replaceQueryParams(null)
								.buildAndExpand(integrationId).toUri());

		var params = Map.of(CloudIntegrationService.AUTHORIZATION_CODE_PARAM, code,
				CloudIntegrationService.AUTHORIZATION_STATE_PARAM, state,
				CloudIntegrationService.REDIRECT_URI_PARAM, uri);

		Map<String, ?> token = service.fetchAccessToken(integration, params, locale);

		biz.mergeConfigurationServiceProperties(id, token, CloudIntegrationConfiguration.class);

		return new ModelAndView("redirect:sec/c2c/oauth");
	}

}
