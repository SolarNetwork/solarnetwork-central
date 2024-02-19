/* ==================================================================
 * OcppV201WebSocketConfig.java - 18/02/2024 7:21:35 am
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

package net.solarnetwork.central.in.ocpp.config;

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V201;
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
import net.solarnetwork.central.ApplicationMetadata;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.in.ocpp.json.CentralOcppWebSocketHandler;
import net.solarnetwork.central.in.ocpp.json.CentralOcppWebSocketHandshakeInterceptor;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.central.ocpp.dao.ChargePointActionStatusDao;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusDao;
import net.solarnetwork.central.ocpp.util.OcppInstructionUtils;
import net.solarnetwork.central.ocpp.v201.service.ConnectorKeyExtractor;
import net.solarnetwork.ocpp.json.ActionPayloadDecoder;
import net.solarnetwork.ocpp.json.WebSocketSubProtocol;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.service.SimpleActionMessageQueue;
import net.solarnetwork.ocpp.v201.domain.Action;
import net.solarnetwork.ocpp.v201.service.ErrorCodeResolver;
import net.solarnetwork.service.PasswordEncoder;

/**
 * OCPP v2.0.1 web socket configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@EnableWebSocket
@Profile(OCPP_V201)
public class OcppV201WebSocketConfig implements WebSocketConfigurer {

	@Autowired
	private ApplicationMetadata applicationMetadata;

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
	private ChargePointStatusDao chargePointStatusDao;

	@Autowired
	private ChargePointActionStatusDao chargePointActionStatusDao;

	@Autowired
	@Qualifier(OCPP_V201)
	private ObjectMapper objectMapper;

	@Autowired
	@Qualifier(OCPP_V201)
	private ActionPayloadDecoder actionPayloadDecoder;

	@Autowired
	@Qualifier(OCPP_V201)
	private List<ActionMessageProcessor<?, ?>> ocppActions;

	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	@Qualifier(OCPP_V201)
	public CentralOcppWebSocketHandler<Action, Action> ocppWebSocketHandler_v201() {
		CentralOcppWebSocketHandler<Action, Action> handler = new CentralOcppWebSocketHandler<>(
				Action.class, Action.class, new ErrorCodeResolver(), taskExecutor, objectMapper,
				new SimpleActionMessageQueue(), actionPayloadDecoder, actionPayloadDecoder,
				WebSocketSubProtocol.OCPP_V201.getValue());
		handler.setTaskScheduler(taskScheduler);
		handler.setChargePointDao(ocppCentralChargePointDao);
		handler.setInstructionDao(nodeInstructionDao);
		handler.setUserEventAppenderBiz(userEventAppenderBiz);
		if ( ocppActions != null ) {
			for ( ActionMessageProcessor<?, ?> processor : ocppActions ) {
				handler.addActionMessageProcessor(processor);
			}
		}
		handler.setApplicationMetadata(applicationMetadata);
		handler.setChargePointStatusDao(chargePointStatusDao);
		handler.setChargePointActionStatusDao(chargePointActionStatusDao);
		handler.setConnectorIdExtractor(new ConnectorKeyExtractor());
		handler.setInstructionTopic(OcppInstructionUtils.OCPP_V201_TOPIC);
		return handler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		WebSocketHandlerRegistration basicAuthReg = registry
				.addHandler(ocppWebSocketHandler_v201(), "/ocpp/v201/**").setAllowedOrigins("*");

		CentralOcppWebSocketHandshakeInterceptor basicAuthInterceptor = new CentralOcppWebSocketHandshakeInterceptor(
				ocppSystemUserDao, passwordEncoder);
		basicAuthInterceptor.setClientIdUriPattern(Pattern.compile("/ocpp/v201/(.*)"));
		basicAuthInterceptor.setUserEventAppenderBiz(userEventAppenderBiz);
		basicAuthReg.addInterceptors(basicAuthInterceptor);

		WebSocketHandlerRegistration pathAuthReg = registry
				.addHandler(ocppWebSocketHandler_v201(), "/ocpp/v201u/**").setAllowedOrigins("*");

		CentralOcppWebSocketHandshakeInterceptor pathAuthInterceptor = new CentralOcppWebSocketHandshakeInterceptor(
				ocppSystemUserDao, passwordEncoder);
		pathAuthInterceptor.setClientIdUriPattern(Pattern.compile("/ocpp/v201u/.*/.*/(.*)"));
		pathAuthInterceptor.setUserEventAppenderBiz(userEventAppenderBiz);
		pathAuthInterceptor.setClientCredentialsExtractor(CentralOcppWebSocketHandshakeInterceptor
				.pathCredentialsExtractor("/ocpp/v201u/(.*)/(.*)/.*"));
		pathAuthReg.addInterceptors(pathAuthInterceptor);
	}

}
