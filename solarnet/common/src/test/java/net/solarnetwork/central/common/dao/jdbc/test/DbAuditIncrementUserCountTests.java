/* ==================================================================
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

import static net.solarnetwork.central.common.dao.jdbc.test.CommonDbTestUtils.assertAuditUserServiceValue;
import static net.solarnetwork.central.common.dao.jdbc.test.CommonDbTestUtils.assertStaleAuditUserServiceValue;
import static net.solarnetwork.central.common.dao.jdbc.test.CommonDbTestUtils.listAuditUserServiceValueHourly;
import static net.solarnetwork.central.common.dao.jdbc.test.CommonDbTestUtils.listStaleAuditUserServiceValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dao.AuditUserServiceEntity;
import net.solarnetwork.central.dao.StaleAuditUserServiceEntity;
import net.solarnetwork.central.domain.AggregateDatumId;
import net.solarnetwork.central.domain.AuditUserServiceValue;
import net.solarnetwork.central.domain.StaleAuditUserServiceValue;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Test cases for the {@literal solardatm.audit_increment_node_count} database
 * procedure.
 * 
 * @author matt
 * @version 1.0
 */
public class DbAuditIncrementUserCountTests extends AbstractJUnit5JdbcDaoTestSupport {

	private static final Long TEST_USER_ID = CommonTestUtils.randomLong();

	private AuditUserServiceValue incrementAndGet(Long userId, String service, Instant ts, int count) {
		CommonDbTestUtils.auditUserService(jdbcTemplate, userId, service, ts, count);
		Instant tsHour = ts.truncatedTo(ChronoUnit.HOURS);
		return listAuditUserServiceValueHourly(jdbcTemplate).stream().filter(e -> {
			return (e.getUserId().equals(userId) && e.getService().equals(service)
					&& e.getTimestamp().equals(tsHour));
		}).findAny().orElseThrow(RuntimeException::new);
	}

	@Test
	public void insert() {
		// GIVEN
		setupTestUser(TEST_USER_ID);
		final String service = "test";
		final Instant ts = Instant.now();
		final int count = 123;

		// WHEN
		AuditUserServiceValue d = incrementAndGet(TEST_USER_ID, service, ts, count);

		// THEN
		assertAuditUserServiceValue("Hourly audit", d, AuditUserServiceEntity
				.hourlyAuditUserService(TEST_USER_ID, service, ts.truncatedTo(ChronoUnit.HOURS), count));

		// verify stale record added for Day
		List<StaleAuditUserServiceValue> stale = listStaleAuditUserServiceValues(jdbcTemplate);
		assertThat("One stale audit row created", stale, hasSize(1));
		assertStaleAuditUserServiceValue("Stale day", stale.get(0),
				new StaleAuditUserServiceEntity(AggregateDatumId.nodeId(TEST_USER_ID, service,
						ts.atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).toInstant(),
						Aggregation.Day), null));
	}

	@Test
	public void update() {
		// GIVEN
		setupTestUser(TEST_USER_ID);
		final String service = "test";
		final Instant ts = Instant.now();
		final int count = 123;

		CommonDbTestUtils.insertAuditUserServiceValues(jdbcTemplate,
				Collections.singleton(AuditUserServiceEntity.hourlyAuditUserService(TEST_USER_ID,
						service, ts.truncatedTo(ChronoUnit.HOURS), 321L)));

		// WHEN
		AuditUserServiceValue d = incrementAndGet(TEST_USER_ID, service, ts, count);

		// THEN
		assertAuditUserServiceValue("Added to hourly audit", d,
				AuditUserServiceEntity.hourlyAuditUserService(TEST_USER_ID, service,
						ts.truncatedTo(ChronoUnit.HOURS), count + 321L));

		// verify stale record still exists for Day
		List<StaleAuditUserServiceValue> stale = listStaleAuditUserServiceValues(jdbcTemplate);
		assertThat("One stale audit row created", stale, hasSize(1));
		assertStaleAuditUserServiceValue("Stale day", stale.get(0),
				new StaleAuditUserServiceEntity(AggregateDatumId.nodeId(TEST_USER_ID, service,
						ts.atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).toInstant(),
						Aggregation.Day), null));
	}

}
