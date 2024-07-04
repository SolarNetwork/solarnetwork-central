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
import java.time.Clock;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;
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
import net.solarnetwork.central.in.ocpp.json.CentralOcppNodeInstructionProvider;
import net.solarnetwork.central.in.ocpp.json.CentralOcppWebSocketHandler;
import net.solarnetwork.central.in.ocpp.json.CentralOcppWebSocketHandshakeInterceptor;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.central.ocpp.dao.ChargePointActionStatusUpdateDao;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusDao;
import net.solarnetwork.central.ocpp.dao.UserSettingsDao;
import net.solarnetwork.central.ocpp.domain.OcppAppEvents;
import net.solarnetwork.central.ocpp.util.OcppInstructionUtils;
import net.solarnetwork.central.ocpp.v201.service.ConnectorKeyExtractor;
import net.solarnetwork.central.support.DelayQueueSet;
import net.solarnetwork.event.AppEventHandlerRegistrar;
import net.solarnetwork.event.AppEventPublisher;
import net.solarnetwork.ocpp.json.ActionPayloadDecoder;
import net.solarnetwork.ocpp.json.WebSocketSubProtocol;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.service.ChargePointBroker;
import net.solarnetwork.ocpp.service.SimpleActionMessageQueue;
import net.solarnetwork.ocpp.v201.domain.Action;
import net.solarnetwork.ocpp.v201.service.ErrorCodeResolver;
import net.solarnetwork.service.PasswordEncoder;
import net.solarnetwork.util.StatTracker;

/**
 * OCPP v2.0.1 web socket configuration.
 * 
 * @author matt
 * @version 1.4
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
	private ChargePointActionStatusUpdateDao chargePointActionStatusUpdateDao;

	@Autowired
	private UserSettingsDao userSettingsDao;

	@Autowired
	private AppEventHandlerRegistrar eventHandlerRegistrar;

	@Autowired
	private AppEventPublisher eventPublisher;

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
		handler.setChargePointActionStatusUpdateDao(chargePointActionStatusUpdateDao);
		handler.setConnectorIdExtractor(new ConnectorKeyExtractor());
		handler.setInstructionTopic(OcppInstructionUtils.OCPP_V201_TOPIC);
		handler.setEventPublisher(eventPublisher);
		return handler;
	}

	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	@Qualifier(OCPP_V201)
	public CentralOcppNodeInstructionProvider ocppNodeInstructionProvider_v201(
			@Qualifier(OCPP_V201) ChargePointBroker broker) {
		final var stats = new StatTracker("OCPP Instruction Provider v201",
				"net.solarnetwork.central.ocpp.stats.OcppInstructionProvider_v201", LoggerFactory
						.getLogger("net.solarnetwork.central.ocpp.stats.OcppInstructionProvider_v201"),
				500);

		CentralOcppNodeInstructionProvider provider = new CentralOcppNodeInstructionProvider(
				Clock.systemUTC(), stats, taskScheduler, new DelayQueueSet<>(), (name) -> {
					for ( Action action : Action.values() ) {
						if ( name.equals(action.getName()) ) {
							return action;
						}
					}
					return null;
				}, objectMapper, actionPayloadDecoder, broker, ocppCentralChargePointDao,
				nodeInstructionDao);
		provider.setInstructionTopic(OcppInstructionUtils.OCPP_V201_TOPIC);

		eventHandlerRegistrar.registerEventHandler(provider,
				OcppAppEvents.EVENT_TOPIC_CHARGE_POINT_CONNECTED,
				OcppAppEvents.EVENT_TOPIC_CHARGE_POINT_DISCONNECTED);

		return provider;
	}

	/** Client ID pattern when Basic authentication used. */
	public static final Pattern BASIC_CLIENT_ID_REGEX = Pattern.compile("/ocpp/v201/(.*)");

	/** Client ID pattern when path authentication used. */
	public static final Pattern PATH_CLIENT_ID_REGEX = Pattern.compile("/ocpp/v201u/.*/.*/(.*)");

	/** Credentials pattern when path authentication used. */
	public static final Pattern PATH_CREDS_REGEX = Pattern.compile("/ocpp/v201u/(.*)/(.*)/.*");

	/** Client ID pattern when Basic authentication used with HID. */
	public static final Pattern HID_BASIC_CLIENT_ID_REGEX = Pattern.compile("/ocpp/v201h/.*/(.*)");

	/** HID pattern when Basic authentication used. */
	public static final Pattern HID_BASIC_HID_REGEX = Pattern.compile("/ocpp/v201h/(.*)/.*");

	/** Client ID pattern when path authentication used with HID. */
	public static final Pattern HID_PATH_CLIENT_ID_REGEX = Pattern.compile("/ocpp/v201hu/.*/.*/.*/(.*)");

	/** Credentials pattern when path authentication used with HID. */
	public static final Pattern HID_PATH_CREDS_REGEX = Pattern.compile("/ocpp/v201hu/.*/(.*)/(.*)/.*");

	/** HID pattern when path authentication used. */
	public static final Pattern HID_PATH_HID_REGEX = Pattern.compile("/ocpp/v201hu/(.*)/.*/.*/.*");

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		// normal path /v201/identifier
		WebSocketHandlerRegistration basicAuthReg = registry
				.addHandler(ocppWebSocketHandler_v201(), "/ocpp/v201/**").setAllowedOrigins("*");

		CentralOcppWebSocketHandshakeInterceptor basicAuthInterceptor = new CentralOcppWebSocketHandshakeInterceptor(
				ocppSystemUserDao, passwordEncoder, userSettingsDao);
		basicAuthInterceptor.setClientIdUriPattern(BASIC_CLIENT_ID_REGEX);
		basicAuthInterceptor.setUserEventAppenderBiz(userEventAppenderBiz);
		basicAuthReg.addInterceptors(basicAuthInterceptor);

		// support path credentials /v201u/username/password/identifier
		WebSocketHandlerRegistration pathAuthReg = registry
				.addHandler(ocppWebSocketHandler_v201(), "/ocpp/v201u/**").setAllowedOrigins("*");

		CentralOcppWebSocketHandshakeInterceptor pathAuthInterceptor = new CentralOcppWebSocketHandshakeInterceptor(
				ocppSystemUserDao, passwordEncoder, userSettingsDao, PATH_CREDS_REGEX);
		pathAuthInterceptor.setClientIdUriPattern(PATH_CLIENT_ID_REGEX);
		pathAuthInterceptor.setUserEventAppenderBiz(userEventAppenderBiz);
		pathAuthReg.addInterceptors(pathAuthInterceptor);

		// support HID /v201h/hid/identifier
		WebSocketHandlerRegistration basicAuthRegHid = registry
				.addHandler(ocppWebSocketHandler_v201(), "/ocpp/v201h/**").setAllowedOrigins("*");

		CentralOcppWebSocketHandshakeInterceptor basicAuthInterceptorHid = new CentralOcppWebSocketHandshakeInterceptor(
				ocppSystemUserDao, passwordEncoder, userSettingsDao, null, HID_BASIC_HID_REGEX);
		basicAuthInterceptorHid.setClientIdUriPattern(HID_BASIC_CLIENT_ID_REGEX);
		basicAuthInterceptorHid.setUserEventAppenderBiz(userEventAppenderBiz);
		basicAuthRegHid.addInterceptors(basicAuthInterceptorHid);

		// support HID + path credentials /v201hu/hid/username/password/identifier
		WebSocketHandlerRegistration pathAuthRegHid = registry
				.addHandler(ocppWebSocketHandler_v201(), "/ocpp/v201hu/**").setAllowedOrigins("*");

		CentralOcppWebSocketHandshakeInterceptor pathAuthInterceptorHid = new CentralOcppWebSocketHandshakeInterceptor(
				ocppSystemUserDao, passwordEncoder, userSettingsDao, HID_PATH_CREDS_REGEX,
				HID_PATH_HID_REGEX);
		pathAuthInterceptorHid.setClientIdUriPattern(HID_PATH_CLIENT_ID_REGEX);
		pathAuthInterceptorHid.setUserEventAppenderBiz(userEventAppenderBiz);
		pathAuthRegHid.addInterceptors(pathAuthInterceptorHid);
	}

}
