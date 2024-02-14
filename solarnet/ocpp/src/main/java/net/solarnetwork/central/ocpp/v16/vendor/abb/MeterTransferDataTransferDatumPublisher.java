/* ==================================================================
 * MeterTransferDataTransferDatumPublisher.java - 17/06/2023 7:23:06 am
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

package net.solarnetwork.central.ocpp.v16.vendor.abb;

import java.io.IOException;
import java.math.BigDecimal;
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
import net.solarnetwork.central.ocpp.v16.controller.DatumPublisherSupport;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.ocpp.v16.jakarta.cs.DataTransferProcessor;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.StringUtils;
import ocpp.v16.jakarta.cs.DataTransferRequest;
import ocpp.v16.jakarta.cs.DataTransferResponse;
import ocpp.v16.jakarta.cs.DataTransferStatus;

/**
 * Publish ABB data transfer {@code MeterTransfer} messages as a datum stream.
 * 
 * <p>
 * The ABB chargers publish {@code DataTransfer} messages with metering data
 * that this processor converts into a datum stream. The datum stream will have
 * a source ID based on the source ID template configured on the associated
 * charger configuration entity in SolarNetwork, with {@code /meter} appended to
 * the end.
 * </p>
 * 
 * <p>
 * The format of the {@code DataTransfer} data is JSON, structured like this
 * example:
 * </p>
 * 
 * <pre>
 * {@code{
 *     "type": "MeterTransfer",
 *     "timestamp": "2023-06-16T19:05:46.000Z",
 *     "sampledValue": [
 *         {
 *             "measurand": "Voltage.L1",
 *             "accuracy": "1",
 *             "unit": "V",
 *             "value": 2351
 *         },
 *         {
 *             "measurand": "Current.L1",
 *             "accuracy": "2",
 *            "unit": "A",
 *            "value": 7
 *         },
 *         {
 *             "measurand": "Active.Power.ALL",
 *             "accuracy": "2",
 *             "unit": "W",
 *             "value": 464
 *         }
 *     ]
 * }}
 * </pre>
 * 
 * @author matt
 * @version 1.0
 */
public class MeterTransferDataTransferDatumPublisher extends DataTransferProcessor {

	/** The {@code sourceIdSuffix} property default value. */
	public static final String DEFAULT_SOURCE_ID_SUFFIX = "/meter";

	/** The DataTransferRequest {@code vendorId} property value. */
	public static final String VENDOR_ID = "abc";

	/** The DataTransferRequest {@code messageId} property value. */
	public static final String MESSAGE_ID = "232";

	private final ObjectMapper mapper;
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
	 * @param mapper
	 *        the mapper to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public MeterTransferDataTransferDatumPublisher(CentralChargePointDao chargePointDao,
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

		DatumSamples s = new DatumSamples();

		// data is a JSON object or a comma-delimited list of colon-delimited key-value pairs
		data = data.trim();

		final JsonNode root;
		try {
			root = mapper.readTree(data);
		} catch ( IOException e ) {
			log.warn("Failed to parse DataTransfer data JSON [{}]: {}", data, e);
			return null;
		}

		final String msgType = root.path("type").textValue();
		final Instant ts = JsonUtils.parseDateAttribute(root, "timestamp", DateTimeFormatter.ISO_INSTANT,
				Instant::from);
		final JsonNode samples = root.path("sampledValue");
		if ( msgType == null || !"MeterTransfer".equals(msgType) || ts == null || !samples.isArray() ) {
			return null;
		}
		for ( JsonNode sampleNode : samples ) {
			MeterTransferMeasurand measurand = MeterTransferMeasurand
					.forKey(sampleNode.path("measurand").textValue());
			if ( measurand == null ) {
				continue;
			}
			String propName = phased(switch (measurand.name()) {
				case "Voltage" -> net.solarnetwork.domain.datum.AcEnergyDatum.VOLTAGE_KEY;
				case "Current" -> net.solarnetwork.domain.datum.AcEnergyDatum.CURRENT_KEY;
				case "Active.Power" -> net.solarnetwork.domain.datum.AcEnergyDatum.WATTS_KEY;
				default -> null;
			}, measurand);
			if ( propName == null ) {
				continue;
			}
			Number value = normalizedValue(sampleNode.path("value").asText(),
					sampleNode.path("accuracy").asText(), sampleNode.path("unit").asText());
			s.putInstantaneousSampleValue(propName, value);
		}

		if ( s.isEmpty() ) {
			return null;
		}

		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setSamples(s);
		d.setCreated(ts);
		d.setNodeId(cp.getNodeId());
		d.setSourceId(pubSupport.sourceId(cps, cp.getInfo().getId(), null));
		return d;
	}

	private static String phased(String propName, MeterTransferMeasurand measurand) {
		if ( propName == null || !measurand.isPhased() ) {
			return propName;
		}
		AcPhase p;
		switch (measurand.phase()) {
			case "L1":
				p = AcPhase.PhaseA;
				break;

			case "L2":
				p = AcPhase.PhaseB;
				break;

			case "L3":
				p = AcPhase.PhaseC;
				break;

			case "ALL":
				return propName;

			default:
				return null;
		}
		return p.withKey(propName);
	}

	private static Number normalizedValue(String value, String accurracy, String unit) {
		BigDecimal n = NumberUtils.bigDecimalForNumber(StringUtils.numberValue(value));
		if ( n == null ) {
			return null;
		}
		if ( accurracy != null ) {
			try {
				int p = Integer.parseInt(accurracy);
				n = n.movePointLeft(p);
			} catch ( NumberFormatException e ) {
				// ignore
			}
		}
		if ( unit.length() > 1 ) {
			unit = unit.toUpperCase();
			if ( unit.startsWith("K") ) {
				n.movePointRight(3);
			}
		}
		return NumberUtils.narrow(n, 2);
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
