/* ==================================================================
 * OcppV16WebSocketConfig.java - 12/11/2021 3:10:28 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.config;

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V16;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.in.ocpp.json.CentralOcppWebSocketHandler;
import net.solarnetwork.central.in.ocpp.json.CentralOcppWebSocketHandshakeInterceptor;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.ocpp.config.OcppCentralServiceQualifier;
import net.solarnetwork.central.ocpp.config.OcppChargePointQualifier;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.service.SimpleActionMessageQueue;
import net.solarnetwork.service.PasswordEncoder;
import ocpp.json.ActionPayloadDecoder;
import ocpp.v16.CentralSystemAction;
import ocpp.v16.ChargePointAction;
import ocpp.v16.ErrorCodeResolver;

/**
 * OCPP v1.6 web socket configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@EnableWebSocket
@Profile(OCPP_V16)
public class OcppV16WebSocketConfig implements WebSocketConfigurer {

	@Autowired
	private NodeInstructionDao nodeInstructionDao;

	@Autowired
	private CentralChargePointDao ocppCentralChargePointDao;

	@Autowired
	private CentralSystemUserDao ocppSystemUserDao;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@Autowired
	private TaskScheduler taskScheduler;

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	@Autowired
	@Qualifier(OCPP_V16)
	private ObjectMapper objectMapper;

	@Autowired
	@OcppCentralServiceQualifier(OCPP_V16)
	private ActionPayloadDecoder centralServiceActionPayloadDecoder;

	@Autowired
	@OcppChargePointQualifier(OCPP_V16)
	private ActionPayloadDecoder chargePointActionPayloadDecoder;

	@Autowired
	@OcppCentralServiceQualifier(OCPP_V16)
	private List<ActionMessageProcessor<?, ?>> ocppCentralServiceActions;

	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	@Qualifier(OCPP_V16)
	public CentralOcppWebSocketHandler<ChargePointAction, CentralSystemAction> ocppWebSocketHandler_v16() {
		CentralOcppWebSocketHandler<ChargePointAction, CentralSystemAction> handler = new CentralOcppWebSocketHandler<>(
				ChargePointAction.class, CentralSystemAction.class, new ErrorCodeResolver(),
				taskExecutor, objectMapper, new SimpleActionMessageQueue(),
				centralServiceActionPayloadDecoder, chargePointActionPayloadDecoder);
		handler.setTaskScheduler(taskScheduler);
		handler.setChargePointDao(ocppCentralChargePointDao);
		handler.setInstructionDao(nodeInstructionDao);
		handler.setUserEventAppenderBiz(userEventAppenderBiz);
		if ( ocppCentralServiceActions != null ) {
			for ( ActionMessageProcessor<?, ?> processor : ocppCentralServiceActions ) {
				handler.addActionMessageProcessor(processor);
			}
		}
		return handler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		WebSocketHandlerRegistration reg = registry.addHandler(ocppWebSocketHandler_v16(),
				"/ocpp/j/v16/**");

		CentralOcppWebSocketHandshakeInterceptor interceptor = new CentralOcppWebSocketHandshakeInterceptor(
				ocppSystemUserDao, passwordEncoder);
		interceptor.setClientIdUriPattern(Pattern.compile("/ocpp/j/v16/(.*)"));
		interceptor.setUserEventAppenderBiz(userEventAppenderBiz);
		reg.addInterceptors(interceptor);
	}

}
