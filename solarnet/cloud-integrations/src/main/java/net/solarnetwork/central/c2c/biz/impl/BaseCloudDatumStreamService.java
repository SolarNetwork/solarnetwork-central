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

import static net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents.eventForConfiguration;
import static net.solarnetwork.util.NumberUtils.narrow;
import static net.solarnetwork.util.NumberUtils.parseNumber;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicCloudDatumStreamLocalizedServiceInfo;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.DatumSamplesExpressionRoot;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.MutableDatum;
import net.solarnetwork.service.IdentifiableConfiguration;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.StringUtils;

/**
 * Base implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.4
 */
public abstract class BaseCloudDatumStreamService extends BaseCloudIntegrationsIdentifiableService
		implements CloudDatumStreamService {

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
	 * @param settings
	 *        the service settings
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseCloudDatumStreamService(String serviceIdentifier, String displayName,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao,
			List<SettingSpecifier> settings) {
		super(serviceIdentifier, displayName, userEventAppenderBiz, encryptor, settings);
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
				getSettingSpecifiers(), requiresPolling(), supportedPlaceholders(),
				supportedDataValueWildcardIdentifierLevels(), dataValueIdentifierLevelsSourceIdRange());
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
	 * @param parameters
	 *        parameters to pass to the expressions
	 */
	public void evaulateExpressions(Collection<CloudDatumStreamPropertyConfiguration> configurations,
			Collection<? extends MutableDatum> datum, Map<String, ?> parameters) {
		if ( configurations == null || configurations.isEmpty() || datum == null || datum.isEmpty() ) {
			return;
		}
		for ( CloudDatumStreamPropertyConfiguration config : configurations ) {
			if ( !config.getValueType().isExpression() ) {
				continue;
			}
			var vars = Map.of("userId", (Object) config.getUserId(), "datumStreamMappingId",
					config.getDatumStreamMappingId());
			for ( MutableDatum d : datum ) {
				DatumSamplesExpressionRoot root = new DatumSamplesExpressionRoot(d,
						d.asSampleOperations(), parameters);
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
							eventForConfiguration(config.getId(), EXPRESSION_ERROR_TAGS,
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
								yield narrow(parseNumber(val.toString(), BigDecimal.class), 2);
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
	 * Populate a non-empty JSON field value onto a map.
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
			map.put(key, s);
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
		final Object sourceIdMap = configuration.serviceProperty(SOURCE_ID_MAP_SETTING, Object.class);
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
					yield narrow(parseNumber(val.asText(), BigDecimal.class), 2);
				}
			}
			case Status, Tag -> val.asText();
		};
	}

}
