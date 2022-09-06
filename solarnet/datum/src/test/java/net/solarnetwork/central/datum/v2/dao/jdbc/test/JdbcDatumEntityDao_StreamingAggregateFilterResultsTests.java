/* ==================================================================
 * JdbcDatumEntityDao_StreamingAggregateFilterResultsTests.java - 6/05/2022 8:51:19 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.UUID_STRING_ORDER;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertObjectDatumStreamMetadata;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonAggregateDatumResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.sortedStreamIds;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.BasicStreamDatumFilteredResultsProcessor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Test cases for stream aggregate filter results in {@link JdbcDatumEntityDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_StreamingAggregateFilterResultsTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	protected DatumEntity lastDatum;

	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	private ObjectDatumStreamMetadata testStreamMetadata() {
		return testStreamMetadata(1L, "a", "UTC");
	}

	private ObjectDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId,
			String timeZoneId) {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				timeZoneId, ObjectDatumKind.Node, nodeId, sourceId, new String[] { "x", "y" },
				new String[] { "w" }, null);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		return meta;
	}

	@Test
	public void find_hour_streamId_orderDefault() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-hour-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final BasicStreamDatumFilteredResultsProcessor processor = new BasicStreamDatumFilteredResultsProcessor();
		final UUID streamId = meta.getStreamId();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(streamId);
		filter.setAggregation(Aggregation.Hour);
		filter.setStartDate(start.plusHours(1).toInstant());
		filter.setEndDate(start.plusHours(12).toInstant());
		dao.findFilteredStream(filter, processor);

		// THEN
		List<StreamDatum> datumList = processor.getData();
		assertThat("Results collected", datumList, hasSize(3));

		SortedSet<UUID> streamIds = sortedStreamIds(processor.getMetadataProvider(), UUID_STRING_ORDER);
		assertThat("Result stream IDs count", streamIds, contains(streamId));

		ObjectDatumStreamMetadata resultMeta = processor.getMetadataProvider()
				.metadataForStreamId(streamId);
		assertThat("Metadata is for node", resultMeta.getKind(), is(equalTo(ObjectDatumKind.Node)));
		assertThat("Node ID", resultMeta.getObjectId(), is(equalTo(meta.getObjectId())));

		Instant ts = start.plusHours(3).toInstant();
		for ( int i = 0; i < 3; i++ ) {
			StreamDatum d = datumList.get(i);
			assertThat("Stream ID ", d.getStreamId(), is(equalTo(streamId)));
			assertThat("Ordered by timestamp " + i, d.getTimestamp(), is(equalTo(ts)));

			assertThat("Aggregate datum returned " + i, d, is(instanceOf(AggregateDatum.class)));
			AggregateDatum ad = (AggregateDatum) d;
			assertThat("Aggregate type matches criteria", ad.getAggregation(),
					is(equalTo(filter.getAggregation())));
			assertThat("Statistics provided", ad.getStatistics(), is(notNullValue()));

			ts = ts.plusSeconds(TimeUnit.HOURS.toSeconds(3));
		}
	}

}
