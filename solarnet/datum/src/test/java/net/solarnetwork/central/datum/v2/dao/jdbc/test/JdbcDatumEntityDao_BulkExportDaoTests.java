/* ==================================================================
 * JdbcDatumEntityDao_BulkExportDaoTests.java - 3/12/2020 11:07:35 am
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
import static java.util.Collections.singletonMap;
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.dao.BasicBulkExportOptions;
import net.solarnetwork.dao.BulkExportingDao;
import net.solarnetwork.dao.BulkExportingDao.ExportCallback;
import net.solarnetwork.dao.BulkExportingDao.ExportCallbackAction;
import net.solarnetwork.dao.BulkExportingDao.ExportResult;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class' implementation of
 * {@link BulkExportingDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_BulkExportDaoTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	private void insertDatum(Long nodeId, String sourceId, String propPrefix, ZonedDateTime start,
			Duration frequency, int count) {
		jdbcTemplate.execute("{call solardatm.store_datum(?,?,?,?,?)}",
				new CallableStatementCallback<Void>() {

					@Override
					public Void doInCallableStatement(CallableStatement cs)
							throws SQLException, DataAccessException {
						ZonedDateTime ts = start;
						Timestamp received = Timestamp.from(Instant.now());
						DatumSamples data = new DatumSamples();
						for ( int i = 0; i < count; i++ ) {
							cs.setTimestamp(1, Timestamp.from(ts.toInstant()));
							cs.setLong(2, nodeId);
							cs.setString(3, sourceId);
							cs.setTimestamp(4, received);

							data.putInstantaneousSampleValue(propPrefix + "_i", Math.random() * 1000000);
							data.putAccumulatingSampleValue(propPrefix + "_a", i + 1);

							String jdata = JsonUtils.getJSONString(data, null);
							cs.setString(5, jdata);
							cs.execute();
							log.debug("Inserted datum node {} source {} ts {}", nodeId, sourceId, ts);
							ts = ts.plus(frequency);
						}
						return null;
					}
				});
	}

	@Test
	public void bulkExport_raw_nodesAndSources() {
		// GIVEN
		setupTestNode();
		UUID streamId = UUID.randomUUID();
		ObjectDatumStreamMetadata meta = emptyMeta(streamId, TEST_TZ, ObjectDatumKind.Node, TEST_NODE_ID,
				TEST_SOURCE_ID);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		ZonedDateTime start = ZonedDateTime.of(2018, 11, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		insertDatum(TEST_NODE_ID, TEST_SOURCE_ID, "a", start, Duration.ofMinutes(1), 10);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusDays(1).toInstant());
		filter.setWithoutTotalResultsCount(false);

		BasicBulkExportOptions options = new BasicBulkExportOptions("test",
				singletonMap(DatumEntityDao.EXPORT_PARAMETER_DATUM_CRITERIA, filter));

		List<Long> totalResultCountEstimates = new ArrayList<>(1);

		ExportResult result = dao.bulkExport(new ExportCallback<GeneralNodeDatumFilterMatch>() {

			private int count = 0;

			@Override
			public void didBegin(Long totalResultCountEstimate) {
				totalResultCountEstimates.add(totalResultCountEstimate);
			}

			@Override
			public ExportCallbackAction handle(GeneralNodeDatumFilterMatch d) {
				assertThat("Datum ts", d.getId().getCreated(),
						equalTo(start.plusMinutes(count).toInstant()));
				assertThat("Datum node ID", d.getId().getNodeId(), equalTo(TEST_NODE_ID));
				assertThat("Datum source ID", d.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
				count++;
				return ExportCallbackAction.CONTINUE;
			}
		}, options);

		assertThat("Result available", result, notNullValue());
		assertThat("Num processed count", result.getNumProcessed(), equalTo(10L));
		assertThat("Total result count estimates", totalResultCountEstimates, contains(10L));
	}

	@Test
	public void bulkExport_aggregate15Minute() {
		// GIVEN
		setupTestNode();
		UUID streamId = UUID.randomUUID();
		ObjectDatumStreamMetadata meta = emptyMeta(streamId, TEST_TZ, ObjectDatumKind.Node, TEST_NODE_ID,
				TEST_SOURCE_ID);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		ZonedDateTime start = ZonedDateTime.of(2018, 11, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		insertDatum(TEST_NODE_ID, TEST_SOURCE_ID, "a", start, Duration.ofMinutes(2), 60);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setWithoutTotalResultsCount(true);

		BasicBulkExportOptions options = new BasicBulkExportOptions("test",
				singletonMap(DatumEntityDao.EXPORT_PARAMETER_DATUM_CRITERIA, filter));

		List<Long> totalResultCountEstimates = new ArrayList<>(1);

		ExportResult result = dao.bulkExport(new ExportCallback<GeneralNodeDatumFilterMatch>() {

			private int count = 0;

			@Override
			public void didBegin(Long totalResultCountEstimate) {
				totalResultCountEstimates.add(totalResultCountEstimate);
			}

			@Override
			public ExportCallbackAction handle(GeneralNodeDatumFilterMatch d) {
				assertThat("Datum ts", d.getId().getCreated(),
						equalTo(start.plusMinutes(count * 15).toInstant()));
				assertThat("Datum node ID", d.getId().getNodeId(), equalTo(TEST_NODE_ID));
				assertThat("Datum source ID", d.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
				log.debug("Exported datum: {}", d);
				count++;
				return ExportCallbackAction.CONTINUE;
			}
		}, options);

		assertThat("Result available", result, notNullValue());
		assertThat("Num processed count", result.getNumProcessed(), equalTo(4L));
		assertThat("Total result count estimates", totalResultCountEstimates, contains((Long) null));
	}

}
