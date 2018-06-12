/* ==================================================================
 * MqttDataCollectorTests.java - 10/06/2018 4:58:04 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.mqtt.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.moquette.interception.messages.InterceptConnectMessage;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.mqtt.MqttDataCollector;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.test.CallingThreadExecutorService;
import net.solarnetwork.domain.GeneralLocationDatumSamples;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.util.JodaDateTimeSerializer;
import net.solarnetwork.util.JodaLocalDateSerializer;
import net.solarnetwork.util.JodaLocalDateTimeSerializer;
import net.solarnetwork.util.JodaLocalTimeSerializer;
import net.solarnetwork.util.ObjectMapperFactoryBean;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link MqttDataCollector} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttDataCollectorTests extends MqttServerSupport {

	private static final String TEST_CLIENT_ID = "solarnet.test";
	private static final Long TEST_NODE_ID = 123L;
	private static final String TEST_SOURCE_ID = "test.source";
	private static final Long TEST_LOC_ID = 321L;
	private static final String TEST_INSTRUCTION_TOPIC = "test.topic";

	private ObjectMapper objectMapper;
	private DataCollectorBiz dataCollectorBiz;
	private NodeInstructionDao nodeInstructionDao;
	private MqttDataCollector service;

	private ObjectMapper createObjectMapper(JsonFactory jsonFactory) {
		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		if ( jsonFactory != null ) {
			factory.setJsonFactory(jsonFactory);
		}
		factory.setSerializers(Arrays.asList(new JodaDateTimeSerializer(), new JodaLocalDateSerializer(),
				new JodaLocalDateTimeSerializer(), new JodaLocalTimeSerializer()));
		factory.setFeaturesToDisable(Arrays.asList(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
		try {
			return factory.getObject();
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	@Before
	public void setup() {
		setupMqttServer();

		objectMapper = createObjectMapper(null);
		dataCollectorBiz = EasyMock.createMock(DataCollectorBiz.class);
		nodeInstructionDao = EasyMock.createMock(NodeInstructionDao.class);

		String serverUri = "mqtt://localhost:" + getMqttServerPort();
		service = new MqttDataCollector(new CallingThreadExecutorService(), objectMapper,
				dataCollectorBiz, new StaticOptionalService<NodeInstructionDao>(nodeInstructionDao),
				null, serverUri, TEST_CLIENT_ID, false);
	}

	@Override
	@After
	public void teardown() {
		super.teardown();
		EasyMock.verify(dataCollectorBiz, nodeInstructionDao);
	}

	private void replayAll() {
		EasyMock.replay(dataCollectorBiz, nodeInstructionDao);
	}

	private String datumTopic(Long nodeId) {
		return String.format(MqttDataCollector.NODE_DATUM_TOPIC_TEMPLATE, nodeId);
	}

	@Test
	public void processGeneralNodeDatum() throws Exception {
		// given
		Capture<Iterable<GeneralNodeDatum>> postDatumCaptor = new Capture<>();
		dataCollectorBiz.postGeneralNodeDatum(capture(postDatumCaptor));

		replayAll();

		// when
		String topic = datumTopic(TEST_NODE_ID);
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new DateTime());
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(TEST_SOURCE_ID);
		GeneralNodeDatumSamples samples = new GeneralNodeDatumSamples();
		datum.setSamples(samples);
		samples.putInstantaneousSampleValue("foo", 123);
		String json = "{\"created\":" + datum.getCreated().getMillis() + ",\"sourceId\":\""
				+ TEST_SOURCE_ID + "\",\"samples\":{\"i\":{\"foo\":123}}}";
		MqttMessage msg = new MqttMessage(json.getBytes("UTF-8"));
		service.messageArrived(topic, msg);

		// then
		assertThat("Datum posted", postDatumCaptor.getValue(), notNullValue());
		List<GeneralNodeDatum> postedDatumList = StreamSupport
				.stream(postDatumCaptor.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat("Posted datum count", postedDatumList, hasSize(1));
		GeneralNodeDatum postedDatum = postedDatumList.get(0);
		assertThat("Posted datum ID", postedDatum.getId(), equalTo(datum.getId()));
		assertThat("Posted datum samples", postedDatum.getSamples(), equalTo(datum.getSamples()));
	}

	@Test
	public void processGeneralLocationDatum() throws Exception {
		// given
		Capture<Iterable<GeneralLocationDatum>> postDatumCaptor = new Capture<>();
		dataCollectorBiz.postGeneralLocationDatum(capture(postDatumCaptor));

		replayAll();

		// when
		String topic = datumTopic(TEST_NODE_ID);
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(new DateTime());
		datum.setLocationId(TEST_LOC_ID);
		datum.setSourceId(TEST_SOURCE_ID);
		GeneralLocationDatumSamples samples = new GeneralLocationDatumSamples();
		datum.setSamples(samples);
		samples.putInstantaneousSampleValue("foo", 123);
		String json = "{\"created\":" + datum.getCreated().getMillis() + ",\"sourceId\":\""
				+ TEST_SOURCE_ID + "\",\"locationId\":" + TEST_LOC_ID
				+ ",\"samples\":{\"i\":{\"foo\":123}}}";
		MqttMessage msg = new MqttMessage(json.getBytes("UTF-8"));
		service.messageArrived(topic, msg);

		// then
		assertThat("Datum posted", postDatumCaptor.getValue(), notNullValue());
		List<GeneralLocationDatum> postedDatumList = StreamSupport
				.stream(postDatumCaptor.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat("Posted datum count", postedDatumList, hasSize(1));
		GeneralLocationDatum postedDatum = postedDatumList.get(0);
		assertThat("Posted datum ID", postedDatum.getId(), equalTo(datum.getId()));
		assertThat("Posted datum samples", postedDatum.getSamples(), equalTo(datum.getSamples()));
	}

	@Test
	public void processInstructionStatus() throws Exception {
		// given
		final Long nodeInstructionId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
		final Long instructionId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
		final Map<String, Object> resultParams = Collections.singletonMap("foo", "bar");
		expect(nodeInstructionDao.updateNodeInstructionState(instructionId, TEST_NODE_ID,
				InstructionState.Completed, resultParams)).andReturn(true);

		replayAll();

		// when
		String topic = datumTopic(TEST_NODE_ID);
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(new DateTime());
		datum.setLocationId(TEST_LOC_ID);
		datum.setSourceId(TEST_SOURCE_ID);
		GeneralLocationDatumSamples samples = new GeneralLocationDatumSamples();
		datum.setSamples(samples);
		samples.putInstantaneousSampleValue("foo", 123);
		String json = "{\"__type__\":\"InstructionStatus\",\"id\":" + nodeInstructionId
				+ ",\"instructionId\":" + instructionId + ",\"topic\":\"" + TEST_INSTRUCTION_TOPIC
				+ "\",\"status\":\"Completed\",\"resultParameters\":{\"foo\":\"bar\"}}";
		MqttMessage msg = new MqttMessage(json.getBytes("UTF-8"));
		service.messageArrived(topic, msg);

		// then
	}

	@Test
	public void connectToServer() throws Exception {
		// given
		final String username = UUID.randomUUID().toString();
		final String password = UUID.randomUUID().toString();
		service.setUsername(username);
		service.setPassword(password);

		replayAll();

		// when
		service.init();

		stopMqttServer(); // to flush messages

		// then
		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Connected to broker", session.connectMessages, hasSize(1));

		InterceptConnectMessage connMsg = session.connectMessages.get(0);
		assertThat("Connect client ID", connMsg.getClientID(), equalTo(TEST_CLIENT_ID));
		assertThat("Connect username", connMsg.getUsername(), equalTo(username));
		assertThat("Connect password", connMsg.getPassword(), equalTo(password.getBytes()));
		assertThat("Connect durable session", connMsg.isCleanSession(), equalTo(false));
	}

	@Test
	public void connectToServerWithRetryEnabled() throws Exception {
		// given
		final String username = UUID.randomUUID().toString();
		final String password = UUID.randomUUID().toString();
		service.setUsername(username);
		service.setPassword(password);
		service.setRetryConnect(true);

		replayAll();

		// when
		service.init();

		// sleep for a bit to allow background thread to connect
		Thread.sleep(1000);

		stopMqttServer(); // to flush messages

		// then
		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Connected to broker", session.connectMessages, hasSize(1));

		InterceptConnectMessage connMsg = session.connectMessages.get(0);
		assertThat("Connect client ID", connMsg.getClientID(), equalTo(TEST_CLIENT_ID));
		assertThat("Connect username", connMsg.getUsername(), equalTo(username));
		assertThat("Connect password", connMsg.getPassword(), equalTo(password.getBytes()));
		assertThat("Connect durable session", connMsg.isCleanSession(), equalTo(false));
	}

	@Test
	public void connectToServerWithRetryEnabledFirstConnectFails() throws Exception {
		stopMqttServer(); // start shut down
		final int mqttPort = getFreePort();

		// given
		final String username = UUID.randomUUID().toString();
		final String password = UUID.randomUUID().toString();
		service.setUsername(username);
		service.setPassword(password);
		service.setRetryConnect(true);
		service.setServerUri("mqtt://localhost:" + mqttPort);

		replayAll();

		// when

		// start in bg thread, because of CallingThreadExecutorService use
		Thread initThread = new Thread(new Runnable() {

			@Override
			public void run() {
				service.init();
			}
		});
		initThread.start();

		// sleep for a bit to allow background thread to attempt first connect
		Thread.sleep(200);

		// bring up MQTT server now
		setupMqttServer(null, null, null, mqttPort);

		// sleep for a bit to allow background thread to attempt second connect
		initThread.join(3000);

		stopMqttServer(); // to flush messages

		// then
		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Connected to broker", session.connectMessages, hasSize(1));

		InterceptConnectMessage connMsg = session.connectMessages.get(0);
		assertThat("Connect client ID", connMsg.getClientID(), equalTo(TEST_CLIENT_ID));
		assertThat("Connect username", connMsg.getUsername(), equalTo(username));
		assertThat("Connect password", connMsg.getPassword(), equalTo(password.getBytes()));
		assertThat("Connect durable session", connMsg.isCleanSession(), equalTo(false));
	}

	@Test
	public void willQueueNodeInstruction() {
		// given
		DateTime now = new DateTime();
		NodeInstruction input = new NodeInstruction(TEST_INSTRUCTION_TOPIC, now, TEST_NODE_ID);
		input.setState(InstructionState.Queued);

		// when
		NodeInstruction instr = service.willQueueNodeInstruction(input);

		replayAll();

		// then
		assertThat("Same instance", instr, sameInstance(input));
		assertThat("State changed", instr.getState(), equalTo(InstructionState.Queuing));
		assertThat("Topic unchanged", instr.getTopic(), equalTo(TEST_INSTRUCTION_TOPIC));
		assertThat("Node ID unchanged", instr.getNodeId(), equalTo(TEST_NODE_ID));
	}

	@Test
	public void didQueueNodeInstruction() throws Exception {
		// given
		DateTime now = new DateTime();
		NodeInstruction input = new NodeInstruction(TEST_INSTRUCTION_TOPIC, now, TEST_NODE_ID);
		input.setState(InstructionState.Queuing);

		final Long instructionId = UUID.randomUUID().getMostSignificantBits();

		replayAll();

		// when
		service.init();
		service.didQueueNodeInstruction(input, instructionId);

		// then
	}

	@Test
	public void didQueueNodeInstructionMqttNotConnected() throws Exception {
		// given
		DateTime now = new DateTime();
		NodeInstruction input = new NodeInstruction(TEST_INSTRUCTION_TOPIC, now, TEST_NODE_ID);
		input.setState(InstructionState.Queuing);

		final Long instructionId = UUID.randomUUID().getMostSignificantBits();

		expect(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, TEST_NODE_ID,
				InstructionState.Queuing, InstructionState.Queued, null)).andReturn(true);

		replayAll();

		// when
		service.init();
		stopMqttServer();
		service.didQueueNodeInstruction(input, instructionId);

		// then
	}
}
