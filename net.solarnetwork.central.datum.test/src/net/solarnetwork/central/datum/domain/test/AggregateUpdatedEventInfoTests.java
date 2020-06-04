/* ==================================================================
 * AggregateUpdatedEventInfoTests.java - 5/06/2020 8:29:18 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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
import java.time.Instant;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.AggregateUpdatedEventInfo;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link AggregateUpdatedEventInfo} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AggregateUpdatedEventInfoTests {

	@Test
	public void asJson() {
		// GIVEN
		Instant now = Instant.now();
		AggregateUpdatedEventInfo info = new AggregateUpdatedEventInfo(Aggregation.Hour, now);

		// WHEN
		String json = JsonUtils.getJSONString(info, null);

		// THEN
		assertThat("JSON value", json,
				equalTo("{\"aggregationKey\":\"h\",\"timestamp\":" + now.toEpochMilli() + "}"));
	}

	@Test
	public void fromJson() {
		// GIVEN
		Instant now = Instant.now();
		String json = "{\"aggregationKey\":\"h\",\"timestamp\":" + now.toEpochMilli() + "}";

		// WHEN
		AggregateUpdatedEventInfo info = JsonUtils.getObjectFromJSON(json,
				AggregateUpdatedEventInfo.class);

		// THEN
		assertThat("Aggregation", info.getAggregation(), equalTo(Aggregation.Hour));
		assertThat("Timestamp", info.getTimeStart(), equalTo(Instant.ofEpochMilli(now.toEpochMilli())));
	}
}
