/* ==================================================================
 * GeneralNodeDatumAuxiliaryTests.java - 1/02/2019 5:35:10 pm
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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.domain.GeneralNodeDatumSamples;

/**
 * Test cases for the {@link GeneralNodeDatumAuxiliary} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeDatumAuxiliaryTests {

	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_ID = "test.source";

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = new ObjectMapper().registerModule(new JodaModule())
				.setSerializationInclusion(Include.NON_NULL)
				.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
	}

	private GeneralNodeDatumAuxiliary getTestInstance() {
		GeneralNodeDatumAuxiliary datum = new GeneralNodeDatumAuxiliary();
		datum.setCreated(new DateTime(2014, 8, 22, 12, 0, 0, 0));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(TEST_SOURCE_ID);

		GeneralNodeDatumSamples samples = new GeneralNodeDatumSamples();
		samples.putInstantaneousSampleValue("watts", 231);
		samples.putAccumulatingSampleValue("watt_hours", 4123);
		datum.setSamplesFinal(samples);

		GeneralNodeDatumSamples samples2 = new GeneralNodeDatumSamples();
		samples2.putInstantaneousSampleValue("watts", 321);
		samples2.putAccumulatingSampleValue("watt_hours", 4321);
		datum.setSamplesStart(samples2);

		return datum;
	}

	@Test
	public void serializeJson() throws Exception {
		String json = objectMapper.writeValueAsString(getTestInstance());
		assertThat("JSON", json,
				equalTo("{\"created\":1408665600000,\"nodeId\":-1,\"sourceId\":\"test.source\","
						+ "\"final\":{\"watts\":231,\"watt_hours\":4123}"
						+ ",\"start\":{\"watts\":321,\"watt_hours\":4321}}"));
	}

	@Test
	public void deserializeJson() throws Exception {
		String json = "{\"created\":1408665600000,\"sourceId\":\"Main\",\"samplesFinal\":{\"i\":{\"watts\":89}}}";
		GeneralNodeDatumAuxiliary datum = objectMapper.readValue(json, GeneralNodeDatumAuxiliary.class);
		assertThat("Result from JSON", datum, notNullValue());
		assertThat("Created", datum.getCreated(),
				allOf(notNullValue(), equalTo(new DateTime(1408665600000L, DateTimeZone.UTC))));
		assertThat("Source ID", datum.getSourceId(), equalTo("Main"));
		assertThat("Final samples", datum.getSamplesFinal(), notNullValue());
		assertThat("Final samples data", datum.getSamplesFinal().getInstantaneousSampleInteger("watts"),
				equalTo(89));
	}

}
