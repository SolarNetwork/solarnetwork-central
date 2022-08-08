/* ==================================================================
 * NqttDataCollectorTests_StreamDatum.java - 5/06/2021 6:53:37 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.easymock.EasyMock.capture;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.mqtt.MqttDataCollector;
import net.solarnetwork.central.in.mqtt.SolarInCountStat;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.MqttStats;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Test cases for ingesting StreamDatum CBOR.
 * 
 * @author matt
 * @version 2.0
 */
public class MqttDataCollectorTests_StreamDatum {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final Long TEST_NODE_ID = 123L;
	private ObjectMapper objectMapper;
	private DataCollectorBiz dataCollectorBiz;
	private NodeInstructionDao nodeInstructionDao;
	private MqttDataCollector service;

	private ObjectMapper createObjectMapper(JsonFactory jsonFactory) {
		return JsonUtils.newDatumObjectMapper(jsonFactory);
	}

	@Before
	public void setup() throws Exception {
		objectMapper = createObjectMapper(new CBORFactory());
		dataCollectorBiz = EasyMock.createMock(DataCollectorBiz.class);
		nodeInstructionDao = EasyMock.createMock(NodeInstructionDao.class);

		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();

		MqttStats mqttStats = new MqttStats(1, SolarInCountStat.values());

		service = new MqttDataCollector(objectMapper, dataCollectorBiz, nodeInstructionDao, mqttStats);
	}

	@After
	public void teardown() {
		EasyMock.verify(dataCollectorBiz, nodeInstructionDao);
	}

	private void replayAll() {
		EasyMock.replay(dataCollectorBiz, nodeInstructionDao);
	}

	private String datumTopic(Long nodeId) {
		return String.format(MqttDataCollector.DEFAULT_NODE_DATUM_TOPIC_TEMPLATE, nodeId);
	}

	private StreamDatum createTestDatum() {
		DatumProperties p = propertiesOf(decimalArray("1.23", "2.34"), decimalArray("3.45", "4.56"),
				new String[] { "one", "two" }, new String[] { "three" });
		return new BasicStreamDatum(UUID.randomUUID(), Instant.now(), p);
	}

	private byte[] getCborMessage(StreamDatum datum) {
		try {
			return objectMapper.writeValueAsBytes(datum);
		} catch ( JsonProcessingException e ) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void processStreamDatum() throws Exception {
		// GIVEN
		Capture<Iterable<StreamDatum>> postDatumCaptor = new Capture<>();
		dataCollectorBiz.postStreamDatum(capture(postDatumCaptor));

		replayAll();

		StreamDatum d = createTestDatum();
		byte[] data = getCborMessage(d);

		// when
		String topic = datumTopic(TEST_NODE_ID);
		MqttMessage msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce, data);
		service.onMqttMessage(msg);

		// then
		assertThat("Datum posted", postDatumCaptor.getValue(), notNullValue());
		List<StreamDatum> postedDatumList = stream(postDatumCaptor.getValue().spliterator(), false)
				.collect(toList());
		assertThat("Posted datum count", postedDatumList, hasSize(1));
		StreamDatum postedDatum = postedDatumList.get(0);

		assertThat("Posted datum stream ID", postedDatum.getStreamId(), is(equalTo(d.getStreamId())));
		DatumProperties props = postedDatum.getProperties();
		assertThat("Instantaneous", props.getInstantaneous(),
				is(arrayContaining(d.getProperties().getInstantaneous())));
		assertThat("Accumulating", props.getAccumulating(),
				is(arrayContaining(d.getProperties().getAccumulating())));
		assertThat("Status", props.getStatus(), is(arrayContaining(d.getProperties().getStatus())));
		assertThat("Tags", props.getTags(), is(arrayContaining(d.getProperties().getTags())));
	}

	@Test
	public void compareDatumSize() throws Exception {
		// GIVEN
		final long now = System.currentTimeMillis();

		GeneralNodeDatum general = new GeneralNodeDatum();
		general.setNodeId(TEST_NODE_ID);
		general.setSourceId("OS Stats");
		general.setCreated(Instant.ofEpochMilli(now));
		general.setSampleJson(
				"{\"i\":{\"cpu_user\":0, \"cpu_system\":0, \"cpu_idle\":100, \"fs_size_/\":3651829760, \"fs_used_/\":855937024, \"fs_used_percent_/\":24, \"fs_size_/run\":484552704, \"fs_used_/run\":11993088, \"fs_used_percent_/run\":3, \"ram_total\":969105408, \"ram_avail\":615411712, \"ram_used_percent\":36.5, \"sys_load_1min\":0.19, \"sys_load_5min\":0.70, \"sys_load_15min\":0.48, \"net_bytes_in_eth0\":0, \"net_bytes_out_eth0\":0, \"net_packets_in_eth0\":0, \"net_packets_out_eth0\":0},\"a\":{\"sys_up\":679245.22}}");

		DatumProperties p = DatumProperties.propertiesOf(decimalArray("0", "0", "100", "3651829760",
				"855937024", "24", "484552704", "11993088", "3", "969105408", "615411712", "36.5",
				"0.19", "0.70", "0.48", "0", "0", "0", "0"), decimalArray("679245.22"), null, null);
		BasicStreamDatum stream = new BasicStreamDatum(UUID.randomUUID(), Instant.ofEpochMilli(now), p);

		// WHEN
		replayAll();
		final byte[] generalData = objectMapper.writeValueAsBytes(general);
		final byte[] streamData = objectMapper.writeValueAsBytes(stream);

		// THEN
		log.info("General datum size: {}, stream datum size: {}", generalData.length, streamData.length);

	}

}
