/* ==================================================================
 * MyBatisAuditDatumEntityDaoTests.java - 15/11/2020 12:45:36 pm
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

package net.solarnetwork.central.datum.v2.dao.mybatis.test;

import static java.util.Collections.singleton;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntity.dailyAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntity.hourlyAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntity.monthlyAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntityRollup.hourlyAuditDatumRollup;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.mybatis.test.AbstractMyBatisDaoTestSupport;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils;
import net.solarnetwork.central.datum.v2.dao.mybatis.MyBatisAuditDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;
import net.solarnetwork.central.datum.v2.domain.BasicNodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link MyBatisAuditDatumEntityDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisAuditDatumEntityDaoTests extends AbstractMyBatisDaoTestSupport {

	protected MyBatisAuditDatumEntityDao dao;

	@Before
	public void setup() {
		dao = new MyBatisAuditDatumEntityDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
	}

	private void assertAuditDatum(String prefix, AuditDatumRollup result, AuditDatumRollup expected) {
		assertThat(prefix + " not null", result, notNullValue());
		if ( expected.getTimestamp() != null ) {
			assertThat(prefix + " timestamp", result.getTimestamp(), equalTo(expected.getTimestamp()));
		} else {
			assertThat(prefix + " timestamp", result.getTimestamp(), nullValue());
		}
		assertThat(prefix + " stream ID", result.getStreamId(), nullValue());
		assertThat(prefix + " node ID", result.getNodeId(), equalTo(expected.getNodeId()));
		assertThat(prefix + " source ID", result.getSourceId(), equalTo(expected.getSourceId()));
		assertThat(prefix + " datum count", result.getDatumCount(), equalTo(expected.getDatumCount()));
		assertThat(prefix + " datum hourly count", result.getDatumHourlyCount(),
				equalTo(expected.getDatumHourlyCount()));
		assertThat(prefix + " datum daily count", result.getDatumDailyCount(),
				equalTo(expected.getDatumDailyCount()));
		assertThat(prefix + " datum monthly count", result.getDatumMonthlyCount(),
				equalTo(expected.getDatumMonthlyCount()));
		assertThat(prefix + " prop posted count", result.getDatumPropertyCount(),
				equalTo(expected.getDatumPropertyCount()));
		assertThat(prefix + " datum query count", result.getDatumQueryCount(),
				equalTo(expected.getDatumQueryCount()));
	}

	private void setupTestAuditDatumRecords(ZonedDateTime start, UUID streamId, int count, int hourStep,
			List<Instant> hours, List<Instant> days, List<Instant> months) {
		List<AuditDatum> audits = new ArrayList<>();
		for ( int i = 0; i < count; i++ ) {
			ZonedDateTime h = start.plusHours(i * hourStep);
			audits.add(hourlyAuditDatum(streamId, h.toInstant(), 60L, 100L, 5L));
			hours.add(h.toInstant());

			Instant d = h.truncatedTo(ChronoUnit.DAYS).toInstant();
			if ( days.isEmpty() || !days.get(days.size() - 1).equals(d) ) {
				audits.add(dailyAuditDatum(streamId, d, 100L, 24L, 1, 1000L, 10L));
				days.add(d);
			}

			Instant m = h.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS)
					.toInstant();
			if ( months.isEmpty() || !months.get(months.size() - 1).equals(m) ) {
				audits.add(monthlyAuditDatum(streamId, m, 3000L, 720L, 30, 1, 30000L, 300L));
				months.add(m);
			}
		}
		DatumTestUtils.insertAuditDatum(log, jdbcTemplate, audits);
	}

	private void setupTestUserNode(Long nodeId, Long userId) {
		//setupTestLocation(TEST_LOC_ID);
		//setupTestNode(nodeId, TEST_LOC_ID);
		//setupTestUser(userId, UUID.randomUUID().toString());
		setupUserNodeEntity(nodeId, userId);
	}

	private void setupTestStream(Long nodeId, String sourceId, UUID streamId) {
		BasicNodeDatumStreamMetadata meta = new BasicNodeDatumStreamMetadata(streamId, nodeId, sourceId,
				null, null, null);
		DatumTestUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
	}

	@Test
	public void findAuditDatum_forUser_hour_noData() {
		// GIVEN

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(TEST_USER_ID);
		FilterResults<AuditDatumRollup, DatumPK> results = dao.findAuditDatumFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("No data available", results.getReturnedResultCount(), equalTo(0));
	}

	@Test
	public void findAuditDatum_forUser_hour_noRollup() {
		// given
		ZonedDateTime start = ZonedDateTime.of(2020, 2, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		List<Instant> hours = new ArrayList<>();
		List<Instant> days = new ArrayList<>();
		List<Instant> months = new ArrayList<>();
		UUID streamId = UUID.randomUUID();
		setupTestAuditDatumRecords(start, streamId, 8, (int) TimeUnit.DAYS.toHours(7), hours, days,
				months);

		setupTestUserNode(TEST_NODE_ID, TEST_USER_ID);
		setupTestStream(TEST_NODE_ID, TEST_SOURCE_ID, streamId);

		// when
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusWeeks(4).toInstant());
		filter.setUserId(TEST_USER_ID);
		filter.setAggregation(Aggregation.Hour);
		FilterResults<AuditDatumRollup, DatumPK> results = dao.findAuditDatumFiltered(filter);

		// then
		assertThat("Hour rows for first 4 weeks returned", results.getReturnedResultCount(), equalTo(4));
		int i = 0;
		for ( AuditDatumRollup row : results ) {
			assertAuditDatum("Hour " + i, row,
					hourlyAuditDatumRollup(TEST_NODE_ID, TEST_SOURCE_ID, hours.get(i), 60L, 100L, 5L));
			i++;
		}
	}

	@Test
	public void findAuditDatum_forUser_hour_timeAndNodeRollup() {
		// given
		ZonedDateTime start = ZonedDateTime.of(2020, 2, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		List<Instant> hours = new ArrayList<>();
		List<Instant> days = new ArrayList<>();
		List<Instant> months = new ArrayList<>();
		UUID streamId_1 = UUID.randomUUID();
		setupTestAuditDatumRecords(start, streamId_1, 8, (int) TimeUnit.DAYS.toHours(7), hours, days,
				months);
		// add another source
		UUID streamId_2 = UUID.randomUUID();
		setupTestAuditDatumRecords(start, streamId_2, 8, (int) TimeUnit.DAYS.toHours(7),
				new ArrayList<Instant>(), new ArrayList<Instant>(), new ArrayList<Instant>());

		setupTestUserNode(TEST_NODE_ID, TEST_USER_ID);
		setupTestStream(TEST_NODE_ID, TEST_SOURCE_ID, streamId_1);
		setupTestStream(TEST_NODE_ID, "s2", streamId_2);

		// when
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusWeeks(4).toInstant());
		filter.setUserId(TEST_USER_ID);
		filter.setAggregation(Aggregation.Hour);
		filter.setDatumRollupTypes(new DatumRollupType[] { DatumRollupType.Time, DatumRollupType.Node });
		FilterResults<AuditDatumRollup, DatumPK> results = dao.findAuditDatumFiltered(filter);

		// then
		assertThat("Rolled up hour rows for first 4 weeks returned", results.getReturnedResultCount(),
				equalTo(4));
		int i = 0;
		for ( AuditDatumRollup row : results ) {
			// audit counts doubled from rollup
			assertAuditDatum("Hour " + i, row,
					hourlyAuditDatumRollup(TEST_NODE_ID, null, hours.get(i), 120L, 200L, 10L));
			i++;
		}
	}

	@Test
	public void findAuditDatum_acc_forUser_noData() {
		// GIVEN

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(TEST_USER_ID);
		FilterResults<AuditDatumRollup, DatumPK> results = dao
				.findAccumulativeAuditDatumFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("No data available", results.getReturnedResultCount(), equalTo(0));
	}
}
