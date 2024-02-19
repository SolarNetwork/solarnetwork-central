/* ==================================================================
 * CentralOcppWebSocketHandlerV16Tests.java - 17/11/2022 2:58:51 pm
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

package net.solarnetwork.central.in.ocpp.json.test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import net.solarnetwork.central.ApplicationMetadata;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.in.ocpp.json.CentralOcppWebSocketHandler;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.ChargePointActionStatusDao;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusDao;
import net.solarnetwork.central.ocpp.domain.CentralOcppUserEvents;
import net.solarnetwork.central.ocpp.util.OcppInstructionUtils;
import net.solarnetwork.central.ocpp.v16.util.ConnectorIdExtractor;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.codec.ObjectMapperFactoryBean;
import net.solarnetwork.ocpp.domain.Action;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.service.SimpleActionMessageQueue;
import net.solarnetwork.ocpp.v16.jakarta.CentralSystemAction;
import net.solarnetwork.ocpp.v16.jakarta.ChargePointAction;
import net.solarnetwork.ocpp.v16.jakarta.ErrorCodeResolver;
import net.solarnetwork.ocpp.v16.jakarta.cp.json.ChargePointActionPayloadDecoder;
import net.solarnetwork.ocpp.v16.jakarta.cs.json.CentralServiceActionPayloadDecoder;
import net.solarnetwork.ocpp.web.jakarta.json.OcppWebSocketHandshakeInterceptor;
import net.solarnetwork.test.CallingThreadExecutorService;
import ocpp.v16.jakarta.cs.ChargePointErrorCode;
import ocpp.v16.jakarta.cs.ChargePointStatus;
import ocpp.v16.jakarta.cs.StatusNotificationRequest;

/**
 * Test cases for the {@link CentralOcppWebSocketHandler} class.
 * 
 * @author matt
 * @version 1.2
 */
@ExtendWith(MockitoExtension.class)
public class CentralOcppWebSocketHandlerV16Tests {

	private final ApplicationMetadata APP_META = new ApplicationMetadata("Test", "X.Y.Z",
			UUID.randomUUID().toString());

	@Mock
	private CentralChargePointDao centralChargePointDao;

	@Mock
	private NodeInstructionDao nodeInstructionDao;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private ChargePointStatusDao chargePointStatusDao;

	@Mock
	private ChargePointActionStatusDao chargePointActionStatusDao;

	@Mock
	private WebSocketSession session;

	@Captor
	private ArgumentCaptor<Instant> dateCaptor;

	@Captor
	private ArgumentCaptor<LogEventInfo> logEventCaptor;

	private ObjectMapper mapper;
	private CentralOcppWebSocketHandler<ChargePointAction, CentralSystemAction> handler;

	@BeforeEach
	public void setup() throws Exception {
		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		factory.setFeaturesToDisable(Arrays.asList(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
		factory.setModules(Arrays.asList(new JakartaXmlBindAnnotationModule()));
		mapper = factory.getObject();

		handler = new CentralOcppWebSocketHandler<>(ChargePointAction.class, CentralSystemAction.class,
				new ErrorCodeResolver(), new TaskExecutorAdapter(new CallingThreadExecutorService()),
				mapper, new SimpleActionMessageQueue(), new CentralServiceActionPayloadDecoder(mapper),
				new ChargePointActionPayloadDecoder(mapper));
		//handler.setTaskScheduler(taskScheduler);
		handler.setChargePointDao(centralChargePointDao);
		handler.setInstructionDao(nodeInstructionDao);
		handler.setUserEventAppenderBiz(userEventAppenderBiz);
		handler.setChargePointStatusDao(chargePointStatusDao);
		handler.setChargePointActionStatusDao(chargePointActionStatusDao);
		handler.setConnectorIdExtractor(new ConnectorIdExtractor());
		handler.setApplicationMetadata(APP_META);
		handler.setInstructionTopic(OcppInstructionUtils.OCPP_V16_TOPIC);
	}

	@Test
	public void connect_updateStatus() throws Exception {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final ChargePointIdentity cpIdentity = new ChargePointIdentity(UUID.randomUUID().toString(),
				userId);
		final String sessionId = UUID.randomUUID().toString();
		given(session.getId()).willReturn(sessionId);
		Map<String, Object> sessionAttributes = Map.of(OcppWebSocketHandshakeInterceptor.CLIENT_ID_ATTR,
				cpIdentity);
		given(session.getAttributes()).willReturn(sessionAttributes);

		// WHEN
		handler.afterConnectionEstablished(session);

		// THEN
		then(chargePointStatusDao).should().updateConnectionStatus(eq(userId),
				eq(cpIdentity.getIdentifier()), eq(APP_META.getInstanceId()), eq(sessionId),
				dateCaptor.capture());
		assertThat("Status data is close to now",
				System.currentTimeMillis() - dateCaptor.getValue().toEpochMilli(),
				is(lessThanOrEqualTo(SECONDS.toMillis(2))));

		then(userEventAppenderBiz).should().addEvent(eq(userId), logEventCaptor.capture());
		LogEventInfo event = logEventCaptor.getValue();
		assertThat("Event tags", event.getTags(), is(arrayContaining("ocpp", "charger", "connected")));
		Map<String, Object> data = JsonUtils.getStringMap(event.getData());
		assertThat("Charger identifier included", data,
				hasEntry(CentralOcppUserEvents.CHARGE_POINT_DATA_KEY, cpIdentity.getIdentifier()));
		assertThat("Session ID included", data,
				hasEntry(CentralOcppUserEvents.SESSION_ID_DATA_KEY, sessionId));

		assertThat("Charger is avaialble", handler.availableChargePointsIds(), contains(cpIdentity));
	}

	@Test
	public void disconnect_updateStatus() throws Exception {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final ChargePointIdentity cpIdentity = new ChargePointIdentity(UUID.randomUUID().toString(),
				userId);
		final String sessionId = UUID.randomUUID().toString();
		given(session.getId()).willReturn(sessionId);
		Map<String, Object> sessionAttributes = Map.of(OcppWebSocketHandshakeInterceptor.CLIENT_ID_ATTR,
				cpIdentity);
		given(session.getAttributes()).willReturn(sessionAttributes);

		// WHEN
		handler.afterConnectionClosed(session, CloseStatus.NORMAL);

		// THEN
		then(chargePointStatusDao).should().updateConnectionStatus(eq(userId),
				eq(cpIdentity.getIdentifier()), eq(APP_META.getInstanceId()), eq(sessionId), isNull());

		then(userEventAppenderBiz).should().addEvent(eq(userId), logEventCaptor.capture());
		LogEventInfo event = logEventCaptor.getValue();
		assertThat("Event tags", event.getTags(),
				is(arrayContaining("ocpp", "charger", "disconnected")));
		Map<String, Object> data = JsonUtils.getStringMap(event.getData());
		assertThat("Charger identifier included", data,
				hasEntry(CentralOcppUserEvents.CHARGE_POINT_DATA_KEY, cpIdentity.getIdentifier()));
		assertThat("Session ID included", data,
				hasEntry(CentralOcppUserEvents.SESSION_ID_DATA_KEY, sessionId));
	}

	private Object[] call(String messageId, Action action, Object payload) {
		if ( payload != null ) {
			return new Object[] { 2, messageId, action.getName(), payload };
		}
		return new Object[] { 2, messageId, action.getName() };
	}

	@Test
	public void handle_StatusNotificationRequest() throws Exception {
		// GIVEN
		final var userId = UUID.randomUUID().getMostSignificantBits();
		final var cpIdentity = new ChargePointIdentity(UUID.randomUUID().toString(), userId);
		final String sessionId = UUID.randomUUID().toString();
		given(session.getId()).willReturn(sessionId);
		Map<String, Object> sessionAttributes = Map.of(OcppWebSocketHandshakeInterceptor.CLIENT_ID_ATTR,
				cpIdentity);
		given(session.getAttributes()).willReturn(sessionAttributes);

		// WHEN
		handler.afterConnectionEstablished(session);

		var req = new StatusNotificationRequest();
		req.setConnectorId(1);
		req.setErrorCode(ChargePointErrorCode.NO_ERROR);
		req.setInfo("Hi!");
		req.setStatus(ChargePointStatus.AVAILABLE);
		var messageId = UUID.randomUUID().toString();
		var call = call(messageId, CentralSystemAction.StatusNotification, req);
		var msg = new TextMessage(mapper.writeValueAsString(call));
		handler.handleMessage(session, msg);

		// THEN
		then(chargePointActionStatusDao).should().updateActionTimestamp(eq(userId),
				eq(cpIdentity.getIdentifier()), eq(0), eq(req.getConnectorId()),
				eq("StatusNotification"), eq(messageId), dateCaptor.capture());

		// 3 events: connected, received, sent(error)
		then(userEventAppenderBiz).should(times(3)).addEvent(eq(userId), logEventCaptor.capture());
		LogEventInfo event = logEventCaptor.getAllValues().get(1);
		assertThat("Event tags", event.getTags(), is(arrayContaining("ocpp", "message", "received")));
		Map<String, Object> data = JsonUtils.getStringMap(event.getData());
		assertThat("Charger identifier included", data,
				hasEntry(CentralOcppUserEvents.CHARGE_POINT_DATA_KEY, cpIdentity.getIdentifier()));
		assertThat("Message ID included", data,
				hasEntry(CentralOcppUserEvents.MESSAGE_ID_DATA_KEY, messageId));
		assertThat("Action included", data, hasEntry(CentralOcppUserEvents.ACTION_DATA_KEY,
				CentralSystemAction.StatusNotification.getName()));
		assertThat("Message payload included", data, hasEntry(CentralOcppUserEvents.MESSAGE_DATA_KEY,
				JsonUtils.getStringMap(mapper.writeValueAsString(req))));
	}

}
