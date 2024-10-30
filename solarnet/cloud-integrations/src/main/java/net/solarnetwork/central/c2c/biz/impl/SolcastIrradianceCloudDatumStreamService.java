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

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DESCRIPTION_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.UNIT_OF_MEASURE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity.resolvePlaceholders;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
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
import org.springframework.context.MessageSource;
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
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Solcast implementation of {@link CloudDatumStreamService} using the
 * irradiance API.
 *
 * @author matt
 * @version 1.0
 */
public class SolcastIrradianceCloudDatumStreamService extends BaseSolcastCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.solcast.irr";

	/** The maximum duration allowed for queries. */
	public static final Duration MAX_QUERY_DURATION = Duration.ofHours(168);

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		// @formatter:off
		SETTINGS = List.of(
				new BasicTextFieldSettingSpecifier(LATITUDE_SETTING, null),
				new BasicTextFieldSettingSpecifier(LONGITUDE_SETTING, null),
				new BasicTextFieldSettingSpecifier(AZIMUTH_SETTING, null),
				new BasicTextFieldSettingSpecifier(TILT_SETTING, null),
				new BasicTextFieldSettingSpecifier(RESOLUTION_SETTING, null),
				ARRAY_TYPE_SETTTING_SPECIFIER,
				RESOLUTION_SETTING_SPECIFIER
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
		return Arrays.stream(SolcastIrradianceType.values()).map(e -> {
			return dataValue(List.of(e.name()), e.getName(), Map.of(UNIT_OF_MEASURE_METADATA,
					e.getUnit(), DESCRIPTION_METADATA, e.getDescription()));
		}).toList();
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

		final var result = datum(datumStream, filter);
		if ( result == null || result.isEmpty() ) {
			return null;
		}
		return result.getResults();
	}

	@Override
	public CloudDatumStreamQueryResult datum(CloudDatumStreamConfiguration datumStream,
			CloudDatumStreamQueryFilter filter) {
		requireNonNullArgument(datumStream, "datumStream");
		requireNonNullArgument(filter, "filter");

		final MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");

		if ( !datumStream.isFullyConfigured() ) {
			String msg = "Datum stream is not fully configured.";
			Errors errors = new BindException(datumStream, "datumStream");
			errors.reject("error.datumStream.notFullyConfigured", null, msg);
			throw new ValidationException(msg, errors, ms);
		}

		final String latitude = requireNonNullArgument(
				datumStream.serviceProperty(LATITUDE_SETTING, String.class),
				"datumStream.serviceProperties.lat");
		final String longitude = requireNonNullArgument(
				datumStream.serviceProperty(LONGITUDE_SETTING, String.class),
				"datumStream.serviceProperties.lon");

		final var mappingId = new UserLongCompositePK(datumStream.getUserId(), requireNonNullArgument(
				datumStream.getDatumStreamMappingId(), "datumStream.datumStreamMappingId"));
		final CloudDatumStreamMappingConfiguration mapping = requireNonNullObject(
				datumStreamMappingDao.get(mappingId), "datumStreamMapping");

		final var integrationId = new UserLongCompositePK(datumStream.getUserId(),
				requireNonNullArgument(mapping.getIntegrationId(), "datumStreamMapping.integrationId"));
		final CloudIntegrationConfiguration integration = requireNonNullObject(
				integrationDao.get(integrationId), "integration");

		final Instant filterStartDate = requireNonNullArgument(filter.getStartDate(),
				"filter.startDate");
		final Instant filterEndDate = requireNonNullArgument(filter.getEndDate(), "filter.startDate");

		final var allProperties = datumStreamPropertyDao.findAll(datumStream.getUserId(),
				mapping.getConfigId(), null);
		final var valueProps = new ArrayList<CloudDatumStreamPropertyConfiguration>(
				allProperties.size());
		final var exprProps = new ArrayList<CloudDatumStreamPropertyConfiguration>(allProperties.size());
		for ( CloudDatumStreamPropertyConfiguration conf : allProperties ) {
			if ( !(conf.isEnabled() && conf.isFullyConfigured()) ) {
				continue;
			}
			if ( conf.getValueType().isExpression() ) {
				exprProps.add(conf);
			} else {
				valueProps.add(conf);
			}
		}
		if ( valueProps.isEmpty() ) {
			String msg = "Datum stream has no properties.";
			Errors errors = new BindException(datumStream, "datumStream");
			errors.reject("error.datumStream.noProperties", null, msg);
			throw new ValidationException(msg, errors, ms);
		}

		final Duration resolution = resolveResolution(datumStream);

		final Map<String, ValueRef> refsByFieldName = resolveReferences(datumStream, valueProps);

		BasicQueryFilter nextQueryFilter = null;

		Instant startDate = CloudIntegrationsUtils.truncateDate(filterStartDate, resolution);
		Instant endDate = CloudIntegrationsUtils.truncateDate(filterEndDate, resolution);
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

		// @formatter:off
		final UriComponentsBuilder uriBuidler = UriComponentsBuilder
				.fromUri(resolveBaseUrl(integration, SolcastCloudIntegrationService.BASE_URI))
				.path(SolcastCloudIntegrationService.LIVE_RADIATION_URL_PATH)
				.queryParam(SolcastCloudIntegrationService.LATITUDE_PARAM, latitude)
				.queryParam(SolcastCloudIntegrationService.LONGITUDE_PARAM, longitude)
				.queryParam(SolcastCloudIntegrationService.HOURS_PARAM,
						resolveHours(startDate, endDate))
				.queryParam(SolcastCloudIntegrationService.PERIOD_PARAM, resolution.toString())
				.queryParam(SolcastCloudIntegrationService.OUTPUT_PARAMETERS_PARAM,
						resolveOutputParametersValue(refsByFieldName.values()))
				;
		// @formatter:on
		String azimuth = nonEmptyString(datumStream.serviceProperty(AZIMUTH_SETTING, String.class));
		if ( azimuth != null ) {
			uriBuidler.queryParam(SolcastCloudIntegrationService.AZIMUTH_PARAM, azimuth);
		}
		String tilt = nonEmptyString(datumStream.serviceProperty(TILT_SETTING, String.class));
		if ( tilt != null ) {
			uriBuidler.queryParam(SolcastCloudIntegrationService.TILT_PARAM, tilt);
		}
		String arrayType = nonEmptyString(datumStream.serviceProperty(ARRAY_TYPE_SETTING, String.class));
		if ( tilt != null ) {
			uriBuidler.queryParam(SolcastCloudIntegrationService.ARRAY_TYPE_PARAM, arrayType);
		}

		final List<GeneralDatum> resultDatum = restOpsHelper.httpGet("List irradiance data", integration,
				JsonNode.class, req -> uriBuidler.buildAndExpand().toUri(),
				res -> parseDatum(res.getBody(), datumStream, valueProps, refsByFieldName, resolution,
						usedQueryFilter.getStartDate(), usedQueryFilter.getEndDate()));

		// evaluate expressions on merged datum
		if ( !exprProps.isEmpty() ) {
			var parameters = Map.of("datumStreamMappingId", datumStream.getDatumStreamMappingId(),
					"integrationId", mapping.getIntegrationId());
			evaulateExpressions(exprProps, resultDatum, parameters);
		}

		return new BasicCloudDatumStreamQueryResult(usedQueryFilter, nextQueryFilter,
				resultDatum.stream().sorted(Identity.sortByIdentity()).map(Datum.class::cast).toList());
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
			// groups: 1 = siteId, 2 = deviceType, 3 = componentId, 4 = field
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
	 *        the from date
	 * @param to
	 *        the to date
	 * @return the number of hours
	 */
	private int resolveHours(Instant from, Instant to) {
		Duration d = Duration.between(from, to);
		long mins = d.toMinutes();
		return ((int) (mins / 60) + (mins % 60 > 0 ? 1 : 0));
	}

	private List<GeneralDatum> parseDatum(JsonNode json, CloudDatumStreamConfiguration datumStream,
			List<CloudDatumStreamPropertyConfiguration> propConfigs,
			Map<String, ValueRef> refsByFieldName, Duration resolution, Instant minDate,
			Instant maxDate) {
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
		return result;
	}

}
