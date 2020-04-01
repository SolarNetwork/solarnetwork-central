/* ==================================================================
 * ConnectorStatusDatumPublisher.java - 2/04/2020 7:29:25 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.StringUtils.expandTemplateString;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTime;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargeSessionDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargePointErrorCode;
import net.solarnetwork.ocpp.domain.ChargeSession;
import net.solarnetwork.ocpp.domain.StatusNotification;
import net.solarnetwork.util.OptionalService;

/**
 * Publish status notification updates as datum.
 * 
 * @author matt
 * @version 1.0
 */
public class ConnectorStatusDatumPublisher {

	public static final String DEFAULT_SOURCE_ID_SUFFIX = "/status";

	private final ChargePointSettingsDao chargePointSettingsDao;
	private final CentralChargePointConnectorDao chargePointConnectorDao;
	private final CentralChargeSessionDao chargeSessionDao;
	private final GeneralNodeDatumDao datumDao;
	private final OptionalService<DatumProcessor> fluxPublisher;
	private String sourceIdTemplate = UserSettings.DEFAULT_SOURCE_ID_TEMPLATE;
	private String sourceIdSuffix = DEFAULT_SOURCE_ID_SUFFIX;

	/**
	 * Constructor.
	 * 
	 * @param chargePointSettingsDao
	 *        the settings DAO to use
	 * @param chargePointConnectorDao
	 *        the connector DAO to use
	 * @param chargeSessionDao
	 *        charge session DAO to use
	 * @param datumDao
	 *        the datum DAO to use
	 * @param fluxPublisher
	 *        the optional SolarFlux publisher to use
	 */
	public ConnectorStatusDatumPublisher(ChargePointSettingsDao chargePointSettingsDao,
			CentralChargePointConnectorDao chargePointConnectorDao,
			CentralChargeSessionDao chargeSessionDao, GeneralNodeDatumDao datumDao,
			OptionalService<DatumProcessor> fluxPublisher) {
		super();
		this.chargePointSettingsDao = chargePointSettingsDao;
		this.chargePointConnectorDao = chargePointConnectorDao;
		this.chargeSessionDao = chargeSessionDao;
		this.datumDao = datumDao;
		this.fluxPublisher = fluxPublisher;
	}

	/**
	 * Datum property name enumeration.
	 */
	public enum DatumProperty {

		/** A status enumeration. */
		Status("status", GeneralDatumSamplesType.Status),

		/** The information string. */
		Info("info", GeneralDatumSamplesType.Status),

		/** An error code enumeration. */
		ErrorCode("errorCode", GeneralDatumSamplesType.Status),

		/** A vendor identifier. */
		VendorId("vendorId", GeneralDatumSamplesType.Status),

		/** A vendor-specific error code. */
		VendorErrorCode("vendorErrorCode", GeneralDatumSamplesType.Status),

		/** A charging session ID. */
		SessionId("sessionId", GeneralDatumSamplesType.Status);

		private final String propertyName;
		private final GeneralDatumSamplesType classification;

		private DatumProperty(String propertyName, GeneralDatumSamplesType classification) {
			this.propertyName = propertyName;
			this.classification = classification;
		}

		/**
		 * Get the property name.
		 * 
		 * @return the property name
		 */
		public String getPropertyName() {
			return propertyName;
		}

		/**
		 * Get the property classification.
		 * 
		 * @return the classification
		 */
		public GeneralDatumSamplesType getClassification() {
			return classification;
		}

	}

	/**
	 * Process a status notification.
	 * 
	 * @param chargePoint
	 *        the charge point associated with the notification
	 * @param info
	 *        the notification
	 */
	public void processStatusNotification(CentralChargePoint chargePoint, StatusNotification info) {
		ChargePointSettings cps = settingsForChargePoint(chargePoint);
		if ( !(cps.isPublishToSolarIn() || cps.isPublishToSolarFlux()) ) {
			return;
		}

		if ( info.getConnectorId() == 0 ) {
			Collection<CentralChargePointConnector> connectors = chargePointConnectorDao
					.findByChargePointId(chargePoint.getUserId(), chargePoint.getId());
			for ( CentralChargePointConnector cpc : connectors ) {
				processStatusNotification(chargePoint, cps, cpc.getInfo());
			}
		} else {
			ChargePointConnectorKey key = new ChargePointConnectorKey(chargePoint.getId(),
					info.getConnectorId());
			CentralChargePointConnector cpc = chargePointConnectorDao.get(chargePoint.getUserId(), key);
			if ( cpc != null ) {
				processStatusNotification(chargePoint, cps, cpc.getInfo());
			}
		}
	}

	private void processStatusNotification(CentralChargePoint chargePoint, ChargePointSettings cps,
			StatusNotification info) {
		if ( info == null ) {
			return;
		}

		GeneralNodeDatumSamples s = new GeneralNodeDatumSamples();
		if ( info.getStatus() != null ) {
			s.putSampleValue(DatumProperty.Status.getClassification(),
					DatumProperty.Status.getPropertyName(), info.getStatus().toString());
		}
		s.putSampleValue(DatumProperty.Info.getClassification(), DatumProperty.Info.getPropertyName(),
				info.getInfo());
		if ( info.getErrorCode() != null && info.getErrorCode() != ChargePointErrorCode.Unknown
				&& info.getErrorCode() != ChargePointErrorCode.NoError ) {
			s.putSampleValue(DatumProperty.ErrorCode.getClassification(),
					DatumProperty.ErrorCode.getPropertyName(), info.getErrorCode().toString());
		}
		s.putSampleValue(DatumProperty.VendorId.getClassification(),
				DatumProperty.VendorId.getPropertyName(), info.getVendorId());
		s.putSampleValue(DatumProperty.VendorErrorCode.getClassification(),
				DatumProperty.VendorErrorCode.getPropertyName(), info.getVendorErrorCode());

		ChargeSession cs = chargeSessionDao.getIncompleteChargeSessionForConnector(chargePoint.getId(),
				info.getConnectorId());
		if ( cs != null && !cs.getCreated().isAfter(info.getTimestamp()) ) {
			s.putSampleValue(DatumProperty.SessionId.getClassification(),
					DatumProperty.SessionId.getPropertyName(), cs.getId().toString());
		}

		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setNodeId(chargePoint.getNodeId());
		d.setSourceId(sourceId(cps, chargePoint.getInfo().getId(), info.getConnectorId()));
		if ( info.getTimestamp() != null ) {
			d.setCreated(new DateTime(info.getTimestamp().toEpochMilli()));
		}
		d.setSamples(s);

		if ( cps.isPublishToSolarIn() ) {
			datumDao.store(d);
		}
		if ( cps.isPublishToSolarFlux() ) {
			DatumProcessor publisher = fluxPublisher.service();
			if ( publisher != null && publisher.isConfigured() ) {
				publisher.processDatum(d);
			}
		}
	}

	private ChargePointSettings settingsForChargePoint(CentralChargePoint chargePoint) {
		ChargePointSettings cps = chargePointSettingsDao.resolveSettings(chargePoint.getUserId(),
				chargePoint.getId());
		if ( cps == null ) {
			// use default fallback
			cps = new ChargePointSettings(chargePoint.getId(), chargePoint.getUserId(), Instant.now());
			cps.setSourceIdTemplate(sourceIdTemplate);
		}
		return cps;
	}

	private String sourceId(ChargePointSettings chargePointSettings, String identifier,
			int connectorId) {
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
	 * Get the source ID template.
	 * 
	 * @return the template; defaults to {@link #DEFAULT_SOURCE_ID_TEMPLATE}
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
