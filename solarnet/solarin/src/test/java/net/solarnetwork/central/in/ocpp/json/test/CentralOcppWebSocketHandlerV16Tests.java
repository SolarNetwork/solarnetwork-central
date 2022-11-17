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
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import net.solarnetwork.central.ApplicationMetadata;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.in.ocpp.json.CentralOcppWebSocketHandler;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusDao;
import net.solarnetwork.central.ocpp.domain.CentralOcppUserEvents;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.codec.ObjectMapperFactoryBean;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.service.SimpleActionMessageQueue;
import net.solarnetwork.ocpp.web.json.OcppWebSocketHandshakeInterceptor;
import net.solarnetwork.test.CallingThreadExecutorService;
import ocpp.v16.CentralSystemAction;
import ocpp.v16.ChargePointAction;
import ocpp.v16.ErrorCodeResolver;
import ocpp.v16.cp.json.ChargePointActionPayloadDecoder;
import ocpp.v16.cs.json.CentralServiceActionPayloadDecoder;

/**
 * Test cases for the {@link CentralOcppWebSocketHandler} class.
 * 
 * @author matt
 * @version 1.0
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
		factory.setModules(Arrays.asList(new JaxbAnnotationModule()));
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
		handler.setApplicationMetadata(APP_META);
	}

	@Test
	public void connect_updateStatus() throws Exception {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final ChargePointIdentity cpIdentity = new ChargePointIdentity(UUID.randomUUID().toString(),
				userId);
		Map<String, Object> sessionAttributes = Map.of(OcppWebSocketHandshakeInterceptor.CLIENT_ID_ATTR,
				cpIdentity);
		given(session.getAttributes()).willReturn(sessionAttributes);

		// WHEN
		handler.afterConnectionEstablished(session);

		// THEN
		then(chargePointStatusDao).should().updateConnectionStatus(eq(userId),
				eq(cpIdentity.getIdentifier()), eq(APP_META.getInstanceId()), dateCaptor.capture());
		assertThat("Status data is close to now",
				System.currentTimeMillis() - dateCaptor.getValue().toEpochMilli(),
				is(lessThanOrEqualTo(SECONDS.toMillis(2))));

		then(userEventAppenderBiz).should().addEvent(eq(userId), logEventCaptor.capture());
		LogEventInfo event = logEventCaptor.getValue();
		assertThat("Event tags", event.getTags(), is(arrayContaining("ocpp", "charger", "connected")));
		Map<String, Object> data = JsonUtils.getStringMap(event.getData());
		assertThat("Charger identifier included", data,
				hasEntry(CentralOcppUserEvents.CHARGE_POINT_DATA_KEY, cpIdentity.getIdentifier()));

		assertThat("Charger is avaialble", handler.availableChargePointsIds(), contains(cpIdentity));
	}

	@Test
	public void disconnect_updateStatus() throws Exception {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final ChargePointIdentity cpIdentity = new ChargePointIdentity(UUID.randomUUID().toString(),
				userId);
		Map<String, Object> sessionAttributes = Map.of(OcppWebSocketHandshakeInterceptor.CLIENT_ID_ATTR,
				cpIdentity);
		given(session.getAttributes()).willReturn(sessionAttributes);

		// WHEN
		handler.afterConnectionClosed(session, CloseStatus.NORMAL);

		// THEN
		then(chargePointStatusDao).should().updateConnectionStatus(eq(userId),
				eq(cpIdentity.getIdentifier()), eq(APP_META.getInstanceId()), isNull());

		then(userEventAppenderBiz).should().addEvent(eq(userId), logEventCaptor.capture());
		LogEventInfo event = logEventCaptor.getValue();
		assertThat("Event tags", event.getTags(),
				is(arrayContaining("ocpp", "charger", "disconnected")));
		Map<String, Object> data = JsonUtils.getStringMap(event.getData());
		assertThat("Charger identifier included", data,
				hasEntry(CentralOcppUserEvents.CHARGE_POINT_DATA_KEY, cpIdentity.getIdentifier()));
	}

}
