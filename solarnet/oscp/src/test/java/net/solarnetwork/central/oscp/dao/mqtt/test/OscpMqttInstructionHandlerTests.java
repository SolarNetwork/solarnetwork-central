/* ==================================================================
 * OscpMqttInstructionHandlerTests.java - 9/10/2022 10:02:23 am
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

package net.solarnetwork.central.oscp.dao.mqtt.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Declined;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Executing;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Queuing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.OscpUserEvents;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.oscp.mqtt.OscpMqttCountStat;
import net.solarnetwork.central.oscp.mqtt.OscpMqttInstructionHandler;
import net.solarnetwork.central.oscp.mqtt.OscpMqttInstructions;
import net.solarnetwork.central.oscp.util.TaskContext;
import net.solarnetwork.central.oscp.web.OscpWebUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.MqttStats;
import net.solarnetwork.test.CallingThreadExecutorService;
import oscp.v20.GroupCapacityComplianceError;

/**
 * Test cases for the {@link OscpMqttInstructionHandler} class.
 * 
 * @author matt
 * @version 1.1
 */
@ExtendWith(MockitoExtension.class)
public class OscpMqttInstructionHandlerTests implements OscpMqttInstructions, OscpUserEvents {

	private static final Logger log = LoggerFactory.getLogger(OscpMqttInstructionHandlerTests.class);

	private static final Long TEST_NODE_ID = UUID.randomUUID().getMostSignificantBits();
	private static final Long TEST_USER_ID = UUID.randomUUID().getMostSignificantBits();
	private static final Long TEST_CO_ID = UUID.randomUUID().getMostSignificantBits();
	private static final Long TEST_CP_ID = UUID.randomUUID().getMostSignificantBits();
	private static final String TEST_CG_IDENT = UUID.randomUUID().toString();

	@Mock
	private NodeInstructionDao nodeInstructionDao;

	@Mock
	private CapacityGroupConfigurationDao capacityGroupDao;

	@Mock
	private CapacityOptimizerConfigurationDao capacityOptimizerDao;

	@Mock
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private ExternalSystemClient client;

	@Mock
	private MqttConnection conn;

	@Captor
	private ArgumentCaptor<LogEventInfo> eventCaptor;

	@Captor
	private ArgumentCaptor<Map<String, ?>> mapCaptor;

	@Captor
	private ArgumentCaptor<TaskContext<?>> taskContextCaptor;

	@Captor
	private ArgumentCaptor<Object> objectCaptor;

	private ObjectMapper mapper;
	private OscpMqttInstructionHandler handler;

	@BeforeEach
	public void setup() {
		mapper = JsonUtils.newObjectMapper();
		handler = new OscpMqttInstructionHandler(
				new MqttStats("SolarOSCP-MQTT", 1, OscpMqttCountStat.values()),
				new CallingThreadExecutorService(), mapper, nodeInstructionDao, capacityGroupDao,
				capacityOptimizerDao, capacityProviderDao, client);
		handler.setUserEventAppenderBiz(userEventAppenderBiz);
	}

	@Test
	public void subscribeWhenConnectionEstablished() {
		// GIVEN		
		given(conn.subscribe(MQTT_TOPIC_V20, MqttQos.AtLeastOnce, handler))
				.willReturn(completedFuture(null));

		// WHEN
		handler.onMqttServerConnectionEstablished(conn, false);
	}

	private String instructionJson(Long instructionId, String action, String msg) {
		var json = """
				{
					"id"            : %d,
					"nodeId"        : %d,
					"userId"        : %d,
					"action"        : "%s",
					"coId"          : %d,
					"cgIdentifier"  : "%s",
					"msg"           : %s
				}
				""";
		return json.formatted(instructionId, TEST_NODE_ID, TEST_USER_ID, action, TEST_CO_ID,
				TEST_CG_IDENT, msg);
	}

	private String instructionJson(Long instructionId, String action, String msg, String correlationId) {
		if ( correlationId == null ) {
			return instructionJson(instructionId, action, msg);
		}
		var json = """
				{
					"id"            : %d,
					"nodeId"        : %d,
					"userId"        : %d,
					"action"        : "%s",
					"coId"          : %d,
					"cgIdentifier"  : "%s",
					"msg"           : %s,
					"correlationId" : "%s"
				}
				""";
		return json.formatted(instructionId, TEST_NODE_ID, TEST_USER_ID, action, TEST_CO_ID,
				TEST_CG_IDENT, msg, correlationId);

	}

	@Test
	public void unknownTopic() throws IOException {
		// GIVEN
		String topic = "not.the.topic";

		// WHEN
		var msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce, null);
		handler.onMqttMessage(msg);

		// THEN
		then(nodeInstructionDao).shouldHaveNoInteractions();
		then(userEventAppenderBiz).shouldHaveNoInteractions();
		then(client).shouldHaveNoInteractions();
	}

	@Test
	public void notQueueing() throws IOException {
		// GIVEN
		final Long instructionId = UUID.randomUUID().getMostSignificantBits();
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		var json = instructionJson(instructionId, action, """
				{
					"message":"Oops!",
					"forecasted_blocks":[
						{
							"capacity"   : 123.456,
							"phase"      : "ALL",
							"unit"       : "KW",
							"start_time" : "2022-10-08T18:00:00Z",
							"end_time"   : "2022-10-08T18:15:00Z"
						}
					]
				}
				""");

		// claim instruction
		given(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, TEST_NODE_ID, Queuing,
				Executing, null)).willReturn(false);

		// WHEN
		var msg = new BasicMqttMessage(MQTT_TOPIC_V20, false, MqttQos.AtLeastOnce, json.getBytes(UTF_8));
		handler.onMqttMessage(msg);

		// THEN
		then(userEventAppenderBiz).shouldHaveNoInteractions();
		then(client).shouldHaveNoInteractions();
	}

	@Test
	public void handleMessage_unknownCapacityOptimizer() throws IOException {
		// GIVEN
		final Long instructionId = UUID.randomUUID().getMostSignificantBits();
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		var json = instructionJson(instructionId, action, """
				{
					"message":"Oops!",
					"forecasted_blocks":[
						{
							"capacity"   : 123.456,
							"phase"      : "ALL",
							"unit"       : "KW",
							"start_time" : "2022-10-08T18:00:00Z",
							"end_time"   : "2022-10-08T18:15:00Z"
						}
					]
				}
				""");

		// claim instruction
		given(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, TEST_NODE_ID, Queuing,
				Executing, null)).willReturn(true);

		// lookup capacity optimizer
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(null);

		// update final node instruction state
		given(nodeInstructionDao.compareAndUpdateInstructionState(eq(instructionId), eq(TEST_NODE_ID),
				eq(Executing), eq(Declined), anyMap())).willReturn(true);

		// WHEN
		var msg = new BasicMqttMessage(MQTT_TOPIC_V20, false, MqttQos.AtLeastOnce, json.getBytes(UTF_8));
		handler.onMqttMessage(msg);

		// THEN
		then(nodeInstructionDao).should().compareAndUpdateInstructionState(eq(instructionId),
				eq(TEST_NODE_ID), eq(Executing), eq(Declined), mapCaptor.capture());
		assertThat("Instruction updated with result error message", mapCaptor.getValue(),
				hasEntry(equalTo("error"), instanceOf(String.class)));

		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_ERROR_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data instruction ID", eventData,
				hasEntry(INSTRUCTION_ID_DATA_KEY, instructionId));
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));
	}

	@Test
	public void handleMessage_disabledCapacityOptimizer() throws IOException {
		// GIVEN
		final Long instructionId = UUID.randomUUID().getMostSignificantBits();
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		var json = instructionJson(instructionId, action, """
				{
					"message":"Oops!",
					"forecasted_blocks":[
						{
							"capacity"   : 123.456,
							"phase"      : "ALL",
							"unit"       : "KW",
							"start_time" : "2022-10-08T18:00:00Z",
							"end_time"   : "2022-10-08T18:15:00Z"
						}
					]
				}
				""");

		// claim instruction
		given(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, TEST_NODE_ID, Queuing,
				Executing, null)).willReturn(true);

		// lookup capacity optimizer
		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(false);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		// update final node instruction state
		given(nodeInstructionDao.compareAndUpdateInstructionState(eq(instructionId), eq(TEST_NODE_ID),
				eq(Executing), eq(Declined), anyMap())).willReturn(true);

		// WHEN
		var msg = new BasicMqttMessage(MQTT_TOPIC_V20, false, MqttQos.AtLeastOnce, json.getBytes(UTF_8));
		handler.onMqttMessage(msg);

		// THEN
		then(nodeInstructionDao).should().compareAndUpdateInstructionState(eq(instructionId),
				eq(TEST_NODE_ID), eq(Executing), eq(Declined), mapCaptor.capture());
		assertThat("Instruction updated with result error message", mapCaptor.getValue(),
				hasEntry(equalTo("error"), instanceOf(String.class)));

		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_ERROR_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data instruction ID", eventData,
				hasEntry(INSTRUCTION_ID_DATA_KEY, instructionId));
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));
	}

	@Test
	public void handleMessage_unknownCapacityGroup() throws IOException {
		// GIVEN
		final Long instructionId = UUID.randomUUID().getMostSignificantBits();
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		var json = instructionJson(instructionId, action, """
				{
					"message":"Oops!",
					"forecasted_blocks":[
						{
							"capacity"   : 123.456,
							"phase"      : "ALL",
							"unit"       : "KW",
							"start_time" : "2022-10-08T18:00:00Z",
							"end_time"   : "2022-10-08T18:15:00Z"
						}
					]
				}
				""");

		// claim instruction
		given(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, TEST_NODE_ID, Queuing,
				Executing, null)).willReturn(true);

		// lookup capacity optimizer
		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(true);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		// lookup capacity group
		given(capacityGroupDao.findForCapacityOptimizer(TEST_USER_ID, TEST_CO_ID, TEST_CG_IDENT))
				.willReturn(null);

		// update final node instruction state
		given(nodeInstructionDao.compareAndUpdateInstructionState(eq(instructionId), eq(TEST_NODE_ID),
				eq(Executing), eq(Declined), anyMap())).willReturn(true);

		// WHEN
		var msg = new BasicMqttMessage(MQTT_TOPIC_V20, false, MqttQos.AtLeastOnce, json.getBytes(UTF_8));
		handler.onMqttMessage(msg);

		// THEN
		then(nodeInstructionDao).should().compareAndUpdateInstructionState(eq(instructionId),
				eq(TEST_NODE_ID), eq(Executing), eq(Declined), mapCaptor.capture());
		assertThat("Instruction updated with result error message", mapCaptor.getValue(),
				hasEntry(equalTo("error"), instanceOf(String.class)));

		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_ERROR_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data instruction ID", eventData,
				hasEntry(INSTRUCTION_ID_DATA_KEY, instructionId));
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));
	}

	@Test
	public void handleMessage_disabledCapacityGroup() throws IOException {
		// GIVEN
		final Long instructionId = UUID.randomUUID().getMostSignificantBits();
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		var json = instructionJson(instructionId, action, """
				{
					"message":"Oops!",
					"forecasted_blocks":[
						{
							"capacity"   : 123.456,
							"phase"      : "ALL",
							"unit"       : "KW",
							"start_time" : "2022-10-08T18:00:00Z",
							"end_time"   : "2022-10-08T18:15:00Z"
						}
					]
				}
				""");

		// claim instruction
		given(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, TEST_NODE_ID, Queuing,
				Executing, null)).willReturn(true);

		// lookup capacity optimizer
		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(true);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		// lookup capacity group
		CapacityGroupConfiguration group = new CapacityGroupConfiguration(TEST_USER_ID,
				UUID.randomUUID().getMostSignificantBits(), Instant.now());
		group.setIdentifier(TEST_CG_IDENT);
		group.setCapacityOptimizerId(TEST_CO_ID);
		group.setCapacityProviderId(TEST_CP_ID);
		group.setEnabled(false);
		given(capacityGroupDao.findForCapacityOptimizer(TEST_USER_ID, TEST_CO_ID, TEST_CG_IDENT))
				.willReturn(group);

		// update final node instruction state
		given(nodeInstructionDao.compareAndUpdateInstructionState(eq(instructionId), eq(TEST_NODE_ID),
				eq(Executing), eq(Declined), anyMap())).willReturn(true);

		// WHEN
		var msg = new BasicMqttMessage(MQTT_TOPIC_V20, false, MqttQos.AtLeastOnce, json.getBytes(UTF_8));
		handler.onMqttMessage(msg);

		// THEN
		then(nodeInstructionDao).should().compareAndUpdateInstructionState(eq(instructionId),
				eq(TEST_NODE_ID), eq(Executing), eq(Declined), mapCaptor.capture());
		assertThat("Instruction updated with result error message", mapCaptor.getValue(),
				hasEntry(equalTo("error"), instanceOf(String.class)));

		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_ERROR_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data instruction ID", eventData,
				hasEntry(INSTRUCTION_ID_DATA_KEY, instructionId));
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));
	}

	@Test
	public void handleMessage_unknownCapacityProvider() throws IOException {
		// GIVEN
		final Long instructionId = UUID.randomUUID().getMostSignificantBits();
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		var json = instructionJson(instructionId, action, """
				{
					"message":"Oops!",
					"forecasted_blocks":[
						{
							"capacity"   : 123.456,
							"phase"      : "ALL",
							"unit"       : "KW",
							"start_time" : "2022-10-08T18:00:00Z",
							"end_time"   : "2022-10-08T18:15:00Z"
						}
					]
				}
				""");

		// claim instruction
		given(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, TEST_NODE_ID, Queuing,
				Executing, null)).willReturn(true);

		// lookup capacity optimizer
		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(true);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		// lookup capacity group
		CapacityGroupConfiguration group = new CapacityGroupConfiguration(TEST_USER_ID,
				UUID.randomUUID().getMostSignificantBits(), Instant.now());
		group.setIdentifier(TEST_CG_IDENT);
		group.setCapacityOptimizerId(TEST_CO_ID);
		group.setCapacityProviderId(TEST_CP_ID);
		group.setEnabled(true);
		given(capacityGroupDao.findForCapacityOptimizer(TEST_USER_ID, TEST_CO_ID, TEST_CG_IDENT))
				.willReturn(group);

		// lookup capacity provider
		given(capacityProviderDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CP_ID)))
				.willReturn(null);

		// update final node instruction state
		given(nodeInstructionDao.compareAndUpdateInstructionState(eq(instructionId), eq(TEST_NODE_ID),
				eq(Executing), eq(Declined), anyMap())).willReturn(true);

		// WHEN
		var msg = new BasicMqttMessage(MQTT_TOPIC_V20, false, MqttQos.AtLeastOnce, json.getBytes(UTF_8));
		handler.onMqttMessage(msg);

		// THEN
		then(nodeInstructionDao).should().compareAndUpdateInstructionState(eq(instructionId),
				eq(TEST_NODE_ID), eq(Executing), eq(Declined), mapCaptor.capture());
		assertThat("Instruction updated with result error message", mapCaptor.getValue(),
				hasEntry(equalTo("error"), instanceOf(String.class)));

		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_ERROR_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data instruction ID", eventData,
				hasEntry(INSTRUCTION_ID_DATA_KEY, instructionId));
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));
	}

	@Test
	public void handleMessage_disabledCapacityProvider() throws IOException {
		// GIVEN
		final Long instructionId = UUID.randomUUID().getMostSignificantBits();
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		var json = instructionJson(instructionId, action, """
				{
					"message":"Oops!",
					"forecasted_blocks":[
						{
							"capacity"   : 123.456,
							"phase"      : "ALL",
							"unit"       : "KW",
							"start_time" : "2022-10-08T18:00:00Z",
							"end_time"   : "2022-10-08T18:15:00Z"
						}
					]
				}
				""");

		// claim instruction
		given(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, TEST_NODE_ID, Queuing,
				Executing, null)).willReturn(true);

		// lookup capacity optimizer
		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(true);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		// lookup capacity group
		CapacityGroupConfiguration group = new CapacityGroupConfiguration(TEST_USER_ID,
				UUID.randomUUID().getMostSignificantBits(), Instant.now());
		group.setIdentifier(TEST_CG_IDENT);
		group.setCapacityOptimizerId(TEST_CO_ID);
		group.setCapacityProviderId(TEST_CP_ID);
		group.setEnabled(true);
		given(capacityGroupDao.findForCapacityOptimizer(TEST_USER_ID, TEST_CO_ID, TEST_CG_IDENT))
				.willReturn(group);

		// lookup capacity provider
		CapacityProviderConfiguration provider = new CapacityProviderConfiguration(TEST_USER_ID,
				TEST_CP_ID, Instant.now());
		provider.setEnabled(false);
		given(capacityProviderDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CP_ID)))
				.willReturn(provider);

		// update final node instruction state
		given(nodeInstructionDao.compareAndUpdateInstructionState(eq(instructionId), eq(TEST_NODE_ID),
				eq(Executing), eq(Declined), anyMap())).willReturn(true);

		// WHEN
		var msg = new BasicMqttMessage(MQTT_TOPIC_V20, false, MqttQos.AtLeastOnce, json.getBytes(UTF_8));
		handler.onMqttMessage(msg);

		// THEN
		then(nodeInstructionDao).should().compareAndUpdateInstructionState(eq(instructionId),
				eq(TEST_NODE_ID), eq(Executing), eq(Declined), mapCaptor.capture());
		assertThat("Instruction updated with result error message", mapCaptor.getValue(),
				hasEntry(equalTo("error"), instanceOf(String.class)));

		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_ERROR_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data instruction ID", eventData,
				hasEntry(INSTRUCTION_ID_DATA_KEY, instructionId));
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));
	}

	@Test
	public void handleMessage_invalidAction() throws IOException {
		// GIVEN
		final Long instructionId = UUID.randomUUID().getMostSignificantBits();
		final String action = "NotSupported";
		var json = instructionJson(instructionId, action, """
				{
					"message":"Oops!",
					"forecasted_blocks":[
						{
							"capacity"   : 123.456,
							"phase"      : "ALL",
							"unit"       : "KW",
							"start_time" : "2022-10-08T18:00:00Z",
							"end_time"   : "2022-10-08T18:15:00Z"
						}
					]
				}
				""");

		// claim instruction
		given(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, TEST_NODE_ID, Queuing,
				Executing, null)).willReturn(true);

		// lookup capacity optimizer
		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(true);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		// lookup capacity group
		CapacityGroupConfiguration group = new CapacityGroupConfiguration(TEST_USER_ID,
				UUID.randomUUID().getMostSignificantBits(), Instant.now());
		group.setIdentifier(TEST_CG_IDENT);
		group.setCapacityOptimizerId(TEST_CO_ID);
		group.setCapacityProviderId(TEST_CP_ID);
		group.setEnabled(true);
		given(capacityGroupDao.findForCapacityOptimizer(TEST_USER_ID, TEST_CO_ID, TEST_CG_IDENT))
				.willReturn(group);

		// lookup capacity provider
		CapacityProviderConfiguration provider = new CapacityProviderConfiguration(TEST_USER_ID,
				TEST_CP_ID, Instant.now());
		provider.setEnabled(true);
		given(capacityProviderDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CP_ID)))
				.willReturn(provider);

		// update final node instruction state
		given(nodeInstructionDao.compareAndUpdateInstructionState(eq(instructionId), eq(TEST_NODE_ID),
				eq(Executing), eq(Declined), anyMap())).willReturn(true);

		// WHEN
		var msg = new BasicMqttMessage(MQTT_TOPIC_V20, false, MqttQos.AtLeastOnce, json.getBytes(UTF_8));
		handler.onMqttMessage(msg);

		// THEN
		then(nodeInstructionDao).should().compareAndUpdateInstructionState(eq(instructionId),
				eq(TEST_NODE_ID), eq(Executing), eq(Declined), mapCaptor.capture());
		assertThat("Instruction updated with result error message", mapCaptor.getValue(),
				hasEntry(equalTo("error"), instanceOf(String.class)));

		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_ERROR_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data instruction ID", eventData,
				hasEntry(INSTRUCTION_ID_DATA_KEY, instructionId));
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));
	}

	@Test
	public void handleMessage() throws IOException {
		// GIVEN
		final Long instructionId = UUID.randomUUID().getMostSignificantBits();
		final String correlationId = UUID.randomUUID().toString();
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		var msgJson = """
					{
					"message":"Oops!",
					"forecasted_blocks":[
						{
							"capacity"   : 123.456,
							"phase"      : "ALL",
							"unit"       : "KW",
							"start_time" : "2022-10-08T18:00:00Z",
							"end_time"   : "2022-10-08T18:15:00Z"
						}
					]
				}
				""";
		var json = instructionJson(instructionId, action, msgJson, correlationId);

		// claim instruction
		given(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, TEST_NODE_ID, Queuing,
				Executing, null)).willReturn(true);

		// lookup capacity optimizer
		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(true);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		// lookup capacity group
		CapacityGroupConfiguration group = new CapacityGroupConfiguration(TEST_USER_ID,
				UUID.randomUUID().getMostSignificantBits(), Instant.now());
		group.setIdentifier(TEST_CG_IDENT);
		group.setCapacityOptimizerId(TEST_CO_ID);
		group.setCapacityProviderId(TEST_CP_ID);
		group.setEnabled(true);
		given(capacityGroupDao.findForCapacityOptimizer(TEST_USER_ID, TEST_CO_ID, TEST_CG_IDENT))
				.willReturn(group);

		// lookup capacity provider
		CapacityProviderConfiguration provider = new CapacityProviderConfiguration(TEST_USER_ID,
				TEST_CP_ID, Instant.now());
		provider.setEnabled(true);
		given(capacityProviderDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CP_ID)))
				.willReturn(provider);

		// update final node instruction state
		given(nodeInstructionDao.compareAndUpdateInstructionState(eq(instructionId), eq(TEST_NODE_ID),
				eq(Executing), eq(Completed), isNull())).willReturn(true);

		// WHEN
		var msg = new BasicMqttMessage(MQTT_TOPIC_V20, false, MqttQos.AtLeastOnce, json.getBytes(UTF_8));
		handler.onMqttMessage(msg);

		// THEN
		// posted message to external system (capacity provider)
		then(client).should().systemExchange(taskContextCaptor.capture(), eq(HttpMethod.POST), any(),
				objectCaptor.capture());
		TaskContext<?> ctx = taskContextCaptor.getValue();
		assertThat("Task context config is Capacity Provider from DAO", ctx.config(),
				is(sameInstance(provider)));
		assertThat("Task context role is Capacity Optimizer", ctx.role(),
				is(equalTo(OscpRole.CapacityOptimizer)));
		assertThat("Task parameters has correlationID", ctx.parameters(),
				hasEntry(OscpWebUtils.CORRELATION_ID_HEADER, correlationId));

		// completed instruction
		then(nodeInstructionDao).should().compareAndUpdateInstructionState(eq(instructionId),
				eq(TEST_NODE_ID), eq(Executing), eq(Completed), isNull());

		// user events published (one for "processing" and one for "sent")
		then(userEventAppenderBiz).should(times(2)).addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		List<LogEventInfo> events = eventCaptor.getAllValues();
		log.debug("Got events: {}", events);

		LogEventInfo event = events.get(0);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data instruction ID", eventData,
				hasEntry(INSTRUCTION_ID_DATA_KEY, instructionId));
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));
		assertThat("Event data content is JSON as Map", eventData,
				hasEntry(CONTENT_DATA_KEY, mapper.convertValue(
						mapper.readValue(msgJson, GroupCapacityComplianceError.class), Map.class)));
		assertThat("Event data correlation ID", eventData,
				hasEntry(CORRELATION_ID_DATA_KEY, correlationId));

		event = events.get(1);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_OUT_TAGS)));
		eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data instruction ID", eventData,
				hasEntry(INSTRUCTION_ID_DATA_KEY, instructionId));
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));
		assertThat("Event data content is JSON as Map", eventData,
				hasEntry(CONTENT_DATA_KEY, mapper.convertValue(
						mapper.readValue(msgJson, GroupCapacityComplianceError.class), Map.class)));
		assertThat("Event data correlation ID", eventData,
				hasEntry(CORRELATION_ID_DATA_KEY, correlationId));
	}

}
