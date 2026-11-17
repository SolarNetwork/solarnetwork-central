/* ==================================================================
 * JdbcDatumEntityDao_StreamLimitTests.java - 18/11/2026 8:00:00 am
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static java.util.Comparator.comparing;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.UUID_STRING_ORDER;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.domain.SimpleSortDescriptor.sorts;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.domain.datum.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Location;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static org.assertj.core.api.BDDAssertions.then;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.support.QueryingDatumStreamsAccessor;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatum;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test that the per-stream limit {@link SelectDatum} applies to sorted, limited
 * criteria returns the same rows, order, pages and total counts as a plain join
 * would.
 *
 * <p>
 * The expected results are computed from the inserted datum in memory, ordered
 * the way the SQL {@code ORDER BY} orders them.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_StreamLimitTests extends BaseDatumJdbcTestSupport {

	private static final Long USER_ID = -9101L;
	private static final String SOURCE_A = "s/a";
	private static final String SOURCE_B = "s/b";
	private static final String SOURCE_C = "s/c";
	private static final String ALIAS_SOURCE = "s/x";
	private static final String LOC_SOURCE = "w/1";
	private static final LocalDateTime START_LOCAL = LocalDateTime.of(2026, 1, 1, 0, 0);
	private static final Duration FREQ = Duration.ofMinutes(10);
	private static final int COUNT = 60;

	/** A datum row with the metadata the SQL sorts by. */
	private record Row(UUID streamId, Long objectId, String sourceId, Instant ts) {

		private DatumPK id() {
			return new DatumPK(streamId, ts);
		}

	}

	private static final Comparator<Row> STREAM_TIME = comparing(Row::streamId, UUID_STRING_ORDER)
			.thenComparing(Row::ts);

	private static final Comparator<Row> STREAM_TIME_DESC = comparing(Row::streamId, UUID_STRING_ORDER)
			.thenComparing(Row::ts, Comparator.reverseOrder());

	private static final Comparator<Row> TIME_DESC_NODE_SOURCE = comparing(Row::ts,
			Comparator.<Instant> reverseOrder()).thenComparing(Row::objectId)
					.thenComparing(Row::sourceId);

	private static final Comparator<Row> TIME_NODE_SOURCE = comparing(Row::ts)
			.thenComparing(Row::objectId).thenComparing(Row::sourceId);

	private JdbcDatumEntityDao dao;
	private Long nodeId;
	private Long aliasNodeId;
	private Instant start;
	private ObjectDatumStreamMetadata streamA;
	private UUID aliasStreamId;
	private final List<Row> rows = new ArrayList<>();
	private final List<Row> hourlyRows = new ArrayList<>();

	@BeforeEach
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
		setupTestLocation();
		setupTestUser(USER_ID, "streamlimit@localhost");
		nodeId = randomLong();
		setupTestNode(nodeId);
		setupTestUserNode(USER_ID, nodeId, "Main");
		aliasNodeId = randomLong();
		setupTestNode(aliasNodeId);
		setupTestUserNode(USER_ID, aliasNodeId, "Alias");

		start = START_LOCAL.atZone(ZoneId.of(TEST_TZ)).toInstant();

		streamA = stream(Node, nodeId, SOURCE_A);
		final ObjectDatumStreamMetadata streamB = stream(Node, nodeId, SOURCE_B);
		final ObjectDatumStreamMetadata streamC = stream(Node, nodeId, SOURCE_C);
		final ObjectDatumStreamMetadata streamLoc = stream(Location, TEST_LOC_ID, LOC_SOURCE);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate,
				List.of(streamA, streamB, streamC, streamLoc));

		final Instant now = Instant.now();
		aliasStreamId = UUID.randomUUID();
		DatumDbUtils.insertObjectDatumStreamAliases(log, jdbcTemplate,
				List.of(new ObjectDatumStreamAliasEntity(aliasStreamId, now, now, Node, aliasNodeId,
						ALIAS_SOURCE, nodeId, SOURCE_A)));

		final List<Datum> datum = new ArrayList<>();
		for ( int i = 0; i < COUNT; i++ ) {
			// B is offset from A, so the two interleave in time
			datum.add(datum(streamA, start.plus(FREQ.multipliedBy(i)), i));
			datum.add(datum(streamB, start.plus(FREQ.multipliedBy(i)).plusSeconds(300), i));
			// the location stream has fewer datum, every 20 minutes
			if ( i % 2 == 0 ) {
				datum.add(datum(streamLoc, start.plus(FREQ.multipliedBy(i)), i));
			}
		}
		// C has fewer datum than the limits used, at the same times as some of A so ties in time
		// are ordered by source
		for ( int i = 10; i < 40; i += 10 ) {
			datum.add(datum(streamC, start.plus(FREQ.multipliedBy(i)), i));
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		for ( Datum d : datum ) {
			final ObjectDatumStreamMetadata meta = List.of(streamA, streamB, streamC, streamLoc).stream()
					.filter(m -> m.getStreamId().equals(d.getStreamId())).findFirst().orElseThrow();
			rows.add(new Row(d.getStreamId(), meta.getObjectId(), meta.getSourceId(), d.getTimestamp()));
			if ( d.getStreamId().equals(streamA.getStreamId()) ) {
				rows.add(new Row(aliasStreamId, aliasNodeId, ALIAS_SOURCE, d.getTimestamp()));
			}
		}

		final List<AggregateDatum> hourly = new ArrayList<>();
		for ( int i = 0; i < 30; i++ ) {
			final Instant ts = start.plus(Duration.ofHours(i));
			hourly.add(
					new AggregateDatumEntity(streamA.getStreamId(), ts, Aggregation.Hour,
							propertiesOf(new BigDecimal[] { BigDecimal.valueOf(i) }, null, null, null),
							statisticsOf(new BigDecimal[][] {
									{ BigDecimal.ONE, BigDecimal.valueOf(i), BigDecimal.valueOf(i) } },
									null)));
			hourlyRows.add(new Row(streamA.getStreamId(), nodeId, SOURCE_A, ts));
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, hourly);
	}

	private static ObjectDatumStreamMetadata stream(ObjectDatumKind kind, Long objectId,
			String sourceId) {
		return new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_TZ, kind, objectId, sourceId,
				new String[] { "x" }, null, null);
	}

	private static Datum datum(ObjectDatumStreamMetadata meta, Instant ts, int value) {
		return new DatumEntity(meta.getStreamId(), ts, Instant.now(),
				propertiesOf(new BigDecimal[] { BigDecimal.valueOf(value) }, null, null, null));
	}

	private static List<DatumPK> expected(List<Row> rows, Predicate<Row> match, Comparator<Row> order) {
		return rows.stream().filter(match).sorted(order).map(Row::id).toList();
	}

	private static Predicate<Row> streamIn(UUID... streamIds) {
		final List<UUID> ids = List.of(streamIds);
		return r -> ids.contains(r.streamId());
	}

	private static Predicate<Row> between(Instant from, Instant to) {
		return r -> !r.ts().isBefore(from) && r.ts().isBefore(to);
	}

	private static List<DatumPK> ids(ObjectDatumStreamFilterResults<Datum, DatumPK> results) {
		return StreamSupport.stream(results.spliterator(), false).map(Datum::getId).toList();
	}

	private static void thenStreamLimited(BasicDatumCriteria filter) {
		// @formatter:off
		then(new SelectDatum(filter).getSql())
			.as("Criteria qualifies for the per-stream limit")
			.contains("INNER JOIN LATERAL")
			;
		// @formatter:on
	}

	/**
	 * Page through a criteria and verify every page and total count.
	 */
	private void thenPagesMatch(BasicDatumCriteria filter, int max, List<DatumPK> all) {
		final List<DatumPK> found = new ArrayList<>();
		long offset = 0;
		while ( true ) {
			BasicDatumCriteria c = filter.clone();
			c.setWithoutTotalResultsCount(false);
			c.setMax(max);
			c.setOffset(offset);
			thenStreamLimited(c);

			// WHEN
			ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(c);

			// THEN
			final List<DatumPK> page = ids(results);
			final List<DatumPK> expectedPage = all.subList((int) Math.min(offset, all.size()),
					(int) Math.min(offset + max, all.size()));
			// @formatter:off
			then(page)
				.as("Page at offset %d", offset)
				.containsExactlyElementsOf(expectedPage)
				;
			then(results.getTotalResults())
				.as("Total count of all rows at offset %d", offset)
				.isEqualTo((long) all.size())
				;
			// @formatter:on
			if ( page.isEmpty() ) {
				break;
			}
			found.addAll(page);
			offset += max;
		}
		// @formatter:off
		then(found)
			.as("All pages together are all rows, in order")
			.containsExactlyElementsOf(all)
			;
		then(offset)
			.as("Paged through several pages, to a page past the end")
			.isGreaterThan(2L * max)
			;
		// @formatter:on
	}

	private BasicDatumCriteria accessorCriteria(Instant endDate) {
		// what QueryingDatumStreamsAccessor.query() asks for
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setObjectKind(Node);
		c.setNodeId(nodeId);
		c.setSourceId(SOURCE_A);
		c.setSorts(QueryingDatumStreamsAccessor.SORT_BY_DATE_DESCENDING);
		c.setUserId(USER_ID);
		c.setStartDate(endDate.minus(Duration.ofDays(90)));
		c.setEndDate(endDate);
		return c;
	}

	@Test
	public void accessorQuery_latest() {
		for ( int max : new int[] { 1, 3, 100 } ) {
			for ( int endIndex : new int[] { 0, 1, 25, COUNT } ) {
				// GIVEN
				final Instant endDate = start.plus(FREQ.multipliedBy(endIndex));
				BasicDatumCriteria c = accessorCriteria(endDate);
				c.setMax(max);
				c.setWithoutTotalResultsCount(false);
				thenStreamLimited(c);

				// WHEN
				ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(c);

				// THEN
				final List<DatumPK> all = expected(rows,
						streamIn(streamA.getStreamId()).and(between(c.getStartDate(), endDate)),
						STREAM_TIME_DESC);
				// @formatter:off
				then(ids(results))
					.as("Latest %d datum before index %d", max, endIndex)
					.containsExactlyElementsOf(all.subList(0, Math.min(max, all.size())))
					;
				then(results.getTotalResults())
					.as("Total count of rows before index %d", endIndex)
					.isEqualTo((long) all.size())
					;
				// @formatter:on
			}
		}
	}

	@Test
	public void accessorQuery_emptyRange() {
		// GIVEN
		BasicDatumCriteria c = accessorCriteria(start.minus(Duration.ofDays(1)));
		c.setStartDate(start.minus(Duration.ofDays(2)));
		c.setMax(1);
		c.setWithoutTotalResultsCount(false);
		thenStreamLimited(c);

		// WHEN
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(c);

		// THEN
		// @formatter:off
		then(ids(results))
			.as("No datum in range")
			.isEmpty()
			;
		then(results.getTotalResults())
			.as("Total count")
			.isEqualTo(0L)
			;
		// @formatter:on
	}

	@Test
	public void fewerRowsThanLimit() {
		// GIVEN
		final UUID streamC = rows.stream().filter(r -> SOURCE_C.equals(r.sourceId())).findFirst()
				.orElseThrow().streamId();
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setNodeId(nodeId);
		c.setSourceId(SOURCE_C);
		c.setStartDate(start);
		c.setEndDate(start.plus(Duration.ofDays(1)));
		c.setMax(10);
		c.setWithoutTotalResultsCount(false);
		thenStreamLimited(c);

		// WHEN
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(c);

		// THEN
		final List<DatumPK> all = expected(rows, streamIn(streamC), STREAM_TIME);
		// @formatter:off
		then(ids(results))
			.as("All datum of stream with fewer than the limit")
			.hasSize(3)
			.containsExactlyElementsOf(all)
			;
		then(results.getTotalResults())
			.as("Total count")
			.isEqualTo(3L)
			;
		// @formatter:on
	}

	@Test
	public void pages_nodeAndSource_defaultSort() {
		// GIVEN
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setNodeId(nodeId);
		c.setSourceId(SOURCE_A);
		c.setStartDate(start.plus(FREQ.multipliedBy(3)));
		c.setEndDate(start.plus(FREQ.multipliedBy(50)));

		// THEN
		thenPagesMatch(c, 7,
				expected(rows,
						streamIn(streamA.getStreamId()).and(between(c.getStartDate(), c.getEndDate())),
						STREAM_TIME));
	}

	@Test
	public void pages_nodeAndSource_timeDescending() {
		// GIVEN
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setNodeId(nodeId);
		c.setSourceId(SOURCE_A);
		c.setUserId(USER_ID);
		c.setSorts(List.of(new SimpleSortDescriptor("time", true)));
		c.setStartDate(start);
		c.setEndDate(start.plus(Duration.ofDays(1)));

		// THEN
		thenPagesMatch(c, 9, expected(rows, streamIn(streamA.getStreamId()), STREAM_TIME_DESC));
	}

	@Test
	public void pages_stream_noDates() {
		// GIVEN
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setStreamId(streamA.getStreamId());
		c.setSorts(QueryingDatumStreamsAccessor.SORT_BY_DATE_DESCENDING);

		// THEN
		thenPagesMatch(c, 11, expected(rows, streamIn(streamA.getStreamId()), STREAM_TIME_DESC));
	}

	@Test
	public void pages_nodeAndSource_localDates() {
		// GIVEN
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setNodeId(nodeId);
		c.setSourceId(SOURCE_A);
		c.setSorts(QueryingDatumStreamsAccessor.SORT_BY_DATE_DESCENDING);
		c.setLocalStartDate(START_LOCAL.plusHours(1));
		c.setLocalEndDate(START_LOCAL.plusHours(8));

		// THEN
		final ZoneId zone = ZoneId.of(TEST_TZ);
		thenPagesMatch(c, 6, expected(rows,
				streamIn(streamA.getStreamId())
						.and(between(START_LOCAL.plusHours(1).atZone(zone).toInstant(),
								START_LOCAL.plusHours(8).atZone(zone).toInstant())),
				STREAM_TIME_DESC));
	}

	@Test
	public void pages_alias() {
		// GIVEN
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setIncludeStreamAliases(true);
		c.setNodeId(aliasNodeId);
		c.setSourceId(ALIAS_SOURCE);
		c.setUserId(USER_ID);
		c.setSorts(QueryingDatumStreamsAccessor.SORT_BY_DATE_DESCENDING);
		c.setStartDate(start.plus(FREQ.multipliedBy(5)));
		c.setEndDate(start.plus(FREQ.multipliedBy(45)));

		// THEN
		// the alias stream returns the original stream's datum under the alias stream ID
		thenPagesMatch(c, 8,
				expected(rows, streamIn(aliasStreamId).and(between(c.getStartDate(), c.getEndDate())),
						STREAM_TIME_DESC));
	}

	@Test
	public void pages_location() {
		// GIVEN
		final UUID locStreamId = rows.stream().filter(r -> LOC_SOURCE.equals(r.sourceId())).findFirst()
				.orElseThrow().streamId();
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setObjectKind(Location);
		c.setLocationId(TEST_LOC_ID);
		c.setSourceId(LOC_SOURCE);
		c.setSorts(QueryingDatumStreamsAccessor.SORT_BY_DATE_DESCENDING);
		c.setStartDate(start);
		c.setEndDate(start.plus(Duration.ofDays(1)));

		// THEN
		thenPagesMatch(c, 4, expected(rows, streamIn(locStreamId), STREAM_TIME_DESC));
	}

	@Test
	public void pages_hourly_nodeAndSource_timeDescending() {
		// GIVEN
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setAggregation(Aggregation.Hour);
		c.setNodeId(nodeId);
		c.setSourceId(SOURCE_A);
		c.setSorts(QueryingDatumStreamsAccessor.SORT_BY_DATE_DESCENDING);
		c.setStartDate(start.plus(Duration.ofHours(2)));
		c.setEndDate(start.plus(Duration.ofHours(27)));

		// THEN
		thenPagesMatch(c, 4,
				expected(hourlyRows, between(c.getStartDate(), c.getEndDate()), STREAM_TIME_DESC));
	}

	@Test
	public void pages_nodeAndSources_sortTimeDescNodeSource() {
		// GIVEN
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setNodeId(nodeId);
		c.setSourceIds(new String[] { SOURCE_A, SOURCE_B, SOURCE_C });
		c.setSorts(List.of(new SimpleSortDescriptor("time", true), new SimpleSortDescriptor("node"),
				new SimpleSortDescriptor("source")));
		c.setStartDate(start.plus(FREQ.multipliedBy(2)));
		c.setEndDate(start.plus(FREQ.multipliedBy(40)));

		// THEN
		thenPagesMatch(c, 13, expected(rows,
				r -> nodeId.equals(r.objectId()) && between(c.getStartDate(), c.getEndDate()).test(r),
				TIME_DESC_NODE_SOURCE));
	}

	@Test
	public void pages_nodesAndSourcePattern_sortTimeNodeSource() {
		// GIVEN
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setIncludeStreamAliases(true);
		c.setUserId(USER_ID);
		c.setNodeIds(new Long[] { nodeId, aliasNodeId });
		c.setSourceId("s/*");
		c.setSorts(sorts("time", "node", "source"));
		c.setStartDate(start.plus(FREQ.multipliedBy(9)));
		c.setEndDate(start.plus(FREQ.multipliedBy(31)));

		// THEN
		// many streams, including the alias, sorted by time first
		thenPagesMatch(c, 10, expected(rows, r -> !LOC_SOURCE.equals(r.sourceId())
				&& between(c.getStartDate(), c.getEndDate()).test(r), TIME_NODE_SOURCE));
	}

}
