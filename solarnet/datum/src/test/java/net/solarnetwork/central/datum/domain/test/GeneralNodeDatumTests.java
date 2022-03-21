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

package net.solarnetwork.central.datum.domain.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * Test cases for the {@link GeneralNodeDatum} class.
 * 
 * @author matt
 * @version 2.0
 */
public class GeneralNodeDatumTests {

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

	private GeneralNodeDatum getTestInstance() {
		GeneralNodeDatum datum = new GeneralNodeDatum();
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
	public void serializeJson() throws Exception {
		String json = objectMapper.writeValueAsString(getTestInstance());
		assertThat(json,
				is("{\"created\":\"" + TEST_TIMESTAMP_STRING
						+ "\",\"nodeId\":-1,\"sourceId\":\"test.source\","
						+ "\"watts\":231,\"watt_hours\":4123}"));
	}

	@Test
	public void serializeJsonWithTags() throws Exception {
		GeneralNodeDatum datum = getTestInstance();
		datum.getSamples().addTag("test");
		String json = objectMapper.writeValueAsString(datum);
		assertThat(json,
				is("{\"created\":\"" + TEST_TIMESTAMP_STRING
						+ "\",\"nodeId\":-1,\"sourceId\":\"test.source\","
						+ "\"watts\":231,\"watt_hours\":4123,\"tags\":[\"test\"]}"));
	}

	@Test
	public void deserializeJson() throws Exception {
		String json = "{\"created\":\"" + TEST_TIMESTAMP_STRING
				+ "\",\"sourceId\":\"Main\",\"samples\":{\"i\":{\"watts\":89, \"temp\":21.2},\"s\":{\"ploc\":2502287}}}";
		GeneralNodeDatum datum = objectMapper.readValue(json, GeneralNodeDatum.class);
		assertThat(datum, is(notNullValue()));
		assertThat(datum.getCreated(), is(TEST_TIMESTAMP));
		assertThat(datum.getSourceId(), is("Main"));
		assertThat(datum.getSamples(), is(notNullValue()));
		assertThat(datum.getSamples().getInstantaneousSampleInteger("watts"), is(89));
		assertThat(datum.getSamples().getStatusSampleLong("ploc"), is(2502287L));
		assertThat(datum.getSamples().getInstantaneousSampleBigDecimal("temp"),
				is(new BigDecimal("21.2")));
	}

}
