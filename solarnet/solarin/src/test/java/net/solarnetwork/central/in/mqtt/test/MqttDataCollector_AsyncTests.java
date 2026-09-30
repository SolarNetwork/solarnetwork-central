/* ==================================================================
 * MqttDataCollector_AsyncTests.java - 30/09/2026 3:10:00 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.mqtt.MqttDataCollector;
import net.solarnetwork.central.in.mqtt.SolarInCountStat;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.util.StatTracker;

/**
 * Test cases for the asynchronous message handling of the
 * {@link MqttDataCollector} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class MqttDataCollector_AsyncTests {

	private static final Long TEST_NODE_ID = 123L;
	private static final String TEST_SOURCE_ID = "test.source";

	@Mock
	private DataCollectorBiz dataCollectorBiz;

	@Mock
	private NodeInstructionDao nodeInstructionDao;

	private StatTracker mqttStats;
	private ExecutorService executor;
	private MqttDataCollector service;

	@BeforeEach
	public void setup() {
		mqttStats = new StatTracker("Test SolarIn", null,
				LoggerFactory.getLogger(MqttDataCollector_AsyncTests.class), 10);
		executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "test-mqtt-worker"));
		service = new MqttDataCollector(DatumJsonUtils.DATUM_JSON_OBJECT_MAPPER, dataCollectorBiz,
				nodeInstructionDao, mqttStats);
		service.setExecutor(executor);
	}

	@AfterEach
	public void teardown() {
		executor.shutdownNow();
		SecurityContextHolder.clearContext();
	}

	private static String datumTopic(Long nodeId) {
		return String.format(MqttDataCollector.DEFAULT_NODE_DATUM_TOPIC_TEMPLATE, nodeId);
	}

	private static MqttMessage datumMessage() {
		GeneralNodeDatum datum = new GeneralNodeDatum(TEST_NODE_ID,
				Instant.now().truncatedTo(ChronoUnit.MILLIS), TEST_SOURCE_ID);
		datum.setSamples(new DatumSamples());
		String json = "{\"created\":" + datum.getCreated().toEpochMilli() + ",\"sourceId\":\""
				+ TEST_SOURCE_ID + "\",\"samples\":{\"i\":{\"foo\":123}}}";
		return new BasicMqttMessage(datumTopic(TEST_NODE_ID), false, MqttQos.AtLeastOnce,
				json.getBytes(UTF_8));
	}

	@Test
	public void handledOnExecutor_notOnCallingThread() throws Exception {
		// GIVEN
		final AtomicReference<String> handlingThread = new AtomicReference<>();
		final CountDownLatch handled = new CountDownLatch(1);
		willAnswer(_ -> {
			handlingThread.set(Thread.currentThread().getName());
			handled.countDown();
			return null;
		}).given(dataCollectorBiz).postGeneralNodeDatum(any());

		// WHEN
		CompletionStage<?> result = service.onMqttMessageAsync(datumMessage());

		// THEN
		// @formatter:off
		and.then(handled.await(5, TimeUnit.SECONDS))
			.as("Message handled")
			.isTrue()
			;
		and.then(result.toCompletableFuture().get(5, TimeUnit.SECONDS))
			.as("Stage completes normally, so the message is acknowledged")
			.isNull()
			;
		and.then(handlingThread.get())
			.as("Handled on the executor, not the calling thread")
			.isEqualTo("test-mqtt-worker")
			;
		// @formatter:on
	}

	@Test
	public void securityContextClearedAfterHandling() throws Exception {
		// GIVEN
		final CountDownLatch handled = new CountDownLatch(1);
		willAnswer(_ -> {
			handled.countDown();
			return null;
		}).given(dataCollectorBiz).postGeneralNodeDatum(any());

		// WHEN
		service.onMqttMessageAsync(datumMessage()).toCompletableFuture().get(5, TimeUnit.SECONDS);
		and.then(handled.await(5, TimeUnit.SECONDS)).as("Message handled").isTrue();

		// read the security context back on the same worker thread
		final AtomicReference<Authentication> leaked = new AtomicReference<>();
		executor.submit(() -> leaked.set(SecurityContextHolder.getContext().getAuthentication()))
				.get(5, TimeUnit.SECONDS);

		// THEN
		// @formatter:off
		and.then(leaked.get())
			.as("Node authentication did not outlive the message on the pooled thread")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void executorRejects_notAcknowledgedAndCounted() {
		// GIVEN
		final Executor rejecting = _ -> {
			throw new RejectedExecutionException("at capacity");
		};
		service.setExecutor(rejecting);

		// WHEN
		CompletionStage<?> result = service.onMqttMessageAsync(datumMessage());

		// THEN
		// @formatter:off
		and.thenThrownBy(() -> result.toCompletableFuture().get(5, TimeUnit.SECONDS))
			.as("Stage completes exceptionally, so the message is not acknowledged")
			.hasCauseInstanceOf(RejectedExecutionException.class)
			;
		and.then(mqttStats.get(SolarInCountStat.MessagesRejected))
			.as("Rejected message counted")
			.isEqualTo(1)
			;
		then(dataCollectorBiz).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void unparsablePayload_discardedAndAcknowledged() throws Exception {
		// GIVEN
		MqttMessage msg = new BasicMqttMessage(datumTopic(TEST_NODE_ID), false, MqttQos.AtLeastOnce,
				"{not valid json".getBytes(UTF_8));

		// WHEN
		CompletionStage<?> result = service.onMqttMessageAsync(msg);

		// THEN
		// @formatter:off
		and.thenCode(() -> result.toCompletableFuture().get(5, TimeUnit.SECONDS))
			.as("Stage completes normally: a payload that cannot be parsed will never"
					+ " parse, so it is acknowledged rather than redelivered forever")
			.doesNotThrowAnyException()
			;
		and.then(mqttStats.get(SolarInCountStat.MessagesDiscarded))
			.as("Discarded message counted")
			.isEqualTo(1)
			;
		then(dataCollectorBiz).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void persistFailed_notAcknowledged() {
		// GIVEN
		willThrow(new RuntimeException("boom!")).given(dataCollectorBiz)
				.postGeneralNodeDatum(any());

		// WHEN
		CompletionStage<?> result = service.onMqttMessageAsync(datumMessage());

		// THEN
		// @formatter:off
		and.thenThrownBy(() -> result.toCompletableFuture().get(5, TimeUnit.SECONDS))
			.as("Stage completes exceptionally, so the message is not acknowledged and"
					+ " the broker redelivers it")
			.cause()
			.isInstanceOf(RepeatableTaskException.class)
			;
		// @formatter:on
	}

}
