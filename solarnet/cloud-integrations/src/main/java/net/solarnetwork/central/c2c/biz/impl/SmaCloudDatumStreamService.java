/* ==================================================================
 * SmaCloudDatumStreamService.java - 29/03/2025 7:32:45 am
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

package net.solarnetwork.central.c2c.biz.impl;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import javax.cache.Cache;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.http.OAuth2RestOperationsHelper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.IntRange;

/**
 * SMA implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.0
 */
public class SmaCloudDatumStreamService extends BaseRestOperationsCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.sma";

	/** The data value filter key for a system ID. */
	public static final String SYSTEM_ID_FILTER = "systemId";

	/** The data value filter key for a device ID. */
	public static final String DEVICE_ID_FILTER = "deviceId";

	/** The setting to upper-case source ID values. */
	public static final String UPPER_CASE_SOURCE_ID_SETTING = "upperCaseSourceId";

	/**
	 * The URI path to view a given system.
	 *
	 * <p>
	 * Accepts a single {@code {plantId}} parameter.
	 * </p>
	 */
	public static final String SYSTEM_VIEW_URL_TEMPLATE = "/v1/plants/{systemId}";

	/**
	 * The URI path to list the devices for a given system.
	 *
	 * <p>
	 * Accepts a single {@code {plantId}} parameter.
	 * </p>
	 */
	public static final String SYSTEM_DEVICES_URL_TEMPLATE = "/v1/plants/{systemId}/devices/lean";

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		SETTINGS = List.of(UPPER_CASE_SOURCE_ID_SETTING_SPECIFIER);
	}

	/** The supported placeholder keys. */
	public static final List<String> SUPPORTED_PLACEHOLDERS = List.of(SYSTEM_ID_FILTER,
			DEVICE_ID_FILTER);

	/** The data value identifier levels source ID range. */
	public static final IntRange DATA_VALUE_IDENTIFIER_LEVELS_SOURCE_ID_RANGE = IntRange.rangeOf(0, 2);

	/** The maximum period of time to request data for in one request. */
	private static final Duration MAX_QUERY_TIME_RANGE = Duration.ofDays(7);

	/**
	 * Constructor.
	 *
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param encryptor
	 *        the sensitive key encryptor
	 * @param expressionService
	 *        the expression service
	 * @param integrationDao
	 *        the integration DAO
	 * @param datumStreamDao
	 *        the datum stream DAO
	 * @param datumStreamMappingDao
	 *        the datum stream mapping DAO
	 * @param datumStreamPropertyDao
	 *        the datum stream property DAO
	 * @param restOps
	 *        the REST operations
	 * @param oauthClientManager
	 *        the OAuth client manager
	 * @param clock
	 *        the instant source to use
	 * @param integrationLocksCache
	 *        an optional cache that, when provided, will be used to obtain a
	 *        lock before acquiring an access token; this can be used in prevent
	 *        concurrent requests using the same {@code config} from making
	 *        multiple token requests; not the cache is assumed to have
	 *        read-through semantics that always returns a new lock for missing
	 *        keys
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SmaCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			OAuth2AuthorizedClientManager oauthClientManager, Clock clock,
			Cache<UserLongCompositePK, Lock> integrationLocksCache) {
		super(SERVICE_IDENTIFIER, "SMA Datum Stream Service", clock, userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS,
				new OAuth2RestOperationsHelper(LoggerFactory.getLogger(SmaCloudDatumStreamService.class),
						userEventAppenderBiz, restOps, INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> SmaCloudIntegrationService.SECURE_SETTINGS,
						oauthClientManager, clock, integrationLocksCache));
	}

	@Override
	protected Iterable<String> supportedPlaceholders() {
		return SUPPORTED_PLACEHOLDERS;
	}

	@Override
	protected IntRange dataValueIdentifierLevelsSourceIdRange() {
		return DATA_VALUE_IDENTIFIER_LEVELS_SOURCE_ID_RANGE;
	}

	@Override
	public Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale) {
		MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");
		List<LocalizedServiceInfo> result = new ArrayList<>(2);
		for ( String key : new String[] { SYSTEM_ID_FILTER, DEVICE_ID_FILTER } ) {
			result.add(new BasicLocalizedServiceInfo(key, locale,
					ms.getMessage("dataValueFilter.%s.key".formatted(key), null, key, locale),
					ms.getMessage("dataValueFilter.%s.desc".formatted(key), null, null, locale), null));
		}
		return result;
	}

	@Override
	public Iterable<CloudDataValue> dataValues(UserLongCompositePK integrationId,
			Map<String, ?> filters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CloudDatumStreamQueryResult datum(CloudDatumStreamConfiguration datumStream,
			CloudDatumStreamQueryFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

}
