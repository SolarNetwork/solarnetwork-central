/* ==================================================================
 * TenantDatumFixturesTests.java - 22/09/2026 9:31:08 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.test.tenant.test;

import static net.solarnetwork.central.datum.test.tenant.TenantDatumFixtures.DEFAULT_START;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.test.tenant.TenantDatumFixtures;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumStreamIdentity;

/**
 * Test cases for the {@link TenantDatumFixtures} class.
 *
 * @author matt
 * @version 1.0
 */
public class TenantDatumFixturesTests extends AbstractJUnit5JdbcDaoTestSupport {

	private TestTenants tenants;
	private Set<UUID> streamIds;

	@BeforeEach
	public void setup() {
		tenants = new TestTenants();
		tenants.insert(jdbcTemplate);
		tenants.insertStreams(jdbcTemplate);
		streamIds = tenants.a().streams().stream().map(ObjectDatumStreamIdentity::getStreamId)
				.collect(Collectors.toSet());
	}

	@Test
	public void insertDatum() {
		// WHEN
		final List<Datum> inserted = TenantDatumFixtures.insertDatum(jdbcTemplate, tenants.a(),
				DEFAULT_START, Duration.ofMinutes(5), 3);

		// THEN
		final List<Datum> datums = DatumDbUtils.listDatum(jdbcTemplate).stream()
				.filter(d -> streamIds.contains(d.getStreamId())).toList();
		// @formatter:off
		then(datums)
			.as("Datum inserted for every stream")
			.hasSize(streamIds.size() * 3)
			.hasSameSizeAs(inserted)
			;
		// @formatter:on
	}

	@Test
	public void insertAggregateDatum() {
		// WHEN
		TenantDatumFixtures.insertAggregateDatum(jdbcTemplate, tenants.a(), Aggregation.Hour,
				DEFAULT_START, 2);
		TenantDatumFixtures.insertAggregateDatum(jdbcTemplate, tenants.a(), Aggregation.Day,
				DEFAULT_START, 1);

		// THEN
		final List<AggregateDatum> hours = DatumDbUtils
				.listAggregateDatum(jdbcTemplate, Aggregation.Hour).stream()
				.filter(d -> streamIds.contains(d.getStreamId())).toList();
		final List<AggregateDatum> days = DatumDbUtils.listAggregateDatum(jdbcTemplate, Aggregation.Day)
				.stream().filter(d -> streamIds.contains(d.getStreamId())).toList();
		// @formatter:off
		then(hours)
			.as("Hourly datum inserted for every stream")
			.hasSize(streamIds.size() * 2)
			;
		then(days)
			.as("Daily datum inserted for every stream")
			.hasSize(streamIds.size())
			;
		// @formatter:on
	}

	@Test
	public void insertDailyAuditDatum() {
		// WHEN
		TenantDatumFixtures.insertDailyAuditDatum(jdbcTemplate, tenants.a(), DEFAULT_START, 2);

		// THEN
		final List<AuditDatum> audits = DatumDbUtils.listAuditDatum(jdbcTemplate, Aggregation.Day)
				.stream().filter(d -> streamIds.contains(d.getStreamId())).toList();
		// @formatter:off
		then(audits)
			.as("Daily audit datum inserted for every stream")
			.hasSize(streamIds.size() * 2)
			;
		// @formatter:on
	}

	@Test
	public void insertDatumAuxiliary() {
		// WHEN
		TenantDatumFixtures.insertDatumAuxiliary(jdbcTemplate, tenants.a(), DEFAULT_START, 2);

		// THEN
		final List<DatumAuxiliary> auxiliaries = DatumDbUtils.listDatumAuxiliary(jdbcTemplate)
				.stream().filter(d -> streamIds.contains(d.getStreamId())).toList();
		// @formatter:off
		then(auxiliaries)
			.as("Auxiliary datum inserted for every stream")
			.hasSize(streamIds.size() * 2)
			;
		// @formatter:on
	}

}
