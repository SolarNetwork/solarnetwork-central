/* ==================================================================
 * DbProcessStaleAuditNodeTests.java - 23/01/2023 10:27:48 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.dao.AuditNodeServiceEntity;
import net.solarnetwork.central.dao.StaleAuditNodeServiceEntity;
import net.solarnetwork.central.domain.AggregateDatumId;
import net.solarnetwork.central.domain.AuditNodeServiceValue;
import net.solarnetwork.central.domain.StaleAuditNodeServiceValue;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumId;

/**
 * Test cases for the {@link solardatm.process_one_aud_stale_node} procedure.
 * 
 * @author matt
 * @version 1.0
 */
public class DbProcessStaleAuditNodeTests extends AbstractJUnit5JdbcDaoTestSupport {

	private int executeCall(Aggregation kind) {
		return jdbcTemplate.execute(new ConnectionCallback<Integer>() {

			@Override
			public Integer doInConnection(Connection con) throws SQLException, DataAccessException {
				try (CallableStatement cs = con
						.prepareCall("{? = call solardatm.process_one_aud_stale_node(?)}")) {
					cs.registerOutParameter(1, Types.INTEGER);
					cs.setString(2, kind.getKey());
					cs.execute();
					return cs.getInt(1);
				}
			}
		});

	}

	@Test
	public void processDaily() {
		// GIVEN
		setupTestNode();

		// insert a couple of audit rows on different hours on different days; will populate 2 stale Day rows
		final Instant ts = ZonedDateTime.now(ZoneId.of(TEST_TZ))
				.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toInstant();
		final String service = "test";

		// day 1
		CommonDbTestUtils.auditNodeService(jdbcTemplate, TEST_NODE_ID, service, ts, 1);
		CommonDbTestUtils.auditNodeService(jdbcTemplate, TEST_NODE_ID, service,
				ts.plus(1, ChronoUnit.HOURS), 2);

		// day 2
		CommonDbTestUtils.auditNodeService(jdbcTemplate, TEST_NODE_ID, service,
				ts.plus(1, ChronoUnit.DAYS), 3);
		CommonDbTestUtils.auditNodeService(jdbcTemplate, TEST_NODE_ID, service,
				ts.plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS), 4);

		CommonDbTestUtils.debugStaleAuditNodeServiceTable(log, jdbcTemplate, "stale node services");

		// WHEN
		int processedCount1 = executeCall(Aggregation.Day);
		int processedCount2 = executeCall(Aggregation.Day);
		int processedCount3 = executeCall(Aggregation.Day);

		assertThat("First stale record processed", processedCount1, is(equalTo(1)));
		assertThat("Second stale record processed", processedCount2, is(equalTo(1)));
		assertThat("No more stale records processed", processedCount3, is(equalTo(0)));

		// verify days are sum of hour records
		List<AuditNodeServiceValue> days = CommonDbTestUtils
				.listAuditNodeServiceValueDaily(jdbcTemplate);
		assertThat("Two days summed", days, hasSize(2));
		CommonDbTestUtils.assertAuditNodeServiceValue("Day 1 counts summed (1 + 2)", days.get(0),
				new AuditNodeServiceEntity(DatumId.nodeId(TEST_NODE_ID, service, ts), Aggregation.Day,
						3));
		CommonDbTestUtils.assertAuditNodeServiceValue("Day 1 counts summed (3 + 4)", days.get(1),
				new AuditNodeServiceEntity(
						DatumId.nodeId(TEST_NODE_ID, service, ts.plus(1, ChronoUnit.DAYS)),
						Aggregation.Day, 7));

		// verify stale month record created
		List<StaleAuditNodeServiceValue> finalStaleRecords = CommonDbTestUtils
				.listStaleAuditNodeServiceValues(jdbcTemplate);
		assertThat("Stale month record created", finalStaleRecords, hasSize(1));
		CommonDbTestUtils.assertStaleAuditNodeServiceValue("Stale month", finalStaleRecords.get(0),
				new StaleAuditNodeServiceEntity(
						AggregateDatumId.nodeId(TEST_NODE_ID, service, ts, Aggregation.Month), null));
	}

	@Test
	public void processMonthly() {
		// GIVEN
		setupTestNode();

		// insert a couple of audit rows on different hours on different days; will populate 2 stale Day rows
		final Instant ts = ZonedDateTime.now(ZoneId.of(TEST_TZ))
				.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toInstant();
		final String service = "test";

		// day 1
		CommonDbTestUtils.auditNodeService(jdbcTemplate, TEST_NODE_ID, service, ts, 1);
		CommonDbTestUtils.auditNodeService(jdbcTemplate, TEST_NODE_ID, service,
				ts.plus(1, ChronoUnit.HOURS), 2);

		// day 2
		CommonDbTestUtils.auditNodeService(jdbcTemplate, TEST_NODE_ID, service,
				ts.plus(1, ChronoUnit.DAYS), 3);
		CommonDbTestUtils.auditNodeService(jdbcTemplate, TEST_NODE_ID, service,
				ts.plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS), 4);

		CommonDbTestUtils.debugStaleAuditNodeServiceTable(log, jdbcTemplate, "stale node services");

		// WHEN
		int processedCount1 = executeCall(Aggregation.Day);
		int processedCount2 = executeCall(Aggregation.Day);
		int processedCount3 = executeCall(Aggregation.Day);
		int processedCount4 = executeCall(Aggregation.Month);
		int processedCount5 = executeCall(Aggregation.Month);

		assertThat("First stale Day record processed", processedCount1, is(equalTo(1)));
		assertThat("Second stale Day record processed", processedCount2, is(equalTo(1)));
		assertThat("No more stale Day records processed", processedCount3, is(equalTo(0)));
		assertThat("First stale Month record processed", processedCount4, is(equalTo(1)));
		assertThat("No more stale Month records processed", processedCount5, is(equalTo(0)));

		// verify month is sum of day records
		List<AuditNodeServiceValue> months = CommonDbTestUtils
				.listAuditNodeServiceValueMonthly(jdbcTemplate);
		assertThat("One month summed", months, hasSize(1));
		CommonDbTestUtils.assertAuditNodeServiceValue("Month counts summed (3 + 7)", months.get(0),
				new AuditNodeServiceEntity(DatumId.nodeId(TEST_NODE_ID, service, ts), Aggregation.Month,
						10));

		// verify stale month records removed
		List<StaleAuditNodeServiceValue> finalStaleRecords = CommonDbTestUtils
				.listStaleAuditNodeServiceValues(jdbcTemplate);
		assertThat("No stale records remain", finalStaleRecords, hasSize(0));
	}

}
