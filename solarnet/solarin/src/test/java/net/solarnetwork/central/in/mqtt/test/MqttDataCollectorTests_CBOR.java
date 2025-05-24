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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.mqtt.MqttDataCollector;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.util.StatTracker;

/**
 * Test cases for the {@link MqttDataCollector} class.
 * 
 * @author matt
 * @version 2.2
 */
public class MqttDataCollectorTests_CBOR {

	private static final Long TEST_NODE_ID = 123L;
	private ObjectMapper objectMapper;
	private DataCollectorBiz dataCollectorBiz;
	private NodeInstructionDao nodeInstructionDao;
	private MqttDataCollector service;

	private ObjectMapper createObjectMapper(JsonFactory jsonFactory) {
		return JsonUtils.newDatumObjectMapper(jsonFactory);
	}

	@BeforeEach
	public void setup() throws Exception {
		objectMapper = createObjectMapper(new CBORFactory());
		dataCollectorBiz = EasyMock.createMock(DataCollectorBiz.class);
		nodeInstructionDao = EasyMock.createMock(NodeInstructionDao.class);

		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();

		StatTracker mqttStats = new StatTracker("Test", null, LoggerFactory.getLogger(getClass()), 1);

		service = new MqttDataCollector(objectMapper, dataCollectorBiz, nodeInstructionDao, mqttStats);
	}

	@AfterEach
	public void teardown() {
		EasyMock.verify(dataCollectorBiz, nodeInstructionDao);
	}

	private void replayAll() {
		EasyMock.replay(dataCollectorBiz, nodeInstructionDao);
	}

	private String datumTopic(Long nodeId) {
		return String.format(MqttDataCollector.DEFAULT_NODE_DATUM_TOPIC_TEMPLATE, nodeId);
	}

	@Test
	public void processGeneralNodeDatum_legacy() throws Exception {
		// given
		Capture<Iterable<GeneralNodeDatum>> postDatumCaptor = new Capture<>();
		dataCollectorBiz.postGeneralNodeDatum(capture(postDatumCaptor));

		replayAll();

		// this does hot have  t:["_v2"] included, so incorrectly encoded positive typed big decimal exponents
		String data = "A467637265617465641B0000016F0D13D080666E6F6465496419016D68736F7572636549646F2F44452F47322F474D2F47454E2F316773616D706C6573A26169A365766F6C7473C482031A0004503B657761747473C482031A0027B8B4696672657175656E6379183C6161A26977617474486F757273C482031A027F57DF7077617474486F757273526576657273651905A0";

		// when
		String topic = datumTopic(TEST_NODE_ID);
		MqttMessage msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce,
				HexFormat.of().parseHex(data));
		service.onMqttMessage(msg);

		// then
		assertThat("Datum posted", postDatumCaptor.getValue(), notNullValue());
		List<GeneralNodeDatum> postedDatumList = StreamSupport
				.stream(postDatumCaptor.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat("Posted datum count", postedDatumList, hasSize(1));
		GeneralNodeDatum postedDatum = postedDatumList.get(0);

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(Instant.ofEpochMilli(1576472400000L));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId("/DE/G2/GM/GEN/1");

		assertThat("Posted datum ID", postedDatum.getId(), equalTo(datum.getId()));
		DatumSamples samples = postedDatum.getSamples();
		assertThat("volts", samples.getInstantaneousSampleBigDecimal("volts"),
				equalTo(new BigDecimal(new BigInteger("282683"), 3)));
		assertThat("watts", samples.getInstantaneousSampleBigDecimal("watts"),
				equalTo(new BigDecimal(new BigInteger("2603188"), 3)));
		assertThat("frequency", samples.getInstantaneousSampleInteger("frequency"), equalTo(60));
		assertThat("wattHours", samples.getAccumulatingSampleBigDecimal("wattHours"),
				equalTo(new BigDecimal(new BigInteger("41899999"), 3)));
		assertThat("wattHoursReverse", samples.getAccumulatingSampleInteger("wattHoursReverse"),
				equalTo(1440));
	}

	@Test
	public void processGeneralNodeDatum_v2() throws Exception {
		// given
		Capture<Iterable<GeneralNodeDatum>> postDatumCaptor = new Capture<>();
		dataCollectorBiz.postGeneralNodeDatum(capture(postDatumCaptor));

		replayAll();

		// this has t:["_v2"] included, and negative typed big decimal exponents
		String data = "A467637265617465641B0000016F0D13D080666E6F6465496419016D68736F7572636549646F2F44452F47322F474D2F47454E2F316773616D706C6573A36169A365766F6C7473C482221A0004503B657761747473C482221A0027B8B4696672657175656E6379183C6161A26977617474486F757273C482221A027F57DF7077617474486F757273526576657273651905A0617481635F7632";

		// when
		String topic = datumTopic(TEST_NODE_ID);
		MqttMessage msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce,
				HexFormat.of().parseHex(data));
		service.onMqttMessage(msg);

		// then
		assertThat("Datum posted", postDatumCaptor.getValue(), notNullValue());
		List<GeneralNodeDatum> postedDatumList = StreamSupport
				.stream(postDatumCaptor.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat("Posted datum count", postedDatumList, hasSize(1));
		GeneralNodeDatum postedDatum = postedDatumList.get(0);

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(Instant.ofEpochMilli(1576472400000L));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId("/DE/G2/GM/GEN/1");

		assertThat("Posted datum ID", postedDatum.getId(), equalTo(datum.getId()));
		DatumSamples samples = postedDatum.getSamples();
		assertThat("volts", samples.getInstantaneousSampleBigDecimal("volts"),
				equalTo(new BigDecimal(new BigInteger("282683"), 3)));
		assertThat("watts", samples.getInstantaneousSampleBigDecimal("watts"),
				equalTo(new BigDecimal(new BigInteger("2603188"), 3)));
		assertThat("frequency", samples.getInstantaneousSampleInteger("frequency"), equalTo(60));
		assertThat("wattHours", samples.getAccumulatingSampleBigDecimal("wattHours"),
				equalTo(new BigDecimal(new BigInteger("41899999"), 3)));
		assertThat("wattHoursReverse", samples.getAccumulatingSampleInteger("wattHoursReverse"),
				equalTo(1440));
		assertThat("_v2 tag should have been removed", postedDatum.getSamples().getTags(), nullValue());
	}

	@Test
	public void processGeneralNodeDatum_v2_otherTags() throws Exception {
		// GIVEN
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("foo", 123);
		s.addTag(MqttDataCollector.TAG_V2);
		s.addTag("bar");

		GeneralDatum datum = GeneralDatum.nodeDatum(TEST_NODE_ID, "/DE/G2/GM/GEN/1",
				Instant.ofEpochMilli(1576472400000L), s);
		byte[] data = objectMapper.writeValueAsBytes(datum);

		Capture<Iterable<GeneralNodeDatum>> postDatumCaptor = new Capture<>();
		dataCollectorBiz.postGeneralNodeDatum(capture(postDatumCaptor));

		// WHEN
		replayAll();
		String topic = datumTopic(TEST_NODE_ID);
		MqttMessage msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce, data);
		service.onMqttMessage(msg);

		// then
		assertThat("Datum posted", postDatumCaptor.getValue(), notNullValue());
		List<GeneralNodeDatum> postedDatumList = StreamSupport
				.stream(postDatumCaptor.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat("Posted datum count", postedDatumList, hasSize(1));
		GeneralNodeDatum postedDatum = postedDatumList.get(0);

		assertThat("Posted datum ID", postedDatum.getId(), equalTo(
				new GeneralNodeDatumPK(datum.getObjectId(), datum.getTimestamp(), datum.getSourceId())));
		DatumSamples samples = postedDatum.getSamples();
		assertThat("foo", samples.getInstantaneousSampleInteger("foo"), equalTo(123));
		assertThat("_v2 tag should have been removed, leaving bar tag",
				postedDatum.getSamples().getTags(), containsInAnyOrder("bar"));

	}

}
