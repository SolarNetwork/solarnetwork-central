/* ==================================================================
 * GeneralLocationDatumMetadataTests.java - Oct 17, 2014 3:16:11 PM
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

import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadata;
import net.solarnetwork.domain.GeneralDatumMetadata;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 * Test cases for the {@link GeneralLocationDatumMetadata} class.
 * 
 * @author matt
 * @version 1.1
 */
public class GeneralLocationDatumMetadataTests {

	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_ID = "test.source";

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = new ObjectMapper().registerModule(new JodaModule())
				.setSerializationInclusion(Include.NON_NULL)
				.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
	}

	private GeneralLocationDatumMetadata getTestInstance() {
		GeneralLocationDatumMetadata datum = new GeneralLocationDatumMetadata();
		datum.setCreated(new DateTime(2014, 8, 22, 12, 0, 0, 0));
		datum.setLocationId(TEST_NODE_ID);
		datum.setUpdated(datum.getCreated());
		datum.setSourceId(TEST_SOURCE_ID);

		GeneralDatumMetadata samples = new GeneralDatumMetadata();
		datum.setMeta(samples);

		// some sample data
		Map<String, Object> info = new HashMap<String, Object>(2);
		info.put("unit", "C");
		samples.setInfo(info);

		return datum;
	}

	@Test
	public void serializeJson() throws Exception {
		String json = objectMapper.writeValueAsString(getTestInstance());
		Assert.assertEquals(
				"{\"created\":1408665600000,\"updated\":1408665600000,\"locationId\":-1,\"sourceId\":\"test.source\","
						+ "\"m\":{\"unit\":\"C\"}}", json);
	}

	@Test
	public void serializeJsonWithTags() throws Exception {
		GeneralLocationDatumMetadata datum = getTestInstance();
		datum.getMeta().addTag("test");
		String json = objectMapper.writeValueAsString(datum);
		Assert.assertEquals(
				"{\"created\":1408665600000,\"updated\":1408665600000,\"locationId\":-1,\"sourceId\":\"test.source\","
						+ "\"m\":{\"unit\":\"C\"},\"t\":[\"test\"]}", json);
	}

	@Test
	public void deserializeJson() throws Exception {
		String json = "{\"created\":1408665600000,\"sourceId\":\"Main\",\"meta\":{\"m\":{\"ploc\":2502287},\"t\":[\"foo\"]}}}";
		GeneralLocationDatumMetadata datum = objectMapper.readValue(json,
				GeneralLocationDatumMetadata.class);
		Assert.assertNotNull(datum);
		Assert.assertNotNull(datum.getCreated());
		Assert.assertEquals(1408665600000L, datum.getCreated().getMillis());
		Assert.assertEquals("Main", datum.getSourceId());
		Assert.assertNotNull(datum.getMeta());
		Assert.assertEquals(Long.valueOf(2502287), datum.getMeta().getInfoLong("ploc"));
		Assert.assertTrue("Has tag", datum.getMeta().hasTag("foo"));
	}

}
