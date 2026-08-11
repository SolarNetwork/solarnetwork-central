/* ==================================================================
 * MqttJsonPublisher_UserEventTests.java - 9 Aug 2026 7:33:16 am
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

package net.solarnetwork.central.support.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import io.moquette.interception.messages.InterceptPublishMessage;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.test.UserEventAppenderBizTests;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.MqttJsonPublisher;
import net.solarnetwork.central.support.ObservableMqttConnection;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.codec.jackson.JsonDateUtils;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.netty.NettyMqttConnectionFactory;
import net.solarnetwork.test.mqtt.MqttServerSupport;
import net.solarnetwork.test.mqtt.TestingInterceptHandler;
import net.solarnetwork.util.StatTracker;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;

/**
 * Test cases for the {@link MqttJsonPublisher} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttJsonPublisher_UserEventTests extends MqttServerSupport {

	private static final int MQTT_TIMEOUT = 10;
	private static final String TEST_CLIENT_ID = "solarnet.test";

	private ObjectMapper objectMapper;
	private ObservableMqttConnection mqttConnection;
	private MqttJsonPublisher<UserEvent> publisher;

	private ObjectMapper createObjectMapper() {
		return JsonUtils.JSON_OBJECT_MAPPER.rebuild().addModule(JsonDateUtils.JAVA_TIMESTAMP_MODULE)
				.enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build();
	}

	@BeforeEach
	public void setup() throws Exception {
		setupMqttServer();

		objectMapper = createObjectMapper();

		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();

		NettyMqttConnectionFactory factory = new NettyMqttConnectionFactory(
				Executors.newCachedThreadPool(), scheduler);

		StatTracker mqttStats = new StatTracker("Test", null, log, 1);
		publisher = new MqttJsonPublisher<>("UserEvent Test", objectMapper,
				UserEventAppenderBiz.SOLARFLUX_TAGGED_TOPIC_FN, false, MqttQos.AtMostOnce,
				UserEventAppenderBiz.SOLARFLUX_TAGGED_ERROR_TOPIC_FN,
				UserEventAppenderBiz.solarFluxTaggedErrorTopicFn(objectMapper));

		publisher.setMqttStats(mqttStats);

		mqttConnection = new ObservableMqttConnection(factory, mqttStats, "Test SolarFlux",
				List.of(publisher));
		mqttConnection.getMqttConfig().setClientId(TEST_CLIENT_ID);
		mqttConnection.getMqttConfig().setServerUri(new URI("mqtt://localhost:" + getMqttServerPort()));
		Future<?> f = mqttConnection.startup();
		f.get(MQTT_TIMEOUT, TimeUnit.SECONDS);

		// give chance for onMqttServerConnection thread to complete
		try {
			Thread.sleep(400L);
		} catch ( InterruptedException e ) {
			// ignore
		}
	}

	@Override
	public void stopMqttServer() {
		mqttConnection.shutdown();
		super.stopMqttServer();
	}

	@Test
	public void publishEvent() throws Exception {
		// GIVEN
		final UserEvent event = new UserEvent(CommonTestUtils.randomLong(), UUID.randomUUID(),
				new String[] { "a", "b", "c" }, "Test message.", "{}");

		final TestingInterceptHandler session = getTestingInterceptHandler();

		// WHEN
		final Future<?> future = publisher.apply(event);

		stopMqttServer(); // to flush messages

		// THEN
		then(future).as("Future returned").isNotNull();
		then(publisher.getMqttStats().get("MessagesDelivered")).as("Stat published count").isEqualTo(1L);
		then(session.publishMessages).as("Only 1 message published").hasSize(1);

		InterceptPublishMessage msg = session.getPublishMessageAtIndex(0);
		// @formatter:off
		then(msg)
			.as("Topic is tagged")
			.returns(UserEventAppenderBiz.SOLARFLUX_TAGGED_TOPIC_FN.apply(event), from(InterceptPublishMessage::getTopicName))
			.as("Payload is event")
			.returns(objectMapper.writeValueAsString(event), from(m -> m.getPayload().toString(UTF_8)))
			;
		// @formatter:on
	}

	@Test
	public void publishEvent_tooLarge() throws Exception {
		// GIVEN
		final String largeContent = utf8StringResource("large-event-data-02.json",
				UserEventAppenderBizTests.class);
		final UserEvent event = new UserEvent(CommonTestUtils.randomLong(), UUID.randomUUID(),
				new String[] { "a", "b", "c" }, "Test message.", largeContent);

		final TestingInterceptHandler session = getTestingInterceptHandler();

		// WHEN
		final Future<?> future = publisher.apply(event);

		stopMqttServer(); // to flush messages

		// THEN
		then(future).as("Future returned").isNotNull();
		then(publisher.getMqttStats().get("MessagesDelivered")).as("Stat published count").isEqualTo(1L);
		then(session.publishMessages).as("Only 1 message published").hasSize(1);

		final UserEvent errEvent = new UserEvent(event.id(), event.getTags(),
				"Unable to publish event because the payload length 14778 exceeds the maximum allowed 8192.",
				"""
						{"message":"Content too large to preserve."}""");
		InterceptPublishMessage msg = session.getPublishMessageAtIndex(0);
		// @formatter:off
		then(msg)
			.as("Topic is tagged as error")
			.returns(UserEventAppenderBiz.SOLARFLUX_TAGGED_ERROR_TOPIC_FN.apply(event, null), 
					from(InterceptPublishMessage::getTopicName))
			.as("Payload is error event")
			.returns(objectMapper.writeValueAsString(errEvent), from(m -> m.getPayload().toString(UTF_8)))
			;
		// @formatter:on
	}

}
