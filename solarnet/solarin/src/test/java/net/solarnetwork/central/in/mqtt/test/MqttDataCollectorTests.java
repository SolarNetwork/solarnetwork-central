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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.mqtt.MqttDataCollector;
import net.solarnetwork.central.in.mqtt.SolarInCountStat;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.support.ObservableMqttConnection;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.MqttStats;
import net.solarnetwork.common.mqtt.netty.NettyMqttConnectionFactory;
import net.solarnetwork.domain.BasicInstructionStatus;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.test.mqtt.MqttServerSupport;
import net.solarnetwork.test.mqtt.TestingInterceptHandler;
import net.solarnetwork.util.DateUtils;

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
	private ObservableMqttConnection mqttConnection;
	private DataCollectorBiz dataCollectorBiz;
	private NodeInstructionDao nodeInstructionDao;
	private MqttDataCollector service;

	private ObjectMapper createObjectMapper(JsonFactory jsonFactory) {
		return JsonUtils.newDatumObjectMapper(jsonFactory);
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

		MqttStats mqttStats = new MqttStats(1, SolarInCountStat.values());

		service = new MqttDataCollector(objectMapper, dataCollectorBiz, nodeInstructionDao, mqttStats);

		mqttConnection = new ObservableMqttConnection(factory, mqttStats, "Test SolarFlux",
				Collections.singletonList(service));
		mqttConnection.getMqttConfig().setClientId(TEST_CLIENT_ID);
		mqttConnection.getMqttConfig().setServerUri(new URI("mqtt://localhost:" + getMqttServerPort()));
		Future<?> f = mqttConnection.startup();
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

	@Override
	public void stopMqttServer() {
		mqttConnection.shutdown();
		super.stopMqttServer();
	}

	private String datumTopic(Long nodeId) {
		return String.format(MqttDataCollector.DEFAULT_NODE_DATUM_TOPIC_TEMPLATE, nodeId);
	}

	@Test
	public void subscribes() throws Exception {
		replayAll();

		// give a little time for subscription to take
		Thread.sleep(1000);

		TestingInterceptHandler session = getTestingInterceptHandler();
		assertThat("Subscribed ", session.subscribeMessages, hasSize(1));

		InterceptSubscribeMessage subMsg = session.subscribeMessages.get(0);
		assertThat("Subscribe topic", subMsg.getTopicFilter(), equalTo("node/+/datum"));
		assertThat("Subscribe QOS", subMsg.getRequestedQos(), equalTo(MqttQoS.AT_LEAST_ONCE));
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
		datum.setCreated(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(TEST_SOURCE_ID);
		DatumSamples samples = new DatumSamples();
		datum.setSamples(samples);
		samples.putInstantaneousSampleValue("foo", 123);
		String json = "{\"created\":" + datum.getCreated().toEpochMilli() + ",\"sourceId\":\""
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

	@Test
	public void processGeneralNodeDatum_twoOh() throws Exception {
		// GIVEN
		Capture<Iterable<GeneralNodeDatum>> postDatumCaptor = new Capture<>();
		dataCollectorBiz.postGeneralNodeDatum(capture(postDatumCaptor));

		replayAll();

		// WHEN
		String topic = datumTopic(TEST_NODE_ID);
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(TEST_SOURCE_ID);
		DatumSamples samples = new DatumSamples();
		datum.setSamples(samples);
		samples.putInstantaneousSampleValue("foo", 123);
		String json = "{\"created\":\""
				+ DateUtils.ISO_DATE_TIME_ALT_UTC
						.format(Instant.ofEpochMilli(datum.getCreated().toEpochMilli()))
				+ "\",\"sourceId\":\"" + TEST_SOURCE_ID + "\",\"i\":{\"foo\":123}}";
		MqttMessage msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce,
				json.getBytes("UTF-8"));
		service.onMqttMessage(msg);

		// THEN
		assertThat("Datum posted", postDatumCaptor.getValue(), notNullValue());
		List<GeneralNodeDatum> postedDatumList = StreamSupport
				.stream(postDatumCaptor.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat("Posted datum count", postedDatumList, hasSize(1));
		GeneralNodeDatum postedDatum = postedDatumList.get(0);
		assertThat("Posted datum ID", postedDatum.getId(), equalTo(datum.getId()));
		assertThat("Posted datum samples", postedDatum.getSamples(), equalTo(datum.getSamples()));
	}

	@Test
	public void processGeneralNodeDatumWithTransientException() throws Exception {
		// given
		Capture<Iterable<GeneralNodeDatum>> postDatumCaptor = new Capture<>(CaptureType.ALL);

		// the first time triggers an exception
		dataCollectorBiz.postGeneralNodeDatum(capture(postDatumCaptor));
		EasyMock.expectLastCall().andThrow(new RepeatableTaskException("Boo"));

		// the second time succeeds
		dataCollectorBiz.postGeneralNodeDatum(capture(postDatumCaptor));

		replayAll();

		// when
		String topic = datumTopic(TEST_NODE_ID);
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(TEST_SOURCE_ID);
		DatumSamples samples = new DatumSamples();
		datum.setSamples(samples);
		samples.putInstantaneousSampleValue("foo", 123);
		String json = "{\"created\":" + datum.getCreated().toEpochMilli() + ",\"sourceId\":\""
				+ TEST_SOURCE_ID + "\",\"samples\":{\"i\":{\"foo\":123}}}";
		MqttMessage msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce,
				json.getBytes("UTF-8"));
		service.onMqttMessage(msg);

		// then
		List<Iterable<GeneralNodeDatum>> postedSets = postDatumCaptor.getValues();
		assertThat("Posted datum invocations", postedSets, hasSize(2));

		List<GeneralNodeDatum> postedDatumList = StreamSupport
				.stream(postedSets.get(0).spliterator(), false).collect(Collectors.toList());
		assertThat("Posted datum count", postedDatumList, hasSize(1));
		GeneralNodeDatum postedDatum = postedDatumList.get(0);
		assertThat("Posted datum ID", postedDatum.getId(), equalTo(datum.getId()));
		assertThat("Posted datum samples", postedDatum.getSamples(), equalTo(datum.getSamples()));

		postedDatumList = StreamSupport.stream(postedSets.get(1).spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Posted datum count", postedDatumList, hasSize(1));
		assertThat("Posted datum ID", postedDatum.getId(), equalTo(datum.getId()));
		assertThat("Posted datum samples", postedDatum.getSamples(), equalTo(datum.getSamples()));
	}

	@Test(expected = RepeatableTaskException.class)
	public void processGeneralNodeDatumWithTransientExceptionRetriesExhausted() throws Exception {
		// given
		Capture<Iterable<GeneralNodeDatum>> postDatumCaptor = new Capture<>(CaptureType.ALL);

		// all 3 tries trigger an exception
		dataCollectorBiz.postGeneralNodeDatum(capture(postDatumCaptor));
		EasyMock.expectLastCall().andThrow(new RepeatableTaskException("Boo")).times(3);

		replayAll();

		// when
		String topic = datumTopic(TEST_NODE_ID);
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(TEST_SOURCE_ID);
		DatumSamples samples = new DatumSamples();
		datum.setSamples(samples);
		samples.putInstantaneousSampleValue("foo", 123);
		String json = "{\"created\":" + datum.getCreated().toEpochMilli() + ",\"sourceId\":\""
				+ TEST_SOURCE_ID + "\",\"samples\":{\"i\":{\"foo\":123}}}";
		MqttMessage msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce,
				json.getBytes("UTF-8"));
		service.onMqttMessage(msg);
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
		datum.setCreated(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(TEST_SOURCE_ID);
		DatumSamples samples = new DatumSamples();
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
		datum.setCreated(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(TEST_SOURCE_ID);
		DatumSamples samples = new DatumSamples();
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
		datum.setCreated(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		datum.setLocationId(TEST_LOC_ID);
		datum.setSourceId(TEST_SOURCE_ID);
		DatumSamples samples = new DatumSamples();
		datum.setSamples(samples);
		samples.putInstantaneousSampleValue("foo", 123);
		String json = "{\"created\":" + datum.getCreated().toEpochMilli() + ",\"sourceId\":\""
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
	public void processGeneralLocationDatum_twoOh() throws Exception {
		// given
		Capture<Iterable<GeneralLocationDatum>> postDatumCaptor = new Capture<>();
		dataCollectorBiz.postGeneralLocationDatum(capture(postDatumCaptor));

		replayAll();

		// when
		String topic = datumTopic(TEST_NODE_ID);
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		datum.setLocationId(TEST_LOC_ID);
		datum.setSourceId(TEST_SOURCE_ID);
		DatumSamples samples = new DatumSamples();
		datum.setSamples(samples);
		samples.putInstantaneousSampleValue("foo", 123);
		String json = "{\"created\":\""
				+ DateUtils.ISO_DATE_TIME_ALT_UTC.format(
						Instant.ofEpochMilli(datum.getCreated().toEpochMilli()))
				+ "\",\"sourceId\":\"" + TEST_SOURCE_ID + "\",\"locationId\":" + TEST_LOC_ID
				+ ",\"i\":{\"foo\":123}}";
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

		String json = "{\"__type__\":\"InstructionStatus\",\"id\":" + nodeInstructionId
				+ ",\"instructionId\":" + instructionId + ",\"topic\":\"" + TEST_INSTRUCTION_TOPIC
				+ "\",\"status\":\"Completed\",\"resultParameters\":{\"foo\":\"bar\"}}";
		MqttMessage msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce,
				json.getBytes("UTF-8"));
		service.onMqttMessage(msg);

		// then
	}

	@Test
	public void processInstructionStatus_twoOh() throws Exception {
		// GIVEN
		final Long instructionId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
		final Map<String, Object> resultParams = Collections.singletonMap("foo", "bar");
		expect(nodeInstructionDao.updateNodeInstructionState(instructionId, TEST_NODE_ID,
				InstructionState.Completed, resultParams)).andReturn(true);

		replayAll();

		// WHEN 
		String topic = datumTopic(TEST_NODE_ID);

		BasicInstructionStatus status = new BasicInstructionStatus(instructionId,
				net.solarnetwork.domain.InstructionStatus.InstructionState.Completed, Instant.now(),
				resultParams);
		String json = objectMapper.writeValueAsString(status);

		MqttMessage msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce,
				json.getBytes("UTF-8"));
		service.onMqttMessage(msg);

		// THEN
	}
}
