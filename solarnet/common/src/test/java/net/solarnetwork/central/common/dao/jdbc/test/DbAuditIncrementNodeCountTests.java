/* ==================================================================
 * DbAuditDatumIncrementQueryCountTests.java - 14/12/2020 9:18:06 am
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static net.solarnetwork.central.common.dao.jdbc.test.CommonDbTestUtils.assertAuditNodeServiceValue;
import static net.solarnetwork.central.common.dao.jdbc.test.CommonDbTestUtils.assertStaleAuditNodeServiceValue;
import static net.solarnetwork.central.common.dao.jdbc.test.CommonDbTestUtils.listAuditNodeServiceValueHourly;
import static net.solarnetwork.central.common.dao.jdbc.test.CommonDbTestUtils.listStaleAuditNodeServiceValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
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

/**
 * Test cases for the {@literal solardatm.audit_increment_node_count} database
 * procedure.
 * 
 * @author matt
 * @version 1.0
 */
public class DbAuditIncrementNodeCountTests extends AbstractJUnit5JdbcDaoTestSupport {

	private AuditNodeServiceValue incrementAndGet(Long nodeId, String service, Instant ts, int count) {
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				log.debug("Incrementing audit node service count for node {} service {} @ {}: +{}",
						nodeId, service, ts, count);
				try (CallableStatement stmt = con
						.prepareCall("{call solardatm.audit_increment_node_count(?,?,?,?)}")) {
					stmt.setObject(1, nodeId);
					stmt.setString(2, service);
					stmt.setTimestamp(3, Timestamp.from(ts));
					stmt.setInt(4, count);
					stmt.execute();
				}
				return null;
			}
		});
		Instant tsHour = ts.truncatedTo(ChronoUnit.HOURS);
		return listAuditNodeServiceValueHourly(jdbcTemplate).stream().filter(e -> {
			return (e.getNodeId().equals(nodeId) && e.getService().equals(service)
					&& e.getTimestamp().equals(tsHour));
		}).findAny().orElseThrow(RuntimeException::new);
	}

	@Test
	public void insert() {
		// GIVEN
		setupTestNode(); // for TZ
		final String service = "test";
		final Instant ts = Instant.now();
		final int count = 123;

		// WHEN
		AuditNodeServiceValue d = incrementAndGet(TEST_NODE_ID, service, ts, count);

		// THEN
		assertAuditNodeServiceValue("Hourly audit", d, AuditNodeServiceEntity
				.hourlyAuditNodeService(TEST_NODE_ID, service, ts.truncatedTo(ChronoUnit.HOURS), count));

		// verify stale record added for Day
		List<StaleAuditNodeServiceValue> stale = listStaleAuditNodeServiceValues(jdbcTemplate);
		assertThat("One stale audit row created", stale, hasSize(1));
		assertStaleAuditNodeServiceValue("Stale day", stale.get(0),
				new StaleAuditNodeServiceEntity(AggregateDatumId.nodeId(TEST_NODE_ID, service,
						ts.atZone(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS).toInstant(),
						Aggregation.Day), null));
	}

	@Test
	public void update() {
		// GIVEN
		setupTestNode(); // for TZ
		final String service = "test";
		final Instant ts = Instant.now();
		final int count = 123;

		CommonDbTestUtils.insertAuditNodeServiceValues(jdbcTemplate,
				Collections.singleton(AuditNodeServiceEntity.hourlyAuditNodeService(TEST_NODE_ID,
						service, ts.truncatedTo(ChronoUnit.HOURS), 321L)));

		// WHEN
		AuditNodeServiceValue d = incrementAndGet(TEST_NODE_ID, service, ts, count);

		// THEN
		assertAuditNodeServiceValue("Added to hourly audit", d,
				AuditNodeServiceEntity.hourlyAuditNodeService(TEST_NODE_ID, service,
						ts.truncatedTo(ChronoUnit.HOURS), count + 321L));

		// verify stale record still exists for Day
		List<StaleAuditNodeServiceValue> stale = listStaleAuditNodeServiceValues(jdbcTemplate);
		assertThat("One stale audit row created", stale, hasSize(1));
		assertStaleAuditNodeServiceValue("Stale day", stale.get(0),
				new StaleAuditNodeServiceEntity(AggregateDatumId.nodeId(TEST_NODE_ID, service,
						ts.atZone(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS).toInstant(),
						Aggregation.Day), null));
	}

}
