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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.ocpp.dao.ChargeSessionDao;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargePointErrorCode;
import net.solarnetwork.ocpp.domain.ChargeSession;
import net.solarnetwork.ocpp.domain.StatusNotification;

/**
 * Publish status notification updates as datum.
 * 
 * @author matt
 * @version 2.2
 */
public class ConnectorStatusDatumPublisher {

	private final DatumPublisherSupport pubSupport;
	private final ChargeSessionDao chargeSessionDao;

	/**
	 * Constructor.
	 * 
	 * @param chargePointDao
	 *        the charge point DAO to use
	 * @param chargePointSettingsDao
	 *        the settings DAO to use
	 * @param chargePointConnectorDao
	 *        the connector DAO to use
	 * @param chargeSessionDao
	 *        charge session DAO to use
	 * @param datumDao
	 *        the datum DAO to use
	 */
	public ConnectorStatusDatumPublisher(CentralChargePointDao chargePointDao,
			ChargePointSettingsDao chargePointSettingsDao,
			CentralChargePointConnectorDao chargePointConnectorDao, ChargeSessionDao chargeSessionDao,
			DatumEntityDao datumDao) {
		super();
		this.pubSupport = new DatumPublisherSupport(chargePointDao, chargePointSettingsDao,
				chargePointConnectorDao, datumDao);
		this.chargeSessionDao = requireNonNullArgument(chargeSessionDao, "chargeSessionDao");
	}

	/**
	 * Datum property name enumeration.
	 */
	public enum DatumProperty {

		/** A status enumeration. */
		Status("status", DatumSamplesType.Status),

		/** The information string. */
		Info("info", DatumSamplesType.Status),

		/** An error code enumeration. */
		ErrorCode("errorCode", DatumSamplesType.Status),

		/** A vendor identifier. */
		VendorId("vendorId", DatumSamplesType.Status),

		/** A vendor-specific error code. */
		VendorErrorCode("vendorErrorCode", DatumSamplesType.Status),

		/** A charging session ID. */
		SessionId("sessionId", DatumSamplesType.Status),

		/** The charging session transaction ID. */
		TransactionId("transactionId", DatumSamplesType.Status),

		;

		private final String propertyName;
		private final DatumSamplesType classification;

		private DatumProperty(String propertyName, DatumSamplesType classification) {
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
		public DatumSamplesType getClassification() {
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
		ChargePointSettings cps = pubSupport.settingsForChargePoint(chargePoint.getUserId(),
				chargePoint.getId());
		if ( !(cps.isPublishToSolarIn() || cps.isPublishToSolarFlux()) ) {
			return;
		}

		ChargePointConnectorKey key = new ChargePointConnectorKey(chargePoint.getId(),
				info.getConnectorId());
		CentralChargePointConnector cpc = pubSupport.getChargePointConnectorDao()
				.get(chargePoint.getUserId(), key);
		if ( cpc != null ) {
			processStatusNotification(chargePoint, cps, cpc.getInfo());
		}
	}

	private void processStatusNotification(CentralChargePoint chargePoint, ChargePointSettings cps,
			StatusNotification info) {
		if ( info == null ) {
			return;
		}

		DatumSamples s = new DatumSamples();
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
			s.putSampleValue(DatumProperty.TransactionId.getClassification(),
					DatumProperty.TransactionId.getPropertyName(),
					String.valueOf(cs.getTransactionId()));
		}

		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setNodeId(chargePoint.getNodeId());
		d.setSourceId(pubSupport.sourceId(cps, chargePoint.getInfo().getId(), info.getConnectorId()));
		if ( info.getTimestamp() != null ) {
			d.setCreated(info.getTimestamp());
		}
		d.setSamples(s);
		pubSupport.publishDatum(cps, d);
	}

	/**
	 * Set the SolarFlux publisher.
	 * 
	 * @param fluxPublisher
	 *        the publisher to set
	 */
	public void setFluxPublisher(DatumProcessor fluxPublisher) {
		pubSupport.setFluxPublisher(fluxPublisher);
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
		pubSupport.setSourceIdTemplate(sourceIdTemplate);
	}

	/**
	 * Set a suffix to append to the resolved source ID template.
	 * 
	 * @param sourceIdSuffix
	 *        the suffix to add
	 */
	public void setSourceIdSuffix(String sourceIdSuffix) {
		pubSupport.setSourceIdSuffix(sourceIdSuffix);
	}

}
