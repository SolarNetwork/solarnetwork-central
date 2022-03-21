/* ==================================================================
 * StaleAggregateDatumTests.java - 11/04/2019 10:24:30 am
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.util.DateUtils;

/**
 * Test cases for the {@link StaleAggregateDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class StaleAggregateDatumTests {

	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_ID = "test.source";
	private static final String TEST_KIND = "test.kind";
	private static final LocalDateTime TEST_DATE = LocalDateTime.of(2014, 8, 22, 12, 1, 2,
			(int) TimeUnit.MILLISECONDS.toNanos(345));
	private static final Instant TEST_TIMESTAMP = TEST_DATE.toInstant(ZoneOffset.UTC);
	private static final String TEST_TIMESTAMP_STRING = "2014-08-22 12:01:02.345Z";

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = DatumJsonUtils.newDatumObjectMapper();
	}

	private StaleAggregateDatum getTestInstance() {
		StaleAggregateDatum datum = new StaleAggregateDatum();
		datum.setCreated(Instant.now());
		datum.setNodeId(TEST_NODE_ID);
		datum.setStartDate(TEST_TIMESTAMP);
		datum.setSourceId(TEST_SOURCE_ID);
		datum.setKind(TEST_KIND);
		return datum;
	}

	@Test
	public void serializeJson() throws Exception {
		StaleAggregateDatum stale = getTestInstance();
		String json = objectMapper.writeValueAsString(stale);
		assertThat("JSON value", json,
				equalTo("{\"nodeId\":-1,\"sourceId\":\"test.source\",\"startDate\":\""
						+ TEST_TIMESTAMP_STRING + "\",\"kind\":\"test.kind\",\"created\":\""
						+ DateUtils.ISO_DATE_TIME_ALT_UTC.format(stale.getCreated()) + "\"}"));

	}

	@Test
	public void deserializeJson() throws Exception {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		String json = "{\"nodeId\":-1,\"sourceId\":\"test.source\",\"startDate\":\""
				+ TEST_TIMESTAMP_STRING + "\",\"kind\":\"test.kind\",\"created\":\""
				+ DateUtils.ISO_DATE_TIME_ALT_UTC.format(now) + "\"}";

		StaleAggregateDatum datum = objectMapper.readValue(json, StaleAggregateDatum.class);
		assertThat(datum, Matchers.notNullValue());
		assertThat("Node ID", datum.getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", datum.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Start date", datum.getStartDate(), equalTo(TEST_TIMESTAMP));
		assertThat("Kind", datum.getKind(), equalTo(TEST_KIND));
		assertThat("Created", datum.getCreated(), equalTo(now));
	}

}
