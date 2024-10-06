/* ==================================================================
 * BaseCloudDatumStreamService.java - 7/10/2024 7:35:35 am
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

package net.solarnetwork.central.c2c.biz.impl;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.List;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Base implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.0
 */
public abstract class BaseCloudDatumStreamService extends BaseCloudIntegrationsIdentifiableService
		implements CloudDatumStreamService {

	/** The integration configuration entity DAO. */
	protected final CloudIntegrationConfigurationDao integrationDao;

	/** The datum stream configuration DAO. */
	protected final CloudDatumStreamConfigurationDao datumStreamDao;

	/**
	 * Constructor.
	 *
	 * @param serviceIdentifier
	 *        the service identifier
	 * @param displayName
	 *        the display name
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param integrationDao
	 *        the integration DAO
	 * @param datumStreamDao
	 *        the datum stream DAO
	 * @param settings
	 *        the service settings
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseCloudDatumStreamService(String serviceIdentifier, String displayName,
			UserEventAppenderBiz userEventAppenderBiz, CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao, List<SettingSpecifier> settings) {
		super(serviceIdentifier, displayName, userEventAppenderBiz, settings);
		this.integrationDao = requireNonNullArgument(integrationDao, "integrationDao");
		this.datumStreamDao = requireNonNullArgument(datumStreamDao, "datumStreamDao");
	}

}
