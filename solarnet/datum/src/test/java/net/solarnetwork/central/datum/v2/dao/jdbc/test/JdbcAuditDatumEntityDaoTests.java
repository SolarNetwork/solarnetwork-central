/* ==================================================================
 * JdbcAuditDatumEntityDaoTests.java - 20/11/2020 8:19:11 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static java.util.Collections.singleton;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntity.accumulativeAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntity.dailyAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntity.ioAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntity.monthlyAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntityRollup.accumulativeAuditDatumRollup;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntityRollup.hourlyAuditDatumRollup;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntityRollup.monthlyAuditDatumRollup;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertObjectDatumStreamMetadata;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntityRollup;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcAuditDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link JdbcAuditDatumEntityDao} class.
 * 
 * @author matt
 * @version 1.1
 */
public class JdbcAuditDatumEntityDaoTests extends BaseDatumJdbcTestSupport {

	private JdbcAuditDatumEntityDao dao;

	@Before
	public void setup() {
		dao = new JdbcAuditDatumEntityDao(jdbcTemplate);
	}

	@Before
	public void setupInTransaction() {
		setupTestNode();
		setupTestUser();
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
		assertThat(prefix + " prop reposted count", result.getDatumPropertyUpdateCount(),
				equalTo(expected.getDatumPropertyUpdateCount()));
		assertThat(prefix + " datum query count", result.getDatumQueryCount(),
				equalTo(expected.getDatumQueryCount()));
	}

	private void setupTestAuditDatumRecords(ZonedDateTime start, UUID streamId, int count, int hourStep,
			List<Instant> hours, List<Instant> days, List<Instant> months) {
		List<AuditDatum> audits = new ArrayList<>();
		for ( int i = 0; i < count; i++ ) {
			ZonedDateTime h = start.plusHours(i * hourStep);
			audits.add(ioAuditDatum(streamId, h.toInstant(), 60L, 100L, 5L, 0L));
			hours.add(h.toInstant());

			Instant d = h.truncatedTo(ChronoUnit.DAYS).toInstant();
			if ( days.isEmpty() || !days.get(days.size() - 1).equals(d) ) {
				audits.add(dailyAuditDatum(streamId, d, 100L, 24L, 1, 1000L, 10L, 0L));
				days.add(d);
			}

			Instant m = h.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS)
					.toInstant();
			if ( months.isEmpty() || !months.get(months.size() - 1).equals(m) ) {
				audits.add(monthlyAuditDatum(streamId, m, 3000L, 720L, 30, 1, 30000L, 300L, 0L));
				months.add(m);
			}
		}
		insertAuditDatum(log, jdbcTemplate, audits);
	}

	private void setupTestAccumulativeAuditDatumRecords(ZonedDateTime start, UUID streamId, int count,
			int dayStep, List<Instant> days) {
		List<AuditDatum> audits = new ArrayList<>();
		ZonedDateTime currMonth = null;
		int iMonth = 1;
		for ( int i = 1; i <= count; i++ ) {
			ZonedDateTime d = start.plusDays((i - 1) * dayStep);
			ZonedDateTime m = d.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
			if ( currMonth == null ) {
				currMonth = m;
			} else if ( !m.isEqual(currMonth) ) {
				iMonth++;
				currMonth = m;
			}
			audits.add(accumulativeAuditDatum(streamId, d.toInstant(), 100L * i, 24L * i, i, iMonth));
			days.add(d.toInstant());
		}
		insertAuditDatum(log, jdbcTemplate, audits);
	}

	private void setupTestStream(Long nodeId, String sourceId, UUID streamId) {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, "UTC",
				ObjectDatumKind.Node, nodeId, sourceId, null, null, null);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
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

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
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
			assertAuditDatum("Hour " + i, row, hourlyAuditDatumRollup(TEST_NODE_ID, TEST_SOURCE_ID,
					hours.get(i), 60L, 100L, 5L, 0L));
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

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
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
					hourlyAuditDatumRollup(TEST_NODE_ID, null, hours.get(i), 120L, 200L, 10L, 0L));
			i++;
		}
	}

	@Test
	public void findAuditDatum_forUser_hour_timeRollup() {
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

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupTestStream(TEST_NODE_ID, TEST_SOURCE_ID, streamId_1);
		setupTestStream(TEST_NODE_ID, "s2", streamId_2);

		// add another node
		UUID streamId_3 = UUID.randomUUID();
		setupTestAuditDatumRecords(start, streamId_3, 8, (int) TimeUnit.DAYS.toHours(7),
				new ArrayList<Instant>(), new ArrayList<Instant>(), new ArrayList<Instant>());

		final Long nodeId_2 = UUID.randomUUID().getLeastSignificantBits();
		setupTestNode(nodeId_2);
		setupUserNodeEntity(nodeId_2, TEST_USER_ID);
		setupTestStream(nodeId_2, TEST_SOURCE_ID, streamId_3);

		// when
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusWeeks(4).toInstant());
		filter.setUserId(TEST_USER_ID);
		filter.setAggregation(Aggregation.Hour);
		filter.setDatumRollupTypes(new DatumRollupType[] { DatumRollupType.Time });
		FilterResults<AuditDatumRollup, DatumPK> results = dao.findAuditDatumFiltered(filter);

		// then
		assertThat("Rolled up hour rows for first 4 weeks returned", results.getReturnedResultCount(),
				equalTo(4));
		int i = 0;
		for ( AuditDatumRollup row : results ) {
			// audit counts doubled from rollup
			assertAuditDatum("Hour " + i, row,
					hourlyAuditDatumRollup(null, null, hours.get(i), 180L, 300L, 15L, 0L));
			i++;
		}
	}

	@Test
	public void findAuditDatum_forUser_hour_allRollup() {
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

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupTestStream(TEST_NODE_ID, TEST_SOURCE_ID, streamId_1);
		setupTestStream(TEST_NODE_ID, "s2", streamId_2);

		// add another node
		UUID streamId_3 = UUID.randomUUID();
		setupTestAuditDatumRecords(start, streamId_3, 8, (int) TimeUnit.DAYS.toHours(7),
				new ArrayList<Instant>(), new ArrayList<Instant>(), new ArrayList<Instant>());

		final Long nodeId_2 = UUID.randomUUID().getLeastSignificantBits();
		setupTestNode(nodeId_2);
		setupUserNodeEntity(nodeId_2, TEST_USER_ID);
		setupTestStream(nodeId_2, TEST_SOURCE_ID, streamId_3);

		// when
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusWeeks(4).toInstant());
		filter.setUserId(TEST_USER_ID);
		filter.setAggregation(Aggregation.Hour);
		filter.setDatumRollupTypes(new DatumRollupType[] { DatumRollupType.All });
		FilterResults<AuditDatumRollup, DatumPK> results = dao.findAuditDatumFiltered(filter);

		// then
		assertThat("Rolled up hour row for first 4 weeks returned", results.getReturnedResultCount(),
				equalTo(1));
		assertAuditDatum("Hour (All)", results.iterator().next(),
				hourlyAuditDatumRollup(null, null, null, 720L, 1200L, 60L, 0L));
	}

	@Test
	public void findAuditDatum_forUser_default_noRollup() {
		// given
		ZonedDateTime start = ZonedDateTime.of(2020, 2, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		List<Instant> hours = new ArrayList<>();
		List<Instant> days = new ArrayList<>();
		List<Instant> months = new ArrayList<>();
		UUID streamId = UUID.randomUUID();
		setupTestAuditDatumRecords(start, streamId, 8, (int) TimeUnit.DAYS.toHours(7), hours, days,
				months);

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupTestStream(TEST_NODE_ID, TEST_SOURCE_ID, streamId);

		// when
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusWeeks(4).toInstant());
		filter.setUserId(TEST_USER_ID);
		FilterResults<AuditDatumRollup, DatumPK> results = dao.findAuditDatumFiltered(filter);

		// then
		assertThat("Day rows for first 4 weeks returned", results.getReturnedResultCount(), equalTo(4));
		int i = 0;
		for ( AuditDatumRollup row : results ) {
			assertAuditDatum("Hour " + i, row, AuditDatumEntityRollup.dailyAuditDatumRollup(TEST_NODE_ID,
					TEST_SOURCE_ID, days.get(i), 100L, 24L, 1, 1000L, 10L, 0L));
			i++;
		}
	}

	@Test
	public void findAuditDatum_forUser_day_noRollup() {
		// given
		ZonedDateTime start = ZonedDateTime.of(2020, 2, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		List<Instant> hours = new ArrayList<>();
		List<Instant> days = new ArrayList<>();
		List<Instant> months = new ArrayList<>();
		UUID streamId = UUID.randomUUID();
		setupTestAuditDatumRecords(start, streamId, 8, (int) TimeUnit.DAYS.toHours(7), hours, days,
				months);

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupTestStream(TEST_NODE_ID, TEST_SOURCE_ID, streamId);

		// when
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusWeeks(4).toInstant());
		filter.setUserId(TEST_USER_ID);
		filter.setAggregation(Aggregation.Day);
		FilterResults<AuditDatumRollup, DatumPK> results = dao.findAuditDatumFiltered(filter);

		// then
		assertThat("Day rows for first 4 weeks returned", results.getReturnedResultCount(), equalTo(4));
		int i = 0;
		for ( AuditDatumRollup row : results ) {
			assertAuditDatum("Hour " + i, row, AuditDatumEntityRollup.dailyAuditDatumRollup(TEST_NODE_ID,
					TEST_SOURCE_ID, days.get(i), 100L, 24L, 1, 1000L, 10L, 0L));
			i++;
		}
	}

	@Test
	public void findAuditDatum_forUser_month_noRollup() {
		// given
		ZonedDateTime start = ZonedDateTime.of(2020, 2, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		List<Instant> hours = new ArrayList<>();
		List<Instant> days = new ArrayList<>();
		List<Instant> months = new ArrayList<>();
		UUID streamId = UUID.randomUUID();
		setupTestAuditDatumRecords(start, streamId, 8, (int) TimeUnit.DAYS.toHours(7), hours, days,
				months);

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupTestStream(TEST_NODE_ID, TEST_SOURCE_ID, streamId);

		// when
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusWeeks(4).toInstant());
		filter.setUserId(TEST_USER_ID);
		filter.setAggregation(Aggregation.Month);
		FilterResults<AuditDatumRollup, DatumPK> results = dao.findAuditDatumFiltered(filter);

		// then
		assertThat("Month rows for first 4 weeks returned", results.getReturnedResultCount(),
				equalTo(1));
		assertAuditDatum("Month 1", results.iterator().next(), monthlyAuditDatumRollup(TEST_NODE_ID,
				TEST_SOURCE_ID, start.toInstant(), 3000L, 720L, 30, 1, 30000L, 300L, 0L));
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

	@Test
	public void findAuditDatum_acc_forUser_day_noRollup() {
		// given
		ZonedDateTime start = ZonedDateTime.of(2020, 2, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		List<Instant> days = new ArrayList<>();
		UUID streamId = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId, 8, 7, days);

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupTestStream(TEST_NODE_ID, TEST_SOURCE_ID, streamId);

		// when
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusWeeks(4).toInstant());
		filter.setUserId(TEST_USER_ID);
		FilterResults<AuditDatumRollup, DatumPK> results = dao
				.findAccumulativeAuditDatumFiltered(filter);

		// then
		assertThat("Day rows for first 4 weeks returned", results.getReturnedResultCount(), equalTo(4));
		ZonedDateTime currMonth = null;
		int i = 1, iMonth = 1;
		for ( AuditDatumRollup row : results ) {
			ZonedDateTime d = days.get(i - 1).atZone(start.getZone());
			ZonedDateTime m = d.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
			if ( currMonth == null ) {
				currMonth = m;
			} else if ( !m.isEqual(currMonth) ) {
				iMonth++;
				currMonth = m;
			}
			assertAuditDatum("Daily acc " + i, row, accumulativeAuditDatumRollup(TEST_NODE_ID,
					TEST_SOURCE_ID, d.toInstant(), 100L * i, 24L * i, i, iMonth));
			i++;
		}
	}

	@Test
	public void findAuditDatum_acc_forUser_day_timeAndNodeRollup() {
		// given
		ZonedDateTime start = ZonedDateTime.of(2020, 2, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		List<Instant> days = new ArrayList<>();
		UUID streamId_1 = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId_1, 8, 7, days);
		// add another source
		UUID streamId_2 = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId_2, 8, 7, days);

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupTestStream(TEST_NODE_ID, TEST_SOURCE_ID, streamId_1);
		setupTestStream(TEST_NODE_ID, "s2", streamId_2);

		// when
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusWeeks(4).toInstant());
		filter.setUserId(TEST_USER_ID);
		filter.setDatumRollupTypes(new DatumRollupType[] { DatumRollupType.Time, DatumRollupType.Node });
		FilterResults<AuditDatumRollup, DatumPK> results = dao
				.findAccumulativeAuditDatumFiltered(filter);

		// then
		assertThat("Day rows for first 4 weeks returned", results.getReturnedResultCount(), equalTo(4));
		ZonedDateTime currMonth = null;
		int i = 1, iMonth = 1;
		for ( AuditDatumRollup row : results ) {
			ZonedDateTime d = days.get(i - 1).atZone(start.getZone());
			ZonedDateTime m = d.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
			if ( currMonth == null ) {
				currMonth = m;
			} else if ( !m.isEqual(currMonth) ) {
				iMonth++;
				currMonth = m;
			}
			assertAuditDatum("Daily acc " + i, row, accumulativeAuditDatumRollup(TEST_NODE_ID, null,
					d.toInstant(), 200L * i, 48L * i, 2 * i, 2 * iMonth));
			i++;
		}
	}

	@Test
	public void findAuditDatum_acc_forUser_day_timeRollup() {
		// given
		ZonedDateTime start = ZonedDateTime.of(2020, 2, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		List<Instant> days = new ArrayList<>();
		UUID streamId_1 = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId_1, 8, 7, days);
		// add another source
		UUID streamId_2 = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId_2, 8, 7, days);

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupTestStream(TEST_NODE_ID, TEST_SOURCE_ID, streamId_1);
		setupTestStream(TEST_NODE_ID, "s2", streamId_2);

		// add another node
		UUID streamId_3 = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId_3, 8, 7, days);

		final Long nodeId_2 = UUID.randomUUID().getLeastSignificantBits();
		setupTestNode(nodeId_2);
		setupUserNodeEntity(nodeId_2, TEST_USER_ID);
		setupTestStream(nodeId_2, TEST_SOURCE_ID, streamId_3);

		// when
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusWeeks(4).toInstant());
		filter.setUserId(TEST_USER_ID);
		filter.setDatumRollupTypes(new DatumRollupType[] { DatumRollupType.Time });
		FilterResults<AuditDatumRollup, DatumPK> results = dao
				.findAccumulativeAuditDatumFiltered(filter);

		// then
		assertThat("Day rows for first 4 weeks returned", results.getReturnedResultCount(), equalTo(4));
		ZonedDateTime currMonth = null;
		int i = 1, iMonth = 1;
		for ( AuditDatumRollup row : results ) {
			ZonedDateTime d = days.get(i - 1).atZone(start.getZone());
			ZonedDateTime m = d.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
			if ( currMonth == null ) {
				currMonth = m;
			} else if ( !m.isEqual(currMonth) ) {
				iMonth++;
				currMonth = m;
			}
			assertAuditDatum("Daily acc " + i, row, accumulativeAuditDatumRollup(null, null,
					d.toInstant(), 300L * i, 72L * i, 3 * i, 3 * iMonth));
			i++;
		}
	}

	@Test
	public void findAuditDatum_acc_forUser_day_allRollup() {
		// given
		ZonedDateTime start = ZonedDateTime.of(2020, 2, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		List<Instant> days = new ArrayList<>();
		UUID streamId_1 = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId_1, 8, 7, days);
		// add another source
		UUID streamId_2 = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId_2, 8, 7, new ArrayList<Instant>());

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupTestStream(TEST_NODE_ID, TEST_SOURCE_ID, streamId_1);
		setupTestStream(TEST_NODE_ID, "s2", streamId_2);

		// add another node
		UUID streamId_3 = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId_3, 8, 7, new ArrayList<Instant>());

		final Long nodeId_2 = UUID.randomUUID().getLeastSignificantBits();
		setupTestNode(nodeId_2);
		setupUserNodeEntity(nodeId_2, TEST_USER_ID);
		setupTestStream(nodeId_2, TEST_SOURCE_ID, streamId_3);

		// when
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusWeeks(4).toInstant());
		filter.setUserId(TEST_USER_ID);
		filter.setDatumRollupTypes(new DatumRollupType[] { DatumRollupType.All });
		FilterResults<AuditDatumRollup, DatumPK> results = dao
				.findAccumulativeAuditDatumFiltered(filter);

		// then
		assertThat("Day rows for first 4 weeks returned", results.getReturnedResultCount(), equalTo(1));
		// results combined from rollup; no ts; no node ID; no source ID
		// e.g. raw = (3 * 100) + (3 * 100 * 2) + (3 * 100 * 3) + (3 * 100 * 4) = 3000
		assertAuditDatum("Daily acc 1", results.iterator().next(),
				accumulativeAuditDatumRollup(null, null, null, 3000L, 720L, 30, 12));
	}

	@Test
	public void findAuditDatum_acc_forUser_day_mostRecent_oneNodeAndSource() {
		// given
		ZonedDateTime start = ZonedDateTime.of(2020, 2, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		List<Instant> days = new ArrayList<>();
		UUID streamId_1 = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId_1, 8, 7, days);
		// add another source
		UUID streamId_2 = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId_2, 8, 7, new ArrayList<Instant>());

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupTestStream(TEST_NODE_ID, TEST_SOURCE_ID, streamId_1);
		setupTestStream(TEST_NODE_ID, "s2", streamId_2);

		// add another node
		UUID streamId_3 = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId_3, 8, 7, new ArrayList<Instant>());

		final Long nodeId_2 = UUID.randomUUID().getLeastSignificantBits();
		setupTestNode(nodeId_2);
		setupUserNodeEntity(nodeId_2, TEST_USER_ID);
		setupTestStream(nodeId_2, TEST_SOURCE_ID, streamId_3);

		// when
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(TEST_USER_ID);
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setMostRecent(true);
		FilterResults<AuditDatumRollup, DatumPK> results = dao
				.findAccumulativeAuditDatumFiltered(filter);
		// then
		assertThat("Most recent row returned", results.getReturnedResultCount(), equalTo(1));
		// results are accumulation of 8 days over 2 months
		// e.g. raw = (3 * 100) + (3 * 100 * 2) + (3 * 100 * 3) + (3 * 100 * 4) = 3000
		assertAuditDatum("Daily acc most recent", results.iterator().next(),
				accumulativeAuditDatumRollup(TEST_NODE_ID, TEST_SOURCE_ID, days.get(days.size() - 1),
						800L, 192L, 8, 2));
	}

	@Test
	public void findAuditDatum_acc_forUser_day_mostRecent_multiNodeAndSource() {
		// given
		ZonedDateTime start = ZonedDateTime.of(2020, 2, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		List<Instant> days = new ArrayList<>();
		UUID streamId_1 = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId_1, 8, 7, days);
		// add another source
		UUID streamId_2 = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId_2, 8, 7, new ArrayList<Instant>());

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupTestStream(TEST_NODE_ID, TEST_SOURCE_ID, streamId_1);
		setupTestStream(TEST_NODE_ID, "s2", streamId_2);

		// add another node
		UUID streamId_3 = UUID.randomUUID();
		setupTestAccumulativeAuditDatumRecords(start, streamId_3, 8, 7, new ArrayList<Instant>());

		final Long nodeId_2 = TEST_NODE_ID - 1L;
		setupTestNode(nodeId_2);
		setupUserNodeEntity(nodeId_2, TEST_USER_ID);
		setupTestStream(nodeId_2, TEST_SOURCE_ID, streamId_3);

		// when
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(TEST_USER_ID);
		filter.setMostRecent(true);
		FilterResults<AuditDatumRollup, DatumPK> results = dao
				.findAccumulativeAuditDatumFiltered(filter);

		// then
		assertThat("Most recent rows returned", results.getReturnedResultCount(), equalTo(3));
		// results are accumulation of 8 days over 2 months for each node+source combo
		// and order is node,source
		Iterator<AuditDatumRollup> itr = results.iterator();
		assertAuditDatum("Daily acc most recent 1", itr.next(), accumulativeAuditDatumRollup(nodeId_2,
				TEST_SOURCE_ID, days.get(days.size() - 1), 800L, 192L, 8, 2));
		assertAuditDatum("Daily acc most recent 2", itr.next(), accumulativeAuditDatumRollup(
				TEST_NODE_ID, "s2", days.get(days.size() - 1), 800L, 192L, 8, 2));
		assertAuditDatum("Daily acc most recent 3", itr.next(), accumulativeAuditDatumRollup(
				TEST_NODE_ID, TEST_SOURCE_ID, days.get(days.size() - 1), 800L, 192L, 8, 2));
	}

}
