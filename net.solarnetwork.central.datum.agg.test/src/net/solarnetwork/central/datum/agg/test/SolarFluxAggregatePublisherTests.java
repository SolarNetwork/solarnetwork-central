/* ==================================================================
 * SolarFluxAggregatePublisherTests.java - 4/11/2019 4:46:56 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.agg.test;

import static java.util.Arrays.asList;
import static net.solarnetwork.central.datum.agg.SolarFluxAggregatePublishCountStat.DailyDatumPublished;
import static net.solarnetwork.central.datum.agg.SolarFluxAggregatePublishCountStat.HourlyDatumPublished;
import static net.solarnetwork.central.datum.agg.SolarFluxAggregatePublishCountStat.MonthlyDatumPublished;
import static net.solarnetwork.central.domain.Aggregation.Day;
import static net.solarnetwork.central.domain.Aggregation.Hour;
import static net.solarnetwork.central.domain.Aggregation.Month;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import io.moquette.interception.messages.InterceptPublishMessage;
import net.solarnetwork.central.datum.agg.SolarFluxAggregatePublisher;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.central.test.CallingThreadExecutorService;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.test.mqtt.MqttServerSupport;
import net.solarnetwork.test.mqtt.TestingInterceptHandler;
import net.solarnetwork.util.JodaDateTimeSerializer;
import net.solarnetwork.util.ObjectMapperFactoryBean;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class SolarFluxAggregatePublisherTests extends MqttServerSupport {

	private static final String TEST_CLIENT_ID = "solarnet.test";
	private static final Long TEST_NODE_ID = -1L;

	private JodaDateTimeSerializer dateTimeSerializer;
	private ObjectMapper objectMapper;
	private SolarFluxAggregatePublisher publisher;

	private ObjectMapper createObjectMapper() {
		dateTimeSerializer = new net.solarnetwork.util.JodaDateTimeSerializer();
		net.solarnetwork.util.ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		factory.setJsonFactory(new CBORFactory());
		factory.setSerializers(
				asList(dateTimeSerializer, new net.solarnetwork.util.JodaLocalDateSerializer(),
						new net.solarnetwork.util.JodaLocalDateTimeSerializer(),
						new net.solarnetwork.util.JodaLocalTimeSerializer()));
		factory.setFeaturesToDisable(asList(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
		try {
			return factory.getObject();
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	@Before
	public void setup() {
		setupMqttServer();

		objectMapper = createObjectMapper();

		publisher = new SolarFluxAggregatePublisher(new CallingThreadExecutorService(), null, false,
				objectMapper);
		publisher.setPersistencePath(System.getProperty("java.io.tmpdir"));
		publisher.setClientId(TEST_CLIENT_ID);
		publisher.setServerUri("mqtt://localhost:" + getMqttServerPort());
		publisher.init();
	}

	@Override
	public void stopMqttServer() {
		publisher.close();
		super.stopMqttServer();
	}

	private String datumAggTopic(Long nodeId, Aggregation agg, String sourceId) {
		if ( sourceId.startsWith("/") ) {
			sourceId = sourceId.substring(1);
		}
		return String.format("node/%d/datum/%s/%s", nodeId, agg.getKey(), sourceId);
	}

	private void assertPublishedDatumEqualTo(String msg, byte[] payload, ReportingGeneralNodeDatum datum)
			throws Exception {
		Map<String, ?> datumProps = datum.getSampleData();
		Set<String> expectedPropNames = new HashSet<>(Arrays.asList("created", "nodeId", "sourceId"));
		expectedPropNames.addAll(datumProps.keySet());
		Map<String, Object> map = JsonUtils.getStringMapFromTree(objectMapper.readTree(payload));
		assertThat(msg + " property keys", map.keySet(), equalTo(expectedPropNames));
		assertThat(msg + " created", map,
				hasEntry("created", dateTimeSerializer.serializeWithFormatter(datum.getCreated())));
		assertThat(msg + " nodeId", map, hasEntry("nodeId", datum.getNodeId().intValue()));
		assertThat(msg + " sourceId", map, hasEntry("sourceId", datum.getSourceId()));
	}

	@Test
	public void publishHourDatum() throws Exception {
		// GIVEN
		ReportingGeneralNodeDatum datum = new ReportingGeneralNodeDatum();
		datum.setCreated(new DateTime().hourOfDay().roundFloorCopy());
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(UUID.randomUUID().toString());
		GeneralNodeDatumSamples samples = new GeneralNodeDatumSamples();
		samples.putInstantaneousSampleValue("foo", 123);
		samples.putAccumulatingSampleValue("bar", 234L);
		datum.setSamples(samples);

		final TestingInterceptHandler session = getTestingInterceptHandler();

		// WHEN
		boolean success = publisher.processStaleAggregateDatum(Hour, datum);

		stopMqttServer(); // to flush messages

		// THEN
		assertThat("Datum published", success, equalTo(true));
		assertThat("Stat published count", publisher.getStats().get(HourlyDatumPublished), equalTo(1L));
		assertThat("Only 1 message published", session.publishMessages, hasSize(1));
		InterceptPublishMessage msg = session.getPublishMessageAtIndex(0);
		assertThat(msg.getTopicName(),
				equalTo(datumAggTopic(datum.getNodeId(), Hour, datum.getSourceId())));
		assertPublishedDatumEqualTo("MQTT published datum", session.getPublishPayloadAtIndex(0), datum);
	}

	@Test
	public void publishDayDatum() throws Exception {
		// GIVEN
		ReportingGeneralNodeDatum datum = new ReportingGeneralNodeDatum();
		datum.setCreated(new DateTime().dayOfMonth().roundFloorCopy());
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(UUID.randomUUID().toString());
		GeneralNodeDatumSamples samples = new GeneralNodeDatumSamples();
		samples.putInstantaneousSampleValue("foo", 123);
		samples.putAccumulatingSampleValue("bar", 234L);
		datum.setSamples(samples);

		final TestingInterceptHandler session = getTestingInterceptHandler();

		// WHEN
		boolean success = publisher.processStaleAggregateDatum(Day, datum);

		stopMqttServer(); // to flush messages

		// THEN
		assertThat("Datum published", success, equalTo(true));
		assertThat("Stat published count", publisher.getStats().get(DailyDatumPublished), equalTo(1L));
		assertThat("Only 1 message published", session.publishMessages, hasSize(1));
		InterceptPublishMessage msg = session.getPublishMessageAtIndex(0);
		assertThat(msg.getTopicName(),
				equalTo(datumAggTopic(datum.getNodeId(), Day, datum.getSourceId())));
		assertPublishedDatumEqualTo("MQTT published datum", session.getPublishPayloadAtIndex(0), datum);
	}

	@Test
	public void publishMonthDatum() throws Exception {
		// GIVEN
		ReportingGeneralNodeDatum datum = new ReportingGeneralNodeDatum();
		datum.setCreated(new DateTime().monthOfYear().roundFloorCopy());
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(UUID.randomUUID().toString());
		GeneralNodeDatumSamples samples = new GeneralNodeDatumSamples();
		samples.putInstantaneousSampleValue("foo", 123);
		samples.putAccumulatingSampleValue("bar", 234L);
		datum.setSamples(samples);

		final TestingInterceptHandler session = getTestingInterceptHandler();

		// WHEN
		boolean success = publisher.processStaleAggregateDatum(Month, datum);

		stopMqttServer(); // to flush messages

		// THEN
		assertThat("Datum published", success, equalTo(true));
		assertThat("Stat published count", publisher.getStats().get(MonthlyDatumPublished), equalTo(1L));
		assertThat("Only 1 message published", session.publishMessages, hasSize(1));
		InterceptPublishMessage msg = session.getPublishMessageAtIndex(0);
		assertThat(msg.getTopicName(),
				equalTo(datumAggTopic(datum.getNodeId(), Month, datum.getSourceId())));
		assertPublishedDatumEqualTo("MQTT published datum", session.getPublishPayloadAtIndex(0), datum);
	}
}
