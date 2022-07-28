/* ==================================================================
 * DatumPublisherSupport.java - 29/07/2022 9:22:55 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.v16.controller;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.expandTemplateString;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.security.AuthorizationException;
import net.solarnetwork.security.AuthorizationException.Reason;

/**
 * Helper class for publishing OCPP entities to a datum stream.
 * 
 * @author matt
 * @version 1.0
 */
public final class DatumPublisherSupport {

	/** The {@code sourceIdSuffix} property default value. */
	public static final String DEFAULT_SOURCE_ID_SUFFIX = "/status";

	private final CentralChargePointDao chargePointDao;
	private final ChargePointSettingsDao chargePointSettingsDao;
	private final CentralChargePointConnectorDao chargePointConnectorDao;
	private final DatumEntityDao datumDao;
	private DatumProcessor fluxPublisher;
	private String sourceIdTemplate = UserSettings.DEFAULT_SOURCE_ID_TEMPLATE;
	private String sourceIdSuffix = DEFAULT_SOURCE_ID_SUFFIX;

	/**
	 * Constructor.
	 * 
	 * @param chargePointDao
	 *        the charge point DAO to use
	 * @param chargePointSettingsDao
	 *        the settings DAO to use
	 * @param chargePointConnectorDao
	 *        the connector DAO to use
	 * @param datumDao
	 *        the datum DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DatumPublisherSupport(CentralChargePointDao chargePointDao,
			ChargePointSettingsDao chargePointSettingsDao,
			CentralChargePointConnectorDao chargePointConnectorDao, DatumEntityDao datumDao) {
		super();
		this.chargePointDao = requireNonNullArgument(chargePointDao, "chargePointDao");
		this.chargePointSettingsDao = requireNonNullArgument(chargePointSettingsDao,
				"chargePointSettingsDao");
		this.chargePointConnectorDao = requireNonNullArgument(chargePointConnectorDao,
				"chargePointConnectorDao");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
	}

	/**
	 * Get a charge point.
	 * 
	 * @param identity
	 *        the identity of the charge point to get
	 * @return the charge point
	 * @throws AuthorizationException
	 *         if the charge point is not found
	 */
	public CentralChargePoint chargePoint(ChargePointIdentity identity) {
		final CentralChargePoint chargePoint = (CentralChargePoint) chargePointDao
				.getForIdentity(identity);
		if ( chargePoint == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, identity);
		}
		return chargePoint;
	}

	/**
	 * Get the settings for a charge point.
	 * 
	 * @param userId
	 *        the ID of the user to get the settings for
	 * @param id
	 *        the ID of the charge point to get the settings for
	 * @return the settings, never {@literal null}
	 */
	public ChargePointSettings settingsForChargePoint(Long userId, Long id) {
		ChargePointSettings cps = chargePointSettingsDao.resolveSettings(userId, id);
		if ( cps == null ) {
			// use default fallback
			cps = new ChargePointSettings(id, userId, Instant.now());
			cps.setSourceIdTemplate(sourceIdTemplate);
		}
		return cps;
	}

	/**
	 * Resolve a source ID from charge point settings.
	 * 
	 * @param chargePointSettings
	 *        the settings
	 * @param identifier
	 *        the charger identifier
	 * @param connectorId
	 *        the connector ID
	 * @return the source ID, never {@literal null}
	 */
	public String sourceId(ChargePointSettings chargePointSettings, String identifier,
			Integer connectorId) {
		Map<String, Object> params = new HashMap<>(4);
		params.put("chargerIdentifier", identifier);
		params.put("chargePointId", chargePointSettings.getId());
		params.put("connectorId", connectorId);
		String template = chargePointSettings.getSourceIdTemplate() != null
				? chargePointSettings.getSourceIdTemplate()
				: sourceIdTemplate;
		String suffix = getSourceIdSuffix();
		if ( suffix != null ) {
			template = template + suffix;
		}
		return UserSettings.removeEmptySourceIdSegments(expandTemplateString(template, params));
	}

	/**
	 * Publish a datum based on charge point settings.
	 * 
	 * @param settings
	 *        the charge point settings
	 * @param datum
	 *        the datum the publish
	 */
	public void publishDatum(ChargePointSettings settings, GeneralNodeDatum datum) {
		if ( settings.isPublishToSolarIn() ) {
			datumDao.store(datum);
		}
		if ( settings.isPublishToSolarFlux() ) {
			final DatumProcessor publisher = getFluxPublisher();
			if ( publisher != null && publisher.isConfigured() ) {
				publisher.processDatum(datum);
			}
		}
	}

	/**
	 * Get the charge point DAO.
	 * 
	 * @return the DAO
	 */
	public CentralChargePointDao getChargePointDao() {
		return chargePointDao;
	}

	/**
	 * Get the charge point settings DAO.
	 * 
	 * @return the DAO
	 */
	public ChargePointSettingsDao getChargePointSettingsDao() {
		return chargePointSettingsDao;
	}

	/**
	 * Get the charge point connector DAO.
	 * 
	 * @return the DAO
	 */
	public CentralChargePointConnectorDao getChargePointConnectorDao() {
		return chargePointConnectorDao;
	}

	/**
	 * Get the datum DAO.
	 * 
	 * @return the DAO
	 */
	public DatumEntityDao getDatumDao() {
		return datumDao;
	}

	/**
	 * Get the SolarFlux publisher.
	 * 
	 * @return the publisher, or {@literal null}
	 */
	public DatumProcessor getFluxPublisher() {
		return fluxPublisher;
	}

	/**
	 * Set the SolarFlux publisher.
	 * 
	 * @param fluxPublisher
	 *        the publisher to set
	 */
	public void setFluxPublisher(DatumProcessor fluxPublisher) {
		this.fluxPublisher = fluxPublisher;
	}

	/**
	 * Get the source ID template.
	 * 
	 * @return the template; defaults to
	 *         {@link UserSettings#DEFAULT_SOURCE_ID_TEMPLATE}
	 */
	public String getSourceIdTemplate() {
		return sourceIdTemplate;
	}

	/**
	 * Set the source ID template.
	 * 
	 * <p>
	 * This template string allows for these parameters:
	 * </p>
	 * 
	 * <ol>
	 * <li><code>{chargePointId}</code> - the Charge Point ID (number)</li>
	 * <li><code>{chargerIdentifier}</code> - the Charge Point info identifier
	 * (string)</li>
	 * <li><code>{connectorId}</code> - the connector ID (integer)</li>
	 * </ol>
	 * 
	 * @param sourceIdTemplate
	 *        the template to set
	 */
	public void setSourceIdTemplate(String sourceIdTemplate) {
		this.sourceIdTemplate = sourceIdTemplate;
	}

	/**
	 * Get a suffix to append to the resolved source ID template.
	 * 
	 * @return the suffix; defaults to {@link #DEFAULT_SOURCE_ID_SUFFIX}
	 */
	public String getSourceIdSuffix() {
		return sourceIdSuffix;
	}

	/**
	 * Set a suffix to append to the resolved source ID template.
	 * 
	 * @param sourceIdSuffix
	 *        the suffix to add
	 */
	public void setSourceIdSuffix(String sourceIdSuffix) {
		this.sourceIdSuffix = sourceIdSuffix;
	}

}
