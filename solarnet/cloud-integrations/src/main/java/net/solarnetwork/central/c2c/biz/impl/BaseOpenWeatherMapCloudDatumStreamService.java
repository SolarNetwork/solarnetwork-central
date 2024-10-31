/* ==================================================================
 * BaseOpenWeatherMapCloudDatumStreamService.java - 31/10/2024 3:19:07 pm
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

import static net.solarnetwork.codec.JsonUtils.parseBigDecimalAttribute;
import static net.solarnetwork.codec.JsonUtils.parseLongAttribute;
import static net.solarnetwork.util.NumberUtils.bigDecimalForNumber;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.domain.datum.AtmosphericDatum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.DayDatum;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Abstract base class for OpenWeatherMap implementations of
 * {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.0
 */
public abstract class BaseOpenWeatherMapCloudDatumStreamService
		extends BaseRestOperationsCloudDatumStreamService {

	/** The setting for latitude. */
	public static final String LATITUDE_SETTING = "lat";

	/** The setting for longitude. */
	public static final String LONGITUDE_SETTING = "lon";

	/** The setting for location identifier. */
	public static final String LOCATION_ID_SETTING = "locId";

	/** The clock. */
	protected final Clock clock;

	/**
	 * Constructor.
	 *
	 * @param serviceIdentifier
	 *        the service identifier
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
	public BaseOpenWeatherMapCloudDatumStreamService(String serviceIdentifier, String displayName,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao,
			List<SettingSpecifier> settings, RestOperations restOps, Logger restOpsLogger, Clock clock) {
		super(serviceIdentifier, displayName, userEventAppenderBiz, encryptor, expressionService,
				integrationDao, datumStreamDao, datumStreamMappingDao, datumStreamPropertyDao, settings,
				new OpenWeatherMapRestOperationsHelper(restOpsLogger, userEventAppenderBiz, restOps,
						HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> OpenWeatherMapCloudIntegrationService.SECURE_SETTINGS));
		this.clock = requireNonNullArgument(clock, "clock");
	}

	/**
	 * Parse a weather data response object.
	 *
	 * <p>
	 * This method supports both weather and forecast data, which are similar
	 * but slightly different.
	 * </p>
	 *
	 * @param node
	 *        the JSON
	 * @param kind
	 *        the datum stream kind
	 * @param objectId
	 *        the datum stream object ID
	 * @param sourceId
	 *        the datum stream source ID
	 * @return the datum, or {@literal null}
	 */
	public static GeneralDatum parseWeatherData(JsonNode node, ObjectDatumKind kind, Long objectId,
			String sourceId) {
		/*- EXAMPLE WEATHER JSON:
		{
		   "coord": {
		      "lon": 7.367,
		      "lat": 45.133
		   },
		   "weather": [
		      {
		         "id": 501,
		         "main": "Rain",
		         "description": "moderate rain",
		         "icon": "10d"
		      }
		   ],
		   "base": "stations",
		   "main": {
		      "temp": 284.2,
		      "feels_like": 282.93,
		      "temp_min": 283.06,
		      "temp_max": 286.82,
		      "pressure": 1021,
		      "humidity": 60,
		      "sea_level": 1021,
		      "grnd_level": 910
		   },
		   "visibility": 10000,
		   "wind": {
		      "speed": 4.09,
		      "deg": 121,
		      "gust": 3.47
		   },
		   "rain": {
		      "1h": 2.73
		   },
		   "clouds": {
		      "all": 83
		   },
		   "dt": 1726660758,
		   "sys": {
		      "type": 1,
		      "id": 6736,
		      "country": "IT",
		      "sunrise": 1726636384,
		      "sunset": 1726680975
		   },
		   "timezone": 7200,
		   "id": 3165523,
		   "name": "Province of Turin",
		   "cod": 200
		}
		 */
		if ( node == null || !node.isObject() ) {
			return null;
		}
		final DatumSamples samples = new DatumSamples();

		populateJsonDatumPropertyValue(node, "visibility", DatumSamplesType.Instantaneous,
				AtmosphericDatum.VISIBILITY_KEY, samples);

		JsonNode main = node.path("main");
		populateJsonDatumPropertyValue(main, "temp", DatumSamplesType.Instantaneous,
				AtmosphericDatum.TEMPERATURE_KEY, samples);
		populateJsonDatumPropertyValue(main, "temp_min", DatumSamplesType.Instantaneous,
				DayDatum.TEMPERATURE_MINIMUM_KEY, samples);
		populateJsonDatumPropertyValue(main, "temp_max", DatumSamplesType.Instantaneous,
				DayDatum.TEMPERATURE_MAXIMUM_KEY, samples);

		populateJsonDatumPropertyValue(main, "pressure", DatumSamplesType.Instantaneous,
				AtmosphericDatum.ATMOSPHERIC_PRESSURE_KEY, samples,
				(val) -> bigDecimalForNumber((Number) val).multiply(new BigDecimal(100)).intValue());

		populateJsonDatumPropertyValue(main, "humidity", DatumSamplesType.Instantaneous,
				AtmosphericDatum.HUMIDITY_KEY, samples);

		JsonNode weather = node.path("weather");
		if ( weather.isArray() && weather.size() > 0 ) {
			weather = weather.iterator().next();
			populateJsonDatumPropertyValue(weather, "main", DatumSamplesType.Status,
					AtmosphericDatum.SKY_CONDITIONS_KEY, samples);

			populateJsonDatumPropertyValue(weather, "icon", DatumSamplesType.Status, "iconId", samples);
		}

		JsonNode wind = node.path("wind");
		populateJsonDatumPropertyValue(wind, "deg", DatumSamplesType.Instantaneous,
				AtmosphericDatum.WIND_DIRECTION_KEY, samples,
				(val) -> bigDecimalForNumber((Number) val).setScale(0, RoundingMode.HALF_UP).intValue());

		populateJsonDatumPropertyValue(wind, "speed", DatumSamplesType.Instantaneous,
				AtmosphericDatum.WIND_SPEED_KEY, samples);

		populateJsonDatumPropertyValue(wind, "gust", DatumSamplesType.Instantaneous, "wgust", samples);

		JsonNode clouds = node.path("clouds");
		populateJsonDatumPropertyValue(clouds, "all", DatumSamplesType.Instantaneous, "cloudiness",
				samples);

		JsonNode rain = node.path("rain");
		BigDecimal oneHourRain = parseBigDecimalAttribute(rain, "1h");
		if ( oneHourRain != null ) {
			samples.putInstantaneousSampleValue(AtmosphericDatum.RAIN_KEY,
					oneHourRain.setScale(0, RoundingMode.HALF_UP).intValue());
		} else {
			BigDecimal threeHourRain = parseBigDecimalAttribute(rain, "3h");
			if ( threeHourRain != null ) {
				samples.putInstantaneousSampleValue(AtmosphericDatum.RAIN_KEY,
						threeHourRain.setScale(0, RoundingMode.HALF_UP).intValue());
			}
		}

		if ( samples.isEmpty() ) {
			return null;
		}
		Instant ts = parseTimestampNode(node, "dt");
		return new GeneralDatum(new DatumId(kind, objectId, sourceId, ts), samples);
	}

	/**
	 * Parse a Unix epoch seconds timestamp (non-fractional).
	 *
	 * @param node
	 *        the node
	 * @param key
	 *        the field name to parse
	 * @return the date, or {@literal null}
	 */
	public static Instant parseTimestampNode(JsonNode node, String key) {
		Long val = parseLongAttribute(node, key);
		if ( val == null ) {
			return null;
		}
		return Instant.ofEpochSecond(val);
	}

}
