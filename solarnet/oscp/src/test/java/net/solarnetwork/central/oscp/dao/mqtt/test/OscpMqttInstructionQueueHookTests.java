/* ==================================================================
 * OscpMqttInstructionQueueHookTests.java - 8/10/2022 5:53:23 pm
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

import static java.util.concurrent.CompletableFuture.completedFuture;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_ACTION_PARAM;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_CAPACITY_OPTIMIZER_ID_PARAM;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_MESSAGE_PARAM;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_V20_TOPIC;
import static net.solarnetwork.codec.JsonUtils.getStringMap;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Declined;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Queuing;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Unknown;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.OscpUserEvents;
import net.solarnetwork.central.oscp.mqtt.OscpMqttCountStat;
import net.solarnetwork.central.oscp.mqtt.OscpMqttInstructionQueueHook;
import net.solarnetwork.central.oscp.mqtt.OscpMqttInstructions;
import net.solarnetwork.central.oscp.util.OscpUtils;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttStats;
import oscp.v20.AdjustGroupCapacityForecast;
import oscp.v20.GroupCapacityComplianceError;

/**
 * Test cases for the {@link OscpMqttInstructionQueueHook} class.
 * 
 * @author matt
 * @version 1.1
 */
@ExtendWith(MockitoExtension.class)
public class OscpMqttInstructionQueueHookTests implements OscpMqttInstructions, OscpUserEvents {

	private static final Long TEST_NODE_ID = UUID.randomUUID().getMostSignificantBits();
	private static final Long TEST_LOC_ID = UUID.randomUUID().getMostSignificantBits();
	private static final Long TEST_USER_ID = UUID.randomUUID().getMostSignificantBits();
	private static final Long TEST_CO_ID = UUID.randomUUID().getMostSignificantBits();
	private static final Long TEST_CP_ID = UUID.randomUUID().getMostSignificantBits();
	private static final String TEST_CG_IDENT = UUID.randomUUID().toString();

	private static final Logger log = LoggerFactory.getLogger(OscpMqttInstructionQueueHookTests.class);

	@Mock
	private UserNodeDao userNodeDao;

	@Mock
	private CapacityGroupConfigurationDao capacityGroupDao;

	@Mock
	private CapacityOptimizerConfigurationDao capacityOptimizerDao;

	@Mock
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private MqttConnection conn;

	@Captor
	private ArgumentCaptor<LogEventInfo> eventCaptor;

	@Captor
	private ArgumentCaptor<MqttMessage> msgCaptor;

	private ObjectMapper mapper;
	private OscpMqttInstructionQueueHook hook;

	@BeforeEach
	public void setup() {
		mapper = JsonUtils.newObjectMapper();
		hook = new OscpMqttInstructionQueueHook(
				new MqttStats("SolarOSCP-MQTT", 1, OscpMqttCountStat.values()), mapper, userNodeDao,
				capacityGroupDao, capacityOptimizerDao, capacityProviderDao);
		hook.setUserEventAppenderBiz(userEventAppenderBiz);
		hook.setJsonSchemaFactory(OscpUtils.oscpSchemaFactory_v20());
	}

	@Test
	public void unknownTopic() throws IOException {
		// GIVEN
		final NodeInstruction instruction = new NodeInstruction("something/else", Instant.now(),
				TEST_NODE_ID);
		instruction.setState(Unknown);

		// WHEN
		hook.onMqttServerConnectionEstablished(conn, false);
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);

		// THEN
		then(userEventAppenderBiz).shouldHaveNoInteractions();
		then(conn).shouldHaveNoInteractions();

		assertThat("Result is same instance", result, is(sameInstance(instruction)));
		assertThat("Instruction status unchanged for unknown topic", result.getState(),
				is(equalTo(Unknown)));
	}

	@Test
	public void unknownCapacityOptimizer() throws IOException {
		// GIVEN
		final String msgJson = """
				{
					"message":"Oops!"
				}
				""";
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		final NodeInstruction instruction = new NodeInstruction(OSCP_V20_TOPIC, Instant.now(),
				TEST_NODE_ID);
		// @formatter:off
		instruction.setParams(Map.of(
				OSCP_ACTION_PARAM, action,
				OSCP_CAPACITY_OPTIMIZER_ID_PARAM, TEST_CO_ID.toString(),
				OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM, TEST_CG_IDENT,
				OSCP_MESSAGE_PARAM, msgJson));
		// @formatter:on
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "user@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		given(userNodeDao.get(TEST_NODE_ID)).willReturn(userNode);

		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(null);

		// WHEN
		hook.onMqttServerConnectionEstablished(conn, false);
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);

		// THEN
		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_ERROR_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));

		then(conn).shouldHaveNoInteractions();

		assertThat("Result is same instance", result, is(sameInstance(instruction)));
		assertThat("Instruction status updated for error", result.getState(), is(equalTo(Declined)));
	}

	@Test
	public void disabledCapacityOptimizer() throws IOException {
		// GIVEN
		final String msgJson = """
				{
					"message":"Oops!"
				}
				""";
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		final NodeInstruction instruction = new NodeInstruction(OSCP_V20_TOPIC, Instant.now(),
				TEST_NODE_ID);
		// @formatter:off
		instruction.setParams(Map.of(
				OSCP_ACTION_PARAM, action,
				OSCP_CAPACITY_OPTIMIZER_ID_PARAM, TEST_CO_ID.toString(),
				OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM, TEST_CG_IDENT,
				OSCP_MESSAGE_PARAM, msgJson));
		// @formatter:on
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "user@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		given(userNodeDao.get(TEST_NODE_ID)).willReturn(userNode);

		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(false);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		// WHEN
		hook.onMqttServerConnectionEstablished(conn, false);
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);

		// THEN
		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_ERROR_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));

		then(conn).shouldHaveNoInteractions();

		assertThat("Result is same instance", result, is(sameInstance(instruction)));
		assertThat("Instruction status updated for error", result.getState(), is(equalTo(Declined)));
	}

	@Test
	public void unknownCapacityGroup() throws IOException {
		// GIVEN
		final String msgJson = """
				{
					"message":"Oops!"
				}
				""";
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		final NodeInstruction instruction = new NodeInstruction(OSCP_V20_TOPIC, Instant.now(),
				TEST_NODE_ID);
		// @formatter:off
		instruction.setParams(Map.of(
				OSCP_ACTION_PARAM, action,
				OSCP_CAPACITY_OPTIMIZER_ID_PARAM, TEST_CO_ID.toString(),
				OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM, TEST_CG_IDENT,
				OSCP_MESSAGE_PARAM, msgJson));
		// @formatter:on
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "user@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		given(userNodeDao.get(TEST_NODE_ID)).willReturn(userNode);

		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(true);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		given(capacityGroupDao.findForCapacityOptimizer(TEST_USER_ID, TEST_CO_ID, TEST_CG_IDENT))
				.willReturn(null);

		// WHEN
		hook.onMqttServerConnectionEstablished(conn, false);
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);

		// THEN
		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_ERROR_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));

		then(conn).shouldHaveNoInteractions();

		assertThat("Result is same instance", result, is(sameInstance(instruction)));
		assertThat("Instruction status updated for error", result.getState(), is(equalTo(Declined)));
	}

	@Test
	public void disabledCapacityGroup() throws IOException {
		// GIVEN
		final String msgJson = """
				{
					"message":"Oops!"
				}
				""";
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		final NodeInstruction instruction = new NodeInstruction(OSCP_V20_TOPIC, Instant.now(),
				TEST_NODE_ID);
		// @formatter:off
		instruction.setParams(Map.of(
				OSCP_ACTION_PARAM, action,
				OSCP_CAPACITY_OPTIMIZER_ID_PARAM, TEST_CO_ID.toString(),
				OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM, TEST_CG_IDENT,
				OSCP_MESSAGE_PARAM, msgJson));
		// @formatter:on
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "user@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		given(userNodeDao.get(TEST_NODE_ID)).willReturn(userNode);

		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(true);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		CapacityGroupConfiguration group = new CapacityGroupConfiguration(TEST_USER_ID,
				UUID.randomUUID().getMostSignificantBits(), Instant.now());
		group.setIdentifier(TEST_CG_IDENT);
		group.setCapacityOptimizerId(TEST_CO_ID);
		group.setCapacityProviderId(TEST_CP_ID);
		group.setEnabled(false);
		given(capacityGroupDao.findForCapacityOptimizer(TEST_USER_ID, TEST_CO_ID, TEST_CG_IDENT))
				.willReturn(group);

		// WHEN
		hook.onMqttServerConnectionEstablished(conn, false);
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);

		// THEN
		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_ERROR_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));

		then(conn).shouldHaveNoInteractions();

		assertThat("Result is same instance", result, is(sameInstance(instruction)));
		assertThat("Instruction status updated for error", result.getState(), is(equalTo(Declined)));
	}

	@Test
	public void disabledCapacityProvider() throws IOException {
		// GIVEN
		final String msgJson = """
				{
					"message":"Oops!"
				}
				""";
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		final NodeInstruction instruction = new NodeInstruction(OSCP_V20_TOPIC, Instant.now(),
				TEST_NODE_ID);
		// @formatter:off
		instruction.setParams(Map.of(
				OSCP_ACTION_PARAM, action,
				OSCP_CAPACITY_OPTIMIZER_ID_PARAM, TEST_CO_ID.toString(),
				OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM, TEST_CG_IDENT,
				OSCP_MESSAGE_PARAM, msgJson));
		// @formatter:on
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "user@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		given(userNodeDao.get(TEST_NODE_ID)).willReturn(userNode);

		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(true);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		CapacityGroupConfiguration group = new CapacityGroupConfiguration(TEST_USER_ID,
				UUID.randomUUID().getMostSignificantBits(), Instant.now());
		group.setIdentifier(TEST_CG_IDENT);
		group.setCapacityOptimizerId(TEST_CO_ID);
		group.setCapacityProviderId(TEST_CP_ID);
		group.setEnabled(true);
		given(capacityGroupDao.findForCapacityOptimizer(TEST_USER_ID, TEST_CO_ID, TEST_CG_IDENT))
				.willReturn(group);

		CapacityProviderConfiguration provider = new CapacityProviderConfiguration(TEST_USER_ID,
				TEST_CP_ID, Instant.now());
		provider.setEnabled(false);
		given(capacityProviderDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CP_ID)))
				.willReturn(provider);

		// WHEN
		hook.onMqttServerConnectionEstablished(conn, false);
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);

		// THEN
		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_ERROR_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));

		then(conn).shouldHaveNoInteractions();

		assertThat("Result is same instance", result, is(sameInstance(instruction)));
		assertThat("Instruction status updated for error", result.getState(), is(equalTo(Declined)));
	}

	@Test
	public void invalidAction() throws IOException {
		// GIVEN
		final String msgJson = """
				{
					"message":"Oops!"
				}
				""";
		final String action = "NotSupported";
		final NodeInstruction instruction = new NodeInstruction(OSCP_V20_TOPIC, Instant.now(),
				TEST_NODE_ID);
		// @formatter:off
		instruction.setParams(Map.of(
				OSCP_ACTION_PARAM, action,
				OSCP_CAPACITY_OPTIMIZER_ID_PARAM, TEST_CO_ID.toString(),
				OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM, TEST_CG_IDENT,
				OSCP_MESSAGE_PARAM, msgJson));
		// @formatter:on
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "user@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		given(userNodeDao.get(TEST_NODE_ID)).willReturn(userNode);

		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(true);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		CapacityGroupConfiguration group = new CapacityGroupConfiguration(TEST_USER_ID,
				UUID.randomUUID().getMostSignificantBits(), Instant.now());
		group.setIdentifier(TEST_CG_IDENT);
		group.setCapacityOptimizerId(TEST_CO_ID);
		group.setCapacityProviderId(TEST_CP_ID);
		group.setEnabled(true);
		given(capacityGroupDao.findForCapacityOptimizer(TEST_USER_ID, TEST_CO_ID, TEST_CG_IDENT))
				.willReturn(group);

		CapacityProviderConfiguration provider = new CapacityProviderConfiguration(TEST_USER_ID,
				TEST_CP_ID, Instant.now());
		provider.setEnabled(true);
		given(capacityProviderDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CP_ID)))
				.willReturn(provider);

		// WHEN
		hook.onMqttServerConnectionEstablished(conn, false);
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);

		// THEN
		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_ERROR_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));

		then(conn).shouldHaveNoInteractions();

		assertThat("Result is same instance", result, is(sameInstance(instruction)));
		assertThat("Instruction status updated for error", result.getState(), is(equalTo(Declined)));
	}

	@Test
	public void invalidJson() throws IOException {
		// GIVEN
		final String msgJson = """
				{
					"message":"Oops!",
					"forecasted_blocks":[
						{
							"capacity"   : 123.456,
							"unit"       : "KW",
							"start_time" : "2022-10-08T18:00:00Z",
							"end_time"   : "2022-10-08T18:15:00Z"
						}
					]
				}
				""";
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		final NodeInstruction instruction = new NodeInstruction(OSCP_V20_TOPIC, Instant.now(),
				TEST_NODE_ID);
		// @formatter:off
		instruction.setParams(Map.of(
				OSCP_ACTION_PARAM, action,
				OSCP_CAPACITY_OPTIMIZER_ID_PARAM, TEST_CO_ID.toString(),
				OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM, TEST_CG_IDENT,
				OSCP_MESSAGE_PARAM, msgJson));
		// @formatter:on
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "user@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		given(userNodeDao.get(TEST_NODE_ID)).willReturn(userNode);

		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(true);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		CapacityGroupConfiguration group = new CapacityGroupConfiguration(TEST_USER_ID,
				UUID.randomUUID().getMostSignificantBits(), Instant.now());
		group.setIdentifier(TEST_CG_IDENT);
		group.setCapacityOptimizerId(TEST_CO_ID);
		group.setCapacityProviderId(TEST_CP_ID);
		group.setEnabled(true);
		given(capacityGroupDao.findForCapacityOptimizer(TEST_USER_ID, TEST_CO_ID, TEST_CG_IDENT))
				.willReturn(group);

		CapacityProviderConfiguration provider = new CapacityProviderConfiguration(TEST_USER_ID,
				TEST_CP_ID, Instant.now());
		provider.setEnabled(true);
		given(capacityProviderDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CP_ID)))
				.willReturn(provider);

		// WHEN
		hook.onMqttServerConnectionEstablished(conn, false);
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);

		// THEN
		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_ERROR_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));
		assertThat("Event data error message provided", eventData,
				hasEntry(equalTo(MESSAGE_DATA_KEY), instanceOf(String.class)));

		then(conn).shouldHaveNoInteractions();

		assertThat("Result is same instance", result, is(sameInstance(instruction)));
		assertThat("Instruction status updated for execution", result.getState(), is(equalTo(Declined)));
	}

	@Test
	public void processInstruction_GroupCapacityComplianceError() throws IOException {
		// GIVEN
		final String msgJson = """
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
		final String action = GroupCapacityComplianceError.class.getSimpleName();
		final NodeInstruction instruction = new NodeInstruction(OSCP_V20_TOPIC, Instant.now(),
				TEST_NODE_ID);
		// @formatter:off
		instruction.setParams(Map.of(
				OSCP_ACTION_PARAM, action,
				OSCP_CAPACITY_OPTIMIZER_ID_PARAM, TEST_CO_ID.toString(),
				OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM, TEST_CG_IDENT,
				OSCP_MESSAGE_PARAM, msgJson));
		// @formatter:on
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "user@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		given(userNodeDao.get(TEST_NODE_ID)).willReturn(userNode);

		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(true);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		CapacityGroupConfiguration group = new CapacityGroupConfiguration(TEST_USER_ID,
				UUID.randomUUID().getMostSignificantBits(), Instant.now());
		group.setIdentifier(TEST_CG_IDENT);
		group.setCapacityOptimizerId(TEST_CO_ID);
		group.setCapacityProviderId(TEST_CP_ID);
		group.setEnabled(true);
		given(capacityGroupDao.findForCapacityOptimizer(TEST_USER_ID, TEST_CO_ID, TEST_CG_IDENT))
				.willReturn(group);

		CapacityProviderConfiguration provider = new CapacityProviderConfiguration(TEST_USER_ID,
				TEST_CP_ID, Instant.now());
		provider.setEnabled(true);
		given(capacityProviderDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CP_ID)))
				.willReturn(provider);

		// publish to MQTT
		given(conn.isEstablished()).willReturn(true);
		given(conn.publish(any())).willReturn(completedFuture(null));

		// WHEN
		hook.onMqttServerConnectionEstablished(conn, false);
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);
		Long instructionId = UUID.randomUUID().getMostSignificantBits();
		hook.didQueueNodeInstruction(result, instructionId);

		// THEN
		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_IN_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(INSTRUCTION_ID_DATA_KEY, instructionId));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));
		assertThat("Event data content is JSON as Map", eventData,
				hasEntry(CONTENT_DATA_KEY, mapper.convertValue(
						mapper.readValue(msgJson, GroupCapacityComplianceError.class), Map.class)));

		then(conn).should().publish(msgCaptor.capture());
		MqttMessage msg = msgCaptor.getValue();
		assertThat("Mqtt message topic", msg.getTopic(), is(equalTo(MQTT_TOPIC_V20)));
		Map<String, Object> msgBody = getStringMap(new String(msg.getPayload(), StandardCharsets.UTF_8));
		assertThat("Mqtt message instruction ID", msgBody,
				hasEntry(INSTRUCTION_ID_PARAM, instructionId));
		assertThat("Mqtt message node ID", msgBody, hasEntry(NODE_ID_PARAM, TEST_NODE_ID));
		assertThat("Mqtt message user ID", msgBody, hasEntry(USER_ID_PARAM, TEST_USER_ID));
		assertThat("Mqtt message action", msgBody, hasEntry(OSCP_ACTION_PARAM, action));
		assertThat("Mqtt message capacity optimizer ID", msgBody,
				hasEntry(OSCP_CAPACITY_OPTIMIZER_ID_PARAM, TEST_CO_ID));
		assertThat("Mqtt message capacity group identifier", msgBody,
				hasEntry(OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM, TEST_CG_IDENT));

		@SuppressWarnings("unchecked")
		Map<String, Object> expectedMsgBody = mapper
				.convertValue(mapper.readValue(msgJson, GroupCapacityComplianceError.class), Map.class);
		assertThat("Mqtt message message content", msgBody,
				hasEntry(OSCP_MESSAGE_PARAM, expectedMsgBody));

		assertThat("Result is NOT same instance", result, is(not(sameInstance(instruction))));
		assertThat("Instruction status updated for queue", result.getState(), is(equalTo(Queuing)));
	}

	@Test
	public void processInstruction_AdjustGroupCapacityForecast() throws IOException {
		// GIVEN
		final String msgJson = """
				{
					"group_id":"%s",
					"type":"CONSUMPTION",
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
				""".formatted(TEST_CG_IDENT);
		final String action = AdjustGroupCapacityForecast.class.getSimpleName();
		final NodeInstruction instruction = new NodeInstruction(OSCP_V20_TOPIC, Instant.now(),
				TEST_NODE_ID);
		// @formatter:off
		instruction.setParams(Map.of(
				OSCP_ACTION_PARAM, action,
				OSCP_CAPACITY_OPTIMIZER_ID_PARAM, TEST_CO_ID.toString(),
				OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM, TEST_CG_IDENT,
				OSCP_MESSAGE_PARAM, msgJson));
		// @formatter:on
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "user@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		given(userNodeDao.get(TEST_NODE_ID)).willReturn(userNode);

		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(true);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		CapacityGroupConfiguration group = new CapacityGroupConfiguration(TEST_USER_ID,
				UUID.randomUUID().getMostSignificantBits(), Instant.now());
		group.setIdentifier(TEST_CG_IDENT);
		group.setCapacityOptimizerId(TEST_CO_ID);
		group.setCapacityProviderId(TEST_CP_ID);
		group.setEnabled(true);
		given(capacityGroupDao.findForCapacityOptimizer(TEST_USER_ID, TEST_CO_ID, TEST_CG_IDENT))
				.willReturn(group);

		CapacityProviderConfiguration provider = new CapacityProviderConfiguration(TEST_USER_ID,
				TEST_CP_ID, Instant.now());
		provider.setEnabled(true);
		given(capacityProviderDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CP_ID)))
				.willReturn(provider);

		// publish to MQTT
		given(conn.isEstablished()).willReturn(true);
		given(conn.publish(any())).willReturn(completedFuture(null));

		// WHEN
		hook.onMqttServerConnectionEstablished(conn, false);
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);
		Long instructionId = UUID.randomUUID().getMostSignificantBits();
		hook.didQueueNodeInstruction(result, instructionId);

		// THEN
		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		LogEventInfo event = eventCaptor.getValue();
		log.debug("Got event: {}", event);
		assertThat("Event tags", event.getTags(), is(arrayContaining(OSCP_INSTRUCTION_IN_TAGS)));
		Map<String, Object> eventData = JsonUtils.getStringMap(event.getData());
		assertThat("Event data action", eventData, hasEntry(ACTION_DATA_KEY, action));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(INSTRUCTION_ID_DATA_KEY, instructionId));
		assertThat("Event data capacity optimizer ID", eventData,
				hasEntry(CAPACITY_OPTIMIZER_ID_DATA_KEY, TEST_CO_ID));
		assertThat("Event data capacity group identifier", eventData,
				hasEntry(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, TEST_CG_IDENT));
		assertThat("Event data content is JSON as Map", eventData, hasEntry(CONTENT_DATA_KEY, mapper
				.convertValue(mapper.readValue(msgJson, AdjustGroupCapacityForecast.class), Map.class)));

		then(conn).should().publish(msgCaptor.capture());
		MqttMessage msg = msgCaptor.getValue();
		assertThat("Mqtt message topic", msg.getTopic(), is(equalTo(MQTT_TOPIC_V20)));
		Map<String, Object> msgBody = getStringMap(new String(msg.getPayload(), StandardCharsets.UTF_8));
		assertThat("Mqtt message instruction ID", msgBody,
				hasEntry(INSTRUCTION_ID_PARAM, instructionId));
		assertThat("Mqtt message node ID", msgBody, hasEntry(NODE_ID_PARAM, TEST_NODE_ID));
		assertThat("Mqtt message user ID", msgBody, hasEntry(USER_ID_PARAM, TEST_USER_ID));
		assertThat("Mqtt message action", msgBody, hasEntry(OSCP_ACTION_PARAM, action));
		assertThat("Mqtt message capacity optimizer ID", msgBody,
				hasEntry(OSCP_CAPACITY_OPTIMIZER_ID_PARAM, TEST_CO_ID));
		assertThat("Mqtt message capacity group identifier", msgBody,
				hasEntry(OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM, TEST_CG_IDENT));

		@SuppressWarnings("unchecked")
		Map<String, Object> expectedMsgBody = mapper
				.convertValue(mapper.readValue(msgJson, AdjustGroupCapacityForecast.class), Map.class);
		assertThat("Mqtt message message content", msgBody,
				hasEntry(OSCP_MESSAGE_PARAM, expectedMsgBody));

		assertThat("Result is NOT same instance", result, is(not(sameInstance(instruction))));
		assertThat("Instruction status updated for queue", result.getState(), is(equalTo(Queuing)));
	}

}
