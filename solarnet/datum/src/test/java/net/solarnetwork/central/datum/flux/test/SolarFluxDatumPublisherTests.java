/* ==================================================================
 * SolarFluxDatumPublisherTests.java - 28/02/2020 5:24:03 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.flux.test;

import static net.solarnetwork.central.datum.flux.SolarFluxDatumPublishCountStat.DailyDatumPublished;
import static net.solarnetwork.central.datum.flux.SolarFluxDatumPublishCountStat.HourlyDatumPublished;
import static net.solarnetwork.central.datum.flux.SolarFluxDatumPublishCountStat.MonthlyDatumPublished;
import static net.solarnetwork.central.datum.flux.SolarFluxDatumPublishCountStat.RawDatumPublished;
import static net.solarnetwork.central.domain.Aggregation.Day;
import static net.solarnetwork.central.domain.Aggregation.Hour;
import static net.solarnetwork.central.domain.Aggregation.Month;
import static net.solarnetwork.central.domain.Aggregation.None;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.moquette.interception.messages.InterceptPublishMessage;
import net.solarnetwork.central.datum.dao.DatumSupportDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;
import net.solarnetwork.central.datum.flux.SolarFluxDatumPublisher;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.netty.NettyMqttConnectionFactory;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.test.mqtt.MqttServerSupport;
import net.solarnetwork.test.mqtt.TestingInterceptHandler;

/**
 * Unit tests for the {@link SolarFluxDatumPublisher}.
 * 
 * @author matt
 * @version 2.0
 */
public class SolarFluxDatumPublisherTests extends MqttServerSupport {

	private static final int MQTT_TIMEOUT = 10;
	private static final String TEST_CLIENT_ID = "solarnet.test";
	private static final Long TEST_NODE_ID = -1L;
	private static final Long TEST_USER_ID = -9L;

	private DatumSupportDao datumSupportDao;
	private ObjectMapper objectMapper;
	private SolarFluxDatumPublisher publisher;

	private ObjectMapper createObjectMapper() {
		return JsonUtils.newDatumObjectMapper();
	}

	@Before
	public void setup() throws Exception {
		setupMqttServer();

		datumSupportDao = EasyMock.createMock(DatumSupportDao.class);

		objectMapper = createObjectMapper();

		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();

		NettyMqttConnectionFactory factory = new NettyMqttConnectionFactory(
				Executors.newCachedThreadPool(), scheduler);

		publisher = new SolarFluxDatumPublisher(factory, datumSupportDao, objectMapper);
		publisher.getMqttConfig().setClientId(TEST_CLIENT_ID);
		publisher.getMqttConfig().setServerUri(new URI("mqtt://localhost:" + getMqttServerPort()));
		Future<?> f = publisher.startup();
		f.get(MQTT_TIMEOUT, TimeUnit.SECONDS);
	}

	@After
	@Override
	public void teardown() {
		super.teardown();
		EasyMock.verify(datumSupportDao);
	}

	private void replayAll() {
		EasyMock.replay(datumSupportDao);
	}

	@Override
	public void stopMqttServer() {
		publisher.shutdown();
		super.stopMqttServer();
	}

	private String datumTopic(Long userId, Long nodeId, Aggregation agg, String sourceId) {
		if ( sourceId.startsWith("/") ) {
			sourceId = sourceId.substring(1);
		}
		return String.format("user/%d/node/%d/datum/%s/%s", userId, nodeId, agg.getKey(), sourceId);
	}

	private void assertPublishedDatumEqualTo(String msg, byte[] payload, GeneralNodeDatum datum)
			throws Exception {
		Map<String, ?> datumProps = datum.getSampleData();
		Set<String> expectedPropNames = new HashSet<>(Arrays.asList("created", "nodeId", "sourceId"));
		expectedPropNames.addAll(datumProps.keySet());
		Map<String, Object> map = JsonUtils.getStringMapFromTree(objectMapper.readTree(payload));
		assertThat(msg + " property keys", map.keySet(), equalTo(expectedPropNames));
		assertThat(msg + " created", map, hasEntry("created", datum.getCreated().toEpochMilli()));
		assertThat(msg + " nodeId", map, hasEntry("nodeId", datum.getNodeId().intValue()));
		assertThat(msg + " sourceId", map, hasEntry("sourceId", datum.getSourceId()));
	}

	@Test
	public void publishRawDatum() throws Exception {
		// GIVEN
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(Instant.now().truncatedTo(ChronoUnit.HOURS));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(UUID.randomUUID().toString());
		DatumSamples samples = new DatumSamples();
		samples.putInstantaneousSampleValue("foo", 123);
		samples.putAccumulatingSampleValue("bar", 234L);
		datum.setSamples(samples);

		expect(datumSupportDao.userIdForNodeId(TEST_NODE_ID)).andReturn(TEST_USER_ID);

		final TestingInterceptHandler session = getTestingInterceptHandler();

		// WHEN
		replayAll();
		boolean success = publisher.processDatum(datum, None);

		stopMqttServer(); // to flush messages

		// THEN
		assertThat("Datum published", success, equalTo(true));
		assertThat("Stat published count", publisher.getMqttStats().get(RawDatumPublished), equalTo(1L));
		assertThat("Only 1 message published", session.publishMessages, hasSize(1));
		InterceptPublishMessage msg = session.getPublishMessageAtIndex(0);
		assertThat(msg.getTopicName(),
				equalTo(datumTopic(TEST_USER_ID, datum.getNodeId(), None, datum.getSourceId())));
		assertPublishedDatumEqualTo("MQTT published datum", session.getPublishPayloadAtIndex(0), datum);
	}

	@Test
	public void publishHourDatum() throws Exception {
		// GIVEN
		ReportingGeneralNodeDatum datum = new ReportingGeneralNodeDatum();
		datum.setCreated(Instant.now().truncatedTo(ChronoUnit.HOURS));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(UUID.randomUUID().toString());
		DatumSamples samples = new DatumSamples();
		samples.putInstantaneousSampleValue("foo", 123);
		samples.putAccumulatingSampleValue("bar", 234L);
		datum.setSamples(samples);

		expect(datumSupportDao.userIdForNodeId(TEST_NODE_ID)).andReturn(TEST_USER_ID);

		final TestingInterceptHandler session = getTestingInterceptHandler();

		// WHEN
		replayAll();
		boolean success = publisher.processDatum(datum, Hour);

		stopMqttServer(); // to flush messages

		// THEN
		assertThat("Datum published", success, equalTo(true));
		assertThat("Stat published count", publisher.getMqttStats().get(HourlyDatumPublished),
				equalTo(1L));
		assertThat("Only 1 message published", session.publishMessages, hasSize(1));
		InterceptPublishMessage msg = session.getPublishMessageAtIndex(0);
		assertThat(msg.getTopicName(),
				equalTo(datumTopic(TEST_USER_ID, datum.getNodeId(), Hour, datum.getSourceId())));
		assertPublishedDatumEqualTo("MQTT published datum", session.getPublishPayloadAtIndex(0), datum);
	}

	@Test
	public void publishDayDatum() throws Exception {
		// GIVEN
		ReportingGeneralNodeDatum datum = new ReportingGeneralNodeDatum();
		datum.setCreated(Instant.now().truncatedTo(ChronoUnit.DAYS));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(UUID.randomUUID().toString());
		DatumSamples samples = new DatumSamples();
		samples.putInstantaneousSampleValue("foo", 123);
		samples.putAccumulatingSampleValue("bar", 234L);
		datum.setSamples(samples);

		expect(datumSupportDao.userIdForNodeId(TEST_NODE_ID)).andReturn(TEST_USER_ID);

		final TestingInterceptHandler session = getTestingInterceptHandler();

		// WHEN
		replayAll();
		boolean success = publisher.processDatum(datum, Day);

		stopMqttServer(); // to flush messages

		// THEN
		assertThat("Datum published", success, equalTo(true));
		assertThat("Stat published count", publisher.getMqttStats().get(DailyDatumPublished),
				equalTo(1L));
		assertThat("Only 1 message published", session.publishMessages, hasSize(1));
		InterceptPublishMessage msg = session.getPublishMessageAtIndex(0);
		assertThat(msg.getTopicName(),
				equalTo(datumTopic(TEST_USER_ID, datum.getNodeId(), Day, datum.getSourceId())));
		assertPublishedDatumEqualTo("MQTT published datum", session.getPublishPayloadAtIndex(0), datum);
	}

	@Test
	public void publishMonthDatum() throws Exception {
		// GIVEN
		ReportingGeneralNodeDatum datum = new ReportingGeneralNodeDatum();
		datum.setCreated(
				Instant.now().with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(UUID.randomUUID().toString());
		DatumSamples samples = new DatumSamples();
		samples.putInstantaneousSampleValue("foo", 123);
		samples.putAccumulatingSampleValue("bar", 234L);
		datum.setSamples(samples);

		expect(datumSupportDao.userIdForNodeId(TEST_NODE_ID)).andReturn(TEST_USER_ID);

		final TestingInterceptHandler session = getTestingInterceptHandler();

		// WHEN
		replayAll();
		boolean success = publisher.processDatum(datum, Month);

		stopMqttServer(); // to flush messages

		// THEN
		assertThat("Datum published", success, equalTo(true));
		assertThat("Stat published count", publisher.getMqttStats().get(MonthlyDatumPublished),
				equalTo(1L));
		assertThat("Only 1 message published", session.publishMessages, hasSize(1));
		InterceptPublishMessage msg = session.getPublishMessageAtIndex(0);
		assertThat(msg.getTopicName(),
				equalTo(datumTopic(TEST_USER_ID, datum.getNodeId(), Month, datum.getSourceId())));
		assertPublishedDatumEqualTo("MQTT published datum", session.getPublishPayloadAtIndex(0), datum);
	}

}
