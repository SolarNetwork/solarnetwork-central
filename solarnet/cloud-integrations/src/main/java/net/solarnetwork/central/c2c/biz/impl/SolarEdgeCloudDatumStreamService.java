/* ==================================================================
 * SolarEdgeCloudDatumStreamService.java - 7/10/2024 7:03:25 am
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

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.http.RestOperationsHelper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;

/**
 * SolarEdge implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.0
 */
public class SolarEdgeCloudDatumStreamService extends BaseRestOperationsCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.solaredge";

	/** The data value filter key for a site ID. */
	public static final String SITE_ID_FILTER = "siteId";

	/**
	 * Constructor.
	 *
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param integrationDao
	 *        the integration DAO
	 * @param datumStreamDao
	 *        the datum stream DAO
	 * @param restOps
	 *        the REST operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SolarEdgeCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao, RestOperations restOps) {
		super(SERVICE_IDENTIFIER, "SolarEdge Datum Stream Service", userEventAppenderBiz, integrationDao,
				datumStreamDao, Collections.emptyList(),
				new RestOperationsHelper(LoggerFactory.getLogger(SolarEdgeCloudDatumStreamService.class),
						userEventAppenderBiz, restOps, HTTP_ERROR_TAGS));
	}

	@Override
	public Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale) {
		MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");
		List<LocalizedServiceInfo> result = new ArrayList<>(2);
		for ( String key : new String[] { SITE_ID_FILTER } ) {
			result.add(new BasicLocalizedServiceInfo(key, locale,
					ms.getMessage("dataValueFilter.%s.key".formatted(key), null, key, locale),
					ms.getMessage("dataValueFilter.%s.desc".formatted(key), null, null, locale), null));
		}
		return result;
	}

	@Override
	public Iterable<CloudDataValue> dataValues(UserLongCompositePK id, Map<String, ?> filters) {
		final CloudDatumStreamConfiguration datumStream = requireNonNullObject(
				datumStreamDao.get(requireNonNullArgument(id, "id")), "datumStream");
		final CloudIntegrationConfiguration integration = integrationDao
				.get(new UserLongCompositePK(datumStream.getUserId(), datumStream.getIntegrationId()));
		List<CloudDataValue> result = Collections.emptyList();
		// TODO
		Collections.sort(result);
		return result;
	}

}
