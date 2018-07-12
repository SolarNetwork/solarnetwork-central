/* ==================================================================
 * AuditDatumRecordCountsTests.java - 12/07/2018 11:54:17 AM
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import net.solarnetwork.central.datum.domain.AuditDatumRecordCounts;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link AuditDatumRecordCounts} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AuditDatumRecordCountsTests {

	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JodaModule());
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		return objectMapper;
	}

	@Test
	public void jsonPropertyOrder() throws Exception {
		AuditDatumRecordCounts c = new AuditDatumRecordCounts(1L, 2L, 3, 4);
		String json = objectMapper().writeValueAsString(c);
		assertThat("JSON", json, equalTo(
				"{\"datumTotalCount\":10,\"datumCount\":1,\"datumHourlyCount\":2,\"datumDailyCount\":3,\"datumMonthlyCount\":4}"));
	}

	@Test
	public void serializeJsonPrimaryKeyProperties() throws Exception {
		// given
		DateTime ts = new DateTime(1408665600000L);
		AuditDatumRecordCounts c = new AuditDatumRecordCounts(100L, "test.source", 1L, 2L, 3, 4);
		c.setCreated(ts);

		// when
		JsonNode tree = objectMapper().valueToTree(c);
		Map<String, Object> data = JsonUtils.getStringMapFromTree(tree);

		//then
		assertThat("JSON serialization", data, allOf(hasEntry("ts", (Object) 1408665600000L),
				hasEntry("nodeId", (Object) 100L), hasEntry("sourceId", (Object) "test.source")));
	}

	@Test
	public void totalCountAllNull() {
		AuditDatumRecordCounts c = new AuditDatumRecordCounts();
		assertThat("Total count", c.getDatumTotalCount(), equalTo(0L));
	}

	@Test
	public void totalCountWithDatum() {
		AuditDatumRecordCounts c = new AuditDatumRecordCounts(1L, null, null, null);
		assertThat("Total count", c.getDatumTotalCount(), equalTo(1L));
	}

	@Test
	public void totalCountWithDatumAndHourly() {
		AuditDatumRecordCounts c = new AuditDatumRecordCounts(1L, 3L, null, null);
		assertThat("Total count", c.getDatumTotalCount(), equalTo(4L));
	}

	@Test
	public void totalCountWithDatumAndHourlyAndDaily() {
		AuditDatumRecordCounts c = new AuditDatumRecordCounts(1L, 3L, 5, null);
		assertThat("Total count", c.getDatumTotalCount(), equalTo(9L));
	}

	@Test
	public void totalCountWithDatumAndHourlyAndDailyAndMonthly() {
		AuditDatumRecordCounts c = new AuditDatumRecordCounts(1L, 3L, 5, 7);
		assertThat("Total count", c.getDatumTotalCount(), equalTo(16L));
	}

}
