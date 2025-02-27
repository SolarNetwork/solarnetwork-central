/* ==================================================================
 * QueryingDatumStreamsAccessorTests.java - 19/02/2025 3:59:41 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.support.test;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.support.QueryingDatumStreamsAccessor;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatum;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@link QueryingDatumStreamsAccessor} class.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class QueryingDatumStreamsAccessorTests {

	@Mock
	private DatumEntityDao datumDao;

	@Mock
	private QueryAuditor queryAuditor;

	@Captor
	private ArgumentCaptor<DatumCriteria> criteriaCaptor;

	@Captor
	private ArgumentCaptor<Datum> datumCaptor;

	private Clock clock;
	private Long userId;
	private Long nodeId;

	@BeforeEach
	public void setup() {
		clock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.MINUTES), ZoneOffset.UTC);
		userId = randomLong();
		nodeId = randomLong();
	}

	private List<ObjectDatumStreamMetadata> testStreamMetas(Long nodeId, int count) {
		List<ObjectDatumStreamMetadata> metas = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			var meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "Pacific/Auckland",
					ObjectDatumKind.Node, nodeId, randomString(), new String[] { "a" }, null, null);
			metas.add(meta);
		}
		return metas;
	}

	private List<ObjectDatum> testNodeDatum(Long nodeId, Iterable<ObjectDatumStreamMetadata> streamMetas,
			Instant end, Duration frequency, int count) {
		List<ObjectDatum> result = new ArrayList<>();
		for ( int i = 0; i < count; i++ ) {
			Instant ts = end.minusSeconds(i * frequency.getSeconds());
			for ( ObjectDatumStreamMetadata meta : streamMetas ) {
				BasicStreamDatum d = new BasicStreamDatum(meta.getStreamId(), ts,
						propertiesOf(new BigDecimal[] { new BigDecimal(i) }, null, null, null));
				DatumSamples s = new DatumSamples();
				s.putInstantaneousSampleValue("a", i);
				result.add(ObjectDatum.forStreamDatum(d, nodeId,
						DatumId.nodeId(nodeId, meta.getSourceId(), ts), meta));
			}
		}
		return result;
	}

	@Test
	public void offsetTimestamp_oneBeforeOldest() {
		// GIVEN
		final int sourceIdCount = 3;
		final var streamMetas = testStreamMetas(nodeId, sourceIdCount);
		final var datumFreq = Duration.ofMinutes(5);
		final var datum = testNodeDatum(nodeId, streamMetas, clock.instant(), datumFreq, 6);

		var randStreamMeta = streamMetas.get(RNG.nextInt(sourceIdCount));

		var accessor = new QueryingDatumStreamsAccessor(new AntPathMatcher(), datum, userId, clock,
				datumDao, null);

		var datumEntity = new DatumEntity(randStreamMeta.getStreamId(),
				datum.getLast().getTimestamp().minus(datumFreq), null,
				propertiesOf(new BigDecimal[] { new BigDecimal(Integer.MAX_VALUE) }, null, null, null));
		var filterResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				streamMetas.stream().collect(toUnmodifiableMap(m -> m.getStreamId(), identity())),
				List.of(datumEntity));

		given(datumDao.findFiltered(any())).willReturn(filterResults);

		// WHEN
		Datum result = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(),
				datum.getLast().getTimestamp(), 1);

		// try again, to validate the datum from query is cached in accessor
		Datum result2 = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(),
				datum.getLast().getTimestamp(), 1);

		// THEN
		then(datumDao).should(times(1)).findFiltered(criteriaCaptor.capture());

		// @formatter:off
		and.then(criteriaCaptor.getValue())
			.as("Query for user")
			.returns(userId, from(DatumCriteria::getUserId))
			.as("Query for stream node")
			.returns(nodeId, from(DatumCriteria::getNodeId))
			.as("Query for stream source")
			.returns(randStreamMeta.getSourceId(), from(DatumCriteria::getSourceId))
			.as("Query for at most one datum")
			.returns(1, from(DatumCriteria::getMax))
			.as("Query end date is given timestamp")
			.returns(datum.getLast().getTimestamp(), from(DatumCriteria::getEndDate))
			.as("Query start date is offset from end date by configured duration")
			.returns(datum.getLast().getTimestamp().minus(accessor.getMaxStartDateDuration()), DatumCriteria::getStartDate)
			;

		and.then(result)
			.as("Datum for node ID returned")
			.returns(nodeId, from(Datum::getObjectId))
			.as("Datum for source ID returned")
			.returns(randStreamMeta.getSourceId(), from(Datum::getSourceId))
			.as("Datum for timestamp offset from DAO returned")
			.returns(datumEntity.getTimestamp(), from(Datum::getTimestamp))
			.as("Returned ObjectDatum for DAO result")
			.isInstanceOf(ObjectDatum.class)
			.asInstanceOf(type(ObjectDatum.class))
			.as("Properties from DAO returned in ObjectDatum")
			.returns(datumEntity.getProperties(), from(ObjectDatum::getProperties))
			;
		and.then(result2)
			.as("Offset from DAO returned 2nd time")
			.isSameAs(result)
			;
		// @formatter:on
	}

	@Test
	public void offsetTimestamp_oneBeforeOldest_notFound() {
		// GIVEN
		final int sourceIdCount = 3;
		final var streamMetas = testStreamMetas(nodeId, sourceIdCount);
		final var datumFreq = Duration.ofMinutes(5);
		final var datum = testNodeDatum(nodeId, streamMetas, clock.instant(), datumFreq, 6);

		var randStreamMeta = streamMetas.get(RNG.nextInt(sourceIdCount));

		var accessor = new QueryingDatumStreamsAccessor(new AntPathMatcher(), datum, userId, clock,
				datumDao, null);

		var filterResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				streamMetas.stream().collect(toUnmodifiableMap(m -> m.getStreamId(), identity())),
				List.of());

		given(datumDao.findFiltered(any())).willReturn(filterResults);

		// WHEN
		Datum result = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(),
				datum.getLast().getTimestamp(), 1);

		// THEN
		then(datumDao).should(times(1)).findFiltered(criteriaCaptor.capture());

		// @formatter:off
		and.then(criteriaCaptor.getValue())
			.as("Query for user")
			.returns(userId, from(DatumCriteria::getUserId))
			.as("Query for stream node")
			.returns(nodeId, from(DatumCriteria::getNodeId))
			.as("Query for stream source")
			.returns(randStreamMeta.getSourceId(), from(DatumCriteria::getSourceId))
			.as("Query for at most one datum")
			.returns(1, from(DatumCriteria::getMax))
			.as("Query end date is given timestamp")
			.returns(datum.getLast().getTimestamp(), from(DatumCriteria::getEndDate))
			.as("Query start date is offset from end date by configured duration")
			.returns(datum.getLast().getTimestamp().minus(accessor.getMaxStartDateDuration()), DatumCriteria::getStartDate)
			;

		and.then(result)
			.as("Datum not found")
			.isNull()
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void offsetTimestamp_someBeforeOldest_notEnoughFound() {
		// GIVEN
		final int sourceIdCount = 3;
		final var streamMetas = testStreamMetas(nodeId, sourceIdCount);
		final var datumFreq = Duration.ofMinutes(5);
		final var datum = testNodeDatum(nodeId, streamMetas, clock.instant(), datumFreq, 6);

		var randStreamMeta = streamMetas.get(RNG.nextInt(sourceIdCount));

		var accessor = new QueryingDatumStreamsAccessor(new AntPathMatcher(), datum, userId, clock,
				datumDao, null);

		// we'll be asking for 2 datum, but get only one back
		var datumEntity = new DatumEntity(randStreamMeta.getStreamId(),
				datum.getLast().getTimestamp().minus(datumFreq), null,
				propertiesOf(new BigDecimal[] { new BigDecimal(Integer.MAX_VALUE) }, null, null, null));
		var filterResults1 = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				streamMetas.stream().collect(toUnmodifiableMap(m -> m.getStreamId(), identity())),
				List.of(datumEntity));
		var filterResults2 = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				streamMetas.stream().collect(toUnmodifiableMap(m -> m.getStreamId(), identity())),
				List.of());

		given(datumDao.findFiltered(any())).willReturn(filterResults1, filterResults2);

		// WHEN
		Datum result = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(),
				datum.getLast().getTimestamp(), 2);

		Datum result2 = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(),
				datum.getLast().getTimestamp(), 2);

		Datum result3 = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(),
				datum.getLast().getTimestamp(), 1);

		// THEN
		then(datumDao).should(times(2)).findFiltered(criteriaCaptor.capture());

		// @formatter:off
		and.then(criteriaCaptor.getAllValues())
			.element(0)
			.as("Query for user")
			.returns(userId, from(DatumCriteria::getUserId))
			.as("Query for stream node")
			.returns(nodeId, from(DatumCriteria::getNodeId))
			.as("Query for stream source")
			.returns(randStreamMeta.getSourceId(), from(DatumCriteria::getSourceId))
			.as("Query for at most two datum (calculated from available data)")
			.returns(2, from(DatumCriteria::getMax))
			.as("Query end date is given timestamp")
			.returns(datum.getLast().getTimestamp(), from(DatumCriteria::getEndDate))
			.as("Query start date is offset from end date by configured duration")
			.returns(datum.getLast().getTimestamp().minus(accessor.getMaxStartDateDuration()), DatumCriteria::getStartDate)
			;

		and.then(criteriaCaptor.getAllValues())
			.element(1)
			.as("Query for user")
			.returns(userId, from(DatumCriteria::getUserId))
			.as("Query for stream node")
			.returns(nodeId, from(DatumCriteria::getNodeId))
			.as("Query for stream source")
			.returns(randStreamMeta.getSourceId(), from(DatumCriteria::getSourceId))
			.as("Query for at most one datum (calculated from available data, which grew in previous query)")
			.returns(1, from(DatumCriteria::getMax))
			.as("Query end date is newly discovered datum from previous query")
			.returns(datum.getLast().getTimestamp().minus(datumFreq), from(DatumCriteria::getEndDate))
			.as("Query start date is offset from end date by configured duration")
			.returns(datum.getLast().getTimestamp().minus(datumFreq).minus(accessor.getMaxStartDateDuration()), DatumCriteria::getStartDate)
			;

		and.then(result)
			.as("Datum not found")
			.isNull()
			;

		and.then(result2)
			.as("Datum not found 2nd time")
			.isNull()
			;

		and.then(result3)
			.as("Datum for node ID returned")
			.returns(nodeId, from(Datum::getObjectId))
			.as("Datum for source ID returned")
			.returns(randStreamMeta.getSourceId(), from(Datum::getSourceId))
			.as("Datum for timestamp offset from DAO returned")
			.returns(datumEntity.getTimestamp(), from(Datum::getTimestamp))
			.as("Returned ObjectDatum for DAO result")
			.isInstanceOf(ObjectDatum.class)
			.asInstanceOf(type(ObjectDatum.class))
			.as("Properties from DAO returned in ObjectDatum")
			.returns(datumEntity.getProperties(), from(ObjectDatum::getProperties))
			;
		// @formatter:on
	}

	@Test
	public void offsetTimestamp_oneBeforeNewest_alreadyAvailable() {
		// GIVEN
		final int sourceIdCount = 3;
		final var streamMetas = testStreamMetas(nodeId, sourceIdCount);
		final var datumFreq = Duration.ofMinutes(5);
		final var datum = testNodeDatum(nodeId, streamMetas, clock.instant(), datumFreq, 6);

		var randStreamMeta = streamMetas.get(RNG.nextInt(sourceIdCount));

		var accessor = new QueryingDatumStreamsAccessor(new AntPathMatcher(), datum, userId, clock,
				datumDao, null);

		// WHEN
		Datum result = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(),
				datum.getFirst().getTimestamp(), 1);

		// THEN

		var expectedDatum = datum.stream()
				.filter(d -> d.getObjectId().equals(nodeId)
						&& d.getSourceId().equals(randStreamMeta.getSourceId())
						&& d.getTimestamp().equals(datum.getFirst().getTimestamp().minus(datumFreq)))
				.findFirst().orElse(null);
		// @formatter:off
		and.then(result)
			.as("Expected datum is returned")
			.isSameAs(expectedDatum)
			;
		// @formatter:on
	}

	@Test
	public void offsetTimestamp_oneMoreThanAvailableBeforeNewest() {
		// GIVEN
		final int sourceIdCount = 3;
		final var streamMetas = testStreamMetas(nodeId, sourceIdCount);
		final var datumFreq = Duration.ofMinutes(5);
		final var datum = testNodeDatum(nodeId, streamMetas, clock.instant(), datumFreq, 6);

		var randStreamMeta = streamMetas.get(RNG.nextInt(sourceIdCount));

		var accessor = new QueryingDatumStreamsAccessor(new AntPathMatcher(), datum, userId, clock,
				datumDao, null);

		var datumEntity = new DatumEntity(randStreamMeta.getStreamId(),
				datum.getLast().getTimestamp().minus(datumFreq), null,
				propertiesOf(new BigDecimal[] { new BigDecimal(Integer.MAX_VALUE) }, null, null, null));
		var filterResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				streamMetas.stream().collect(toUnmodifiableMap(m -> m.getStreamId(), identity())),
				List.of(datumEntity));

		given(datumDao.findFiltered(any())).willReturn(filterResults);

		// WHEN
		// ask for 6 before first datum, which is 1 more than available so we should query for one more datum
		Datum result = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(),
				datum.getFirst().getTimestamp(), 6);

		// try again, to validate the datum from query is cached in accessor
		Datum result2 = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(),
				datum.getFirst().getTimestamp(), 6);

		// THEN
		then(datumDao).should(times(1)).findFiltered(criteriaCaptor.capture());

		// @formatter:off
		and.then(criteriaCaptor.getValue())
			.as("Query for user")
			.returns(userId, from(DatumCriteria::getUserId))
			.as("Query for stream node")
			.returns(nodeId, from(DatumCriteria::getNodeId))
			.as("Query for stream source")
			.returns(randStreamMeta.getSourceId(), from(DatumCriteria::getSourceId))
			.as("Query for at most one datum, calculated as needed offset from available")
			.returns(1, from(DatumCriteria::getMax))
			.as("Query end date is from last available")
			.returns(datum.getLast().getTimestamp(), from(DatumCriteria::getEndDate))
			.as("Query start date is offset from end date by configured duration")
			.returns(datum.getLast().getTimestamp().minus(accessor.getMaxStartDateDuration()), DatumCriteria::getStartDate)
			;

		and.then(result)
			.as("Datum for node ID returned")
			.returns(nodeId, from(Datum::getObjectId))
			.as("Datum for source ID returned")
			.returns(randStreamMeta.getSourceId(), from(Datum::getSourceId))
			.as("Datum for timestamp offset from DAO returned")
			.returns(datumEntity.getTimestamp(), from(Datum::getTimestamp))
			.as("Returned ObjectDatum for DAO result")
			.isInstanceOf(ObjectDatum.class)
			.asInstanceOf(type(ObjectDatum.class))
			.as("Properties from DAO returned in ObjectDatum")
			.returns(datumEntity.getProperties(), from(ObjectDatum::getProperties))
			;
		and.then(result2)
			.as("Offset from DAO returned 2nd time")
			.isSameAs(result)
			;
		// @formatter:on
	}

	@Test
	public void offsetTimestamp_someMoreThanAvailableBeforeNewest() {
		// GIVEN
		final int sourceIdCount = 3;
		final var streamMetas = testStreamMetas(nodeId, sourceIdCount);
		final var datumFreq = Duration.ofMinutes(5);
		final var datum = testNodeDatum(nodeId, streamMetas, clock.instant(), datumFreq, 6);

		var randStreamMeta = streamMetas.get(RNG.nextInt(sourceIdCount));

		var accessor = new QueryingDatumStreamsAccessor(new AntPathMatcher(), datum, userId, clock,
				datumDao, queryAuditor);

		var datumEntity1 = new DatumEntity(randStreamMeta.getStreamId(),
				datum.getLast().getTimestamp().minus(datumFreq), null,
				propertiesOf(new BigDecimal[] { new BigDecimal(Integer.MAX_VALUE) }, null, null, null));
		var datumEntity2 = new DatumEntity(randStreamMeta.getStreamId(),
				datum.getLast().getTimestamp().minusSeconds(datumFreq.getSeconds() * 2), null,
				propertiesOf(new BigDecimal[] { new BigDecimal(Integer.MAX_VALUE) }, null, null, null));
		var filterResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				streamMetas.stream().collect(toUnmodifiableMap(m -> m.getStreamId(), identity())),
				List.of(datumEntity1, datumEntity2));

		given(datumDao.findFiltered(any())).willReturn(filterResults);

		// WHEN
		// ask for 6 before first datum, which is 1 more than available so we should query for one more datum
		Datum result = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(),
				datum.getFirst().getTimestamp(), 7);

		// try again, to validate the datum from query is cached in accessor
		Datum result2 = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(),
				datum.getFirst().getTimestamp(), 7);

		// also validate the datum from in-between query is cached in accessor
		Datum result3 = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(),
				datum.getFirst().getTimestamp(), 6);

		// THEN
		then(datumDao).should(times(1)).findFiltered(criteriaCaptor.capture());

		// @formatter:off
		and.then(criteriaCaptor.getValue())
			.as("Query for user")
			.returns(userId, from(DatumCriteria::getUserId))
			.as("Query for stream node")
			.returns(nodeId, from(DatumCriteria::getNodeId))
			.as("Query for stream source")
			.returns(randStreamMeta.getSourceId(), from(DatumCriteria::getSourceId))
			.as("Query for at most two datum, calculated as needed offset from available")
			.returns(2, from(DatumCriteria::getMax))
			.as("Query end date is from last available")
			.returns(datum.getLast().getTimestamp(), from(DatumCriteria::getEndDate))
			.as("Query start date is offset from end date by configured duration")
			.returns(datum.getLast().getTimestamp().minus(accessor.getMaxStartDateDuration()), DatumCriteria::getStartDate)
			;

		and.then(result)
			.as("Datum for node ID returned")
			.returns(nodeId, from(Datum::getObjectId))
			.as("Datum for source ID returned")
			.returns(randStreamMeta.getSourceId(), from(Datum::getSourceId))
			.as("Datum for timestamp offset from DAO returned")
			.returns(datumEntity2.getTimestamp(), from(Datum::getTimestamp))
			.as("Returned ObjectDatum for DAO result")
			.isInstanceOf(ObjectDatum.class)
			.asInstanceOf(type(ObjectDatum.class))
			.as("Properties from DAO returned in ObjectDatum")
			.returns(datumEntity2.getProperties(), from(ObjectDatum::getProperties))
			;
		and.then(result2)
			.as("Offset from DAO returned 2nd time")
			.isSameAs(result)
			;
		and.then(result3)
			.as("Datum for node ID returned")
			.returns(nodeId, from(Datum::getObjectId))
			.as("Datum for source ID returned")
			.returns(randStreamMeta.getSourceId(), from(Datum::getSourceId))
			.as("Datum for timestamp offset from DAO returned")
			.returns(datumEntity1.getTimestamp(), from(Datum::getTimestamp))
			.as("Returned ObjectDatum for DAO result")
			.isInstanceOf(ObjectDatum.class)
			.asInstanceOf(type(ObjectDatum.class))
			.as("Properties from DAO returned in ObjectDatum")
			.returns(datumEntity1.getProperties(), from(ObjectDatum::getProperties))
			;

		then(queryAuditor).should(times(2)).auditNodeDatum(datumCaptor.capture());

		and.then(datumCaptor.getAllValues())
			.as("Audit same datum returned")
			.containsExactly(result3, result)
			;
		// @formatter:on
	}

	@Test
	public void offset_someMoreThanAvailableBeforeNewest() {
		// GIVEN
		final int sourceIdCount = 3;
		final var streamMetas = testStreamMetas(nodeId, sourceIdCount);
		final var datumFreq = Duration.ofMinutes(5);
		final var datum = testNodeDatum(nodeId, streamMetas, clock.instant(), datumFreq, 6);

		var randStreamMeta = streamMetas.get(RNG.nextInt(sourceIdCount));

		var accessor = new QueryingDatumStreamsAccessor(new AntPathMatcher(), datum, userId, clock,
				datumDao, null);

		var datumEntity1 = new DatumEntity(randStreamMeta.getStreamId(),
				datum.getLast().getTimestamp().minus(datumFreq), null,
				propertiesOf(new BigDecimal[] { new BigDecimal(Integer.MAX_VALUE) }, null, null, null));
		var datumEntity2 = new DatumEntity(randStreamMeta.getStreamId(),
				datum.getLast().getTimestamp().minusSeconds(datumFreq.getSeconds() * 2), null,
				propertiesOf(new BigDecimal[] { new BigDecimal(Integer.MAX_VALUE) }, null, null, null));
		var filterResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				streamMetas.stream().collect(toUnmodifiableMap(m -> m.getStreamId(), identity())),
				List.of(datumEntity1, datumEntity2));

		given(datumDao.findFiltered(any())).willReturn(filterResults);

		// WHEN
		// ask for 6 before first datum, which is 1 more than available so we should query for one more datum
		Datum result = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(), 7);

		// try again, to validate the datum from query is cached in accessor
		Datum result2 = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(), 7);

		// also validate the datum from in-between query is cached in accessor
		Datum result3 = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(), 6);

		// THEN
		then(datumDao).should(times(1)).findFiltered(criteriaCaptor.capture());

		// @formatter:off
		and.then(criteriaCaptor.getValue())
			.as("Query for user")
			.returns(userId, from(DatumCriteria::getUserId))
			.as("Query for stream node")
			.returns(nodeId, from(DatumCriteria::getNodeId))
			.as("Query for stream source")
			.returns(randStreamMeta.getSourceId(), from(DatumCriteria::getSourceId))
			.as("Query for at most two datum, calculated as needed offset from available")
			.returns(2, from(DatumCriteria::getMax))
			.as("Query end date is from last available")
			.returns(datum.getLast().getTimestamp(), from(DatumCriteria::getEndDate))
			.as("Query start date is offset from end date by configured duration")
			.returns(datum.getLast().getTimestamp().minus(accessor.getMaxStartDateDuration()), DatumCriteria::getStartDate)
			;

		and.then(result)
			.as("Datum for node ID returned")
			.returns(nodeId, from(Datum::getObjectId))
			.as("Datum for source ID returned")
			.returns(randStreamMeta.getSourceId(), from(Datum::getSourceId))
			.as("Datum for timestamp offset from DAO returned")
			.returns(datumEntity2.getTimestamp(), from(Datum::getTimestamp))
			.as("Returned ObjectDatum for DAO result")
			.isInstanceOf(ObjectDatum.class)
			.asInstanceOf(type(ObjectDatum.class))
			.as("Properties from DAO returned in ObjectDatum")
			.returns(datumEntity2.getProperties(), from(ObjectDatum::getProperties))
			;
		and.then(result2)
			.as("Offset from DAO returned 2nd time")
			.isSameAs(result)
			;
		and.then(result3)
			.as("Datum for node ID returned")
			.returns(nodeId, from(Datum::getObjectId))
			.as("Datum for source ID returned")
			.returns(randStreamMeta.getSourceId(), from(Datum::getSourceId))
			.as("Datum for timestamp offset from DAO returned")
			.returns(datumEntity1.getTimestamp(), from(Datum::getTimestamp))
			.as("Returned ObjectDatum for DAO result")
			.isInstanceOf(ObjectDatum.class)
			.asInstanceOf(type(ObjectDatum.class))
			.as("Properties from DAO returned in ObjectDatum")
			.returns(datumEntity1.getProperties(), from(ObjectDatum::getProperties))
			;
		// @formatter:on
	}

	@Test
	public void latestAvailable_newStream() {
		// GIVEN
		final int sourceIdCount = 3;
		final var streamMetas = testStreamMetas(nodeId, sourceIdCount);
		final var datumFreq = Duration.ofMinutes(5);
		final var datum = testNodeDatum(nodeId, streamMetas, clock.instant(), datumFreq, 6);

		// a stream not even present in the data
		var randStreamMeta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "Pacific/Auckland",
				ObjectDatumKind.Node, nodeId, randomString(), new String[] { "a" }, null, null);

		var accessor = new QueryingDatumStreamsAccessor(new AntPathMatcher(), datum, userId, clock,
				datumDao, null);

		var datumEntity = new DatumEntity(randStreamMeta.getStreamId(),
				datum.getLast().getTimestamp().minus(datumFreq), null,
				propertiesOf(new BigDecimal[] { new BigDecimal(Integer.MAX_VALUE) }, null, null, null));
		var filterResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				Map.of(randStreamMeta.getStreamId(), randStreamMeta), List.of(datumEntity));

		given(datumDao.findFiltered(any())).willReturn(filterResults);

		// WHEN
		// ask for latest
		Datum result = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(), 0);

		// try again, to validate the datum from query is cached in accessor
		Datum result2 = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(), 0);

		// THEN
		then(datumDao).should(times(1)).findFiltered(criteriaCaptor.capture());

		// @formatter:off
		and.then(criteriaCaptor.getValue())
			.as("Query for user")
			.returns(userId, from(DatumCriteria::getUserId))
			.as("Query for stream node")
			.returns(nodeId, from(DatumCriteria::getNodeId))
			.as("Query for stream source")
			.returns(randStreamMeta.getSourceId(), from(DatumCriteria::getSourceId))
			.as("Query for at most one datum")
			.returns(1, from(DatumCriteria::getMax))
			.as("Query end date is from clock time")
			.returns(clock.instant(), from(DatumCriteria::getEndDate))
			.as("Query start date is offset from end date by configured duration")
			.returns(clock.instant().minus(accessor.getMaxStartDateDuration()), DatumCriteria::getStartDate)
			;

		and.then(result)
			.as("Datum for node ID returned")
			.returns(nodeId, from(Datum::getObjectId))
			.as("Datum for source ID returned")
			.returns(randStreamMeta.getSourceId(), from(Datum::getSourceId))
			.as("Datum for timestamp offset from DAO returned")
			.returns(datumEntity.getTimestamp(), from(Datum::getTimestamp))
			.as("Returned ObjectDatum for DAO result")
			.isInstanceOf(ObjectDatum.class)
			.asInstanceOf(type(ObjectDatum.class))
			.as("Properties from DAO returned in ObjectDatum")
			.returns(datumEntity.getProperties(), from(ObjectDatum::getProperties))
			;
		and.then(result2)
			.as("Offset from DAO returned 2nd time")
			.isSameAs(result)
			;
		// @formatter:on
	}

	@Test
	public void latestAvailable_newStream_audit() {
		// GIVEN
		final int sourceIdCount = 3;
		final var streamMetas = testStreamMetas(nodeId, sourceIdCount);
		final var datumFreq = Duration.ofMinutes(5);
		final var datum = testNodeDatum(nodeId, streamMetas, clock.instant(), datumFreq, 6);

		// a stream not even present in the data
		var randStreamMeta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "Pacific/Auckland",
				ObjectDatumKind.Node, nodeId, randomString(), new String[] { "a" }, null, null);

		var accessor = new QueryingDatumStreamsAccessor(new AntPathMatcher(), datum, userId, clock,
				datumDao, queryAuditor);

		var datumEntity = new DatumEntity(randStreamMeta.getStreamId(),
				datum.getLast().getTimestamp().minus(datumFreq), null,
				propertiesOf(new BigDecimal[] { new BigDecimal(Integer.MAX_VALUE) }, null, null, null));
		var filterResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				Map.of(randStreamMeta.getStreamId(), randStreamMeta), List.of(datumEntity));

		given(datumDao.findFiltered(any())).willReturn(filterResults);

		// WHEN
		// ask for latest
		Datum result = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(), 0);

		// try again, to validate the datum from query is cached in accessor
		Datum result2 = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(), 0);

		// THEN
		then(datumDao).should(times(1)).findFiltered(criteriaCaptor.capture());

		// @formatter:off
		and.then(criteriaCaptor.getValue())
			.as("Query for user")
			.returns(userId, from(DatumCriteria::getUserId))
			.as("Query for stream node")
			.returns(nodeId, from(DatumCriteria::getNodeId))
			.as("Query for stream source")
			.returns(randStreamMeta.getSourceId(), from(DatumCriteria::getSourceId))
			.as("Query for at most one datum")
			.returns(1, from(DatumCriteria::getMax))
			.as("Query end date is from clock time")
			.returns(clock.instant(), from(DatumCriteria::getEndDate))
			.as("Query start date is offset from end date by configured duration")
			.returns(clock.instant().minus(accessor.getMaxStartDateDuration()), DatumCriteria::getStartDate)
			;

		and.then(result)
			.as("Datum for node ID returned")
			.returns(nodeId, from(Datum::getObjectId))
			.as("Datum for source ID returned")
			.returns(randStreamMeta.getSourceId(), from(Datum::getSourceId))
			.as("Datum for timestamp offset from DAO returned")
			.returns(datumEntity.getTimestamp(), from(Datum::getTimestamp))
			.as("Returned ObjectDatum for DAO result")
			.isInstanceOf(ObjectDatum.class)
			.asInstanceOf(type(ObjectDatum.class))
			.as("Properties from DAO returned in ObjectDatum")
			.returns(datumEntity.getProperties(), from(ObjectDatum::getProperties))
			;
		and.then(result2)
			.as("Offset from DAO returned 2nd time")
			.isSameAs(result)
			;

		then(queryAuditor).should(times(1)).auditNodeDatum(datumCaptor.capture());

		and.then(datumCaptor.getValue())
			.as("Audit same datum returned")
			.isSameAs(result)
			;
		// @formatter:on
	}

	@Test
	public void latestAvailable_notAfterTimestampOlderThanOldestAvailable() {
		// GIVEN
		final int sourceIdCount = 3;
		final var streamMetas = testStreamMetas(nodeId, sourceIdCount);
		final var datumFreq = Duration.ofMinutes(5);
		final var datum = testNodeDatum(nodeId, streamMetas, clock.instant(), datumFreq, 6);

		var randStreamMeta = streamMetas.get(RNG.nextInt(sourceIdCount));

		var accessor = new QueryingDatumStreamsAccessor(new AntPathMatcher(), datum, userId, clock,
				datumDao, null);

		var datumEntity = new DatumEntity(randStreamMeta.getStreamId(),
				datum.getLast().getTimestamp().minus(datumFreq), null,
				propertiesOf(new BigDecimal[] { new BigDecimal(Integer.MAX_VALUE) }, null, null, null));
		var filterResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				streamMetas.stream().collect(toUnmodifiableMap(m -> m.getStreamId(), identity())),
				List.of(datumEntity));

		given(datumDao.findFiltered(any())).willReturn(filterResults);

		// WHEN
		// query for "latest" before a date that is before the oldest available datum, triggering a query for at most 1
		Datum result = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(),
				datum.getLast().getTimestamp().minusSeconds(1), 0);

		// try again, to validate the datum from query is cached in accessor
		Datum result2 = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(),
				datum.getLast().getTimestamp().minusSeconds(1), 0);

		// THEN
		then(datumDao).should(times(1)).findFiltered(criteriaCaptor.capture());

		// @formatter:off
		and.then(criteriaCaptor.getValue())
			.as("Query for user")
			.returns(userId, from(DatumCriteria::getUserId))
			.as("Query for stream node")
			.returns(nodeId, from(DatumCriteria::getNodeId))
			.as("Query for stream source")
			.returns(randStreamMeta.getSourceId(), from(DatumCriteria::getSourceId))
			.as("Query for at most one datum")
			.returns(1, from(DatumCriteria::getMax))
			.as("Query end date is given timestamp")
			.returns(datum.getLast().getTimestamp(), from(DatumCriteria::getEndDate))
			.as("Query start date is offset from end date by configured duration")
			.returns(datum.getLast().getTimestamp().minus(accessor.getMaxStartDateDuration()), DatumCriteria::getStartDate)
			;

		and.then(result)
			.as("Datum for node ID returned")
			.returns(nodeId, from(Datum::getObjectId))
			.as("Datum for source ID returned")
			.returns(randStreamMeta.getSourceId(), from(Datum::getSourceId))
			.as("Datum for timestamp offset from DAO returned")
			.returns(datumEntity.getTimestamp(), from(Datum::getTimestamp))
			.as("Returned ObjectDatum for DAO result")
			.isInstanceOf(ObjectDatum.class)
			.asInstanceOf(type(ObjectDatum.class))
			.as("Properties from DAO returned in ObjectDatum")
			.returns(datumEntity.getProperties(), from(ObjectDatum::getProperties))
			;
		and.then(result2)
			.as("Offset from DAO returned 2nd time")
			.isSameAs(result)
			;
		// @formatter:on
	}

	@Test
	public void offset_maxResultLimitEnforced() {
		// GIVEN
		final int sourceIdCount = 3;
		final var streamMetas = testStreamMetas(nodeId, sourceIdCount);
		final var datumFreq = Duration.ofMinutes(5);
		final var datum = testNodeDatum(nodeId, streamMetas, clock.instant(), datumFreq, 6);

		var randStreamMeta = streamMetas.get(RNG.nextInt(sourceIdCount));

		var accessor = new QueryingDatumStreamsAccessor(new AntPathMatcher(), datum, userId, clock,
				datumDao, null);
		accessor.setMaxResults(3);

		var datumEntity = new DatumEntity(randStreamMeta.getStreamId(),
				datum.getLast().getTimestamp().minus(datumFreq), null,
				propertiesOf(new BigDecimal[] { new BigDecimal(Integer.MAX_VALUE) }, null, null, null));
		var filterResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				streamMetas.stream().collect(toUnmodifiableMap(m -> m.getStreamId(), identity())),
				List.of(datumEntity));

		given(datumDao.findFiltered(any())).willReturn(filterResults);

		// WHEN
		// ask for 6 before first datum, which is 1 more than available so we should query for one more datum
		Datum result = accessor.offset(Node, nodeId, randStreamMeta.getSourceId(), 100);

		// THEN
		then(datumDao).should(times(1)).findFiltered(criteriaCaptor.capture());

		// @formatter:off
		and.then(criteriaCaptor.getValue())
			.as("Query for user")
			.returns(userId, from(DatumCriteria::getUserId))
			.as("Query for stream node")
			.returns(nodeId, from(DatumCriteria::getNodeId))
			.as("Query for stream source")
			.returns(randStreamMeta.getSourceId(), from(DatumCriteria::getSourceId))
			.as("Query for at most 3 datum, limited by maxResults")
			.returns(3, from(DatumCriteria::getMax))
			.as("Query end date is from last available")
			.returns(datum.getLast().getTimestamp(), from(DatumCriteria::getEndDate))
			.as("Query start date is offset from end date by configured duration")
			.returns(datum.getLast().getTimestamp().minus(accessor.getMaxStartDateDuration()), DatumCriteria::getStartDate)
			;

		and.then(result)
			.as("Datum not found")
			.isNull()
			;
		// @formatter:on
	}

}
