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
import java.util.Map;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.domain.datum.DatumSamplesExpressionRoot;
import net.solarnetwork.domain.datum.MutableDatum;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Base implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.1
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

	/**
	 * Evaluate a set of property expressions.
	 *
	 * @param configurations
	 *        the property configurations
	 * @param datum
	 * @param parameters
	 */
	public void evaulateExpressions(Collection<CloudDatumStreamPropertyConfiguration> configurations,
			MutableDatum datum, Map<String, ?> parameters) {
		if ( configurations == null || configurations.isEmpty() || datum == null ) {
			return;
		}
		DatumSamplesExpressionRoot root = new DatumSamplesExpressionRoot(datum,
				datum.asSampleOperations(), parameters);
		for ( CloudDatumStreamPropertyConfiguration config : configurations ) {
			if ( !config.getValueType().isExpression() ) {
				continue;
			}
			var vars = Map.of("userId", (Object) config.getUserId(), "datumStreamMappingId",
					config.getDatumStreamMappingId());
			Object val = null;
			try {
				val = expressionService.evaluateDatumPropertyExpression(config, root, vars,
						Object.class);
			} catch ( Exception e ) {
				Throwable t = e;
				while ( t.getCause() != null ) {
					t = t.getCause();
				}
				String exMsg = (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
				userEventAppenderBiz.addEvent(config.getUserId(), eventForConfiguration(config.getId(),
						EXPRESSION_ERROR_TAGS, "Error evaluating datum stream property expression.",
						Map.of(MESSAGE_DATA_KEY, exMsg, SOURCE_DATA_KEY, config.getValueReference())));
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
				datum.asMutableSampleOperations().putSampleValue(config.getPropertyType(),
						config.getPropertyName(), propVal);
			}
		}
	}

}
