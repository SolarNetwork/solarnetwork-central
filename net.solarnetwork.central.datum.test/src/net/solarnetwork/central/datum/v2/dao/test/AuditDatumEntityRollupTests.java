/* ==================================================================
 * AuditDatumEntityRollupTests.java - 21/11/2020 7:04:33 am
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

package net.solarnetwork.central.datum.v2.dao.test;

import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntityRollup.accumulativeAuditDatumRollup;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.Map;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntityRollup;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link AuditDatumEntityRollup} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AuditDatumEntityRollupTests {

	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
		return objectMapper;
	}

	@Test
	public void jsonPropertyOrder() throws Exception {
		AuditDatumEntityRollup c = accumulativeAuditDatumRollup(null, null, null, 1L, 2L, 3, 4);
		String json = JsonUtils.getJSONString(c, null);
		assertThat("JSON", json, equalTo(
				"{\"aggregation\":\"RunningTotal\",\"datumTotalCount\":10,\"datumCount\":1,\"datumHourlyCount\":2,\"datumDailyCount\":3,\"datumMonthlyCount\":4}"));
	}

	@Test
	public void serializeJsonPrimaryKeyProperties() throws Exception {
		// given
		Instant ts = Instant.ofEpochMilli(1408665600000L);
		AuditDatumEntityRollup c = accumulativeAuditDatumRollup(100L, "test.source", ts, 1L, 2L, 3, 4);

		// when
		JsonNode tree = objectMapper().valueToTree(c);
		Map<String, Object> data = JsonUtils.getStringMapFromTree(tree);

		// then
		assertThat("Timestamp", data.get("ts"), instanceOf(Number.class));
		assertThat("Timestamp value", ((Number) data.get("ts")).longValue(), equalTo(1408665600000L));
		assertThat("Node ID", data.get("nodeId"), instanceOf(Number.class));
		assertThat("Node ID value", ((Number) data.get("nodeId")).longValue(), equalTo(100L));
		assertThat("Source ID value", data.get("sourceId"), equalTo("test.source"));
	}

	@Test
	public void totalCount_allNull() {
		AuditDatumEntityRollup c = accumulativeAuditDatumRollup(null, null, null, null, null, null,
				null);
		assertThat("Total count", c.getDatumTotalCount(), equalTo(0L));
	}

	@Test
	public void totalCount_datum() {
		AuditDatumEntityRollup c = accumulativeAuditDatumRollup(null, null, null, 1L, null, null, null);
		assertThat("Total count", c.getDatumTotalCount(), equalTo(1L));
	}

	@Test
	public void totalCount_datumAndHourly() {
		AuditDatumEntityRollup c = accumulativeAuditDatumRollup(null, null, null, 1L, 3L, null, null);
		assertThat("Total count", c.getDatumTotalCount(), equalTo(4L));
	}

	@Test
	public void totalCount_datumAndHourlyAndDaily() {
		AuditDatumEntityRollup c = accumulativeAuditDatumRollup(null, null, null, 1L, 3L, 5, null);
		assertThat("Total count", c.getDatumTotalCount(), equalTo(9L));
	}

	@Test
	public void totalCount_datumAndHourlyAndDailyAndMonthly() {
		AuditDatumEntityRollup c = accumulativeAuditDatumRollup(null, null, null, 1L, 3L, 5, 7);
		assertThat("Total count", c.getDatumTotalCount(), equalTo(16L));
	}

}
