/* ==================================================================
 * JdbcDatumEntityDao_StreamingFilterResultsTests.java - 1/05/2022 8:36:27 pm
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

import static java.util.stream.Collectors.toMap;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.UUID_STRING_ORDER;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listNodeMetadata;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.sortedStreamIds;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.populateBasicDatumStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.UUID;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.support.BasicStreamDatumFilteredResultsProcessor;
import net.solarnetwork.domain.datum.DatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Test cases for stream filter results in {@link JdbcDatumEntityDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_StreamingFilterResultsTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	protected DatumEntity lastDatum;

	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	@Test
	public void stream_byStreamId() throws IOException {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		populateBasicDatumStream(jdbcTemplate, log, 1L, "s1", "foo", start, freq, 4);
		populateBasicDatumStream(jdbcTemplate, log, 2L, "s2", "bim", start.plusMinutes(15), freq, 4);

		Map<UUID, ObjectDatumStreamMetadata> metas = listNodeMetadata(jdbcTemplate).stream()
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));

		// WHEN
		final BasicStreamDatumFilteredResultsProcessor processor = new BasicStreamDatumFilteredResultsProcessor();
		final UUID streamId = metas.keySet().iterator().next();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(streamId);
		dao.findFilteredStream(filter, processor);

		// THEN
		List<StreamDatum> datumList = processor.getData();
		assertThat("Results collected", datumList, hasSize(1));

		SortedSet<UUID> streamIds = sortedStreamIds(processor.getMetadataProvider(), UUID_STRING_ORDER);
		assertThat("Result stream IDs count", streamIds, contains(streamId));

		ObjectDatumStreamMetadata meta = processor.getMetadataProvider().metadataForStreamId(streamId);
		assertThat("Metadata is for node", meta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", meta.getObjectId(), equalTo(metas.get(streamId).getObjectId()));
		Instant ts = start.toInstant();
		for ( int i = 0; i < 4; i++ ) {
			StreamDatum d = datumList.get(i);
			assertThat("Stream ID ", d.getStreamId(), equalTo(streamId));
			assertThat("Ordered by timestamp", d.getTimestamp(), not(lessThan(ts)));
			ts = d.getTimestamp();
		}
	}

}
