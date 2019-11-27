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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.moquette.interception.messages.InterceptPublishMessage;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.mqtt.MqttDataCollector;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.central.test.CallingThreadExecutorService;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.netty.NettyMqttConnectionFactory;
import net.solarnetwork.domain.GeneralLocationDatumSamples;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.test.mqtt.MqttServerSupport;
import net.solarnetwork.test.mqtt.TestingInterceptHandler;
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

	private static final int MQTT_TIMEOUT = 10;
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
	public void setup() throws Exception {
		setupMqttServer();

		objectMapper = createObjectMapper(null);
		dataCollectorBiz = EasyMock.createMock(DataCollectorBiz.class);
		nodeInstructionDao = EasyMock.createMock(NodeInstructionDao.class);

		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();

		NettyMqttConnectionFactory factory = new NettyMqttConnectionFactory(
				Executors.newCachedThreadPool(), scheduler);

		service = new MqttDataCollector(factory, objectMapper, new CallingThreadExecutorService(),
				dataCollectorBiz, new StaticOptionalService<NodeInstructionDao>(nodeInstructionDao));
		service.getMqttConfig().setClientId(TEST_CLIENT_ID);
		service.getMqttConfig().setServerUri(new URI("mqtt://localhost:" + getMqttServerPort()));
		Future<?> f = service.startup();
		f.get(MQTT_TIMEOUT, TimeUnit.SECONDS);
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
		return String.format(MqttDataCollector.DEFAULT_NODE_DATUM_TOPIC_TEMPLATE, nodeId);
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
		MqttMessage msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce,
				json.getBytes("UTF-8"));
		service.onMqttMessage(msg);

		// then
		assertThat("Datum posted", postDatumCaptor.getValue(), notNullValue());
		List<GeneralNodeDatum> postedDatumList = StreamSupport
				.stream(postDatumCaptor.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat("Posted datum count", postedDatumList, hasSize(1));
		GeneralNodeDatum postedDatum = postedDatumList.get(0);
		assertThat("Posted datum ID", postedDatum.getId(), equalTo(datum.getId()));
		assertThat("Posted datum samples", postedDatum.getSamples(), equalTo(datum.getSamples()));
	}

	/*- the following test does not work; appears to be a bug in Moquette not re-sending in-flight messages without a PUBACK
	@Test
	public void processGeneralNodeDatumThrowsExceptionWithRetryEnabled() throws Exception {
		// given
		final String username = UUID.randomUUID().toString();
		final String password = UUID.randomUUID().toString();
		service.setUsername(username);
		service.setPassword(password);
		service.setRetryConnect(true);
		service.init();
	
		// sleep for a bit to allow background thread to connect
		Thread.sleep(1000);
	
		Capture<Iterable<GeneralNodeDatum>> postDatumCaptor = new Capture<>(CaptureType.ALL);
		dataCollectorBiz.postGeneralNodeDatum(capture(postDatumCaptor));
		EasyMock.expectLastCall().andThrow(new RuntimeException("Egads!"));
	
		// second time OK
		dataCollectorBiz.postGeneralNodeDatum(capture(postDatumCaptor));
	
		replayAll();
	
		TestingMqttCallback nodeClientCallback = new TestingMqttCallback();
		setupMqttClient("test.node", nodeClientCallback);
		IMqttClient nodeClient = getClient();
	
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
		msg.setQos(1);
		nodeClient.publish(topic, msg);
	
		// sleep for a bit to allow failure and re-connect
		Thread.sleep(10000);
	
		stopMqttServer();
	
		// then
		assertThat("Datum posted", postDatumCaptor.getValue(), notNullValue());
		List<GeneralNodeDatum> postedDatumList = StreamSupport
				.stream(postDatumCaptor.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat("Posted datum count", postedDatumList, hasSize(1));
		GeneralNodeDatum postedDatum = postedDatumList.get(0);
		assertThat("Posted datum ID", postedDatum.getId(), equalTo(datum.getId()));
		assertThat("Posted datum samples", postedDatum.getSamples(), equalTo(datum.getSamples()));
	}
	*/

	/*- the following test requires an external MQTT server to test against
	private MqttClient createMqttClient(String clientId, String host, int port, MqttCallback callback) {
		try {
			MemoryPersistence persistence = new MemoryPersistence();
			MqttClient client = new MqttClient("tcp://" + host + ":" + port, clientId, persistence);
			client.setCallback(callback);
			MqttConnectOptions connOptions = new MqttConnectOptions();
			connOptions.setCleanSession(false);
			connOptions.setAutomaticReconnect(false);
			client.connect(connOptions);
			return client;
		} catch ( MqttException e ) {
			throw new RuntimeException(e);
		}
	}
	
	@Test
	public void processGeneralNodeDatumThrowsExceptionWithRetryEnabledServer() throws Exception {
		// given
		stopMqttServer();
	
		service.setUsername("solarnet");
		service.setPassword("solarnet");
		service.setRetryConnect(true);
		service.setServerUri("mqtt://vernemq:1883");
		service.init();
	
		// sleep for a bit to allow background thread to connect
		Thread.sleep(1000);
	
		Capture<Iterable<GeneralNodeDatum>> postDatumCaptor = new Capture<>(CaptureType.ALL);
		dataCollectorBiz.postGeneralNodeDatum(capture(postDatumCaptor));
		EasyMock.expectLastCall().andThrow(new RuntimeException("Egads!"));
	
		// second time OK
		dataCollectorBiz.postGeneralNodeDatum(capture(postDatumCaptor));
		EasyMock.expectLastCall().anyTimes();
	
		replayAll();
	
		TestingMqttCallback nodeClientCallback = new TestingMqttCallback();
		IMqttClient nodeClient = createMqttClient(TEST_NODE_ID.toString(), "vernemq", 1883,
				nodeClientCallback);
	
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
		msg.setQos(1);
		nodeClient.publish(topic, msg);
	
		nodeClient.disconnect();
	
		// sleep for a bit to allow failure and re-connect
		Thread.sleep(4000);
	
		// then
		assertThat("Datum posted", postDatumCaptor.getValues().size(), greaterThanOrEqualTo(2));
		List<GeneralNodeDatum> postedDatumList = new ArrayList<>(2);
		for ( Iterable<GeneralNodeDatum> iterable : postDatumCaptor.getValues() ) {
			for ( GeneralNodeDatum d : iterable ) {
				postedDatumList.add(d);
				if ( postedDatumList.size() > 1 ) {
					break;
				}
			}
			if ( postedDatumList.size() > 1 ) {
				break;
			}
		}
		assertThat("Posted datum count", postedDatumList, hasSize(2));
		for ( GeneralNodeDatum postedDatum : postedDatumList ) {
			assertThat("Posted datum ID", postedDatum.getId(), equalTo(datum.getId()));
			assertThat("Posted datum samples", postedDatum.getSamples(), equalTo(datum.getSamples()));
		}
	}
	*/

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
		MqttMessage msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce,
				json.getBytes("UTF-8"));
		service.onMqttMessage(msg);

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
		MqttMessage msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce,
				json.getBytes("UTF-8"));
		service.onMqttMessage(msg);

		// then
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

		final TestingInterceptHandler session = getTestingInterceptHandler();

		replayAll();

		// when
		service.didQueueNodeInstruction(input, instructionId);

		// sleep for a bit to allow background thread to process
		Thread.sleep(200);

		// stop server to flush messages
		stopMqttServer();

		// then
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
		// given
		DateTime now = new DateTime();
		NodeInstruction input = new NodeInstruction(TEST_INSTRUCTION_TOPIC, now, TEST_NODE_ID);
		input.setState(InstructionState.Queuing);

		final Long instructionId = UUID.randomUUID().getMostSignificantBits();

		expect(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, TEST_NODE_ID,
				InstructionState.Queuing, InstructionState.Queued, null)).andReturn(true);

		replayAll();

		// when
		stopMqttServer();
		service.didQueueNodeInstruction(input, instructionId);

		// then
	}
}
