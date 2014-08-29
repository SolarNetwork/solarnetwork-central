/* ==================================================================
 * GeneralNodeDatumTests.java - Aug 22, 2014 3:15:33 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link GeneralNodeDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeDatumTests {

	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_ID = "test.source";

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Inclusion.NON_NULL);
		objectMapper.setDeserializationConfig(objectMapper.getDeserializationConfig().with(
				DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS));

	}

	private GeneralNodeDatum getTestInstance() {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new DateTime(2014, 8, 22, 12, 0, 0, 0));
		datum.setNodeId(TEST_NODE_ID);
		datum.setPosted(datum.getCreated());
		datum.setSourceId(TEST_SOURCE_ID);

		GeneralNodeDatumSamples samples = new GeneralNodeDatumSamples();
		datum.setSamples(samples);

		// some sample data
		Map<String, Number> instants = new HashMap<String, Number>(2);
		instants.put("watts", 231);
		samples.setInstantaneous(instants);

		Map<String, Number> accum = new HashMap<String, Number>(2);
		accum.put("watt_hours", 4123);
		samples.setAccumulating(accum);

		return datum;
	}

	@Test
	public void serializeJson() throws Exception {
		String json = objectMapper.writeValueAsString(getTestInstance());
		Assert.assertEquals("{\"created\":1408665600000,\"nodeId\":-1,\"sourceId\":\"test.source\","
				+ "\"watts\":231,\"watt_hours\":4123}", json);
	}

	@Test
	public void serializeJsonWithTags() throws Exception {
		GeneralNodeDatum datum = getTestInstance();
		datum.getSamples().addTag("test");
		String json = objectMapper.writeValueAsString(datum);
		Assert.assertEquals("{\"created\":1408665600000,\"nodeId\":-1,\"sourceId\":\"test.source\","
				+ "\"watts\":231,\"watt_hours\":4123,\"tags\":[\"test\"]}", json);
	}

	@Test
	public void deserializeJson() throws Exception {
		String json = "{\"created\":1408665600000,\"sourceId\":\"Main\",\"samples\":{\"i\":{\"watts\":89, \"temp\":21.2},\"s\":{\"ploc\":2502287}}}";
		GeneralNodeDatum datum = objectMapper.readValue(json, GeneralNodeDatum.class);
		Assert.assertNotNull(datum);
		Assert.assertNotNull(datum.getCreated());
		Assert.assertEquals(1408665600000L, datum.getCreated().getMillis());
		Assert.assertEquals("Main", datum.getSourceId());
		Assert.assertNotNull(datum.getSamples());
		Assert.assertEquals(Integer.valueOf(89),
				datum.getSamples().getInstantaneousSampleInteger("watts"));
		Assert.assertEquals(Long.valueOf(2502287), datum.getSamples().getStatusSampleLong("ploc"));
		Assert.assertEquals(new BigDecimal("21.2"),
				datum.getSamples().getInstantaneousSampleBigDecimal("temp"));
	}

}
