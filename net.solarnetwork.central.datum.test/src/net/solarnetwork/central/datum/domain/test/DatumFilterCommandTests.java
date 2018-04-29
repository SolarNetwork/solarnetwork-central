/* ==================================================================
 * DatumFilterCommandTests.java - 21/03/2018 11:54:59 AM
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
import static org.junit.Assert.assertThat;
import java.io.IOException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the {@link DatumFilterCommand} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumFilterCommandTests {

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = new ObjectMapper().registerModule(new JodaModule())
				.setSerializationInclusion(Include.NON_NULL)
				.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
	}

	@Test
	public void serializeJson() throws IOException {
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setAggregate(Aggregation.Day);
		cmd.setNodeId(1L);
		cmd.setSourceId("test");
		cmd.setStartDate(new DateTime(2017, 3, 21, 12, 0, 0, DateTimeZone.UTC));
		cmd.setEndDate(new DateTime(2017, 3, 28, 12, 0, 0, DateTimeZone.UTC));

		String json = objectMapper.writeValueAsString(cmd);
		assertThat(json, notNullValue());
		assertThat(json, equalTo(
				"{\"nodeIds\":[1],\"sourceIds\":[\"test\"],\"aggregation\":\"Day\",\"aggregationKey\":\"d\",\"mostRecent\":false,\"startDate\":1490097600000,\"endDate\":1490702400000,\"offset\":0,\"location\":{},\"withoutTotalResultsCount\":false}"));
	}

	@Test
	public void deserializeJson() throws IOException {
		DatumFilterCommand cmd = objectMapper.readValue(
				"{\"nodeIds\":[1],\"sourceIds\":[\"test\"],\"aggregation\":\"Day\",\"mostRecent\":false,\"startDate\":1490097600000,\"endDate\":1490702400000,\"offset\":0,\"location\":{}}",
				DatumFilterCommand.class);
		assertThat(cmd, notNullValue());
		assertThat(cmd.getAggregation(), equalTo(Aggregation.Day));
		assertThat(cmd.getNodeId(), equalTo(1L));
		assertThat(cmd.getSourceId(), equalTo("test"));
		assertThat(cmd.getStartDate(), equalTo(new DateTime(2017, 3, 21, 12, 0, 0, DateTimeZone.UTC)));
		assertThat(cmd.getEndDate(), equalTo(new DateTime(2017, 3, 28, 12, 0, 0, DateTimeZone.UTC)));
	}

}
