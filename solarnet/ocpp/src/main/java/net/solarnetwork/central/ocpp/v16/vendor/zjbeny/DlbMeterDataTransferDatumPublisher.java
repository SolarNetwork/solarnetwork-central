/* ==================================================================
 * DlbMeterDataTransferDatumPublisher.java - 29/07/2022 5:34:57 pm
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

package net.solarnetwork.central.ocpp.v16.vendor.zjbeny;

import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import net.solarnetwork.util.StringUtils;
import ocpp.v16.jakarta.cs.DataTransferRequest;
import ocpp.v16.jakarta.cs.DataTransferResponse;
import ocpp.v16.jakarta.cs.DataTransferStatus;

/**
 * Publish ZJBENY data transfer DLB meter messages as a datum stream.
 * 
 * <p>
 * The ZJBENY chargers publish {@code DataTransfer} messages with metering data
 * that this processor converts into a datum stream. The datum stream will have
 * a source ID based on the source ID template configured on the associated
 * charger configuration entity in SolarNetwork, with {@code /dlb} appended to
 * the end.
 * </p>
 * 
 * <p>
 * The format of the {@code DataTransfer} message has changed over time. The
 * original format used the Vendor ID {@link #VENDOR_ID} and used a delimited
 * text structure. An update switched the Vendor ID to {@link #VENDOR_ID_2} and
 * changed to a JSON structure. A further update switched the data units from
 * {@code A} to {@code kW} and changed the JSON property names as a result, as
 * well as adding a {@link #DLB_STATUS_KEY} property.
 * </p>
 * 
 * @author matt
 * @version 1.4
 */
public class DlbMeterDataTransferDatumPublisher extends DataTransferProcessor {

	/** The {@code sourceIdSuffix} property default value. */
	public static final String DEFAULT_SOURCE_ID_SUFFIX = "/dlb";

	/** The DataTransferRequest {@code vendorId} property value. */
	public static final String VENDOR_ID = "com.zjbeny.dlb";

	/**
	 * The DataTransferRequest {@code vendorId} property value used in later
	 * protocol versions.
	 */
	public static final String VENDOR_ID_2 = "ZJBENY";

	/** The DataTransferRequest {@code messageId} property value. */
	public static final String MESSAGE_ID = "dlbMeter";

	/** The data key for the DLB mode. */
	public static final String DLB_MODE_KEY = "DLBMode";

	/** The data value pattern for an ampere value. */
	public static final Pattern AMP_VALUE = Pattern.compile("(-*\\d+(\\.\\d*)?)A");

	/**
	 * The data value pattern for a kilowatt value.
	 * 
	 * @since 1.4
	 */
	public static final Pattern KW_VALUE = Pattern.compile("(-*\\d+(\\.\\d*)?)kW",
			Pattern.CASE_INSENSITIVE);

	/**
	 * The data key for the DLB status, whose value can be Normal, Error, or
	 * Offline.
	 * 
	 * @since 1.4
	 */
	public static final String DLB_STATUS_KEY = "DLBStatus";

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
	public DlbMeterDataTransferDatumPublisher(CentralChargePointDao chargePointDao,
			ChargePointSettingsDao chargePointSettingsDao,
			CentralChargePointConnectorDao chargePointConnectorDao, DatumEntityDao datumDao) {
		super();
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
		return ((VENDOR_ID.equals(req.getVendorId()) || VENDOR_ID_2.equals(req.getVendorId()))
				&& MESSAGE_ID.equals(req.getMessageId()));
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
		Map<String, ?> map = (data.startsWith("{") ? JsonUtils.getStringMap(data)
				: StringUtils.delimitedStringToMap(data, ",", ":"));
		for ( Entry<String, ?> e : map.entrySet() ) {
			final String key = e.getKey();
			if ( DLB_MODE_KEY.equals(key) ) {
				s.putStatusSampleValue("mode", e.getValue());
			} else if ( DLB_STATUS_KEY.equals(key) ) {
				s.putStatusSampleValue("status", e.getValue());
			} else {
				Number value = ampValue(e.getValue().toString());
				if ( value == null ) {
					value = kwValue(e.getValue().toString());
					if ( value != null ) {
						// convert to W
						value = NumberUtils.scaled(value, 3);
					}
				}
				if ( value != null ) {
					DlbMeterKey meterKey = DlbMeterKey.forKey(key);
					if ( meterKey != null ) {
						String propName = switch (meterKey.name()) {
							case HomeLoad -> "load";
							case Solar -> "solar";
							case EVSE -> "evse";
							case Grid -> "grid";
						};
						if ( meterKey.isPhased() ) {
							propName = phased(propName, meterKey.phase());
						}
						if ( propName != null ) {
							s.putInstantaneousSampleValue(propName, value);
						}
					}
				}
			}
		}

		if ( s.isEmpty() ) {
			return null;
		}

		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setSamples(s);
		d.setCreated(Instant.now());
		d.setNodeId(cp.getNodeId());
		d.setSourceId(pubSupport.sourceId(cps, cp.getInfo().getId(), null));
		return d;
	}

	private static Number ampValue(String value) {
		Matcher m = AMP_VALUE.matcher(value);
		return (m.matches() ? NumberUtils.narrow(StringUtils.numberValue(m.group(1)), 2) : null);
	}

	private static Number kwValue(String value) {
		Matcher m = KW_VALUE.matcher(value);
		return (m.matches() ? NumberUtils.narrow(StringUtils.numberValue(m.group(1)), 2) : null);
	}

	private static String phased(String key, String phase) {
		AcPhase p;
		switch (phase) {
			case "1":
				p = AcPhase.PhaseA;
				break;

			case "2":
				p = AcPhase.PhaseB;
				break;

			case "3":
				p = AcPhase.PhaseC;
				break;

			default:
				return null;
		}
		return p.withKey(key);
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
