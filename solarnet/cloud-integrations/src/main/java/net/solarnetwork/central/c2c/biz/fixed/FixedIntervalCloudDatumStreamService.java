/* ==================================================================
 * FixedIntervalCloudDatumStreamService.java - 13 Sept 2026 6:21:48 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.fixed;

import static java.util.Collections.unmodifiableMap;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicCloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * {@link CloudDatumStreamService} for statically-configured ("fixed") data at
 * regular intervals.
 *
 * @author matt
 * @version 1.0
 */
public class FixedIntervalCloudDatumStreamService extends BaseCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.fixed.interval";

	/** The setting for granularity. */
	public static final String GRANULARITY_SETTING = "granularity";

	/** The setting for time zone identifier. */
	public static final String TIME_ZONE_SETTING = "tz";

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;

	static {
		// menu for granularity
		var granularitySpec = new BasicMultiValueSettingSpecifier(GRANULARITY_SETTING,
				FixedGranularity.FifteenMinute.name());
		var granularityTitles = unmodifiableMap(Arrays.stream(FixedGranularity.values())
				.collect(Collectors.toMap(FixedGranularity::name, FixedGranularity::name, (_, r) -> r,
						() -> new LinkedHashMap<>(FixedGranularity.values().length))));
		granularitySpec.setValueTitles(granularityTitles);

		// @formatter:off
		SETTINGS = List.of(
				  granularitySpec
				, new BasicTextFieldSettingSpecifier(TIME_ZONE_SETTING, null)
				, VIRTUAL_SOURCE_IDS_SETTING_SPECIFIER
			);
		// @formatter:on
	}

	/** The number of datum to generate within one request. */
	public static final int MAX_DATUM = 500;

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the instant source to use
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
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public FixedIntervalCloudDatumStreamService(Clock clock, UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao) {
		super(SERVICE_IDENTIFIER, "Fixed Interval Datum Stream Service", clock, userEventAppenderBiz,
				encryptor, expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS);
	}

	@Override
	public Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale) {
		return List.of();
	}

	@Override
	public Iterable<CloudDataValue> dataValues(UserLongCompositePK integrationId,
			@Nullable Map<String, ?> filters) {
		return List.of();
	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		requireNonNullArgument(datumStream, "datumStream");
		final ZoneId zone = resolveTimeZone(datumStream, TIME_ZONE_SETTING, null);
		final FixedGranularity granularity = resolveGranularity(datumStream, null);

		final Instant now = clock.instant();
		final Instant endDate = granularity.tickStart(now, zone);
		final Instant startDate = granularity.prevTickStart(endDate, zone);

		final var filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);

		final var result = datum(datumStream, filter);
		if ( result == null ) {
			return List.of();
		}
		return result.getResults();
	}

	@Override
	public CloudDatumStreamQueryResult datum(final CloudDatumStreamConfiguration datumStream,
			final CloudDatumStreamQueryFilter filter) {
		requireNonNullArgument(datumStream, "datumStream");
		requireNonNullArgument(filter, "filter");
		return performAction(datumStream, (_, ds, mapping, integration, valueProps, exprProps) -> {
			final ZoneId zone = resolveTimeZone(ds, TIME_ZONE_SETTING, filter.getParameters());
			final FixedGranularity granularity = resolveGranularity(ds, filter.getParameters());

			final Instant filterStartDate = requireNonNullArgument(filter.getStartDate(),
					"filter.startDate");
			final Instant filterEndDate = requireNonNullArgument(filter.getEndDate(),
					"filter.startDate");

			final Instant startDate = granularity.tickStart(filterStartDate, zone);
			Instant endDate = granularity.tickStart(filterEndDate, zone);
			if ( endDate.isBefore(filterEndDate) ) {
				endDate = granularity.nextTickStart(endDate, zone);
			}

			BasicQueryFilter nextQueryFilter = null;

			final List<GeneralDatum> resultDatum = new ArrayList<>(32);
			for ( ZonedDateTime ts = startDate.atZone(zone), end = endDate.atZone(zone); ts
					.isBefore(end); ts = ts.plus(granularity.getTickAmount()) ) {
				if ( resultDatum.size() == MAX_DATUM ) {
					nextQueryFilter = new BasicQueryFilter();
					nextQueryFilter.setStartDate(ts.toInstant());
					nextQueryFilter.setEndDate(filterEndDate);
					break;
				}
				final GeneralDatum d = new GeneralDatum(DatumId.datumId(ds.getKind(), ds.getObjectId(),
						ds.getSourceId(), ts.toInstant()), new DatumSamples());

				for ( CloudDatumStreamPropertyConfiguration valueProp : valueProps ) {
					Object val = valueProp.getValueReference();
					if ( valueProp.getPropertyType() == DatumSamplesType.Instantaneous
							|| valueProp.getPropertyType() == DatumSamplesType.Accumulating ) {
						// must be a numeric value
						val = NumberUtils.narrow(StringUtils.numberValue(val.toString()), 2);
					}
					if ( val != null ) {
						d.asMutableSampleOperations().putSampleValue(valueProp.getPropertyType(),
								valueProp.getPropertyName(), val);
					}
				}

				resultDatum.add(d);
			}

			// evaluate expressions
			var r = evaluateExpressions(ds, exprProps, resultDatum, mapping.getConfigId(),
					integration.getConfigId());

			return new BasicCloudDatumStreamQueryResult(filter, nextQueryFilter,
					r.stream().filter(d -> !d.isEmpty()).map(Datum.class::cast).toList(), null);
		});
	}

	/**
	 * Resolve the appropriate granularity to use.
	 *
	 * @param datumStream
	 *        the datum stream
	 * @param parameters
	 *        optional parameters to override the datum stream settings
	 * @return the granularity to use, falling back to {@code FifteenMinute} if
	 *         not otherwise available
	 */
	public static FixedGranularity resolveGranularity(CloudDatumStreamConfiguration datumStream,
			@Nullable Map<String, ?> parameters) {
		FixedGranularity result = null;
		try {
			String settingVal = null;
			if ( parameters != null && parameters.get(GRANULARITY_SETTING) instanceof String s ) {
				settingVal = s;
			} else if ( datumStream != null ) {
				settingVal = datumStream.serviceProperty(GRANULARITY_SETTING, String.class);
			}
			if ( settingVal != null && !settingVal.isEmpty() ) {
				result = FixedGranularity.fromValue(settingVal);
			}
		} catch ( IllegalArgumentException e ) {
			// ignore
		}
		return (result != null ? result : FixedGranularity.FifteenMinute);
	}

}
