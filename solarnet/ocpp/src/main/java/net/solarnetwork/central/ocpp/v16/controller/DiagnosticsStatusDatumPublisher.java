/* ==================================================================
 * DiagnosticsStatusDatumPublisher.java - 29/07/2022 9:16:28 am
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

import java.time.Instant;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.ocpp.v16.jakarta.cs.DiagnosticsStatusNotificationProcessor;
import ocpp.v16.jakarta.cs.DiagnosticsStatusNotificationRequest;
import ocpp.v16.jakarta.cs.DiagnosticsStatusNotificationResponse;

/**
 * Publish diagnostics status notifications as a datum stream.
 * 
 * @author matt
 * @version 1.1
 */
public class DiagnosticsStatusDatumPublisher extends DiagnosticsStatusNotificationProcessor {

	/** The {@code sourceIdSuffix} property default value. */
	public static final String DEFAULT_SOURCE_ID_SUFFIX = "/diagnostics-status";

	private final DatumPublisherSupport pubSupport;

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
	public DiagnosticsStatusDatumPublisher(CentralChargePointDao chargePointDao,
			ChargePointSettingsDao chargePointSettingsDao,
			CentralChargePointConnectorDao chargePointConnectorDao, DatumEntityDao datumDao) {
		super();
		this.pubSupport = new DatumPublisherSupport(chargePointDao, chargePointSettingsDao,
				chargePointConnectorDao, datumDao);
		this.pubSupport.setSourceIdSuffix(DEFAULT_SOURCE_ID_SUFFIX);
	}

	/**
	 * Datum property name enumeration.
	 */
	public enum DatumProperty {

		/** A status enumeration. */
		Status("status", DatumSamplesType.Status),

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

	@Override
	public void processActionMessage(ActionMessage<DiagnosticsStatusNotificationRequest> message,
			ActionMessageResultHandler<DiagnosticsStatusNotificationRequest, DiagnosticsStatusNotificationResponse> resultHandler) {
		if ( message != null && message.getMessage() != null ) {
			DiagnosticsStatusNotificationRequest notif = message.getMessage();
			DatumSamples s = new DatumSamples();
			if ( notif.getStatus() != null ) {
				s.putSampleValue(DatumProperty.Status.getClassification(),
						DatumProperty.Status.getPropertyName(), notif.getStatus().toString());
			}

			if ( !s.isEmpty() ) {
				final CentralChargePoint cp = pubSupport.chargePoint(message.getClientId());
				final ChargePointSettings cps = pubSupport.settingsForChargePoint(cp.getUserId(),
						cp.getId());

				GeneralNodeDatum d = new GeneralNodeDatum();
				d.setCreated(Instant.now());
				d.setNodeId(cp.getNodeId());
				d.setSourceId(pubSupport.sourceId(cps, cp.getInfo().getId(), null, null));
				d.setSamples(s);
				pubSupport.publishDatum(cps, d);
			}
		}
		super.processActionMessage(message, resultHandler);
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
