/* ==================================================================
 * SolarOcppOcppV201WarmUpTask.java - 3/07/2024 4:35:45 pm
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

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V201;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.AppWarmUpTask;
import net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration;
import net.solarnetwork.ocpp.json.ActionPayloadDecoder;
import net.solarnetwork.ocpp.v201.domain.Action;
import ocpp.v201.BootNotificationRequest;
import ocpp.v201.BootNotificationResponse;
import ocpp.v201.BootReasonEnum;
import ocpp.v201.ChargingStation;
import ocpp.v201.ConnectorStatusEnum;
import ocpp.v201.RegistrationStatusEnum;
import ocpp.v201.StatusNotificationRequest;
import ocpp.v201.StatusNotificationResponse;

/**
 * Component to "warm up" the application OCPP v16 components.
 * 
 * @author matt
 * @version 1.0
 */
@Component
@Profile(SolarOcppOcppV201WarmUpTask.PROFILE)
public class SolarOcppOcppV201WarmUpTask implements AppWarmUpTask {

	/** The profile required by this component. */
	public static final String PROFILE = AppWarmUpTask.WARMUP + " & "
			+ SolarNetOcppConfiguration.OCPP_V201;

	private static final Logger log = LoggerFactory.getLogger(SolarOcppOcppV201WarmUpTask.class);

	private final ObjectMapper objectMapper;
	private final ActionPayloadDecoder actionPayloadDecoder;

	/**
	 * Constructor.
	 * 
	 * @param objectMapper
	 *        the object mapper
	 * @param actionPayloadDecoder
	 *        the action payload decoder
	 */
	public SolarOcppOcppV201WarmUpTask(@Qualifier(OCPP_V201) ObjectMapper objectMapper,
			@Qualifier(OCPP_V201) ActionPayloadDecoder actionPayloadDecoder) {
		super();
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.actionPayloadDecoder = requireNonNullArgument(actionPayloadDecoder, "actionPayloadDecoder");
	}

	@Override
	public void warmUp() throws Exception {
		log.info("Performing OCPP v201 warm-up tasks...");

		try {
			log.debug("Validating BootNotification message...");
			BootNotificationRequest boot = new BootNotificationRequest(new ChargingStation(IDENT, IDENT),
					BootReasonEnum.POWER_UP);
			boot.getChargingStation().setSerialNumber("1.2.3");
			JsonNode json = objectMapper.valueToTree(boot);
			actionPayloadDecoder.decodeActionPayload(Action.BootNotification, false, json);

			log.debug("Encoding BootNotification response message...");
			BootNotificationResponse bootResp = new BootNotificationResponse();
			bootResp.setCurrentTime(Instant.now());
			bootResp.setInterval(300);
			bootResp.setStatus(RegistrationStatusEnum.ACCEPTED);
			objectMapper.writeValueAsString(bootResp);

			log.debug("Validating StatusNotification message...");
			StatusNotificationRequest status = new StatusNotificationRequest();
			status.setTimestamp(Instant.now());
			status.setConnectorStatus(ConnectorStatusEnum.AVAILABLE);
			status.setEvseId(1);
			status.setConnectorId(1);
			json = objectMapper.valueToTree(status);
			actionPayloadDecoder.decodeActionPayload(Action.StatusNotification, false, json);

			log.debug("Encoding StatusNotification response message...");
			StatusNotificationResponse statusResp = new StatusNotificationResponse();
			objectMapper.writeValueAsString(statusResp);
		} catch ( Exception e ) {
			log.error("App warm-up tasks threw exception: {}", e.getMessage(), e);
		}

		log.info("OCPP v201 warm-up tasks complete.");
	}

}
