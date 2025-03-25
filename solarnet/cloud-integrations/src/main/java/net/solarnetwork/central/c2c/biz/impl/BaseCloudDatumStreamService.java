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

import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents.eventForConfiguration;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.NumberUtils.narrow;
import static net.solarnetwork.util.NumberUtils.parseNumber;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import static org.springframework.util.StringUtils.delimitedListToStringArray;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import javax.cache.Cache;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicCloudDatumStreamLocalizedServiceInfo;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.support.BasicDatumStreamsAccessor;
import net.solarnetwork.central.datum.support.LazyDatumMetadataOperations;
import net.solarnetwork.central.datum.support.QueryingDatumStreamsAccessor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.support.HttpOperations;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesExpressionRoot;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.MutableDatum;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.service.IdentifiableConfiguration;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.ToggleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Base implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.14
 */
public abstract class BaseCloudDatumStreamService extends BaseCloudIntegrationsIdentifiableService
		implements CloudDatumStreamService {

	/**
	 * A setting specifier for the {@code UPPER_CASE_SOURCE_ID_SETTING}.
	 *
	 * @since 1.12
	 */
	public static final ToggleSettingSpecifier UPPER_CASE_SOURCE_ID_SETTING_SPECIFIER = new BasicToggleSettingSpecifier(
			UPPER_CASE_SOURCE_ID_SETTING, Boolean.FALSE);

	/** A clock to use. */
	protected final Clock clock;

	/** The integration configuration entity DAO. */
	protected final CloudIntegrationConfigurationDao integrationDao;

	/** The datum stream configuration DAO. */
	protected final CloudDatumStreamConfigurationDao datumStreamDao;

	/** The datum stream mapping configuration DAO. */
	protected final CloudDatumStreamMappingConfigurationDao datumStreamMappingDao;

	/** The datum stream property configuration DAO. */
	protected final CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao;

	/** The expression service. */
	protected final CloudIntegrationsExpressionService expressionService;

	private DatumEntityDao datumDao;
	private DatumStreamMetadataDao datumStreamMetadataDao;
	private QueryAuditor queryAuditor;
	private Cache<ObjectDatumStreamMetadataId, GeneralDatumMetadata> datumStreamMetadataCache;

	/**
	 * Constructor.
	 *
	 * @param serviceIdentifier
	 *        the service identifier
	 * @param displayName
	 *        the display name
	 * @param clock
	 *        the clock to use
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
	 * @param settings
	 *        the service settings
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseCloudDatumStreamService(String serviceIdentifier, String displayName, Clock clock,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao,
			List<SettingSpecifier> settings) {
		super(serviceIdentifier, displayName, userEventAppenderBiz, encryptor, settings);
		this.clock = requireNonNullArgument(clock, "clock");
		this.integrationDao = requireNonNullArgument(integrationDao, "integrationDao");
		this.expressionService = requireNonNullArgument(expressionService, "expressionService");
		this.datumStreamDao = requireNonNullArgument(datumStreamDao, "datumStreamDao");
		this.datumStreamMappingDao = requireNonNullArgument(datumStreamMappingDao,
				"datumStreamMappingDao");
		this.datumStreamPropertyDao = requireNonNullArgument(datumStreamPropertyDao,
				"datumStreamPropertyDao");
	}

	@Override
	public LocalizedServiceInfo getLocalizedServiceInfo(Locale locale) {
		return new BasicCloudDatumStreamLocalizedServiceInfo(
				super.getLocalizedServiceInfo(locale != null ? locale : Locale.getDefault()),
				getSettingSpecifiers(), requiresPolling(), dataValuesRequireDatumStream(),
				arbitraryDateRangesSupported(), supportedPlaceholders(),
				supportedDataValueWildcardIdentifierLevels(), dataValueIdentifierLevelsSourceIdRange());
	}

	/**
	 * API to perform an action on a full datum stream configuration model.
	 *
	 * @param <T>
	 *        the result type
	 */
	@FunctionalInterface
	public static interface IntegrationAction<T> {

		/**
		 * Handle a full datum stream integration configuration model.
		 *
		 * <p>
		 * All arguments will be non-null.
		 * </p>
		 *
		 * @param ms
		 *        the message source
		 * @param datumStream
		 *        the datum stream
		 * @param mapping
		 *        the mapping
		 * @param integration
		 *        the integration
		 * @param valueProps
		 *        the value properties
		 * @param exprProps
		 *        the expression properties
		 */
		T doWithDatumStreamIntegration(MessageSource ms, CloudDatumStreamConfiguration datumStream,
				CloudDatumStreamMappingConfiguration mapping, CloudIntegrationConfiguration integration,
				List<CloudDatumStreamPropertyConfiguration> valueProps,
				List<CloudDatumStreamPropertyConfiguration> exprProps);

	}

	/**
	 * Fetch the entities related to a datum stream and perform an action with
	 * them.
	 *
	 * <p>
	 * This is a convenient method to invoke in implementations of
	 * {@link CloudDatumStreamService#latestDatum(CloudDatumStreamConfiguration)}
	 * and similar methods.
	 * </p>
	 *
	 * @param <T>
	 *        the result type
	 * @param datumStream
	 *        the datum stream to fetch the related entities of
	 * @param handler
	 *        callback to handle the entities in some way
	 * @since 1.6
	 */
	protected <T> T performAction(CloudDatumStreamConfiguration datumStream,
			IntegrationAction<T> handler) {
		assert datumStream != null && handler != null;
		final MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");

		if ( !datumStream.isFullyConfigured() ) {
			String msg = "Datum stream is not fully configured.";
			Errors errors = new BindException(datumStream, "datumStream");
			errors.reject("error.datumStream.notFullyConfigured", null, msg);
			throw new ValidationException(msg, errors, ms);
		}

		final var mappingId = new UserLongCompositePK(datumStream.getUserId(), requireNonNullArgument(
				datumStream.getDatumStreamMappingId(), "datumStream.datumStreamMappingId"));
		final CloudDatumStreamMappingConfiguration mapping = requireNonNullObject(
				datumStreamMappingDao.get(mappingId), "datumStreamMapping");

		final var integrationId = new UserLongCompositePK(datumStream.getUserId(),
				requireNonNullArgument(mapping.getIntegrationId(), "datumStreamMapping.integrationId"));
		final CloudIntegrationConfiguration integration = requireNonNullObject(
				integrationDao.get(integrationId), "integration");

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

		return handler.doWithDatumStreamIntegration(ms, datumStream, mapping, integration, valueProps,
				exprProps);
	}

	/**
	 * Get the polling requirement.
	 *
	 * @return {@literal true} if polling for data is required
	 * @since 1.3
	 */
	protected boolean requiresPolling() {
		return true;
	}

	/**
	 * Get the data values datum stream requirement.
	 *
	 * @return {@literal true} if a datum stream is required to work with data
	 *         values
	 * @since 1.5
	 */
	protected boolean dataValuesRequireDatumStream() {
		return false;
	}

	/**
	 * Tell if the service supports returning datum for arbitrary date ranges.
	 *
	 * @return {@code true} if this service supports querying datum with
	 *         arbitrary date ranges, {@code false} if date ranges are not
	 *         supported or limited in some way
	 * @since 1.7
	 */
	protected boolean arbitraryDateRangesSupported() {
		return true;
	}

	/**
	 * Get the supported placeholder keys.
	 *
	 * @return the supported placeholder key, or {@literal null}
	 * @since 1.3
	 */
	protected Iterable<String> supportedPlaceholders() {
		return null;
	}

	/**
	 * Get the supported data value wildcard levels.
	 *
	 * @return the supported data value wildcard levels, or {@literal null}
	 * @since 1.3
	 */
	protected Iterable<Integer> supportedDataValueWildcardIdentifierLevels() {
		return null;
	}

	/**
	 * Get the supported data value identifier levels source ID range.
	 *
	 * @return the supported data value identifier levels source ID range, or
	 *         {@literal null}
	 * @since 1.4
	 */
	protected IntRange dataValueIdentifierLevelsSourceIdRange() {
		return null;
	}

	/**
	 * Evaluate a set of property expressions on a set of datum.
	 *
	 * @param configurations
	 *        the property configurations
	 * @param datum
	 *        the datum to evaluate expressions on
	 * @param mappingId
	 *        the {@link CloudDatumStreamMappingConfiguration} ID to provide as
	 *        a {@code datumStreamMappingId} parameter
	 * @param integrationId
	 *        the {@link CloudIntegrationConfiguration} ID to provide as a
	 *        {@code integrationId} parameter
	 * @since 1.6
	 */
	public void evaluateExpressions(Collection<CloudDatumStreamPropertyConfiguration> configurations,
			Collection<? extends MutableDatum> datum, Long mappingId, Long integrationId) {
		assert mappingId != null && integrationId != null;
		if ( datum != null && !datum.isEmpty() && configurations != null && !configurations.isEmpty() ) {
			evaluateExpressions(configurations, datum, mappingId, integrationId, null);
		}
	}

	/**
	 * Evaluate a set of property expressions on a set of datum.
	 *
	 * @param configurations
	 *        the property configurations
	 * @param datum
	 *        the datum to evaluate expressions on
	 * @param mappingId
	 *        the {@link CloudDatumStreamMappingConfiguration} ID to provide as
	 *        a {@code datumStreamMappingId} parameter
	 * @param integrationId
	 *        the {@link CloudIntegrationConfiguration} ID to provide as a
	 *        {@code integrationId} parameter
	 * @param parameters
	 *        optional parameters to pass to the expressions
	 */
	public void evaluateExpressions(Collection<CloudDatumStreamPropertyConfiguration> configurations,
			Collection<? extends MutableDatum> datum, Long mappingId, Long integrationId,
			Map<String, ?> parameters) {
		if ( configurations == null || configurations.isEmpty() || datum == null || datum.isEmpty() ) {
			return;
		}

		final Map<String, ?> params;
		if ( parameters == null ) {
			params = Map.of("datumStreamMappingId", mappingId, "integrationId", integrationId);
		} else {
			var tmp = new LinkedHashMap<String, Object>(parameters);
			tmp.put("datumStreamMappingId", mappingId);
			tmp.put("integrationId", integrationId);
			params = tmp;
		}

		// assume all configurations owned by the same user; extract the user ID from the first one
		final Long userId = configurations.iterator().next().getUserId();

		final var datumStreamsAccessor = (datumDao != null
				? new QueryingDatumStreamsAccessor(expressionService.sourceIdPathMatcher(), datum,
						userId, clock, datumDao, queryAuditor)
				: new BasicDatumStreamsAccessor(expressionService.sourceIdPathMatcher(), datum));
		for ( CloudDatumStreamPropertyConfiguration config : configurations ) {
			if ( !config.getValueType().isExpression() ) {
				continue;
			}
			var vars = Map.of("userId", (Object) config.getUserId(), "datumStreamMappingId",
					config.getDatumStreamMappingId());
			for ( MutableDatum d : datum ) {
				DatumMetadataOperations metaOps = null;
				if ( datumStreamMetadataDao != null ) {
					metaOps = new LazyDatumMetadataOperations(
							new ObjectDatumStreamMetadataId(d.getKind(), d.getObjectId(),
									d.getSourceId()),
							datumStreamMetadataDao, datumStreamMetadataCache);
				}
				DatumSamplesExpressionRoot root = expressionService.createDatumExpressionRoot(userId,
						integrationId, d, params, metaOps, datumStreamsAccessor,
						this instanceof HttpOperations httpOps ? httpOps : null);
				Object val = null;
				try {
					val = expressionService.evaluateDatumPropertyExpression(config, root, vars,
							Object.class);
				} catch ( Exception e ) {
					Throwable t = e;
					while ( t.getCause() != null ) {
						t = t.getCause();
					}
					String exMsg = (t.getMessage() != null ? t.getMessage()
							: t.getClass().getSimpleName());
					userEventAppenderBiz.addEvent(config.getUserId(),
							eventForConfiguration(config.getId(), DATUM_STREAM_EXPRESSION_ERROR_TAGS,
									"Error evaluating datum stream property expression.",
									Map.of(MESSAGE_DATA_KEY, exMsg, SOURCE_DATA_KEY,
											config.getValueReference())));
				}
				if ( val != null ) {
					Object propVal = switch (config.getPropertyType()) {
						case Accumulating, Instantaneous -> {
							// convert to number
							if ( val instanceof Number ) {
								yield val;
							} else {
								try {
									yield narrow(parseNumber(val.toString(), BigDecimal.class), 2);
								} catch ( IllegalArgumentException e ) {
									yield null;
								}
							}
						}
						case Status, Tag -> val.toString();
					};
					propVal = config.applyValueTransforms(propVal);
					d.asMutableSampleOperations().putSampleValue(config.getPropertyType(),
							config.getPropertyName(), propVal);
				}
			}
		}
	}

	/**
	 * Populate a non-empty JSON field value onto a map as a string.
	 *
	 * @param node
	 *        the JSON node to read the field from
	 * @param fieldName
	 *        the name of the JSON field to read
	 * @param key
	 *        the map key to populate if the field is a non-empty string
	 * @param map
	 *        the map to populate with the non-empty string
	 */
	public static void populateNonEmptyValue(JsonNode node, String fieldName, String key,
			Map<String, Object> map) {
		String s = JsonUtils.parseNonEmptyStringAttribute(node, fieldName);
		if ( s != null ) {
			map.put(key, s.trim());
		}
	}

	/**
	 * Populate a non-empty JSON field value onto a map as a boolean.
	 *
	 * @param node
	 *        the JSON node to read the field from
	 * @param fieldName
	 *        the name of the JSON field to read
	 * @param key
	 *        the map key to populate if the field is present
	 * @param map
	 *        the map to populate with the boolean value
	 * @since 1.14
	 */
	public static void populateBooleanValue(JsonNode node, String fieldName, String key,
			Map<String, Object> map) {
		JsonNode field = node.path(fieldName);
		if ( field.isMissingNode() ) {
			return;
		}
		map.put(key, field.asBoolean());
	}

	/**
	 * Populate a non-empty JSON field value onto a map as an {@code Instant}.
	 *
	 * <p>
	 * If the JSON field value is a number, it will be treated as a millisecond
	 * Unix epoch value and {@code parser} will not be invoked.
	 * </p>
	 *
	 * @param node
	 *        the JSON node to read the field from
	 * @param fieldName
	 *        the name of the JSON field to read
	 * @param key
	 *        the map key to populate if the field is present
	 * @param map
	 *        the map to populate with the {@link Instant} value
	 * @param parser
	 *        the function to use for parsing the timestamp value into an
	 *        {@link Instant}
	 * @since 1.14
	 */
	public static void populateTimestampValue(JsonNode node, String fieldName, String key,
			Map<String, Object> map, Function<String, Instant> parser) {
		JsonNode field = node.path(fieldName);
		if ( field.isMissingNode() ) {
			return;
		}
		if ( field.isNumber() ) {
			// treat as epoch
			map.put(key, Instant.ofEpochMilli(field.asLong()));
		}
		try {
			map.put(key, parser.apply(field.asText()));
		} catch ( DateTimeParseException e ) {
			// ignore
		}
	}

	/**
	 * Populate a non-empty JSON field value onto a map as an ISO 8601 timestamp
	 * formatted {@code Instant}.
	 *
	 * @param node
	 *        the JSON node to read the field from
	 * @param fieldName
	 *        the name of the JSON field to read
	 * @param key
	 *        the map key to populate if the field is present
	 * @param map
	 *        the map to populate with the {@link Instant} value
	 * @since 1.14
	 */
	public static void populateIsoTimestampValue(JsonNode node, String fieldName, String key,
			Map<String, Object> map) {
		populateTimestampValue(node, fieldName, key, map, Instant::parse);
	}

	/**
	 * Populate a number JSON field value onto a map.
	 *
	 * @param node
	 *        the JSON node to read the field from
	 * @param fieldName
	 *        the name of the JSON field to read
	 * @param key
	 *        the map key to populate if the field is a number
	 * @param map
	 *        the map to populate with the number
	 * @since 1.5
	 */
	public static void populateNumberValue(JsonNode node, String fieldName, String key,
			Map<String, Object> map) {
		JsonNode field = node.path(fieldName);
		Number n = null;
		if ( field.isNumber() ) {
			n = field.numberValue();
		} else if ( field.isTextual() ) {
			n = StringUtils.numberValue(field.asText());
		}
		if ( n != null ) {
			map.put(key, NumberUtils.narrow(n, 2));
		}
	}

	/**
	 * Resolve a mapping from a setting on a configuration.
	 *
	 * @param configuration
	 *        the configuration to extract the mapping from
	 * @param key
	 *        the service property key to extract
	 * @return the mapping, or {@literal null}
	 * @since 1.4
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> servicePropertyStringMap(IdentifiableConfiguration configuration,
			String key) {
		if ( configuration == null ) {
			return null;
		}
		final Object sourceIdMap = configuration.serviceProperty(key, Object.class);
		final Map<String, String> componentSourceIdMapping;
		if ( sourceIdMap instanceof Map<?, ?> ) {
			componentSourceIdMapping = (Map<String, String>) sourceIdMap;
		} else if ( sourceIdMap != null ) {
			componentSourceIdMapping = StringUtils.commaDelimitedStringToMap(sourceIdMap.toString());
		} else {
			componentSourceIdMapping = null;
		}
		return componentSourceIdMapping;
	}

	/**
	 * Parse a JSON datum property value.
	 *
	 * @param val
	 *        the JSON value to parse as a datum property value.
	 * @param propType
	 *        the desired datum property type
	 * @return the value, or {@literal null}
	 */
	public static Object parseJsonDatumPropertyValue(JsonNode val, DatumSamplesType propType) {
		if ( val.isMissingNode() || val.isNull() ) {
			return null;
		}
		return switch (propType) {
			case Accumulating, Instantaneous -> {
				// convert to number
				if ( val.isBigDecimal() ) {
					yield val.decimalValue();
				} else if ( val.isFloat() ) {
					yield val.floatValue();
				} else if ( val.isDouble() ) {
					yield val.doubleValue();
				} else if ( val.isBigInteger() ) {
					yield val.bigIntegerValue();
				} else if ( val.isLong() ) {
					yield val.longValue();
				} else if ( val.isFloat() ) {
					yield val.floatValue();
				} else {
					try {
						yield narrow(parseNumber(val.asText(), BigDecimal.class), 2);
					} catch ( IllegalArgumentException e ) {
						yield null;
					}
				}
			}
			case Status, Tag -> nonEmptyString(val.asText());
		};
	}

	/**
	 * Populate a samples property value.
	 *
	 * @param json
	 *        the JSON object
	 * @param key
	 *        the JSON field name
	 * @param propType
	 *        the datum property type
	 * @param propName
	 *        the datum property name
	 * @param samples
	 *        the samples to populate
	 * @param xforms
	 *        optional transformations to apply
	 */
	@SuppressWarnings("unchecked")
	public static void populateJsonDatumPropertyValue(JsonNode json, String key,
			DatumSamplesType propType, String propName, DatumSamples samples, Function<?, ?>... xforms) {
		if ( json == null ) {
			return;
		}
		Object val = parseJsonDatumPropertyValue(json.path(key), propType);
		if ( val == null ) {
			return;
		}
		if ( xforms != null ) {
			for ( @SuppressWarnings("rawtypes")
			Function xform : xforms ) {
				val = xform.apply(val);
				if ( val == null ) {
					return;
				}
			}
		}
		samples.putSampleValue(propType, propName, val);
	}

	/**
	 * Resolve a list of placeholder sets for a collection of source value
	 * references.
	 *
	 * <p>
	 * This method works with the {@link #supportedPlaceholders()} list of
	 * placeholder keys, and decodes value references as used in the
	 * {@code sourceIdMap} service property into a set of placeholder maps. For
	 * example, imagine {@link #supportedPlaceholders()} returned:
	 * </p>
	 *
	 * <pre>{@code ["siteId", "componentId"]}</pre>
	 *
	 * <p>
	 * and that a configuration entity had a {@code sourceIdMap} service
	 * property like this:
	 * </p>
	 *
	 * <pre>{@code {
	 *   "sourceIdMap": {
	 *     "/123/abc" : "/GEN/1",
	 *     "/123/def" : "/INV/1"
	 *   }
	 * }}</pre>
	 *
	 * <p>
	 * Here the {@code sourceIdMap} keys are partial value references in the
	 * form
	 * </p>
	 *
	 * <pre>{@code /{siteId}/{componentId}}</pre>
	 *
	 * <p>
	 * Assuming the {@code placeholders} argument is {@code null} or empty, and
	 * the keys of {@code sourceIdMap} are passed on the {@code sourceValueRefs}
	 * argument, it would return a list of maps like this:
	 * </p>
	 *
	 * <pre>{@code [
	 *   {"siteId": "123", "componentId": "abc"},
	 *   {"siteId": "123", "componentId": "def"}
	 * ]}</pre>
	 *
	 * @param placeholders
	 *        an optional map of static placeholder values
	 * @param sourceValueRefs
	 *        an optional collection of partial value references, which are like
	 *        URL paths starting with {@code /}, of placeholder values, whose
	 *        segment offsets equate to the placeholder keys returned by
	 *        {@link #supportedPlaceholders()}
	 * @return a list of placeholder sets, never {@code null} but possibly
	 *         holding a single empty map if no placeholders are provided
	 * @since 1.9
	 */
	protected List<Map<String, ?>> resolvePlaceholderSets(Map<String, ?> placeholders,
			Collection<String> sourceValueRefs) {
		final Iterable<String> supportedPlaceholdersIterable = supportedPlaceholders();
		final List<String> supportedPlaceholders = (supportedPlaceholdersIterable instanceof List<String> l
				? l
				: supportedPlaceholdersIterable != null
						? stream(supportedPlaceholdersIterable.spliterator(), false).toList()
						: null);
		List<Map<String, ?>> placeholderSets;
		if ( sourceValueRefs != null && !sourceValueRefs.isEmpty() && supportedPlaceholders != null
				&& !supportedPlaceholders.isEmpty() ) {
			// sourceIdMap provided: generate set of placeholders
			placeholderSets = new ArrayList<>(sourceValueRefs.size());
			for ( String sourceValueRef : sourceValueRefs ) {
				Map<String, Object> ph = new LinkedHashMap<>(4);
				if ( placeholders != null ) {
					ph.putAll(placeholders);
				}
				if ( sourceValueRef.startsWith("/") ) {
					String[] components = delimitedListToStringArray(sourceValueRef.substring(1), "/");
					for ( int i = 0, len = supportedPlaceholders.size(); i < len
							&& i < components.length; i++ ) {
						if ( components[i] != null && !components[i].isEmpty() ) {
							ph.put(supportedPlaceholders.get(i), components[i]);
						}
					}
				}
				placeholderSets.add(ph);
			}
		} else if ( placeholders != null && !placeholders.isEmpty() ) {
			// no sourceIdMap: provide a single static set of given placeholders
			placeholderSets = Collections.singletonList(placeholders);
		} else {
			// no placeholders: provide a single static (empty) set
			placeholderSets = Collections.singletonList(Collections.emptyMap());
		}
		return placeholderSets;
	}

	/**
	 * Get the datum DAO.
	 *
	 * @return the datum DAO
	 * @since 1.11
	 */
	public final DatumEntityDao getDatumDao() {
		return datumDao;
	}

	/**
	 * Set the datum DAO.
	 *
	 * <p>
	 * If configured, then {@link QueryingDatumStreamsAccessor} will be used.
	 * Otherwise {@link BasicDatumStreamsAccessor} will be.
	 * </p>
	 *
	 * @param datumDao
	 *        the datum DAO to set
	 * @since 1.11
	 */
	public final void setDatumDao(DatumEntityDao datumDao) {
		this.datumDao = datumDao;
	}

	/**
	 * Get the datum stream metadata DAO.
	 *
	 * @return the DAO
	 * @since 1.13
	 */
	public final DatumStreamMetadataDao getDatumStreamMetadataDao() {
		return datumStreamMetadataDao;
	}

	/**
	 * Set the datum stream metadata DAO.
	 *
	 * @param datumStreamMetadataDao
	 *        the DAO to set
	 * @since 1.13
	 */
	public final void setDatumStreamMetadataDao(DatumStreamMetadataDao datumStreamMetadataDao) {
		this.datumStreamMetadataDao = datumStreamMetadataDao;
	}

	/**
	 * Get the query auditor.
	 *
	 * @return the auditor
	 * @since 1.11
	 */
	public final QueryAuditor getQueryAuditor() {
		return queryAuditor;
	}

	/**
	 * Set the query auditor.
	 *
	 * @param queryAuditor
	 *        the auditor to set
	 * @since 1.11
	 */
	public final void setQueryAuditor(QueryAuditor queryAuditor) {
		this.queryAuditor = queryAuditor;
	}

	/**
	 * Get the datum stream metadata cache.
	 *
	 * @return the cache
	 * @since 1.13
	 */
	public final Cache<ObjectDatumStreamMetadataId, GeneralDatumMetadata> getDatumStreamMetadataCache() {
		return datumStreamMetadataCache;
	}

	/**
	 * Set the datum stream metadata cache.
	 *
	 * @param datumStreamMetadataCache
	 *        the cache to use
	 * @since 1.13
	 */
	public final void setDatumStreamMetadataCache(
			Cache<ObjectDatumStreamMetadataId, GeneralDatumMetadata> datumStreamMetadataCache) {
		this.datumStreamMetadataCache = datumStreamMetadataCache;
	}

}
