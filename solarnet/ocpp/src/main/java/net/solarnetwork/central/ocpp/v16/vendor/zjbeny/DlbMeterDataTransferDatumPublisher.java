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
import net.solarnetwork.ocpp.v16.cs.DataTransferProcessor;
import net.solarnetwork.util.StringUtils;
import ocpp.v16.cs.DataTransferRequest;
import ocpp.v16.cs.DataTransferResponse;
import ocpp.v16.cs.DataTransferStatus;

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
 * @author matt
 * @version 1.1
 */
public class DlbMeterDataTransferDatumPublisher extends DataTransferProcessor {

	/** The {@code sourceIdSuffix} property default value. */
	public static final String DEFAULT_SOURCE_ID_SUFFIX = "/dlb";

	/** The DataTransferRequest {@code vendorId} property value. */
	public static final String VENDOR_ID = "com.zjbeny.dlb";

	/** The DataTransferRequest {@code messageId} property value. */
	public static final String MESSAGE_ID = "dlbMeter";

	/** THe data key for the DLB mode. */
	public static final String DLB_MODE_KEY = "DLBMode";

	/**
	 * The data key for the single-phase home load current, as an amperes value
	 * with {@literal A} suffix, e.g. "1A".
	 */
	public static final String CURRENT_HOME_LOAD = "Current.HomeLoad";

	/**
	 * The data key for the single-phase solar current, as an amperes value with
	 * {@literal A} suffix, e.g. "1A".
	 */
	public static final String CURRENT_SOLAR = "Current.Solar";

	/**
	 * The data key pattern for the multi-phase home load current, as an amperes
	 * value with {@literal A} suffix, e.g. "1A".
	 */
	public static final Pattern CURRENT_HOME_LOAD_PHASED = Pattern
			.compile("Current\\.HomeLoad\\.L(\\d)");

	/**
	 * The data key pattern for the multi-phase home load current, as an amperes
	 * value with {@literal A} suffix, e.g. "1A".
	 */
	public static final Pattern CURRENT_SOLAR_PHASED = Pattern.compile("Current\\.Solar\\.L(\\d)");

	/** The data value pattern for an ampere value. */
	public static final Pattern AMP_VALUE = Pattern.compile("(\\d+(\\.\\d*)?)A");

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
		Map<String, ?> map = (data.startsWith("{") ? JsonUtils.getStringMap(data)
				: StringUtils.delimitedStringToMap(data, ",", ":"));
		for ( Entry<String, ?> e : map.entrySet() ) {
			final String key = e.getKey();
			if ( DLB_MODE_KEY.equals(key) ) {
				s.putStatusSampleValue("mode", e.getValue());
			} else {
				Number amps = ampValue(e.getValue().toString());
				if ( amps != null ) {
					String propName = null;
					if ( CURRENT_HOME_LOAD.equals(key) ) {
						propName = "load";
					} else if ( CURRENT_SOLAR.equals(key) ) {
						propName = "solar";
					} else {
						Matcher m = CURRENT_HOME_LOAD_PHASED.matcher(key);
						if ( m.matches() ) {
							propName = phased("load", m.group(1));
						} else {
							m = CURRENT_SOLAR_PHASED.matcher(key);
							if ( m.matches() ) {
								propName = phased("solar", m.group(1));
							}
						}
					}
					if ( propName != null ) {
						s.putInstantaneousSampleValue(propName, amps);
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
		return (m.matches() ? StringUtils.numberValue(m.group(1)) : null);
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
