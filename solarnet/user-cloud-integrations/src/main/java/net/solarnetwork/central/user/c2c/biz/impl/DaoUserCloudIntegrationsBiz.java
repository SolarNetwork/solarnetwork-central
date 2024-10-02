/* ==================================================================
 * DaoUserCloudIntegrationsBiz.java - 30/09/2024 11:24:58 am
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

package net.solarnetwork.central.user.c2c.biz.impl;

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.service.LocalizedServiceInfoProvider.localizedServiceSettings;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.Result;

/**
 * DAO based implementation of {@link UserCloudIntegrationsBiz}.
 *
 * @author matt
 * @version 1.0
 */
public class DaoUserCloudIntegrationsBiz implements UserCloudIntegrationsBiz {

	private final CloudIntegrationConfigurationDao configurationDao;
	private final Map<String, CloudIntegrationService> integrationServices;

	/**
	 * Constructor.
	 *
	 * @param configurationDao
	 *        the configuration DAO
	 * @param integrationServices
	 *        the integration services
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserCloudIntegrationsBiz(CloudIntegrationConfigurationDao configurationDao,
			Collection<CloudIntegrationService> integrationServices) {
		super();
		this.configurationDao = requireNonNullArgument(configurationDao, "configurationDao");
		this.integrationServices = requireNonNullArgument(integrationServices, "integrationServices")
				.stream().collect(Collectors.toUnmodifiableMap(CloudIntegrationService::getId,
						Function.identity()));

	}

	@Override
	public Iterable<LocalizedServiceInfo> availableIntegrationServices(Locale locale) {
		return localizedServiceSettings(integrationServices.values(), locale);
	}

	@Override
	public Result<Void> validateIntegrationConfigurationForId(UserLongCompositePK id) {
		final CloudIntegrationConfiguration conf = requireNonNullObject(
				configurationDao.get(requireNonNullArgument(id, "id")), id);

		final CloudIntegrationService service = requireNonNullObject(
				integrationServices.get(conf.getServiceIdentifier()), conf.getServiceIdentifier());

		return service.validate(conf);
	}

}
