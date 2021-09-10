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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.codec.binary.Hex;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.mqtt.CBORFactoryBean;
import net.solarnetwork.central.in.mqtt.MqttDataCollector;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.test.CallingThreadExecutorService;
import net.solarnetwork.codec.BasicGeneralDatumDeserializer;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.netty.NettyMqttConnectionFactory;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.util.ObjectMapperFactoryBean;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link MqttDataCollector} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttDataCollectorTests_CBOR {

	private static final String TEST_CLIENT_ID = "solarnet.test";
	private static final Long TEST_NODE_ID = 123L;
	private ObjectMapper objectMapper;
	private ObjectMapper objectMapper2;
	private DataCollectorBiz dataCollectorBiz;
	private NodeInstructionDao nodeInstructionDao;
	private MqttDataCollector service;

	private static final class GeneralNodeDatumSerializer extends StdScalarSerializer<GeneralNodeDatum>
			implements Serializable {

		private static final long serialVersionUID = 1564431501442940772L;

		/**
		 * Default constructor.
		 */
		public GeneralNodeDatumSerializer() {
			super(GeneralNodeDatum.class);
		}

		@Override
		public void serialize(GeneralNodeDatum datum, JsonGenerator generator,
				SerializerProvider provider) throws IOException, JsonGenerationException {
			GeneralDatumSamples samples = datum.getSamples();

			generator.writeStartObject();
			if ( datum.getCreated() != null ) {
				generator.writeNumberField("created", datum.getCreated().getMillis());
			}

			if ( datum.getSourceId() != null ) {
				generator.writeStringField("sourceId", datum.getSourceId());
			}

			generator.writeObjectField("samples", samples);

			generator.writeEndObject();
		}
	}

	private ObjectMapper createObjectMapper(JsonFactory jsonFactory) {
		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		if ( jsonFactory != null ) {
			factory.setJsonFactory(jsonFactory);
		}
		factory.setDeserializers(Arrays.asList(BasicGeneralDatumDeserializer.INSTANCE));
		factory.setSerializers(Arrays.asList(new GeneralNodeDatumSerializer()));
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
		MqttMessage msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce, Hex.decodeHex(data));
		service.onMqttMessage(msg);

		// then
		assertThat("Datum posted", postDatumCaptor.getValue(), notNullValue());
		List<GeneralNodeDatum> postedDatumList = StreamSupport
				.stream(postDatumCaptor.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat("Posted datum count", postedDatumList, hasSize(1));
		GeneralNodeDatum postedDatum = postedDatumList.get(0);

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new DateTime(1576472400000L));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId("/DE/G2/GM/GEN/1");

		assertThat("Posted datum ID", postedDatum.getId(), equalTo(datum.getId()));
		GeneralNodeDatumSamples samples = postedDatum.getSamples();
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
		MqttMessage msg = new BasicMqttMessage(topic, false, MqttQos.AtLeastOnce, Hex.decodeHex(data));
		service.onMqttMessage(msg);

		// then
		assertThat("Datum posted", postDatumCaptor.getValue(), notNullValue());
		List<GeneralNodeDatum> postedDatumList = StreamSupport
				.stream(postDatumCaptor.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat("Posted datum count", postedDatumList, hasSize(1));
		GeneralNodeDatum postedDatum = postedDatumList.get(0);

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new DateTime(1576472400000L));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId("/DE/G2/GM/GEN/1");

		assertThat("Posted datum ID", postedDatum.getId(), equalTo(datum.getId()));
		GeneralNodeDatumSamples samples = postedDatum.getSamples();
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
		GeneralNodeDatumSamples s = new GeneralNodeDatumSamples();
		s.putInstantaneousSampleValue("foo", 123);
		s.addTag(MqttDataCollector.TAG_V2);
		s.addTag("bar");

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setSamples(s);
		datum.setCreated(new DateTime(1576472400000L));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId("/DE/G2/GM/GEN/1");

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

		assertThat("Posted datum ID", postedDatum.getId(), equalTo(datum.getId()));
		GeneralNodeDatumSamples samples = postedDatum.getSamples();
		assertThat("foo", samples.getInstantaneousSampleInteger("foo"), equalTo(123));
		assertThat("_v2 tag should have been removed, leaving bar tag",
				postedDatum.getSamples().getTags(), containsInAnyOrder("bar"));

	}

}
