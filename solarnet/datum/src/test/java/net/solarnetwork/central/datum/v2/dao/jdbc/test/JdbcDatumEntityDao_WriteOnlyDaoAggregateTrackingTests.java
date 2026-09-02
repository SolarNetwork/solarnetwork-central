/* ==================================================================
 * JdbcDatumEntityDao_WriteOnlyDaoAggregateTrackingTests.java - 22/04/2026 4:03:57 pm
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

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumId.nodeId;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;

/**
 * Test write-only DAO concurrent transactions.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_WriteOnlyDaoAggregateTrackingTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	private Long locId;
	private Long userId;
	private Long nodeId;
	private Instant start;

	@BeforeEach
	public void setup() {
		locId = CommonDbTestUtils.insertLocation(jdbcTemplate, TEST_LOC_COUNTRY, TEST_TZ);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate, randomString() + "@localhost");
		nodeId = CommonDbTestUtils.insertNode(jdbcTemplate, locId);
		CommonDbTestUtils.insertUserNode(jdbcTemplate, userId, nodeId);

		dao = new JdbcDatumEntityDao(jdbcTemplate);

		start = Instant.now().truncatedTo(ChronoUnit.HOURS);
	}

	private GeneralDatum datum(String sourceId, Instant ts) {
		final DatumSamples samples = new DatumSamples();
		samples.putInstantaneousSampleValue("a", CommonTestUtils.randomInt());
		samples.putAccumulatingSampleValue("m", ChronoUnit.SECONDS.between(start, ts));
		return new GeneralDatum(nodeId(nodeId, sourceId, ts), samples);
	}

	private void thenStaleHoursCreated(String prefix, UUID streamId, Instant... hours) {
		final List<StaleAggregateDatum> stale = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		log.debug("Stale aggs: [{}]",
				stale.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		// @formatter:off
		then(stale)
			.as("%s has stale hours created", prefix)
			.hasSize(hours.length)
			;
		// @formatter:on
	}

	/**
	 * Test the start of a completely new stream.
	 *
	 * @param minOffset
	 *        the minutes offset from {@code start}
	 */
	@ParameterizedTest
	@ValueSource(ints = { 0, 30 })
	public void newStream(int minOffset) {
		// GIVEN
		final Instant ts = start.plus(minOffset, ChronoUnit.MINUTES);
		final GeneralDatum d = datum(randomSourceId(), ts);

		// WHEN
		final DatumPK pk = dao.store(d);

		// THEN
		thenStaleHoursCreated("New stream", pk.getStreamId(), start);
	}

	/**
	 * Test continuing an adjacent hour of an existing stream.
	 *
	 * @param minOffset
	 *        the minutes offset from {@code start} plus 1 hour
	 */
	@ParameterizedTest
	@ValueSource(ints = { 0, 30 })
	public void continueStream(int minOffset) {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum startDatum = datum(sourceId, start);
		final DatumPK startDatumPk = dao.store(startDatum);

		// WHEN
		final Instant ts = start.plus(1, HOURS).plus(minOffset, MINUTES);
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		then(pk.getStreamId())
			.as("Existing stream used")
			.isEqualTo(startDatumPk.getStreamId())
			;

		thenStaleHoursCreated("Continued stream", startDatumPk.getStreamId(),
			start,
			start.plus(1, HOURS)
		);
		// @formatter:on
	}

	/**
	 * Test continuing an hour after a gap in an existing stream and new datum
	 * is exactly on the hour.
	 */
	@Test
	public void continueStream_afterGap_exactHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum startDatum = datum(sourceId, start);
		final DatumPK startDatumPk = dao.store(startDatum);

		// WHEN
		final Instant ts = start.plus(24, HOURS);
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		then(pk.getStreamId())
			.as("Existing stream used")
			.isEqualTo(startDatumPk.getStreamId())
			;

		thenStaleHoursCreated("Continued stream after gap", startDatumPk.getStreamId(),
			start,
			start.plus(23, HOURS), // because 2nd datum exactly on hour
			start.plus(24, HOURS)
		);
		// @formatter:on
	}

	/**
	 * Test continuing an hour after a gap in an existing stream and the new
	 * datum is not exactly on the hour.
	 */
	@Test
	public void continueStream_afterGap_notExactHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum startDatum = datum(sourceId, start);
		final DatumPK startDatumPk = dao.store(startDatum);

		// WHEN
		final Instant ts = start.plus(24, HOURS).plus(30, MINUTES); // not exact hour
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		then(pk.getStreamId())
			.as("Existing stream used")
			.isEqualTo(startDatumPk.getStreamId())
			;

		thenStaleHoursCreated("Continued stream after gap", startDatumPk.getStreamId(),
			start,
			start.plus(24, HOURS)
		);
		// @formatter:on
	}

	/**
	 * Test continuing an hour after a gap in an existing stream where the
	 * previous datum is not exactly on the hour and new datum is exactly on the
	 * hour.
	 */
	@Test
	public void continueStream_afterGap_prevNotExactHour_exactHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum startDatum = datum(sourceId, start.plus(30, MINUTES));
		final DatumPK startDatumPk = dao.store(startDatum);

		// WHEN
		final Instant ts = start.plus(24, HOURS);
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		then(pk.getStreamId())
			.as("Existing stream used")
			.isEqualTo(startDatumPk.getStreamId())
			;

		thenStaleHoursCreated("Continued stream after gap", startDatumPk.getStreamId(),
			start,
			start.plus(23, HOURS), // because 2nd datum exactly on hour
			start.plus(24, HOURS)
		);
		// @formatter:on
	}

	/**
	 * Test continuing an hour after a gap in an existing stream where the
	 * previous datum is not exactly on the hour and new datum is not exactly on
	 * the hour.
	 */
	@Test
	public void continueStream_afterGap_prevNotExactHour_notExactHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum startDatum = datum(sourceId, start.plus(30, MINUTES));
		final DatumPK startDatumPk = dao.store(startDatum);

		// WHEN
		final Instant ts = start.plus(24, HOURS).plus(30, MINUTES);
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		then(pk.getStreamId())
			.as("Existing stream used")
			.isEqualTo(startDatumPk.getStreamId())
			;

		thenStaleHoursCreated("Continued stream after gap", startDatumPk.getStreamId(),
			start,
			start.plus(24, HOURS)
		);
		// @formatter:on
	}

	private void deleteStaleDatumRecords() {
		jdbcTemplate.update("delete from solardatm.agg_stale_datm");
	}

	/**
	 * Test continuing an adjacent hour of an existing stream, where the
	 * existing stream is not stale.
	 *
	 * @param minOffset
	 *        the minutes offset from {@code start} plus 1 hour
	 */
	@ParameterizedTest
	@ValueSource(ints = { 0, 30 })
	public void continueNonStaleStream(int minOffset) {
		// GIVEN
		// setup stream with existing hour
		final String sourceId = randomSourceId();
		final GeneralDatum startDatum = datum(sourceId, start);
		final DatumPK startDatumPk = dao.store(startDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start.plus(1, HOURS).plus(minOffset, MINUTES);
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		then(pk.getStreamId())
			.as("Existing stream used")
			.isEqualTo(startDatumPk.getStreamId())
			;

		thenStaleHoursCreated("Continued non-stale stream", startDatumPk.getStreamId(),
			start,
			start.plus(1, HOURS)
		);
		// @formatter:on
	}

	/**
	 * Test continuing an hour after a gap in an existing stream and new datum
	 * is exactly on the hour, where the existing stream is not stale.
	 */
	@Test
	public void continueNonStaleStream_afterGap_exactHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum startDatum = datum(sourceId, start);
		final DatumPK startDatumPk = dao.store(startDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start.plus(24, HOURS);
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		then(pk.getStreamId())
			.as("Existing stream used")
			.isEqualTo(startDatumPk.getStreamId())
			;

		thenStaleHoursCreated("Continued non-stale stream after gap", startDatumPk.getStreamId(),
			start,
			start.plus(23, HOURS), // because 2nd datum exactly on hour
			start.plus(24, HOURS)
		);
		// @formatter:on
	}

	/**
	 * Test continuing an hour after a gap in an existing stream and the new
	 * datum is not exactly on the hour, where the existing stream is not stale.
	 */
	@Test
	public void continueNonStaleStream_afterGap_notExactHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum startDatum = datum(sourceId, start);
		final DatumPK startDatumPk = dao.store(startDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start.plus(24, HOURS).plus(30, MINUTES); // not exact hour
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		then(pk.getStreamId())
			.as("Existing stream used")
			.isEqualTo(startDatumPk.getStreamId())
			;

		thenStaleHoursCreated("Continued stream after gap", startDatumPk.getStreamId(),
			start,
			start.plus(24, HOURS)
		);
		// @formatter:on
	}

	/**
	 * Test continuing an hour after a gap in an existing stream where the
	 * previous datum is not exactly on the hour and new datum is exactly on the
	 * hour, where the existing stream is not stale.
	 */
	@Test
	public void continueNonStaleStream_afterGap_prevNotExactHour_exactHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum startDatum = datum(sourceId, start.plus(30, MINUTES));
		final DatumPK startDatumPk = dao.store(startDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start.plus(24, HOURS);
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		then(pk.getStreamId())
			.as("Existing stream used")
			.isEqualTo(startDatumPk.getStreamId())
			;

		thenStaleHoursCreated("Continued stream after gap", startDatumPk.getStreamId(),
			start,
			start.plus(23, HOURS), // because 2nd datum exactly on hour
			start.plus(24, HOURS)
		);
		// @formatter:on
	}

	/**
	 * Test continuing an hour after a gap in an existing stream where the
	 * previous datum is not exactly on the hour and new datum is not exactly on
	 * the hour, where the existing stream is not stale.
	 */
	@Test
	public void continueNonStaleStream_afterGap_prevNotExactHour_notExactHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum startDatum = datum(sourceId, start.plus(30, MINUTES));
		final DatumPK startDatumPk = dao.store(startDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start.plus(24, HOURS).plus(30, MINUTES);
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		then(pk.getStreamId())
			.as("Existing stream used")
			.isEqualTo(startDatumPk.getStreamId())
			;

		thenStaleHoursCreated("Continued stream after gap", startDatumPk.getStreamId(),
			start,
			start.plus(24, HOURS)
		);
		// @formatter:on
	}

	@Test
	public void updateStream_withinHour_datumBeforeAfterWithinHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum leftDatum = datum(sourceId, start.plus(1, MINUTES));
		dao.store(leftDatum);
		final GeneralDatum rightDatum = datum(sourceId, start.plus(59, MINUTES));
		dao.store(rightDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start.plus(30, MINUTES); // between left/right
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		thenStaleHoursCreated("Updated stream within single hour impacts datum hour only", pk.getStreamId(),
			start
		);
		// @formatter:on
	}

	@Test
	public void updateStream_exactHour_previousDatumWithinPreviousHour_nextDatumWithinHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum leftDatum = datum(sourceId, start.minus(30, MINUTES));
		dao.store(leftDatum);
		final GeneralDatum rightDatum = datum(sourceId, start.plus(30, MINUTES));
		dao.store(rightDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start;
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		thenStaleHoursCreated("Updated stream exactly on hour impacts previous hour", pk.getStreamId(),
			start.minus(1, HOURS),
			start
		);
		// @formatter:on
	}

	@Test
	public void updateStream_exactHour_previousDatumGapWithinHour_nextDatumWithinHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum leftDatum = datum(sourceId, start.minus(24, HOURS).minus(30, MINUTES));
		dao.store(leftDatum);
		final GeneralDatum rightDatum = datum(sourceId, start.plus(30, MINUTES));
		dao.store(rightDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start;
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		thenStaleHoursCreated("Updated stream exactly on hour impacts previous hour and previous datum's hour", pk.getStreamId(),
			leftDatum.getTimestamp().truncatedTo(HOURS),
			start.minus(1, HOURS),
			start
		);
		// @formatter:on
	}

	@Test
	public void updateStream_exactHour_previousDatumGapOnHour_nextDatumWithinHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum leftDatum = datum(sourceId, start.minus(24, HOURS));
		dao.store(leftDatum);
		final GeneralDatum rightDatum = datum(sourceId, start.plus(30, MINUTES));
		dao.store(rightDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start;
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		thenStaleHoursCreated("Updated stream exactly on hour impacts previous hour and previous datum's hour", pk.getStreamId(),
			leftDatum.getTimestamp().truncatedTo(HOURS),
			start.minus(1, HOURS),
			start
		);
		// @formatter:on
	}

	@Test
	public void updateStream_exactHour_previousDatumGapOnHour_nextDatumOnHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum leftDatum = datum(sourceId, start.minus(24, HOURS));
		dao.store(leftDatum);
		final GeneralDatum rightDatum = datum(sourceId, start.plus(1, HOURS));
		dao.store(rightDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start;
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		thenStaleHoursCreated("Updated stream exactly on hour impacts previous hour and previous datum's hour", pk.getStreamId(),
			leftDatum.getTimestamp().truncatedTo(HOURS),
			start.minus(1, HOURS),
			start
		);
		// @formatter:on
	}

	@Test
	public void updateStream_exactHour_previousDatumGapOnHour_nextDatumGapWithinHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum leftDatum = datum(sourceId, start.minus(24, HOURS));
		dao.store(leftDatum);
		final GeneralDatum rightDatum = datum(sourceId, start.plus(24, HOURS).plus(30, MINUTES));
		dao.store(rightDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start;
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		thenStaleHoursCreated("Updated stream exactly on hour impacts previous hour and previous and next datum's hours", pk.getStreamId(),
			leftDatum.getTimestamp().truncatedTo(HOURS),
			start.minus(1, HOURS),
			start,
			rightDatum.getTimestamp().truncatedTo(HOURS)
		);
		// @formatter:on
	}

	@Test
	public void updateStream_exactHour_previousDatumGapOnHour_nextDatumGapOnHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum leftDatum = datum(sourceId, start.minus(24, HOURS));
		dao.store(leftDatum);
		final GeneralDatum rightDatum = datum(sourceId, start.plus(24, HOURS));
		dao.store(rightDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start;
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		thenStaleHoursCreated("Updated stream exactly on hour impacts previous hour and previous datum's hour and next datum's hour - 1", pk.getStreamId(),
			leftDatum.getTimestamp().truncatedTo(HOURS),
			start.minus(1, HOURS),
			start,
			rightDatum.getTimestamp().truncatedTo(HOURS).minus(1, HOURS)
		);
		// @formatter:on
	}

	@Test
	public void updateStream_exactHour_previousDatumWithinPreviousHour_nextDatumGapWithinHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum leftDatum = datum(sourceId, start.minus(30, MINUTES));
		dao.store(leftDatum);
		final GeneralDatum rightDatum = datum(sourceId, start.plus(24, HOURS).plus(30, MINUTES));
		dao.store(rightDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start;
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		thenStaleHoursCreated("Updated stream exactly on hour impacts previous hour and next datum's hour", pk.getStreamId(),
			start.minus(1, HOURS),
			start,
			rightDatum.getTimestamp().truncatedTo(HOURS)
		);
		// @formatter:on
	}

	@Test
	public void updateStream_exactHour_previousDatumWithinPreviousHour_nextDatumGapOnHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum leftDatum = datum(sourceId, start.minus(30, MINUTES));
		dao.store(leftDatum);
		final GeneralDatum rightDatum = datum(sourceId, start.plus(24, HOURS));
		dao.store(rightDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start;
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		thenStaleHoursCreated("Updated stream exactly on hour impacts previous hour and next datum's hour - 1", pk.getStreamId(),
			start.minus(1, HOURS),
			start,
			rightDatum.getTimestamp().truncatedTo(HOURS).minus(1, HOURS)
		);
		// @formatter:on
	}

	@Test
	public void updateStream_withinHour_previousDatumWithinHour_nextDatumGapWithinHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum leftDatum = datum(sourceId, start);
		dao.store(leftDatum);
		final GeneralDatum rightDatum = datum(sourceId, start.plus(24, HOURS).plus(30, MINUTES));
		dao.store(rightDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start.plus(1, MINUTES);
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		thenStaleHoursCreated("Updated stream within hour impacts hour and next datum's hour", pk.getStreamId(),
			start,
			rightDatum.getTimestamp().truncatedTo(HOURS)
		);
		// @formatter:on
	}

	@Test
	public void updateStream_withinHour_previousDatumWithinHour_nextDatumGapOnHour() {
		// GIVEN
		// setup stream with existing stale hour
		final String sourceId = randomSourceId();
		final GeneralDatum leftDatum = datum(sourceId, start);
		dao.store(leftDatum);
		final GeneralDatum rightDatum = datum(sourceId, start.plus(24, HOURS));
		dao.store(rightDatum);

		deleteStaleDatumRecords();

		// WHEN
		final Instant ts = start.plus(1, MINUTES);
		final GeneralDatum d = datum(sourceId, ts);
		final DatumPK pk = dao.store(d);

		// THEN
		// @formatter:off
		thenStaleHoursCreated("Updated stream within hour impacts hour and next datum's hour - 1", pk.getStreamId(),
			start,
			rightDatum.getTimestamp().truncatedTo(HOURS).minus(1, HOURS)
		);
		// @formatter:on
	}

}
