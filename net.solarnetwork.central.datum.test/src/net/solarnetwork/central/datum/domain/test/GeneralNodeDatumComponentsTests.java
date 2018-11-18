/* ==================================================================
 * GeneralNodeDatumComponentsTests.java - 13/11/2018 7:30:20 AM
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

package net.solarnetwork.central.datum.domain.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumComponents;
import net.solarnetwork.domain.GeneralNodeDatumSamples;

/**
 * Test cases for the {@link GeneralNodeDatumComponents} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeDatumComponentsTests {

	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_ID = "test.source";

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = new ObjectMapper().registerModule(new JodaModule())
				.setSerializationInclusion(Include.NON_NULL)
				.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
	}

	private GeneralNodeDatumComponents getTestInstance() {
		GeneralNodeDatumComponents datum = new GeneralNodeDatumComponents();
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
	public void copy() {
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setCreated(new DateTime());
		d.setNodeId(TEST_NODE_ID);
		d.setPosted(new DateTime());
		d.setSourceId(TEST_SOURCE_ID);
		d.setSamples(new GeneralNodeDatumSamples());

		GeneralNodeDatumComponents dc = new GeneralNodeDatumComponents(d);
		assertThat(dc.getCreated(), sameInstance(d.getCreated()));
		assertThat(dc.getPosted(), sameInstance(d.getPosted()));
		assertThat(dc.getNodeId(), sameInstance(d.getNodeId()));
		assertThat(dc.getSourceId(), sameInstance(d.getSourceId()));
		assertThat(dc.getSamples(), sameInstance(d.getSamples()));
	}

	@Test
	public void serializeJson() throws Exception {
		String json = objectMapper.writeValueAsString(getTestInstance());
		assertThat("{\"created\":1408665600000,\"nodeId\":-1,\"sourceId\":\"test.source\","
				+ "\"i\":{\"watts\":231},\"a\":{\"watt_hours\":4123}}", equalTo(json));
	}

	@Test
	public void serializeJsonWithTags() throws Exception {
		GeneralNodeDatumComponents datum = getTestInstance();
		datum.getSamples().addTag("test");
		String json = objectMapper.writeValueAsString(datum);
		assertThat(
				"{\"created\":1408665600000,\"nodeId\":-1,\"sourceId\":\"test.source\","
						+ "\"i\":{\"watts\":231},\"a\":{\"watt_hours\":4123},\"t\":[\"test\"]}",
				equalTo(json));
	}

	@Test
	public void deserializeJson() throws Exception {
		String json = "{\"created\":1408665600000,\"sourceId\":\"Main\",\"samples\":{\"i\":{\"watts\":89, \"temp\":21.2},\"s\":{\"ploc\":2502287}}}";
		GeneralNodeDatumComponents datum = objectMapper.readValue(json,
				GeneralNodeDatumComponents.class);
		assertThat(datum, notNullValue());
		assertThat(datum.getCreated(), notNullValue());
		assertThat(1408665600000L, equalTo(datum.getCreated().getMillis()));
		assertThat("Main", equalTo(datum.getSourceId()));
		assertThat(datum.getSamples(), notNullValue());
		assertThat(Integer.valueOf(89),
				equalTo(datum.getSamples().getInstantaneousSampleInteger("watts")));
		assertThat(Long.valueOf(2502287), equalTo(datum.getSamples().getStatusSampleLong("ploc")));
		assertThat(new BigDecimal("21.2"),
				equalTo(datum.getSamples().getInstantaneousSampleBigDecimal("temp")));
	}

}
