/* ==================================================================
 * OscpActionDatumPublisherTests.java - 11/10/2022 3:51:17 pm
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

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.OwnedGeneralNodeDatum;
import net.solarnetwork.central.datum.flux.SolarFluxDatumPublisher;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupSettings;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.DatumPublishEvent;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.central.oscp.domain.UserSettings;
import net.solarnetwork.central.oscp.mqtt.OscpActionDatumPublisher;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumSamples;
import oscp.v20.UpdateGroupCapacityForecast;

/**
 * Test cases for the {@link OscpActionDatumPublisher} class.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class OscpActionDatumPublisherTests {

	private static final Long TEST_NODE_ID = randomUUID().getMostSignificantBits();
	private static final Long TEST_USER_ID = randomUUID().getMostSignificantBits();
	private static final Long TEST_CO_ID = randomUUID().getMostSignificantBits();
	private static final Long TEST_CP_ID = randomUUID().getMostSignificantBits();
	private static final String TEST_CG_IDENT = randomUUID().toString();

	@Mock
	private MqttConnection conn;

	@Captor
	private ArgumentCaptor<LogEventInfo> eventCaptor;

	@Captor
	private ArgumentCaptor<MqttMessage> msgCaptor;

	private ObjectMapper mapper;
	private OscpActionDatumPublisher publisher;

	@BeforeEach
	public void setup() {
		mapper = JsonUtils.newObjectMapper();
		publisher = new OscpActionDatumPublisher(mapper);
		publisher.onMqttServerConnectionEstablished(conn, false);
	}

	private void givenMqttConnectionPublish() {
		// publish to MQTT
		given(conn.isEstablished()).willReturn(true);
		given(conn.publish(any())).willReturn(completedFuture(null));
	}

	private DatumPublishEvent newPubEvent(OscpRole role, String action,
			Collection<OwnedGeneralNodeDatum> datum, KeyValuePair... sourceIdParameters) {
		var provider = new CapacityProviderConfiguration(TEST_USER_ID, TEST_CP_ID, now());
		provider.setOscpVersion("2.0");
		provider.setBaseUrl(null);
		provider.setRegistrationStatus(RegistrationStatus.Registered);
		provider.setFlexibilityProviderId(randomUUID().getMostSignificantBits());

		var optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID, TEST_CO_ID, Instant.now());
		optimizer.setOscpVersion("2.0");
		optimizer.setBaseUrl(null);
		optimizer.setRegistrationStatus(RegistrationStatus.Registered);
		optimizer.setFlexibilityProviderId(randomUUID().getMostSignificantBits());

		// find the group
		var group = new CapacityGroupConfiguration(TEST_USER_ID, randomUUID().getMostSignificantBits(),
				now());
		group.setCapacityOptimizerId(randomUUID().getMostSignificantBits());
		group.setCapacityProviderId(randomUUID().getMostSignificantBits());
		group.setIdentifier(TEST_CG_IDENT);

		// get the provider

		// get the group publish settings
		var settings = new CapacityGroupSettings(TEST_USER_ID, group.getEntityId(), now());
		settings.setPublishToSolarIn(true);
		settings.setPublishToSolarFlux(true);
		settings.setSourceIdTemplate(UserSettings.DEFAULT_SOURCE_ID_TEMPLATE);
		settings.setNodeId(TEST_NODE_ID);

		return new DatumPublishEvent(role, action,
				role == OscpRole.CapacityProvider ? provider : optimizer,
				role == OscpRole.CapacityProvider ? optimizer : provider, group, settings, datum,
				sourceIdParameters);
	}

	@Test
	public void publish_one() throws IOException {
		// GIVEN
		OwnedGeneralNodeDatum d = new OwnedGeneralNodeDatum(TEST_USER_ID);
		d.setCreated(Instant.now().truncatedTo(ChronoUnit.HOURS));
		d.setNodeId(TEST_NODE_ID);
		d.setSourceId("/foo/bar/bam");
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("foo", 123);
		d.setSamples(s);

		List<OwnedGeneralNodeDatum> datum = Arrays.asList(d);
		DatumPublishEvent pubEvent = newPubEvent(OscpRole.CapacityProvider,
				UpdateGroupCapacityForecast.class.getSimpleName(), datum);

		givenMqttConnectionPublish();

		// WHEN
		publisher.asConsumer().accept(pubEvent);

		// THEN
		then(conn).should().publish(msgCaptor.capture());
		MqttMessage msg = msgCaptor.getValue();
		String expectedTopic = SolarFluxDatumPublisher.NODE_AGGREGATE_DATUM_TOPIC_TEMPLATE.formatted(
				TEST_USER_ID, TEST_NODE_ID, Aggregation.None.getKey(), d.getSourceId().substring(1));
		assertThat("Mqtt message topic", msg.getTopic(), is(equalTo(expectedTopic)));

		String msgBody = new String(msg.getPayload(), StandardCharsets.UTF_8);
		String expectedMsgBody = mapper.writeValueAsString(d);
		assertThat("Mqtt message body", msgBody, is(equalTo(expectedMsgBody)));
	}

	@Test
	public void publish_several() throws IOException {
		// GIVEN
		final int count = 5;
		final String sourceId = "/%s/foo".formatted(randomUUID().toString());
		List<OwnedGeneralNodeDatum> datum = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			OwnedGeneralNodeDatum d = new OwnedGeneralNodeDatum(TEST_USER_ID);
			d.setCreated(Instant.now().truncatedTo(ChronoUnit.HOURS).plusSeconds(i));
			d.setNodeId(TEST_NODE_ID);
			d.setSourceId(sourceId);
			DatumSamples s = new DatumSamples();
			s.putInstantaneousSampleValue("foo", 123);
			d.setSamples(s);
			datum.add(d);
		}

		DatumPublishEvent pubEvent = newPubEvent(OscpRole.CapacityProvider,
				UpdateGroupCapacityForecast.class.getSimpleName(), datum);

		givenMqttConnectionPublish();

		// WHEN
		publisher.asConsumer().accept(pubEvent);

		// THEN
		then(conn).should(times(count)).publish(msgCaptor.capture());

		String expectedTopic = SolarFluxDatumPublisher.NODE_AGGREGATE_DATUM_TOPIC_TEMPLATE
				.formatted(TEST_USER_ID, TEST_NODE_ID, Aggregation.None.getKey(), sourceId.substring(1));

		List<MqttMessage> msgs = msgCaptor.getAllValues();
		for ( int i = 0; i < count; i++ ) {
			MqttMessage msg = msgs.get(i);
			assertThat("Mqtt message %d topic".formatted(i), msg.getTopic(), is(equalTo(expectedTopic)));

			String msgBody = new String(msg.getPayload(), StandardCharsets.UTF_8);
			String expectedMsgBody = mapper.writeValueAsString(datum.get(i));
			assertThat("Mqtt message %d body".formatted(i), msgBody, is(equalTo(expectedMsgBody)));
		}
	}

}
