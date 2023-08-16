/* ==================================================================
 * SolarInputDatumObserverTests.java - 10/08/2023 10:38:09 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.mqtt.test;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.mqtt.SolarInputDatumObserver;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.ObjectDatum;
import net.solarnetwork.central.support.ObservableMqttConnection;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.MqttStats;
import net.solarnetwork.common.mqtt.netty.NettyMqttConnectionFactory;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.test.mqtt.MqttServerSupport;

/**
 * Test cases for the {@link SolarInputDatumObserver} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SolarInputDatumObserverTests extends MqttServerSupport {

	private static final int MQTT_TIMEOUT = 10;
	private static final String TEST_CLIENT_ID = "solarnet.test";
	private static final String TEST_PUB_CLIENT_ID = "solarnode.test";

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private DatumStreamMetadataDao datumStreamMetadataDao;

	@Captor
	private ArgumentCaptor<ObjectStreamCriteria> streamCriteriaCaptor;

	private ExecutorService executor;
	private ObjectMapper objectMapper;
	private ObservableMqttConnection mqttConnection;
	private SolarInputDatumObserver service;

	private ObjectMapper createObjectMapper() {
		return JsonUtils.newDatumObjectMapper();
	}

	@BeforeEach
	public void setup() throws Exception {
		setupMqttServer();

		executor = Executors.newSingleThreadExecutor();
		objectMapper = createObjectMapper();

		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();

		NettyMqttConnectionFactory factory = new NettyMqttConnectionFactory(
				Executors.newCachedThreadPool(), scheduler);

		MqttStats mqttStats = new MqttStats(1);
		service = new SolarInputDatumObserver(executor, objectMapper, nodeOwnershipDao,
				datumStreamMetadataDao);

		mqttConnection = new ObservableMqttConnection(factory, mqttStats, "Test SolarInput Datum Obs",
				Collections.singletonList(service));
		mqttConnection.getMqttConfig().setReconnectDelaySeconds(1);
		mqttConnection.getMqttConfig().setClientId(TEST_CLIENT_ID);
		mqttConnection.getMqttConfig().setServerUri(new URI("mqtt://localhost:" + getMqttServerPort()));
	}

	private void startConnection() {
		Future<?> f = mqttConnection.startup();

		// give chance for onMqttServerConnection thread to complete
		try {
			f.get(MQTT_TIMEOUT, TimeUnit.SECONDS);
			Thread.sleep(400L);
		} catch ( InterruptedException e ) {
			// ignore
		} catch ( Exception e ) {
			throw new RuntimeException("Error starting MQTT connection.", e);
		}
	}

	private IMqttClient startConnectionAndClient() {
		startConnection();
		setupMqttClient(TEST_PUB_CLIENT_ID, null);
		return getClient();
	}

	@AfterEach
	@Override
	public void teardown() {
		IMqttClient client = getClient();
		if ( client != null ) {
			try {
				client.close();
			} catch ( MqttException e ) {
				// ignore
			}
		}
		executor.shutdown();
		super.teardown();
	}

	@Override
	public void stopMqttServer() {
		mqttConnection.shutdown();
		super.stopMqttServer();
	}

	private String datumTopic(Long nodeId) {
		return String.format(SolarInputDatumObserver.DEFAULT_NODE_DATUM_TOPIC_TEMPLATE, nodeId);
	}

	private GeneralDatum publishDatum(IMqttClient client, Long nodeId, String sourceId) {
		String topic = datumTopic(nodeId);

		DatumSamples samples = new DatumSamples();
		samples.putInstantaneousSampleValue("a", randomUUID().getMostSignificantBits());
		samples.putInstantaneousSampleValue("b", randomUUID().getMostSignificantBits());
		samples.putStatusSampleValue("b", randomUUID().toString());

		GeneralDatum datum = new GeneralDatum(sourceId, now().truncatedTo(ChronoUnit.MILLIS), samples);
		try {
			byte[] json = objectMapper.writeValueAsBytes(datum);
			client.publish(topic, json, MqttQos.AtMostOnce.getValue(), false);
		} catch ( Exception e ) {
			throw new RuntimeException("Error publishing datum.", e);
		}
		return datum;
	}

	@Test
	public void startConnectionWithoutSubscribers() throws Exception {
		// GIVEN
		final var nodeId = Math.abs(randomUUID().getMostSignificantBits());
		final String sourceId = randomUUID().toString();
		final IMqttClient client = startConnectionAndClient();

		// WHEN
		publishDatum(client, nodeId, sourceId);

		// THEN
		Thread.sleep(500L);

		verifyNoInteractions(nodeOwnershipDao);
		verifyNoInteractions(datumStreamMetadataDao);
	}

	private static final class TestHandler implements Consumer<ObjectDatum> {

		private final CopyOnWriteArrayList<ObjectDatum> accepted = new CopyOnWriteArrayList<>();

		private final CountDownLatch latch;

		private TestHandler(int expectedAcceptCount) {
			this(expectedAcceptCount > 0 ? new CountDownLatch(expectedAcceptCount) : null);
		}

		private TestHandler(CountDownLatch latch) {
			super();
			this.latch = latch;
		}

		@Override
		public void accept(ObjectDatum t) {
			accepted.add(t);
			if ( latch != null ) {
				latch.countDown();
			}
		}

	}

	@Test
	public void startConnectionWithSubscribers() throws Exception {
		// GIVEN
		final var handler = new TestHandler(1);
		final var userId = randomUUID().getMostSignificantBits();
		final var nodeId = Math.abs(randomUUID().getMostSignificantBits());
		final var streamId = randomUUID();
		final var sourceId = randomUUID().toString();
		final var owner = ownershipFor(nodeId, userId);

		service.registerNodeObserver(handler, nodeId);

		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(owner);
		given(datumStreamMetadataDao.findDatumStreamMetadata(any())).willAnswer(i -> {
			return singleton(new BasicObjectDatumStreamMetadata(streamId, "UTC", Node, nodeId, sourceId,
					new String[] { "a" }, new String[] { "b" }, new String[] { "c" }));
		});

		// WHEN
		final IMqttClient client = startConnectionAndClient();
		GeneralDatum d = publishDatum(client, nodeId, sourceId);

		// THEN
		then(handler.latch.await(5, TimeUnit.SECONDS)).as("Datum handled in time").isTrue();

		verify(datumStreamMetadataDao).findDatumStreamMetadata(streamCriteriaCaptor.capture());

		// @formatter:off
		then(streamCriteriaCaptor.getValue())
			.as("User ID criteria included")
			.returns(userId, ObjectStreamCriteria::getUserId)
			.as("Node type criteria included")
			.returns(ObjectDatumKind.Node, ObjectStreamCriteria::getObjectKind)
			.as("Node ID criteria included")
			.returns(nodeId, ObjectStreamCriteria::getObjectId)
			.as("Source ID criteria included")
			.returns(sourceId, ObjectStreamCriteria::getSourceId)
			;
		then(handler.accepted).as("Handled the published datum").hasSize(1).element(0)
			.as("User ID provided in datum")
			.returns(userId, ObjectDatum::getUserId)
			.as("Node type provided in datum")
			.returns(ObjectDatumKind.Node, ObjectDatum::getKind)
			.as("Node ID provided in datum")
			.returns(nodeId, ObjectDatum::getObjectId)
			.as("Source ID provided in datum")
			.returns(sourceId, ObjectDatum::getSourceId)
			.as("Stream ID provided in datum")
			.returns(streamId, ObjectDatum::getStreamId)
			.as("Samples extracted from datum")
			.returns(d.getSamples(), ObjectDatum::getSamples)
			;
		// @formatter:on
	}

	@Test
	public void startConnectionWithoutSubscribers_addSubscriber() throws Exception {
		// GIVEN
		final var handler = new TestHandler(1);
		final var userId = randomUUID().getMostSignificantBits();
		final var nodeId = Math.abs(randomUUID().getMostSignificantBits());
		final var streamId = randomUUID();
		final var sourceId = randomUUID().toString();
		final var owner = ownershipFor(nodeId, userId);
		final IMqttClient client = startConnectionAndClient();

		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(owner);
		given(datumStreamMetadataDao.findDatumStreamMetadata(any())).willAnswer(i -> {
			return singleton(new BasicObjectDatumStreamMetadata(streamId, "UTC", Node, nodeId, sourceId,
					new String[] { "a" }, new String[] { "b" }, new String[] { "c" }));
		});

		// WHEN
		service.registerNodeObserver(handler, nodeId);
		GeneralDatum d = publishDatum(client, nodeId, sourceId);

		// THEN
		then(handler.latch.await(5, TimeUnit.SECONDS)).as("Datum handled in time").isTrue();

		verify(datumStreamMetadataDao).findDatumStreamMetadata(streamCriteriaCaptor.capture());

		// @formatter:off
		then(streamCriteriaCaptor.getValue())
			.as("User ID criteria included")
			.returns(userId, ObjectStreamCriteria::getUserId)
			.as("Node type criteria included")
			.returns(ObjectDatumKind.Node, ObjectStreamCriteria::getObjectKind)
			.as("Node ID criteria included")
			.returns(nodeId, ObjectStreamCriteria::getObjectId)
			.as("Source ID criteria included")
			.returns(sourceId, ObjectStreamCriteria::getSourceId)
			;
		then(handler.accepted).as("Handled the published datum").hasSize(1).element(0)
			.as("User ID provided in datum")
			.returns(userId, ObjectDatum::getUserId)
			.as("Node type provided in datum")
			.returns(ObjectDatumKind.Node, ObjectDatum::getKind)
			.as("Node ID provided in datum")
			.returns(nodeId, ObjectDatum::getObjectId)
			.as("Source ID provided in datum")
			.returns(sourceId, ObjectDatum::getSourceId)
			.as("Stream ID provided in datum")
			.returns(streamId, ObjectDatum::getStreamId)
			.as("Samples extracted from datum")
			.returns(d.getSamples(), ObjectDatum::getSamples)
			.as("Timestamp extracted from datum")
			.returns(d.getTimestamp(), ObjectDatum::getTimestamp)
			;
		// @formatter:on
	}

	@Test
	public void startConnectionWithoutSubscribers_addSubscriber_removeSubscriber() throws Exception {
		// GIVEN
		final var handler = new TestHandler(1);
		final var userId = randomUUID().getMostSignificantBits();
		final var nodeId = Math.abs(randomUUID().getMostSignificantBits());
		final var streamId = randomUUID();
		final var sourceId = randomUUID().toString();
		final var owner = ownershipFor(nodeId, userId);
		final IMqttClient client = startConnectionAndClient();

		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(owner);
		given(datumStreamMetadataDao.findDatumStreamMetadata(any())).willAnswer(i -> {
			return singleton(new BasicObjectDatumStreamMetadata(streamId, "UTC", Node, nodeId, sourceId,
					new String[] { "a" }, new String[] { "b" }, new String[] { "c" }));
		});

		// WHEN
		// register, then publish a datum (which should be handled)
		service.registerNodeObserver(handler, nodeId);
		GeneralDatum d = publishDatum(client, nodeId, sourceId);

		then(handler.latch.await(5, TimeUnit.SECONDS)).as("Datum handled in time").isTrue();

		Thread.sleep(20L);

		// unregister, then publish another datum (which should not be handled)
		service.unregisterNodeObserver(handler);
		publishDatum(client, nodeId, sourceId);

		Thread.sleep(1500L);

		// THEN

		verify(datumStreamMetadataDao).findDatumStreamMetadata(streamCriteriaCaptor.capture());

		// @formatter:off
		then(streamCriteriaCaptor.getValue())
			.as("User ID criteria included")
			.returns(userId, ObjectStreamCriteria::getUserId)
			.as("Node type criteria included")
			.returns(ObjectDatumKind.Node, ObjectStreamCriteria::getObjectKind)
			.as("Node ID criteria included")
			.returns(nodeId, ObjectStreamCriteria::getObjectId)
			.as("Source ID criteria included")
			.returns(sourceId, ObjectStreamCriteria::getSourceId)
			;
		then(handler.accepted).as("Handled the published datum").hasSize(1).element(0)
			.as("User ID provided in datum")
			.returns(userId, ObjectDatum::getUserId)
			.as("Node type provided in datum")
			.returns(ObjectDatumKind.Node, ObjectDatum::getKind)
			.as("Node ID provided in datum")
			.returns(nodeId, ObjectDatum::getObjectId)
			.as("Source ID provided in datum")
			.returns(sourceId, ObjectDatum::getSourceId)
			.as("Stream ID provided in datum")
			.returns(streamId, ObjectDatum::getStreamId)
			.as("Samples extracted from datum")
			.returns(d.getSamples(), ObjectDatum::getSamples)
			.as("Timestamp extracted from datum")
			.returns(d.getTimestamp(), ObjectDatum::getTimestamp)
			;
		// @formatter:on
	}

	@Test
	public void startConnectionWithoutSubscribers_addSubscribers_sameNodeId() throws Exception {
		// GIVEN
		final var handler1 = new TestHandler(1);
		final var handler2 = new TestHandler(1);
		final var userId = randomUUID().getMostSignificantBits();
		final var nodeId = Math.abs(randomUUID().getMostSignificantBits());
		final var streamId = randomUUID();
		final var sourceId = randomUUID().toString();
		final var owner = ownershipFor(nodeId, userId);
		final IMqttClient client = startConnectionAndClient();

		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(owner).willReturn(owner);
		given(datumStreamMetadataDao.findDatumStreamMetadata(any())).willAnswer(i -> {
			return singleton(new BasicObjectDatumStreamMetadata(streamId, "UTC", Node, nodeId, sourceId,
					new String[] { "a" }, new String[] { "b" }, new String[] { "c" }));
		});

		// WHEN
		service.registerNodeObserver(handler1, nodeId);
		service.registerNodeObserver(handler2, nodeId);
		GeneralDatum d = publishDatum(client, nodeId, sourceId);

		// THEN
		then(handler1.latch.await(5, TimeUnit.SECONDS)).as("Handler 1 handled in time").isTrue();
		then(handler2.latch.await(5, TimeUnit.SECONDS)).as("Handler 2 handled in time").isTrue();

		verify(datumStreamMetadataDao, times(2)).findDatumStreamMetadata(streamCriteriaCaptor.capture());

		// @formatter:off
		then(streamCriteriaCaptor.getAllValues()).hasSize(2).allSatisfy(c -> {
			then(c).as("User ID criteria included")
				.returns(userId, ObjectStreamCriteria::getUserId)
				.as("Node type criteria included")
				.returns(ObjectDatumKind.Node, ObjectStreamCriteria::getObjectKind)
				.as("Node ID criteria included")
				.returns(nodeId, ObjectStreamCriteria::getObjectId)
				.as("Source ID criteria included")
				.returns(sourceId, ObjectStreamCriteria::getSourceId)
				;
		});
		then(handler1.accepted).as("Handler 1 handled 1 datum").hasSize(1);
		then(handler2.accepted).as("Handler 2 handled 1 datum").hasSize(1);
		then(asList(handler1.accepted, handler2.accepted).stream().flatMap(l -> l.stream()).toList()).allSatisfy(accepted -> {
			then(accepted)
				.as("User ID provided in datum")
				.returns(userId, ObjectDatum::getUserId)
				.as("Node type provided in datum")
				.returns(ObjectDatumKind.Node, ObjectDatum::getKind)
				.as("Node ID provided in datum")
				.returns(nodeId, ObjectDatum::getObjectId)
				.as("Source ID provided in datum")
				.returns(sourceId, ObjectDatum::getSourceId)
				.as("Stream ID provided in datum")
				.returns(streamId, ObjectDatum::getStreamId)
				.as("Samples extracted from datum")
				.returns(d.getSamples(), ObjectDatum::getSamples)
				.as("Timestamp extracted from datum")
				.returns(d.getTimestamp(), ObjectDatum::getTimestamp)
				;
		});
		// @formatter:on
	}

}
