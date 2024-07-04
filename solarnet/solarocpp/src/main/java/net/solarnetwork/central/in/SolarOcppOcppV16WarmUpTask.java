/* ==================================================================
 * SolarOcppOcppV16WarmUpTask.java - 3/07/2024 4:35:45 pm
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

package net.solarnetwork.central.in;

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V16;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.AppWarmUpTask;
import net.solarnetwork.central.ocpp.config.OcppCentralServiceQualifier;
import net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration;
import net.solarnetwork.ocpp.json.ActionPayloadDecoder;
import net.solarnetwork.ocpp.v16.jakarta.CentralSystemAction;
import net.solarnetwork.ocpp.xml.jakarta.support.XmlDateUtils;
import ocpp.v16.jakarta.cs.BootNotificationRequest;
import ocpp.v16.jakarta.cs.BootNotificationResponse;
import ocpp.v16.jakarta.cs.ChargePointErrorCode;
import ocpp.v16.jakarta.cs.ChargePointStatus;
import ocpp.v16.jakarta.cs.RegistrationStatus;
import ocpp.v16.jakarta.cs.StatusNotificationRequest;
import ocpp.v16.jakarta.cs.StatusNotificationResponse;

/**
 * Component to "warm up" the application OCPP v16 components.
 * 
 * @author matt
 * @version 1.0
 */
@Component
@Profile(SolarOcppOcppV16WarmUpTask.PROFILE)
public class SolarOcppOcppV16WarmUpTask implements AppWarmUpTask {

	/** The profile required by this component. */
	public static final String PROFILE = AppWarmUpTask.WARMUP + " & "
			+ SolarNetOcppConfiguration.OCPP_V16;

	private static final Logger log = LoggerFactory.getLogger(SolarOcppOcppV16WarmUpTask.class);

	private final ObjectMapper objectMapper;
	private final ActionPayloadDecoder centralServiceActionPayloadDecoder;

	/**
	 * Constructor.
	 * 
	 * @param objectMapper
	 *        the object mapper
	 * @param centralServiceActionPayloadDecoder
	 *        the action payload decoder
	 */
	public SolarOcppOcppV16WarmUpTask(@Qualifier(OCPP_V16) ObjectMapper objectMapper,
			@OcppCentralServiceQualifier(OCPP_V16) ActionPayloadDecoder centralServiceActionPayloadDecoder) {
		super();
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.centralServiceActionPayloadDecoder = requireNonNullArgument(
				centralServiceActionPayloadDecoder, "centralServiceActionPayloadDecoder");
	}

	@Override
	public void warmUp() throws Exception {
		log.info("Performing OCPP v16 warm-up tasks...");

		try {
			log.debug("Validating BootNotification message...");
			BootNotificationRequest boot = new BootNotificationRequest();
			boot.setChargePointVendor(IDENT);
			boot.setChargePointModel(IDENT);
			boot.setMeterSerialNumber("1.2.3");
			JsonNode json = objectMapper.valueToTree(boot);
			centralServiceActionPayloadDecoder.decodeActionPayload(CentralSystemAction.BootNotification,
					false, json);

			log.debug("Encoding BootNotification response message...");
			BootNotificationResponse bootResp = new BootNotificationResponse();
			bootResp.setCurrentTime(XmlDateUtils.newXmlCalendar());
			bootResp.setInterval(300);
			bootResp.setStatus(RegistrationStatus.ACCEPTED);
			objectMapper.writeValueAsString(bootResp);

			log.debug("Validating StatusNotification message...");
			StatusNotificationRequest status = new StatusNotificationRequest();
			status.setStatus(ChargePointStatus.AVAILABLE);
			status.setErrorCode(ChargePointErrorCode.NO_ERROR);
			json = objectMapper.valueToTree(status);
			centralServiceActionPayloadDecoder
					.decodeActionPayload(CentralSystemAction.StatusNotification, false, json);

			log.debug("Encoding StatusNotification response message...");
			StatusNotificationResponse statusResp = new StatusNotificationResponse();
			objectMapper.writeValueAsString(statusResp);
		} catch ( Exception e ) {
			log.error("App warm-up tasks threw exception: {}", e.getMessage(), e);
		}

		log.info("OCPP v16 warm-up tasks complete.");
	}

}
