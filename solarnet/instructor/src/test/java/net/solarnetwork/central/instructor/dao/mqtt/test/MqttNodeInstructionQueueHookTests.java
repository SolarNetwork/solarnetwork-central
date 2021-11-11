/* ==================================================================
 * MqttNodeInstructionQueueHookTests.java - 11/11/2021 3:57:43 PM
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

package net.solarnetwork.central.instructor.dao.mqtt.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.moquette.interception.messages.InterceptPublishMessage;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.dao.mqtt.MqttNodeInstructionQueueHook;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.netty.NettyMqttConnectionFactory;
import net.solarnetwork.test.CallingThreadExecutorService;
import net.solarnetwork.test.mqtt.MqttServerSupport;
import net.solarnetwork.test.mqtt.TestingInterceptHandler;

/**
 * Test cases for the {@link MqttNodeInstructionQueueHook} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttNodeInstructionQueueHookTests extends MqttServerSupport {

	private static final int MQTT_TIMEOUT = 10;
	private static final String TEST_CLIENT_ID = "solarnet.test";
	private static final Long TEST_NODE_ID = 123L;
	private static final String TEST_INSTRUCTION_TOPIC = "test.topic";

	private ObjectMapper objectMapper;
	private NodeInstructionDao nodeInstructionDao;
	private MqttNodeInstructionQueueHook service;

	@BeforeEach
	public void setup() throws Exception {
		setupMqttServer();

		objectMapper = JsonUtils.newDatumObjectMapper();
		nodeInstructionDao = EasyMock.createMock(NodeInstructionDao.class);

		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();

		NettyMqttConnectionFactory factory = new NettyMqttConnectionFactory(
				Executors.newCachedThreadPool(), scheduler);

		service = new MqttNodeInstructionQueueHook(factory, objectMapper,
				new CallingThreadExecutorService(), nodeInstructionDao);
		service.getMqttConfig().setClientId(TEST_CLIENT_ID);
		service.getMqttConfig().setServerUri(new URI("mqtt://localhost:" + getMqttServerPort()));
		Future<?> f = service.startup();
		f.get(MQTT_TIMEOUT, TimeUnit.SECONDS);
	}

	@Override
	@AfterEach
	public void teardown() {
		super.teardown();
		EasyMock.verify(nodeInstructionDao);
	}

	private void replayAll() {
		EasyMock.replay(nodeInstructionDao);
	}

	@Test
	public void willQueueNodeInstruction() {
		// GIVEN
		Instant now = Instant.now();
		NodeInstruction input = new NodeInstruction(TEST_INSTRUCTION_TOPIC, now, TEST_NODE_ID);
		input.setState(InstructionState.Queued);

		// WHEN
		NodeInstruction instr = service.willQueueNodeInstruction(input);

		replayAll();

		// THEN
		assertThat("Same instance", instr, sameInstance(input));
		assertThat("State changed", instr.getState(), equalTo(InstructionState.Queuing));
		assertThat("Topic unchanged", instr.getTopic(), equalTo(TEST_INSTRUCTION_TOPIC));
		assertThat("Node ID unchanged", instr.getNodeId(), equalTo(TEST_NODE_ID));
	}

	@Test
	public void didQueueNodeInstruction() throws Exception {
		// GIVEN
		Instant now = Instant.now();
		NodeInstruction input = new NodeInstruction(TEST_INSTRUCTION_TOPIC, now, TEST_NODE_ID);
		input.setState(InstructionState.Queuing);

		final Long instructionId = UUID.randomUUID().getMostSignificantBits();

		final TestingInterceptHandler session = getTestingInterceptHandler();

		replayAll();

		// WHEN
		service.didQueueNodeInstruction(input, instructionId);

		// sleep for a bit to allow background thread to process
		Thread.sleep(200);

		// stop server to flush messages
		stopMqttServer();

		// THEN
		List<InterceptPublishMessage> published = session.publishMessages;
		assertThat(published, hasSize(1));
		InterceptPublishMessage msg = session.publishMessages.get(0);
		assertThat(msg.getTopicName(), equalTo("node/" + TEST_NODE_ID + "/instr"));
		Map<String, Object> map = JsonUtils
				.getStringMapFromTree(objectMapper.readTree(session.publishPayloads.get(0).array()));
		assertThat("Message body", map.keySet(), containsInAnyOrder("instructions"));
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<Map<String, Object>> instructions = (List) map.get("instructions");
		assertThat("Instruction count", instructions, hasSize(1));
		assertThat("Instruction topic", instructions.get(0), hasEntry("topic", TEST_INSTRUCTION_TOPIC));
		assertThat("Instruction state", instructions.get(0),
				hasEntry("state", InstructionState.Queuing.toString()));
		assertThat("Instruction node ID", instructions.get(0),
				hasEntry("nodeId", TEST_NODE_ID.intValue()));
	}

	@Test
	public void didQueueNodeInstructionMqttNotConnected() throws Exception {
		// GIVEN
		Instant now = Instant.now();
		NodeInstruction input = new NodeInstruction(TEST_INSTRUCTION_TOPIC, now, TEST_NODE_ID);
		input.setState(InstructionState.Queuing);

		final Long instructionId = UUID.randomUUID().getMostSignificantBits();

		expect(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, TEST_NODE_ID,
				InstructionState.Queuing, InstructionState.Queued, null)).andReturn(true);

		replayAll();

		// WHEN
		stopMqttServer();
		service.didQueueNodeInstruction(input, instructionId);

		// THEN
	}

}
