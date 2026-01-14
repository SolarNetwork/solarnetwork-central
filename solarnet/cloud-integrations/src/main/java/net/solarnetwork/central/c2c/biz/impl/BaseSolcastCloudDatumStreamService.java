/* ==================================================================
 * BaseSolcastCloudDatumStreamService.java - 30/10/2024 5:25:39 am
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

import static net.solarnetwork.util.StringUtils.nonEmptyString;
import java.time.Clock;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.settings.MultiValueSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;

/**
 * Abstract base class for Solcast implementations of
 * {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.2
 */
public abstract class BaseSolcastCloudDatumStreamService
		extends BaseRestOperationsCloudDatumStreamService {

	/** The setting for latitude. */
	public static final String LATITUDE_SETTING = "lat";

	/** The setting for longitude. */
	public static final String LONGITUDE_SETTING = "lon";

	/** The setting for azimuth. */
	public static final String AZIMUTH_SETTING = "azimuth";

	/** The setting for tilt. */
	public static final String TILT_SETTING = "tilt";

	/** The setting for array type. */
	public static final String ARRAY_TYPE_SETTING = "arrayType";

	/** The setting for resolution. */
	public static final String RESOLUTION_SETTING = "resolution";

	/** The {@code resolution} default value. */
	public static final Duration DEFAULT_RESOLUTION = Duration.ofMinutes(5);

	/**
	 * The setting to disallow use of the historic API.
	 *
	 * @since 1.2
	 */
	public static final String DISALLOW_HISTORIC_API_SETTING = "noHistoric";

	/**
	 * An internal query parameter used to indicate the live API should be used.
	 *
	 * @since 1.2
	 */
	protected static final String QUERY_PARAM_USE_LIVE_DATA = "live";

	/**
	 * An internal parameter map with just the
	 * {@link #QUERY_PARAM_USE_LIVE_DATA} value set.
	 *
	 * @since 1.2
	 */
	protected static final Map<String, Object> USE_LIVE_DATA = Map.of(QUERY_PARAM_USE_LIVE_DATA, true);

	/** The Solcast supported resolutions. */
	public static final Set<Duration> SUPPORTED_RESOLUTIONS;
	static {
		SUPPORTED_RESOLUTIONS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
		// @formatter:off
				Duration.ofMinutes(5),
				Duration.ofMinutes(10),
				Duration.ofMinutes(15),
				Duration.ofMinutes(20),
				Duration.ofMinutes(30),
				Duration.ofMinutes(60)
		// @formatter:on
		)));
	}

	/** Setting specifier for the {@link #ARRAY_TYPE_SETTING} setting. */
	public static final MultiValueSettingSpecifier ARRAY_TYPE_SETTTING_SPECIFIER;
	static {
		BasicMultiValueSettingSpecifier arrayTypeSpec = new BasicMultiValueSettingSpecifier(
				ARRAY_TYPE_SETTING, "");
		Map<String, String> arrayTypeValues = new LinkedHashMap<>(3);
		arrayTypeValues.put("", "");
		arrayTypeValues.put("fixed", "Fixed");
		arrayTypeValues.put("horizontal_single_axis", "Horizontal Single Axis");
		arrayTypeSpec.setValueTitles(arrayTypeValues);
		ARRAY_TYPE_SETTTING_SPECIFIER = arrayTypeSpec;
	}

	/** Setting specifier for the {@link #RESOLUTION_SETTING} setting. */
	public static final MultiValueSettingSpecifier RESOLUTION_SETTING_SPECIFIER;
	static {
		BasicMultiValueSettingSpecifier resolutionSpec = new BasicMultiValueSettingSpecifier(
				RESOLUTION_SETTING, DEFAULT_RESOLUTION.toString());
		Map<String, String> resolutionMenuValues = new LinkedHashMap<>(SUPPORTED_RESOLUTIONS.size());
		for ( Duration d : SUPPORTED_RESOLUTIONS ) {
			String key = d.toString();
			resolutionMenuValues.put(key, (d.toSeconds() / 60) + " minute");
		}
		resolutionSpec.setValueTitles(resolutionMenuValues);
		RESOLUTION_SETTING_SPECIFIER = resolutionSpec;

	}

	/**
	 * Constructor.
	 *
	 * @param serviceIdentifier
	 *        the service identifier
	 * @param displayName
	 *        the display name
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
	 * @param restOpsLogger
	 *        the logger to use with the REST operations
	 * @param clock
	 *        the clock to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseSolcastCloudDatumStreamService(String serviceIdentifier, String displayName,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao,
			List<SettingSpecifier> settings, RestOperations restOps, Logger restOpsLogger, Clock clock) {
		super(serviceIdentifier, displayName, clock, userEventAppenderBiz, encryptor, expressionService,
				integrationDao, datumStreamDao, datumStreamMappingDao, datumStreamPropertyDao, settings,
				new SolcastRestOperationsHelper(restOpsLogger, userEventAppenderBiz, restOps,
						INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						_ -> SolcastCloudIntegrationService.SECURE_SETTINGS));
	}

	/**
	 * Resolve a resolution setting value.
	 *
	 * <p>
	 * The returned resolution will be limited to those in the
	 * {@link #SUPPORTED_RESOLUTIONS} set. If no {@link #RESOLUTION_SETTING} is
	 * available, or is not a supported resolution, then
	 * {@link #DEFAULT_RESOLUTION} will be returned.
	 * </p>
	 *
	 * @param datumStream
	 *        the datum stream to resolve the resolution setting from
	 * @return the resolution, never {@literal null}
	 */
	public static Duration resolveResolution(CloudDatumStreamConfiguration datumStream) {
		String resoValue = nonEmptyString(datumStream.serviceProperty(RESOLUTION_SETTING, String.class));
		Duration result = DEFAULT_RESOLUTION;
		if ( resoValue != null ) {
			try {
				result = Duration.ofSeconds(Long.parseLong(resoValue));
			} catch ( NumberFormatException e ) {
				try {
					result = Duration.parse(resoValue);
				} catch ( DateTimeParseException e2 ) {
					// ignore and fall back to default
				}
			}
		}
		if ( !SUPPORTED_RESOLUTIONS.contains(result) ) {
			result = DEFAULT_RESOLUTION;
		}
		return result;
	}

}
