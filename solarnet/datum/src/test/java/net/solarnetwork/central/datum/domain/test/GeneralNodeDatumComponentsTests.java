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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumComponents;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * Test cases for the {@link GeneralNodeDatumComponents} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeDatumComponentsTests {

	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_ID = "test.source";
	private static final LocalDateTime TEST_DATE = LocalDateTime.of(2014, 8, 22, 12, 1, 2,
			(int) TimeUnit.MILLISECONDS.toNanos(345));
	private static final Instant TEST_TIMESTAMP = TEST_DATE.toInstant(ZoneOffset.UTC);
	private static final String TEST_TIMESTAMP_STRING = "2014-08-22 12:01:02.345Z";

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = DatumJsonUtils.newDatumObjectMapper();
	}

	private GeneralNodeDatumComponents getTestInstance() {
		GeneralNodeDatumComponents datum = new GeneralNodeDatumComponents();
		datum.setCreated(TEST_TIMESTAMP);
		datum.setNodeId(TEST_NODE_ID);
		datum.setPosted(datum.getCreated());
		datum.setSourceId(TEST_SOURCE_ID);

		DatumSamples samples = new DatumSamples();
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
		d.setCreated(Instant.now());
		d.setNodeId(TEST_NODE_ID);
		d.setPosted(Instant.now());
		d.setSourceId(TEST_SOURCE_ID);
		d.setSamples(new DatumSamples());

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
		assertThat("{\"created\":\"" + TEST_TIMESTAMP_STRING
				+ "\",\"nodeId\":-1,\"sourceId\":\"test.source\","
				+ "\"i\":{\"watts\":231},\"a\":{\"watt_hours\":4123}}", equalTo(json));
	}

	@Test
	public void serializeJsonWithTags() throws Exception {
		GeneralNodeDatumComponents datum = getTestInstance();
		datum.getSamples().addTag("test");
		String json = objectMapper.writeValueAsString(datum);
		assertThat(
				"{\"created\":\"" + TEST_TIMESTAMP_STRING
						+ "\",\"nodeId\":-1,\"sourceId\":\"test.source\","
						+ "\"i\":{\"watts\":231},\"a\":{\"watt_hours\":4123},\"t\":[\"test\"]}",
				equalTo(json));
	}

	@Test
	public void deserializeJson() throws Exception {
		String json = "{\"created\":\"" + TEST_TIMESTAMP_STRING
				+ "\",\"sourceId\":\"Main\",\"samples\":{\"i\":{\"watts\":89, \"temp\":21.2},\"s\":{\"ploc\":2502287}}}";
		GeneralNodeDatumComponents datum = objectMapper.readValue(json,
				GeneralNodeDatumComponents.class);
		assertThat(datum, is(notNullValue()));
		assertThat(datum.getCreated(), is(TEST_TIMESTAMP));
		assertThat(datum.getSourceId(), is("Main"));
		assertThat(datum.getSamples(), notNullValue());
		assertThat(datum.getSamples().getInstantaneousSampleInteger("watts"), is(89));
		assertThat(datum.getSamples().getStatusSampleLong("ploc"), is(2502287L));
		assertThat(datum.getSamples().getInstantaneousSampleBigDecimal("temp"),
				is(new BigDecimal("21.2")));
	}

}
