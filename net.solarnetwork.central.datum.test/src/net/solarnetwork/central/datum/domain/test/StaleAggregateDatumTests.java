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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import net.solarnetwork.central.datum.domain.StaleAggregateDatum;

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
	private static final DateTime TEST_DATE = new DateTime(2014, 8, 22, 12, 0, 0, 0);

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = new ObjectMapper().registerModule(new JodaModule())
				.setSerializationInclusion(Include.NON_NULL)
				.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
	}

	private StaleAggregateDatum getTestInstance() {
		StaleAggregateDatum datum = new StaleAggregateDatum();
		datum.setCreated(new DateTime());
		datum.setNodeId(TEST_NODE_ID);
		datum.setStartDate(TEST_DATE);
		datum.setSourceId(TEST_SOURCE_ID);
		datum.setKind(TEST_KIND);
		return datum;
	}

	@Test
	public void serializeJson() throws Exception {
		StaleAggregateDatum stale = getTestInstance();
		String json = objectMapper.writeValueAsString(stale);
		assertThat("JSON value", json, equalTo(
				"{\"nodeId\":-1,\"sourceId\":\"test.source\",\"startDate\":1408665600000,\"kind\":\"test.kind\",\"created\":"
						+ stale.getCreated().getMillis() + "}"));
	}

	@Test
	public void deserializeJson() throws Exception {
		String json = "{\"nodeId\":-1,\"sourceId\":\"test.source\",\"startDate\":1408665600000,\"kind\":\"test.kind\",\"created\":1408665900000}";
		StaleAggregateDatum datum = objectMapper.readValue(json, StaleAggregateDatum.class);
		assertThat(datum, Matchers.notNullValue());
		assertThat("Node ID", datum.getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", datum.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Start date", datum.getStartDate(), equalTo(TEST_DATE.withZone(DateTimeZone.UTC)));
		assertThat("Kind", datum.getKind(), equalTo(TEST_KIND));
		assertThat("Created", datum.getCreated(),
				equalTo(TEST_DATE.plusSeconds(300).withZone(DateTimeZone.UTC)));
	}

}
