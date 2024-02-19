/* ==================================================================
 * VehicleMacDataTransferDatumPublisher.java - 21/11/2023 8:40:05 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.v16.vendor.hiconics;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.service.DatumPublisherSupport;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.ocpp.v16.jakarta.cs.DataTransferProcessor;
import net.solarnetwork.util.ObjectUtils;
import ocpp.v16.jakarta.cs.DataTransferRequest;
import ocpp.v16.jakarta.cs.DataTransferResponse;
import ocpp.v16.jakarta.cs.DataTransferStatus;

/**
 * Publish Hiconics data transfer vehicle MAC messages as a datum stream.
 * 
 * <p>
 * The Hiconics chargers publish {@code DataTransfer} messages with vehicle MAC
 * information this processor converts into a datum stream. The datum stream
 * will have a source ID based on the source ID template configured on the
 * associated charger configuration entity in SolarNetwork, with
 * {@code /vehicleMac} appended to the end.
 * </p>
 * 
 * <p>
 * The format of the {@code DataTransfer} data is JSON, structured like this
 * example:
 * </p>
 * 
 * <pre>
 * {@code{
 *     "vehicleId": "xyz123",
 *     "connectorId": 1,
 *     "timestamp": "2023-06-16T19:05:46Z"
 * }}
 * </pre>
 * 
 * @author matt
 * @version 1.1
 */
public class VehicleMacDataTransferDatumPublisher extends DataTransferProcessor {

	/** The {@code sourceIdSuffix} property default value. */
	public static final String DEFAULT_SOURCE_ID_SUFFIX = "/vid";

	/** The DataTransferRequest {@code vendorId} property value. */
	public static final String VENDOR_ID = "HKZN";

	/** The DataTransferRequest {@code messageId} property value. */
	public static final String MESSAGE_ID = "vehicleMAC";

	private final ObjectMapper mapper;
	private final DatumPublisherSupport pubSupport;

	/**
	 * Constructor.
	 * 
	 * @param chargePointDao
	 *        the charge point DAO
	 * @param chargePointSettingsDao
	 *        the charge point settings DAO
	 * @param chargePointConnectorDao
	 *        the charge point connector DAO
	 * @param datumDao
	 *        the datum DAO
	 * @param mapper
	 *        the object mapper
	 */
	public VehicleMacDataTransferDatumPublisher(CentralChargePointDao chargePointDao,
			ChargePointSettingsDao chargePointSettingsDao,
			CentralChargePointConnectorDao chargePointConnectorDao, DatumEntityDao datumDao,
			ObjectMapper mapper) {
		super();
		this.mapper = ObjectUtils.requireNonNullArgument(mapper, "mapper");
		this.pubSupport = new DatumPublisherSupport(chargePointDao, chargePointSettingsDao,
				chargePointConnectorDao, datumDao);
		setSourceIdSuffix(DEFAULT_SOURCE_ID_SUFFIX);
	}

	@Override
	public boolean isMessageSupported(ActionMessage<?> message) {
		if ( !super.isMessageSupported(message) ) {
			return false;
		}
		DataTransferRequest req = (DataTransferRequest) message.getMessage();
		return (VENDOR_ID.equals(req.getVendorId()) && MESSAGE_ID.equals(req.getMessageId()));
	}

	@Override
	public void processActionMessage(ActionMessage<DataTransferRequest> message,
			ActionMessageResultHandler<DataTransferRequest, DataTransferResponse> resultHandler) {
		DataTransferRequest req = message.getMessage();
		log.info("OCPP DataTransfer received from {}; message ID = {}; vendor ID = {}; data = {}",
				message.getClientId(), req.getMessageId(), req.getVendorId(), req.getData());
		final CentralChargePoint cp = pubSupport.chargePoint(message.getClientId());
		final ChargePointSettings cps = pubSupport.settingsForChargePoint(cp.getUserId(), cp.getId());

		DataTransferResponse res = new DataTransferResponse();

		GeneralNodeDatum d = datum(req, cp, cps);
		if ( d != null ) {
			res.setStatus(DataTransferStatus.ACCEPTED);
			pubSupport.publishDatum(cps, d);
		} else {
			res.setStatus(DataTransferStatus.REJECTED);
		}

		resultHandler.handleActionMessageResult(message, res, null);
	}

	private GeneralNodeDatum datum(DataTransferRequest req, CentralChargePoint cp,
			ChargePointSettings cps) {
		String data = req.getData();
		if ( data == null || data.isBlank() ) {
			return null;
		}

		final JsonNode root;
		try {
			root = mapper.readTree(data.trim());
		} catch ( IOException e ) {
			log.warn("Failed to parse DataTransfer data JSON [{}]: {}", data, e);
			return null;
		}

		final String vid = root.path("vehicleId").textValue();
		final Instant ts = JsonUtils.parseDateAttribute(root, "timestamp", DateTimeFormatter.ISO_INSTANT,
				Instant::from);
		final int connId = root.path("connectorId").intValue();
		if ( vid == null || vid.isEmpty() || ts == null || connId < 1 ) {
			return null;
		}

		DatumSamples s = new DatumSamples();
		s.putStatusSampleValue("vid", vid);

		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setSamples(s);
		d.setCreated(ts);
		d.setNodeId(cp.getNodeId());
		d.setSourceId(pubSupport.sourceId(cps, cp.getInfo().getId(), null, connId));
		return d;
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
