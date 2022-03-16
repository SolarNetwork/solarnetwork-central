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

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Test cases for the {@link GeneralNodeDatumAuxiliary} class.
 * 
 * @author matt
 * @version 2.0
 */
public class GeneralNodeDatumAuxiliaryTests {

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

	private GeneralNodeDatumAuxiliary getTestInstance() {
		GeneralNodeDatumAuxiliary datum = new GeneralNodeDatumAuxiliary();
		datum.setCreated(TEST_TIMESTAMP);
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(TEST_SOURCE_ID);
		datum.setType(DatumAuxiliaryType.Reset);

		DatumSamples samples = new DatumSamples();
		samples.putInstantaneousSampleValue("watts", 231);
		samples.putAccumulatingSampleValue("watt_hours", 4123);
		datum.setSamplesFinal(samples);

		DatumSamples samples2 = new DatumSamples();
		samples2.putInstantaneousSampleValue("watts", 321);
		samples2.putAccumulatingSampleValue("watt_hours", 4321);
		datum.setSamplesStart(samples2);

		return datum;
	}

	@Test
	public void serializeJson() throws Exception {
		String json = objectMapper.writeValueAsString(getTestInstance());
		assertThat("JSON", json,
				equalTo("{\"created\":\"" + TEST_TIMESTAMP_STRING
						+ "\",\"nodeId\":-1,\"sourceId\":\"test.source\",\"type\":\"Reset\""
						+ ",\"final\":{\"i\":{\"watts\":231},\"a\":{\"watt_hours\":4123}}"
						+ ",\"start\":{\"i\":{\"watts\":321},\"a\":{\"watt_hours\":4321}}}"));
	}

	@Test
	public void deserializeJson() throws Exception {
		String json = "{\"created\":\"" + TEST_TIMESTAMP_STRING
				+ "\",\"nodeId\":-1,\"sourceId\":\"Main\",\"type\":\"Reset\",\"final\":{\"i\":{\"watts\":89}}}";
		GeneralNodeDatumAuxiliary datum = objectMapper.readValue(json, GeneralNodeDatumAuxiliary.class);
		assertThat("Result from JSON", datum, notNullValue());
		assertThat("Created", datum.getCreated(), equalTo(TEST_TIMESTAMP));
		assertThat("Node ID", datum.getNodeId(), equalTo(-1L));
		assertThat("Source ID", datum.getSourceId(), equalTo("Main"));
		assertThat("Type", datum.getType(), equalTo(DatumAuxiliaryType.Reset));
		assertThat("Final samples", datum.getSamplesFinal(), notNullValue());
		assertThat("Final samples data", datum.getSamplesFinal().getInstantaneousSampleInteger("watts"),
				equalTo(89));
	}

	private GeneralDatumMetadata getTestMetadataInstance() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("bim", "bam", "pow");
		return meta;
	}

	@Test
	public void serializeJsonWithMetadata() throws Exception {
		GeneralNodeDatumAuxiliary aux = getTestInstance();
		aux.setMeta(getTestMetadataInstance());
		String json = objectMapper.writeValueAsString(aux);
		assertThat("JSON", json,
				equalTo("{\"created\":\"" + TEST_TIMESTAMP_STRING
						+ "\",\"nodeId\":-1,\"sourceId\":\"test.source\",\"type\":\"Reset\""
						+ ",\"final\":{\"i\":{\"watts\":231},\"a\":{\"watt_hours\":4123}}"
						+ ",\"start\":{\"i\":{\"watts\":321},\"a\":{\"watt_hours\":4321}}"
						+ ",\"meta\":{\"m\":{\"foo\":\"bar\"},\"pm\":{\"bim\":{\"bam\":\"pow\"}}}}"));
	}

	@Test
	public void deserializeJsonWithMetadata() throws Exception {
		String json = "{\"created\":\"" + TEST_TIMESTAMP_STRING
				+ "\",\"nodeId\":-1,\"sourceId\":\"Main\",\"type\":\"Reset\",\"final\":{\"i\":{\"watts\":89}},\"meta\":{\"m\":{\"foo\":\"bar\"},\"pm\":{\"bim\":{\"bam\":\"pow\"}}}}";
		GeneralNodeDatumAuxiliary datum = objectMapper.readValue(json, GeneralNodeDatumAuxiliary.class);
		assertThat("Result from JSON", datum, notNullValue());
		assertThat("Created", datum.getCreated(), equalTo(TEST_TIMESTAMP));
		assertThat("Node ID", datum.getNodeId(), equalTo(-1L));
		assertThat("Source ID", datum.getSourceId(), equalTo("Main"));
		assertThat("Type", datum.getType(), equalTo(DatumAuxiliaryType.Reset));
		assertThat("Final samples", datum.getSamplesFinal(), notNullValue());
		assertThat("Final samples data", datum.getSamplesFinal().getInstantaneousSampleInteger("watts"),
				equalTo(89));
		assertThat("Metadata available", datum.getMeta(), notNullValue());
		assertThat("Metadata m", datum.getMeta().getInfo(), hasEntry("foo", "bar"));
		assertThat("Metadata pm", datum.getMeta().getPropertyInfo(),
				hasEntry("bim", singletonMap("bam", "pow")));
	}

}
