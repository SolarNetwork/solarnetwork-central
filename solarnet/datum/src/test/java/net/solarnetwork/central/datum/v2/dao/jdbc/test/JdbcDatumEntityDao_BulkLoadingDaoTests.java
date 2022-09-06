/* ==================================================================
 * JdbcDatumEntityDao_BulkLoadingDaoTests.java - 2/12/2020 5:15:18 pm
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
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntity.ioAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertStaleAggregateDatum;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.transaction.TestTransaction;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.StaleAggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.domain.datum.DatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.dao.BasicBulkLoadingOptions;
import net.solarnetwork.dao.BulkLoadingDao;
import net.solarnetwork.dao.BulkLoadingDao.LoadingContext;
import net.solarnetwork.dao.BulkLoadingDao.LoadingExceptionHandler;
import net.solarnetwork.dao.BulkLoadingDao.LoadingTransactionMode;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class' implementation of
 * {@link BulkLoadingDao}.
 * 
 * @author matt
 * @version 2.0
 */
public class JdbcDatumEntityDao_BulkLoadingDaoTests extends BaseDatumJdbcTestSupport {

	protected static final String TEST_2ND_SOURCE = "2nd source";
	protected static final Long TEST_2ND_NODE = -200L;

	private JdbcDatumEntityDao dao;

	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
		dao.setBulkLoadDataSource(jdbcTemplate.getDataSource());
		dao.setBulkLoadTransactionManager(txManager);
	}

	private List<GeneralNodeDatum> createSampleData(int count, ZonedDateTime start) {
		return createSampleData(count, start, TEST_NODE_ID, TEST_SOURCE_ID);
	}

	private List<GeneralNodeDatum> createSampleData(int count, ZonedDateTime start, Long nodeId,
			String sourceId) {
		List<GeneralNodeDatum> data = new ArrayList<>(4);
		long wh = (long) (Math.random() * 1000000000.0);
		for ( int i = 0; i < count; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setNodeId(nodeId);
			d.setCreated(start.plusMinutes(i).toInstant());
			d.setSourceId(sourceId);

			DatumSamples s = new DatumSamples();
			int watts = (int) (Math.random() * 50000);
			s.putInstantaneousSampleValue("watts", watts);
			wh += (long) (watts / 60.0);
			s.putAccumulatingSampleValue("wattHours", wh);
			d.setSamples(s);
			data.add(d);
		}
		return data;
	}

	private void bulkLoad(List<GeneralNodeDatum> data, BulkLoadingDao.LoadingOptions options) {
		try (LoadingContext<GeneralNodeDatum> ctx = dao.createBulkLoadingContext(options,
				new LoadingExceptionHandler<GeneralNodeDatum>() {

					@Override
					public void handleLoadingException(Throwable t,
							LoadingContext<GeneralNodeDatum> context) {
						throw new RuntimeException(t);
					}
				})) {
			for ( GeneralNodeDatum d : data ) {
				ctx.load(d);
			}
			ctx.commit();
		}
	}

	@Test
	public void bulkImport() {
		try {
			// GIVEN
			TestTransaction.end();

			// load 1 hour of data
			final int datumCount = 59;
			ZonedDateTime start = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).minusHours(1);
			List<GeneralNodeDatum> data = createSampleData(datumCount, start);

			// WHEN
			BasicBulkLoadingOptions options = new BasicBulkLoadingOptions("Test load", null,
					LoadingTransactionMode.SingleTransaction, null);
			bulkLoad(data, options);

			// THEN
			List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
			log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));
			assertThat("Datum rows imported", loaded, hasSize(data.size()));

			List<StaleAggregateDatum> staleHours = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate,
					Aggregation.Hour);
			assertThat("One stale hour recorded", staleHours, hasSize(1));
			assertStaleAggregateDatum("Stale hour", staleHours.get(0), new StaleAggregateDatumEntity(
					loaded.get(0).getStreamId(), start.toInstant(), Aggregation.Hour, null));

			List<AuditDatum> audits = DatumDbUtils.listAuditDatum(jdbcTemplate, Aggregation.None);
			ZonedDateTime thisHour = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
			assertThat("One audit hour", audits, hasSize(1));
			assertAuditDatum("Audit hour", audits.get(0), ioAuditDatum(loaded.get(0).getStreamId(),
					thisHour.toInstant(), (long) datumCount, (long) datumCount * 2, 0L, 0L));
		} finally {
			// manually clean up transactionally circumvented data import data
			DatumTestUtils.cleanupDatabase(jdbcTemplate);
		}
	}

	@Test
	public void bulkImport_updateExistingStream() {
		try {
			// GIVEN
			TestTransaction.end();

			ZonedDateTime thisHour = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);

			// add existing stream/audit data
			UUID streamId = UUID.randomUUID();
			ObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(streamId,
					ZoneId.systemDefault().toString(), ObjectDatumKind.Node, TEST_NODE_ID,
					TEST_SOURCE_ID);
			DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
			AuditDatum hourAudit = AuditDatumEntity.ioAuditDatum(streamId, thisHour.toInstant(), 1000L,
					10000L, 123456789L, 0L);
			DatumDbUtils.insertAuditDatum(log, jdbcTemplate, singleton(hourAudit));

			// load 1 hour of data
			ZonedDateTime start = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).minusHours(1);
			final int datumCount = 59;
			BasicBulkLoadingOptions options = new BasicBulkLoadingOptions("Test load", null,
					LoadingTransactionMode.SingleTransaction, null);
			List<GeneralNodeDatum> data = createSampleData(datumCount, start);

			// WHEN
			bulkLoad(data, options);

			// THEN
			List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
			log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));
			assertThat("Datum rows imported", loaded, hasSize(data.size()));

			List<StaleAggregateDatum> staleHours = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate,
					Aggregation.Hour);
			assertThat("One stale hour recorded", staleHours, hasSize(1));
			assertStaleAggregateDatum("Stale hour", staleHours.get(0), new StaleAggregateDatumEntity(
					loaded.get(0).getStreamId(), start.toInstant(), Aggregation.Hour, null));

			List<AuditDatum> audits = DatumDbUtils.listAuditDatum(jdbcTemplate, Aggregation.None);
			assertThat("One audit hour", audits, hasSize(1));
			// NOTE: the prop update count is NOT incremented during bulk import, as we  can't tell insert vs. update
			assertAuditDatum("Existing audit hour updated", audits.get(0),
					ioAuditDatum(loaded.get(0).getStreamId(), thisHour.toInstant(),
							datumCount + hourAudit.getDatumCount(),
							(long) datumCount * 2 + hourAudit.getDatumPropertyCount(),
							hourAudit.getDatumQueryCount(), 0L));
		} finally {
			// manually clean up transactionally circumvented data import data
			DatumTestUtils.cleanupDatabase(jdbcTemplate);
		}
	}

	@Test
	public void bulkImport_multipleStreams() {
		// GIVEN
		try {
			TestTransaction.end();

			final int datumCount = 59;
			ZonedDateTime start1 = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).minusMonths(1)
					.minusHours(1);
			List<GeneralNodeDatum> data1 = createSampleData(datumCount, start1, TEST_NODE_ID,
					TEST_SOURCE_ID);
			ZonedDateTime start2 = start1.minusDays(1);
			List<GeneralNodeDatum> data2 = createSampleData(datumCount, start2, TEST_2ND_NODE,
					TEST_2ND_SOURCE);
			List<GeneralNodeDatum> data = new ArrayList<>(data1);
			data.addAll(data2);

			// WHEN
			BasicBulkLoadingOptions options = new BasicBulkLoadingOptions("Test load", null,
					LoadingTransactionMode.SingleTransaction, null);
			bulkLoad(data, options);

			// THEN
			List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
			log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));
			assertThat("Datum rows imported", loaded, hasSize(data.size()));

			Map<UUID, Long> streamToNodeIds = DatumDbUtils.listNodeMetadata(jdbcTemplate).stream()
					.collect(toMap(DatumStreamMetadata::getStreamId,
							ObjectDatumStreamMetadata::getObjectId));

			List<StaleAggregateDatum> staleHours = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate,
					Aggregation.Hour);
			assertThat("Two stale hour recorded (one each stream)", staleHours, hasSize(2));

			List<AuditDatum> audits = DatumDbUtils.listAuditDatum(jdbcTemplate, Aggregation.None);
			assertThat("One audit hour per stream", audits, hasSize(2));

			ZonedDateTime thisHour = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
			for ( int i = 0; i < 2; i++ ) {
				UUID streamId = staleHours.get(i).getStreamId();
				Long nodeId = streamToNodeIds.get(streamId);
				ZonedDateTime date;
				if ( nodeId.equals(TEST_NODE_ID) ) {
					date = start1;
				} else {
					date = start2;
				}
				assertStaleAggregateDatum("Stale hour " + i, staleHours.get(i),
						new StaleAggregateDatumEntity(streamId, date.toInstant(), Aggregation.Hour,
								null));

				streamId = audits.get(i).getStreamId();
				nodeId = streamToNodeIds.get(streamId);
				if ( nodeId.equals(TEST_NODE_ID) ) {
					date = start1;
				} else {
					date = start2;
				}
				assertAuditDatum("Audit hour " + i, audits.get(i), ioAuditDatum(streamId,
						thisHour.toInstant(), (long) datumCount, (long) datumCount * 2, 0L, 0L));
			}
		} finally {
			// manually clean up transactionally circumvented data import data
			DatumTestUtils.cleanupDatabase(jdbcTemplate);
		}
	}

}
