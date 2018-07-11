/* ==================================================================
 * DatumRecordCountsTests.java - 11/07/2018 1:13:21 PM
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

package net.solarnetwork.central.user.expire.domain.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.user.expire.domain.DatumRecordCounts;

/**
 * Test cases for the {@link DatumRecordCounts} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumRecordCountsTests {

	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		return objectMapper;
	}

	@Test
	public void testJsonPropertyOrder() throws Exception {
		DatumRecordCounts c = new DatumRecordCounts(1L, 2L, 3, 4);
		String json = objectMapper().writeValueAsString(c);
		assertThat("JSON", json, equalTo(
				"{\"datumTotalCount\":10,\"datumCount\":1,\"datumHourlyCount\":2,\"datumDailyCount\":3,\"datumMonthlyCount\":4}"));
	}

	@Test
	public void testTotalCountAllNull() {
		DatumRecordCounts c = new DatumRecordCounts();
		assertThat("Total count", c.getDatumTotalCount(), equalTo(0L));
	}

	@Test
	public void testTotalCountWithDatum() {
		DatumRecordCounts c = new DatumRecordCounts(1L, null, null, null);
		assertThat("Total count", c.getDatumTotalCount(), equalTo(1L));
	}

	@Test
	public void testTotalCountWithDatumAndHourly() {
		DatumRecordCounts c = new DatumRecordCounts(1L, 3L, null, null);
		assertThat("Total count", c.getDatumTotalCount(), equalTo(4L));
	}

	@Test
	public void testTotalCountWithDatumAndHourlyAndDaily() {
		DatumRecordCounts c = new DatumRecordCounts(1L, 3L, 5, null);
		assertThat("Total count", c.getDatumTotalCount(), equalTo(9L));
	}

	@Test
	public void testTotalCountWithDatumAndHourlyAndDailyAndMonthly() {
		DatumRecordCounts c = new DatumRecordCounts(1L, 3L, 5, 7);
		assertThat("Total count", c.getDatumTotalCount(), equalTo(16L));
	}
}
