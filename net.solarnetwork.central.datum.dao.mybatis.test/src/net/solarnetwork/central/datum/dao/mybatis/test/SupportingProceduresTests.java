/* ==================================================================
 * SupportingProceduresTests.java - 24/11/2018 6:01:15 PM
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

package net.solarnetwork.central.datum.dao.mybatis.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import net.solarnetwork.util.StringUtils;

/**
 * Tests for some supporting database procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class SupportingProceduresTests extends AbstractMyBatisDaoTestSupport {

	private static final Long TEST_NODE_ID2 = -2L;
	private static final String TEST_SOURCE_ID = "test.source";

	private List<Map<String, Object>> nlt(Long[] nodeIds, String[] sourceIds, LocalDateTime s,
			LocalDateTime e) {
		return jdbcTemplate.queryForList(
				"SELECT time_zone, ts_start, ts_end, array_to_string(node_ids,',') as node_ids, "
						+ "array_to_string(source_ids,',') as source_ids "
						+ "FROM solarnet.node_source_time_ranges_local(?::bigint[], ?::text[], ?::timestamp, ?::timestamp) "
						+ "ORDER BY ts_start",
				"{" + StringUtils.commaDelimitedStringFromCollection(Arrays.asList(nodeIds)) + "}",
				"{" + Arrays.stream(sourceIds).collect(Collectors.joining(",")) + "}", s.toString(),
				e.toString());
	}

	@Test
	public void nodeLocalTimesMultipleNodes() {
		final DateTimeZone tz2 = DateTimeZone.forID("America/Los_Angeles");
		setupTestLocation(-9889L, tz2.getID());
		setupTestNode(TEST_NODE_ID2, -9889L);

		LocalDateTime s = new LocalDateTime(2018, 11, 1, 0, 0);
		LocalDateTime e = new LocalDateTime(2018, 12, 1, 0, 0);
		List<Map<String, Object>> nlt = nlt(new Long[] { TEST_NODE_ID, TEST_NODE_ID2 },
				new String[] { TEST_SOURCE_ID }, s, e);

		assertThat("Time ranges count", nlt, hasSize(2));
		assertThat("Range 1 zone", nlt.get(0).get("time_zone"), equalTo(TEST_TZ));
		assertThat("Range 1 start", nlt.get(0).get("ts_start"),
				equalTo(new Timestamp(s.toDateTime(DateTimeZone.forID(TEST_TZ)).getMillis())));
		assertThat("Range 1 nodes", nlt.get(0).get("node_ids"), equalTo(TEST_NODE_ID.toString()));
		assertThat("Range 1 sources", nlt.get(0).get("source_ids"), equalTo(TEST_SOURCE_ID));

		assertThat("Range 2 zone", nlt.get(1).get("time_zone"), equalTo(tz2.getID()));
		assertThat("Range 2 start", nlt.get(1).get("ts_start"),
				equalTo(new Timestamp(s.toDateTime(tz2).getMillis())));
		assertThat("Range 2 nodes", nlt.get(1).get("node_ids"), equalTo(TEST_NODE_ID2.toString()));
		assertThat("Range 2 sources", nlt.get(1).get("source_ids"), equalTo(TEST_SOURCE_ID));
	}

}
