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
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.mqtt.CBORFactoryBean;
import net.solarnetwork.central.in.mqtt.MqttDataCollector;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.test.CallingThreadExecutorService;
import net.solarnetwork.codec.BasicStreamDatumArrayDeserializer;
import net.solarnetwork.codec.BasicStreamDatumArraySerializer;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.netty.NettyMqttConnectionFactory;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.util.ObjectMapperFactoryBean;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for ingesting StreamDatum CBOR.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttDataCollectorTests_StreamDatum {

	private static final String TEST_CLIENT_ID = "solarnet.test";
	private static final Long TEST_NODE_ID = 123L;
	private ObjectMapper objectMapper;
	private ObjectMapper objectMapper2;
	private DataCollectorBiz dataCollectorBiz;
	private NodeInstructionDao nodeInstructionDao;
	private MqttDataCollector service;

	private ObjectMapper createObjectMapper(JsonFactory jsonFactory) {
		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		if ( jsonFactory != null ) {
			factory.setJsonFactory(jsonFactory);
		}
		factory.setSerializers(Arrays.asList(BasicStreamDatumArraySerializer.INSTANCE));
		factory.setDeserializers(Arrays.asList(BasicStreamDatumArrayDeserializer.INSTANCE));
		factory.setFeaturesToDisable(Arrays.asList(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
		try {
			return factory.getObject();
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	@Before
	public void setup() throws Exception {
		objectMapper = createObjectMapper(new CBORFactoryBean(false).getObject());
		objectMapper2 = createObjectMapper(new CBORFactoryBean(true).getObject());
		dataCollectorBiz = EasyMock.createMock(DataCollectorBiz.class);
		nodeInstructionDao = EasyMock.createMock(NodeInstructionDao.class);

		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();

		NettyMqttConnectionFactory factory = new NettyMqttConnectionFactory(
				Executors.newCachedThreadPool(), scheduler);

		service = new MqttDataCollector(factory, objectMapper, objectMapper2,
				new CallingThreadExecutorService(), dataCollectorBiz,
				new StaticOptionalService<NodeInstructionDao>(nodeInstructionDao), null);
		service.getMqttConfig().setClientId(TEST_CLIENT_ID);
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

}
