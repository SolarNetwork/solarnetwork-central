/* ==================================================================
 * SolcastIrradianceCloudDatumStreamService.java - 30/10/2024 5:44:29 am
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

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DESCRIPTION_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.UNIT_OF_MEASURE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity.resolvePlaceholders;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
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
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * Solcast implementation of {@link CloudDatumStreamService} using the
 * irradiance API.
 *
 * @author matt
 * @version 1.6
 */
public class SolcastIrradianceCloudDatumStreamService extends BaseSolcastCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.solcast.irr";

	/** The maximum duration allowed for queries. */
	public static final Duration MAX_QUERY_DURATION = Duration.ofHours(168);

	/**
	 * The maximum offset from the current time allowed for "live" date range
	 * queries.
	 *
	 * @since 1.2
	 */
	public static final Duration MAX_LIVE_API_OFFSET = Duration.ofHours(36);

	/**
	 * The URL path for historic radiation and weather data.
	 *
	 * @since 1.2
	 */
	public static final String HISTORIC_RADIATION_URL_PATH = "/data/historic/radiation_and_weather";

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		// @formatter:off
		SETTINGS = List.of(
				new BasicToggleSettingSpecifier(DISALLOW_HISTORIC_API_SETTING, false),
				new BasicTextFieldSettingSpecifier(LATITUDE_SETTING, null),
				new BasicTextFieldSettingSpecifier(LONGITUDE_SETTING, null),
				new BasicTextFieldSettingSpecifier(AZIMUTH_SETTING, null),
				new BasicTextFieldSettingSpecifier(TILT_SETTING, null),
				ARRAY_TYPE_SETTTING_SPECIFIER,
				RESOLUTION_SETTING_SPECIFIER,
				VIRTUAL_SOURCE_IDS_SETTING_SPECIFIER
				);
		// @formatter:on
	}

	/** The service secure setting keys. */
	public static final Set<String> SECURE_SETTINGS = Collections.emptySet();

	/** The supported placeholder keys. */
	public static final List<String> SUPPORTED_PLACEHOLDERS = Collections.emptyList();

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
	 * @param clock
	 *        the clock to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SolcastIrradianceCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			Clock clock) {
		super(SERVICE_IDENTIFIER, "Solcast Irradiance Datum Stream Service", userEventAppenderBiz,
				encryptor, expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS, restOps,
				LoggerFactory.getLogger(SolcastIrradianceCloudDatumStreamService.class), clock);
	}

	@Override
	public Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale) {
		return Collections.emptyList();
	}

	@Override
	public Iterable<CloudDataValue> dataValues(UserLongCompositePK integrationId,
			Map<String, ?> filters) {
		return Arrays.stream(SolcastIrradianceType.values()).map(e -> dataValue(List.of(e.name()),
				e.getName(),
				Map.of(UNIT_OF_MEASURE_METADATA, e.getUnit(), DESCRIPTION_METADATA, e.getDescription())))
				.toList();
	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		requireNonNullArgument(datumStream, "datumStream");
		final Duration resolution = resolveResolution(datumStream);
		final Clock queryClock = Clock.tick(clock, resolution);
		final Instant endDate = queryClock.instant();
		final Instant startDate = endDate.minus(resolution);

		final var filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);
		filter.setParameters(USE_LIVE_DATA);

		final var result = datum(datumStream, filter);
		if ( result == null ) {
			return Collections.emptyList();
		}
		return result.getResults();
	}

	@Override
	public CloudDatumStreamQueryResult datum(CloudDatumStreamConfiguration datumStream,
			CloudDatumStreamQueryFilter filter) {
		requireNonNullArgument(datumStream, "datumStream");
		requireNonNullArgument(filter, "filter");
		return performAction(datumStream, (ms, ds, mapping, integration, valueProps, exprProps) -> {

			if ( valueProps.isEmpty() ) {
				String msg = "Datum stream has no properties.";
				Errors errors = new BindException(ds, "datumStream");
				errors.reject("error.datumStream.noProperties", null, msg);
				throw new ValidationException(msg, errors, ms);
			}

			final String latitude = requireNonNullArgument(
					ds.serviceProperty(LATITUDE_SETTING, String.class),
					"datumStream.serviceProperties.lat");
			final String longitude = requireNonNullArgument(
					ds.serviceProperty(LONGITUDE_SETTING, String.class),
					"datumStream.serviceProperties.lon");

			final Instant filterStartDate = requireNonNullArgument(filter.getStartDate(),
					"filter.startDate");
			final Instant filterEndDate = requireNonNullArgument(filter.getEndDate(),
					"filter.startDate");

			final Duration resolution = resolveResolution(ds);

			final Map<String, ValueRef> refsByFieldName = resolveReferences(ds, valueProps);

			BasicQueryFilter nextQueryFilter = null;

			Instant startDate = CloudIntegrationsUtils.truncateDate(filterStartDate, resolution, UTC);
			Instant endDate = CloudIntegrationsUtils.truncateDate(filterEndDate, resolution, UTC);
			if ( Duration.between(startDate, endDate).compareTo(MAX_QUERY_DURATION) > 0 ) {
				Instant nextEndDate = startDate.plus(MAX_QUERY_DURATION.multipliedBy(2));
				if ( nextEndDate.isAfter(endDate) ) {
					nextEndDate = endDate;
				}

				endDate = startDate.plus(MAX_QUERY_DURATION);

				nextQueryFilter = new BasicQueryFilter();
				nextQueryFilter.setStartDate(endDate);
				nextQueryFilter.setEndDate(nextEndDate);
			}

			final BasicQueryFilter usedQueryFilter = new BasicQueryFilter();
			usedQueryFilter.setStartDate(startDate);
			usedQueryFilter.setEndDate(endDate);

			// use the live API if requested or if query start near current date
			final boolean useLiveApi = filter.hasParameter(QUERY_PARAM_USE_LIVE_DATA)
					|| (ds.hasServiceProperty(DISALLOW_HISTORIC_API_SETTING, Boolean.class)
							&& ds.serviceProperty(DISALLOW_HISTORIC_API_SETTING, Boolean.class))
					|| Duration.between(startDate, clock.instant()).compareTo(MAX_LIVE_API_OFFSET) < 0;

			// @formatter:off
			final UriComponentsBuilder uriBuilder = UriComponentsBuilder
					.fromUri(resolveBaseUrl(integration, SolcastCloudIntegrationService.BASE_URI))
					.path(useLiveApi
							? SolcastCloudIntegrationService.LIVE_RADIATION_URL_PATH
							: HISTORIC_RADIATION_URL_PATH)
					.queryParam(SolcastCloudIntegrationService.LATITUDE_PARAM, latitude)
					.queryParam(SolcastCloudIntegrationService.LONGITUDE_PARAM, longitude)

					.queryParam(SolcastCloudIntegrationService.PERIOD_PARAM, resolution.toString())
					.queryParam(SolcastCloudIntegrationService.OUTPUT_PARAMETERS_PARAM,
							resolveOutputParametersValue(refsByFieldName.values()))
					;
			// @formatter:on

			if ( useLiveApi ) {
				uriBuilder.queryParam(SolcastCloudIntegrationService.HOURS_PARAM,
						resolveHours(startDate, endDate));
			} else {
				uriBuilder.queryParam(SolcastCloudIntegrationService.START_DATE_PARAM,
						startDate.toString());
				uriBuilder.queryParam(SolcastCloudIntegrationService.END_DATE_PARAM, endDate.toString());
			}

			String azimuth = nonEmptyString(ds.serviceProperty(AZIMUTH_SETTING, String.class));
			if ( azimuth != null ) {
				uriBuilder.queryParam(SolcastCloudIntegrationService.AZIMUTH_PARAM, azimuth);
			}
			String tilt = nonEmptyString(ds.serviceProperty(TILT_SETTING, String.class));
			if ( tilt != null ) {
				uriBuilder.queryParam(SolcastCloudIntegrationService.TILT_PARAM, tilt);
			}
			String arrayType = nonEmptyString(ds.serviceProperty(ARRAY_TYPE_SETTING, String.class));
			if ( tilt != null ) {
				uriBuilder.queryParam(SolcastCloudIntegrationService.ARRAY_TYPE_PARAM, arrayType);
			}

			final List<GeneralDatum> resultDatum = restOpsHelper.httpGet("List irradiance data",
					integration, JsonNode.class, req -> uriBuilder.buildAndExpand().toUri(),
					res -> parseDatum(res.getBody(), ds, refsByFieldName, resolution,
							usedQueryFilter.getStartDate(), usedQueryFilter.getEndDate()));

			// evaluate expressions on merged datum
			var r = evaluateExpressions(datumStream, exprProps, resultDatum, mapping.getConfigId(),
					integration.getConfigId());

			return new BasicCloudDatumStreamQueryResult(usedQueryFilter, nextQueryFilter,
					r.stream().sorted(Identity.sortByIdentity()).map(Datum.class::cast).toList());
		});
	}

	/**
	 * Value reference pattern, with component matching groups.
	 *
	 * <p>
	 * The matching groups are
	 * </p>
	 *
	 * <ol>
	 * <li>field</li>
	 * </ol>
	 */
	private static final Pattern VALUE_REF_PATTERN = Pattern.compile("/(.+)");

	private static record ValueRef(String fieldName, SolcastIrradianceType type,
			CloudDatumStreamPropertyConfiguration property) {

	}

	/**
	 * Resolve a mapping of field names to references.
	 *
	 * @param datumStream
	 *        the datum stream
	 * @param propConfigs
	 *        the property configurations
	 * @return the mapping
	 */
	private Map<String, ValueRef> resolveReferences(CloudDatumStreamConfiguration datumStream,
			List<CloudDatumStreamPropertyConfiguration> propConfigs) {
		final Map<String, ValueRef> refsByFieldName = new LinkedHashMap<>(propConfigs.size());
		for ( CloudDatumStreamPropertyConfiguration config : propConfigs ) {
			String ref = resolvePlaceholders(config.getValueReference(), datumStream);
			Matcher m = VALUE_REF_PATTERN.matcher(ref);
			if ( !m.matches() ) {
				continue;
			}
			// groups: 1 = field
			String fieldName = m.group(1);

			SolcastIrradianceType type;
			try {
				type = SolcastIrradianceType.fromValue(fieldName);
			} catch ( IllegalArgumentException e ) {
				continue;
			}

			ValueRef valueRef = new ValueRef(fieldName, type, config);
			refsByFieldName.put(fieldName, valueRef);
		}
		return refsByFieldName;
	}

	/**
	 * Resolve the query output_parameters value.
	 *
	 * @param refs
	 *        the references of the desired properties
	 * @return the query parameter value
	 */
	private String resolveOutputParametersValue(Collection<ValueRef> refs) {
		return refs.stream().map(e -> e.type.getKey()).collect(Collectors.joining(","));
	}

	/**
	 * Resolve the number of hours to query.
	 *
	 * @param from
	 *        the start date
	 * @param to
	 *        the end date
	 * @return the number of hours
	 */
	private int resolveHours(Instant from, Instant to) {
		Duration d = Duration.between(from, to);
		long mins = d.toMinutes();
		return ((int) (mins / 60) + (mins % 60 > 0 ? 1 : 0));
	}

	@SuppressWarnings("MixedMutabilityReturnType")
	private List<GeneralDatum> parseDatum(JsonNode json, CloudDatumStreamConfiguration datumStream,
			Map<String, ValueRef> refsByFieldName, Duration resolution, Instant minDate,
			Instant maxDate) {
		if ( json == null ) {
			return Collections.emptyList();
		}
		/*- EXAMPLE JSON:
		{
		  "estimated_actuals": [
		    {
		      "air_temp": 16,
		      "dni": 0,
		      "ghi": 0,
		      "period_end": "2024-10-29T18:30:00.0000000Z",
		      "period": "PT30M"
		    },
		  ]
		}
		*/
		List<GeneralDatum> result = new ArrayList<>(8);
		for ( JsonNode node : json.path("estimated_actuals") ) {
			String end = nonEmptyString(node.path("period_end").asText());
			if ( end == null ) {
				continue;
			}
			String per = nonEmptyString(node.path("period").asText());
			Duration d = resolution;
			if ( per != null ) {
				d = Duration.parse(per);
			}
			Instant ts = ISO_INSTANT.parse(end, Instant::from).minus(d);
			if ( ts.isBefore(minDate) || !ts.isBefore(maxDate) ) {
				continue;
			}
			DatumSamples samples = new DatumSamples();
			for ( ValueRef ref : refsByFieldName.values() ) {
				JsonNode fieldNode = node.path(ref.type.getKey());
				if ( fieldNode == null || fieldNode.isMissingNode() || fieldNode.isNull() ) {
					continue;
				}
				Object propVal = parseJsonDatumPropertyValue(fieldNode, ref.property.getPropertyType());
				propVal = ref.property.applyValueTransforms(propVal);
				if ( propVal != null ) {
					samples.putSampleValue(ref.property.getPropertyType(),
							ref.property.getPropertyName(), propVal);
				}
			}
			if ( !samples.isEmpty() ) {
				result.add(new GeneralDatum(new DatumId(datumStream.getKind(), datumStream.getObjectId(),
						datumStream.getSourceId(), ts), samples));
			}
		}

		// Solcast API returns data in reverse time order, so reverse it now
		result.sort(Identity.sortByIdentity());

		return result;
	}

}
