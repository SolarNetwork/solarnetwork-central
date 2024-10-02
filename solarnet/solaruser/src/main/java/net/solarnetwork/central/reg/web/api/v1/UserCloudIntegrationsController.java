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
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.central.web.GlobalExceptionRestController;
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
	 * List the available transform services.
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

	/**
	 * List the available transform services.
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
