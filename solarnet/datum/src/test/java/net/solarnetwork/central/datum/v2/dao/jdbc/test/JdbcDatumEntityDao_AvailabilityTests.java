/* ==================================================================
 * JdbcDatumEntityDao_AvailabilityTests.java - 30/11/2020 7:56:33 am
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
import static java.util.stream.Collectors.toList;
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumDateInterval;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;

/**
 * Test cases for {@link JdbcDatumEntityDao} availability methods.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_AvailabilityTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	private DatumProperties testProps() {
		return DatumProperties.propertiesOf(decimalArray("1.1", "1.2"), decimalArray("2.1"), null, null);
	}

	private Datum testDatum(UUID streamId, ZonedDateTime date, DatumProperties props) {
		return new DatumEntity(streamId, date.toInstant(), Instant.now(), props);
	}

	private void assertDatumDateInterval(String prefix, DatumDateInterval result,
			DatumDateInterval expected) {
		assertThat(prefix + " stream ID", result.getObjectDatumId().getStreamId(),
				equalTo(expected.getObjectDatumId().getStreamId()));
		assertThat(prefix + " object ID", result.getObjectDatumId().getObjectId(),
				equalTo(expected.getObjectDatumId().getObjectId()));
		assertThat(prefix + " source ID", result.getObjectDatumId().getSourceId(),
				equalTo(expected.getObjectDatumId().getSourceId()));
		assertThat(prefix + " kind", result.getObjectDatumId().getKind(),
				equalTo(expected.getObjectDatumId().getKind()));
		assertThat(prefix + " start", result.getStart(), equalTo(expected.getStart()));
		assertThat(prefix + " end", result.getEnd(), equalTo(expected.getEnd()));
	}

	@Test
	public void availableRange_nodes() throws IOException {
		// GIVEN
		DatumProperties props = testProps();
		Map<UUID, ObjectDatumStreamMetadata> metas = new LinkedHashMap<>();
		Map<UUID, List<Datum>> datas = new LinkedHashMap<>();
		ZonedDateTime date = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			ObjectDatumStreamMetadata meta = emptyMeta(streamId, "UTC", ObjectDatumKind.Node, (long) i,
					"a");
			metas.put(streamId, meta);
			List<Datum> data = new ArrayList<>();
			for ( int j = 1; j <= 5; j++ ) {
				data.add(testDatum(streamId, date.plusHours(((i - 1) * 3) + (j - 1)), props));
			}
			datas.put(streamId, data);
		}
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, metas.values());
		DatumDbUtils.insertDatum(log, jdbcTemplate,
				datas.values().stream().flatMap(Collection::stream).collect(toList()));

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 3L });
		Iterable<DatumDateInterval> results = dao.findAvailableInterval(filter);

		// THEN
		List<DatumDateInterval> resultList = StreamSupport.stream(results.spliterator(), false)
				.collect(toList());
		List<UUID> streamIds = datas.keySet().stream().collect(toList());
		log.debug("Got intervals:\n{}",
				resultList.stream().map(Object::toString).collect(joining("\n")));
		assertThat("Results for nodes * sources", resultList, hasSize(2));
		assertDatumDateInterval("1", resultList.get(0),
				new DatumDateInterval(date.toInstant(), date.plusHours(4).toInstant(), ZoneOffset.UTC,
						new ObjectDatumId(ObjectDatumKind.Node, streamIds.get(0), 1L, "a", null, null)));
		assertDatumDateInterval("2", resultList.get(1),
				new DatumDateInterval(date.plusHours(6).toInstant(), date.plusHours(10).toInstant(),
						ZoneOffset.UTC,
						new ObjectDatumId(ObjectDatumKind.Node, streamIds.get(2), 3L, "a", null, null)));
	}

	@Test
	public void availableRange_nodesAndSources() throws IOException {
		// GIVEN
		DatumProperties props = testProps();
		Map<UUID, ObjectDatumStreamMetadata> metas = new LinkedHashMap<>();
		Map<UUID, List<Datum>> datas = new LinkedHashMap<>();
		ZonedDateTime date = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			ObjectDatumStreamMetadata meta = emptyMeta(streamId, "UTC", ObjectDatumKind.Node, (long) i,
					"s" + i);
			metas.put(streamId, meta);
			List<Datum> data = new ArrayList<>();
			for ( int j = 1; j <= 5; j++ ) {
				data.add(testDatum(streamId, date.plusHours(((i - 1) * 3) + (j - 1)), props));
			}
			datas.put(streamId, data);
		}
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, metas.values());
		DatumDbUtils.insertDatum(log, jdbcTemplate,
				datas.values().stream().flatMap(Collection::stream).collect(toList()));

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "s1", "s3" });
		Iterable<DatumDateInterval> results = dao.findAvailableInterval(filter);

		// THEN
		List<DatumDateInterval> resultList = StreamSupport.stream(results.spliterator(), false)
				.collect(toList());
		List<UUID> streamIds = datas.keySet().stream().collect(toList());
		log.debug("Got intervals:\n{}",
				resultList.stream().map(Object::toString).collect(joining("\n")));
		assertThat("Results for nodes * sources", resultList, hasSize(2));
		assertDatumDateInterval("1", resultList.get(0), new DatumDateInterval(date.toInstant(),
				date.plusHours(4).toInstant(), ZoneOffset.UTC,
				new ObjectDatumId(ObjectDatumKind.Node, streamIds.get(0), 1L, "s1", null, null)));
		assertDatumDateInterval("2", resultList.get(1), new DatumDateInterval(
				date.plusHours(6).toInstant(), date.plusHours(10).toInstant(), ZoneOffset.UTC,
				new ObjectDatumId(ObjectDatumKind.Node, streamIds.get(2), 3L, "s3", null, null)));
	}

	@Test
	public void availableRange_nodesAndSourcePattern() throws IOException {
		// GIVEN
		DatumProperties props = testProps();
		Map<UUID, ObjectDatumStreamMetadata> metas = new LinkedHashMap<>();
		Map<UUID, List<Datum>> datas = new LinkedHashMap<>();
		ZonedDateTime date = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			ObjectDatumStreamMetadata meta = emptyMeta(streamId, "UTC", ObjectDatumKind.Node, (long) i,
					"s" + i);
			metas.put(streamId, meta);
			List<Datum> data = new ArrayList<>();
			for ( int j = 1; j <= 5; j++ ) {
				data.add(testDatum(streamId, date.plusHours(((i - 1) * 3) + (j - 1)), props));
			}
			datas.put(streamId, data);
		}
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, metas.values());
		DatumDbUtils.insertDatum(log, jdbcTemplate,
				datas.values().stream().flatMap(Collection::stream).collect(toList()));

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 3L });
		filter.setSourceIds(new String[] { "s*" });
		Iterable<DatumDateInterval> results = dao.findAvailableInterval(filter);

		// THEN
		List<DatumDateInterval> resultList = StreamSupport.stream(results.spliterator(), false)
				.collect(toList());
		List<UUID> streamIds = datas.keySet().stream().collect(toList());
		log.debug("Got intervals:\n{}",
				resultList.stream().map(Object::toString).collect(joining("\n")));
		assertThat("Results for nodes * sources", resultList, hasSize(2));
		assertDatumDateInterval("1", resultList.get(0), new DatumDateInterval(date.toInstant(),
				date.plusHours(4).toInstant(), ZoneOffset.UTC,
				new ObjectDatumId(ObjectDatumKind.Node, streamIds.get(0), 1L, "s1", null, null)));
		assertDatumDateInterval("2", resultList.get(1), new DatumDateInterval(
				date.plusHours(6).toInstant(), date.plusHours(10).toInstant(), ZoneOffset.UTC,
				new ObjectDatumId(ObjectDatumKind.Node, streamIds.get(2), 3L, "s3", null, null)));
	}

	@Test
	public void availableRange_nodes_onlyOne() throws IOException {
		// GIVEN
		DatumProperties props = testProps();
		ZonedDateTime date = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		UUID streamId = UUID.randomUUID();
		ObjectDatumStreamMetadata meta = emptyMeta(streamId, "UTC", ObjectDatumKind.Node, 1L, "a");
		Datum datum = testDatum(streamId, date, props);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, singleton(datum));

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		Iterable<DatumDateInterval> results = dao.findAvailableInterval(filter);

		// THEN
		List<DatumDateInterval> resultList = StreamSupport.stream(results.spliterator(), false)
				.collect(toList());
		log.debug("Got intervals:\n{}",
				resultList.stream().map(Object::toString).collect(joining("\n")));
		assertThat("Results for nodes * sources", resultList, hasSize(1));
		assertDatumDateInterval("Start end same when only 1 datum", resultList.get(0),
				new DatumDateInterval(date.toInstant(), date.toInstant(), ZoneOffset.UTC,
						new ObjectDatumId(ObjectDatumKind.Node, streamId, 1L, "a", null, null)));
	}

	@Test
	public void availableRange_locations() throws IOException {
		// GIVEN
		DatumProperties props = testProps();
		Map<UUID, ObjectDatumStreamMetadata> metas = new LinkedHashMap<>();
		Map<UUID, List<Datum>> datas = new LinkedHashMap<>();
		ZonedDateTime date = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			ObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(streamId, "UTC",
					ObjectDatumKind.Location, (long) i, "a");
			metas.put(streamId, meta);
			List<Datum> data = new ArrayList<>();
			for ( int j = 1; j <= 5; j++ ) {
				data.add(testDatum(streamId, date.plusHours(((i - 1) * 3) + (j - 1)), props));
			}
			datas.put(streamId, data);
		}
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, metas.values());
		DatumDbUtils.insertDatum(log, jdbcTemplate,
				datas.values().stream().flatMap(Collection::stream).collect(toList()));

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationIds(new Long[] { 1L, 3L });
		filter.setObjectKind(ObjectDatumKind.Location);
		Iterable<DatumDateInterval> results = dao.findAvailableInterval(filter);

		// THEN
		List<DatumDateInterval> resultList = StreamSupport.stream(results.spliterator(), false)
				.collect(toList());
		List<UUID> streamIds = datas.keySet().stream().collect(toList());
		log.debug("Got intervals:\n{}",
				resultList.stream().map(Object::toString).collect(joining("\n")));
		assertThat("Results for locations * sources", resultList, hasSize(2));
		assertDatumDateInterval("1", resultList.get(0), new DatumDateInterval(date.toInstant(),
				date.plusHours(4).toInstant(), ZoneOffset.UTC,
				new ObjectDatumId(ObjectDatumKind.Location, streamIds.get(0), 1L, "a", null, null)));
		assertDatumDateInterval("2", resultList.get(1), new DatumDateInterval(
				date.plusHours(6).toInstant(), date.plusHours(10).toInstant(), ZoneOffset.UTC,
				new ObjectDatumId(ObjectDatumKind.Location, streamIds.get(2), 3L, "a", null, null)));
	}

}
