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

package net.solarnetwork.central.datum.domain.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadata;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Test cases for the {@link GeneralLocationDatumMetadata} class.
 * 
 * @author matt
 * @version 2.0
 */
public class GeneralLocationDatumMetadataTests {

	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_ID = "test.source";

	private static final LocalDateTime TEST_TIMESTAMP = LocalDateTime.of(2014, 8, 22, 12, 1, 2,
			(int) TimeUnit.MILLISECONDS.toNanos(345));
	private static final String TEST_TIMESTAMP_STRING = "2014-08-22 12:01:02.345Z";

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = DatumJsonUtils.newDatumObjectMapper();
	}

	private GeneralLocationDatumMetadata getTestInstance() {
		GeneralLocationDatumMetadata datum = new GeneralLocationDatumMetadata();
		datum.setCreated(TEST_TIMESTAMP.toInstant(ZoneOffset.UTC));
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
		assertThat(json,
				is("{\"created\":\"" + TEST_TIMESTAMP_STRING + "\",\"updated\":\""
						+ TEST_TIMESTAMP_STRING + "\",\"locationId\":-1,\"sourceId\":\"test.source\","
						+ "\"m\":{\"unit\":\"C\"}}"));
	}

	@Test
	public void serializeJsonWithTags() throws Exception {
		GeneralLocationDatumMetadata datum = getTestInstance();
		datum.getMeta().addTag("test");
		String json = objectMapper.writeValueAsString(datum);
		assertThat(json,
				is("{\"created\":\"" + TEST_TIMESTAMP_STRING + "\",\"updated\":\""
						+ TEST_TIMESTAMP_STRING + "\",\"locationId\":-1,\"sourceId\":\"test.source\","
						+ "\"m\":{\"unit\":\"C\"},\"t\":[\"test\"]}"));
	}

	@Test
	public void deserializeJson() throws Exception {
		String json = "{\"created\":\"" + TEST_TIMESTAMP_STRING
				+ "\",\"sourceId\":\"Main\",\"meta\":{\"m\":{\"ploc\":2502287},\"t\":[\"foo\"]}}}";
		GeneralLocationDatumMetadata datum = objectMapper.readValue(json,
				GeneralLocationDatumMetadata.class);
		assertThat(datum, is(notNullValue()));
		assertThat(datum.getCreated(), is(notNullValue()));
		assertThat(datum.getCreated(), is(TEST_TIMESTAMP.toInstant(ZoneOffset.UTC)));
		assertThat(datum.getSourceId(), is("Main"));
		assertThat(datum.getMeta(), is(notNullValue()));
		assertThat(datum.getMeta().getInfoLong("ploc"), is(Long.valueOf(2502287)));
		assertThat("Has tag", datum.getMeta().hasTag("foo"), is(true));
	}

}
