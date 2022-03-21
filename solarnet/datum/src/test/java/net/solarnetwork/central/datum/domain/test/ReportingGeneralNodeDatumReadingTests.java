/* ==================================================================
 * ReportingGeneralNodeDatumReadingTests.java - 13/02/2019 1:54:10 pm
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

package net.solarnetwork.central.datum.domain.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumReading;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * Test cases for the {@link ReportingGeneralNodeDatumReading} class.
 * 
 * @author matt
 * @version 2.0
 * @since 1.10
 */
public class ReportingGeneralNodeDatumReadingTests {

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

	private ReportingGeneralNodeDatumReading getTestInstance() {
		ReportingGeneralNodeDatumReading datum = new ReportingGeneralNodeDatumReading();
		datum.setCreated(TEST_TIMESTAMP);
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(TEST_SOURCE_ID);

		DatumSamples starting = new DatumSamples();
		starting.putAccumulatingSampleValue("watt_hours", 4231);
		datum.setSamplesStart(starting);

		DatumSamples ending = new DatumSamples();
		ending.putAccumulatingSampleValue("watt_hours", 4321);
		datum.setSamplesFinal(ending);

		DatumSamples samples = new DatumSamples();
		samples.putInstantaneousSampleValue("watts", 276);
		samples.putAccumulatingSampleValue("watt_hours", 90);
		datum.setSamples(samples);

		return datum;
	}

	@Test
	public void serializeJson() throws Exception {
		String json = objectMapper.writeValueAsString(getTestInstance());
		assertThat("JSON", json, equalTo("{\"created\":\"" + TEST_TIMESTAMP_STRING
				+ "\",\"nodeId\":-1,\"sourceId\":\"test.source\""
				+ ",\"watts\":276,\"watt_hours\":90,\"watt_hours_start\":4231,\"watt_hours_end\":4321}"));
	}

	@Test
	public void deserializeJson() throws Exception {
		String json = "{\"created\":\"" + TEST_TIMESTAMP_STRING
				+ "\",\"nodeId\":-1,\"sourceId\":\"Main\""
				+ ",\"samples\":{\"i\":{\"watts\":123},\"a\":{\"watt_hours\":80}}"
				+ ",\"start\":{\"a\":{\"watt_hours\":9}},\"final\":{\"a\":{\"watt_hours\":89}}}";

		ReportingGeneralNodeDatumReading datum = objectMapper.readValue(json,
				ReportingGeneralNodeDatumReading.class);
		assertThat("Result from JSON", datum, notNullValue());
		assertThat("Created", datum.getCreated(), equalTo(TEST_TIMESTAMP));
		assertThat("Node ID", datum.getNodeId(), equalTo(-1L));
		assertThat("Source ID", datum.getSourceId(), equalTo("Main"));
		assertThat("Start samples", datum.getSamplesStart(), notNullValue());
		assertThat("Start samples data",
				datum.getSamplesStart().getAccumulatingSampleInteger("watt_hours"), equalTo(9));
		assertThat("Final samples", datum.getSamplesFinal(), notNullValue());
		assertThat("Final samples data",
				datum.getSamplesFinal().getAccumulatingSampleInteger("watt_hours"), equalTo(89));
		assertThat("Samples", datum.getSamples(), notNullValue());
		assertThat("Samples data watts", datum.getSamples().getInstantaneousSampleInteger("watts"),
				equalTo(123));
		assertThat("Samples data watt_hours",
				datum.getSamples().getAccumulatingSampleInteger("watt_hours"), equalTo(80));
	}

	@Test
	public void deserializeJsonComponents() throws Exception {
		ReportingGeneralNodeDatumReading datum = new ReportingGeneralNodeDatumReading();
		datum.setSampleJson("{\"i\":{\"watts\":123},\"a\":{\"watt_hours\":80}}");
		datum.setSampleJsonStart("{\"a\":{\"watt_hours\":9}}");
		datum.setSampleJsonFinal("{\"a\":{\"watt_hours\":89}}");

		assertThat("Start samples", datum.getSamplesStart(), notNullValue());
		assertThat("Start samples data",
				datum.getSamplesStart().getAccumulatingSampleInteger("watt_hours"), equalTo(9));
		assertThat("Final samples", datum.getSamplesFinal(), notNullValue());
		assertThat("Final samples data",
				datum.getSamplesFinal().getAccumulatingSampleInteger("watt_hours"), equalTo(89));
		assertThat("Samples", datum.getSamples(), notNullValue());
		assertThat("Samples data watts", datum.getSamples().getInstantaneousSampleInteger("watts"),
				equalTo(123));
		assertThat("Samples data watt_hours",
				datum.getSamples().getAccumulatingSampleInteger("watt_hours"), equalTo(80));
	}
}
