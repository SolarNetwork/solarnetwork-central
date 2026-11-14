/* ==================================================================
 * DbDatumRollup_Net469Tests.java - 2/08/2025 7:18:06 am
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.datumResourceToList;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DbDatumRollupTests.rollup;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.test.DbDatumRollupTests.RollupCallback;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for datum rollup validating NET-537.
 *
 * @author matt
 * @version 1.0
 */
public class DbDatumRollup_Net537Tests extends BaseDatumJdbcTestSupport {

	private ObjectDatumStreamMetadata load(String resource) {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", new String[] { "i" }, new String[] { "val" }, null);
		List<Datum> datum = datumResourceToList(getClass(), resource, staticProvider(singleton(meta)));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
		log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));

		return meta;
	}

	private List<AggregateDatum> rollupHour(ObjectDatumStreamMetadata meta, ZonedDateTime hour)
			throws IOException {
		final List<AggregateDatum> hourResults = new ArrayList<>();
		rollup(jdbcTemplate, meta.getStreamId(), hour, hour.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> metas, UUID sid,
					List<AggregateDatum> results) {
				hourResults.addAll(results);
			}
		});
		return hourResults;
	}

	@Test
	public void nextDatumWithoutAccumulating_nextReadingWithinClockTolerance() throws IOException {
		// GIVEN
		// the first datum after the hour has no accumulating value; the one after that does,
		// and is within the 1 hour clock tolerance of the last reading in the hour
		ObjectDatumStreamMetadata meta = load("sample-raw-data-10-net537.csv");

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2023, 2, 6, 18, 0, 0, 0, ZoneOffset.UTC);
		final List<AggregateDatum> hourResults = rollupHour(meta, start);

		// THEN
		// @formatter:off
		then(hourResults)
			.as("One aggregate hour generated")
			.hasSize(1)
			.element(0)
			.as("Stream ID matches")
			.returns(meta.getStreamId(), AggregateDatum::getStreamId)
			.as("Timestamp is hour")
			.returns(start.toInstant(), AggregateDatum::getTimestamp)
			.satisfies(h -> {
				then(h.getProperties().getInstantaneous())
					.as("Instantaneous is average (2,3)")
					.containsExactly(decimalArray("2.5"))
					;
				then(h.getStatistics().getInstantaneous())
					.as("Instantaneous stats are count, min, max")
					.isDeepEqualTo(new BigDecimal[][] { decimalArray("2", "2", "3") })
					;
				then(h.getProperties().getAccumulating())
					.as("""
						Accumulating is 15/20 of difference crossing start (0.75), difference \
						within hour (1), and 15/20 of difference crossing end to next reading \
						with an accumulating value (1.5)\
						""")
					.containsExactly(decimalArray("3.25"))
					;
				then(h.getStatistics().getAccumulating())
					.as("Accumulating reading is difference, start, end")
					.isDeepEqualTo(new BigDecimal[][] { decimalArray("2", "100", "102") })
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void nextDatumWithoutAccumulating_nextReadingBeyondClockTolerance() throws IOException {
		// GIVEN
		// the first datum after the hour has no accumulating value; the one after that does,
		// but is more than the 1 hour clock tolerance after the last reading in the hour
		ObjectDatumStreamMetadata meta = load("sample-raw-data-11-net537.csv");

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2023, 2, 6, 18, 0, 0, 0, ZoneOffset.UTC);
		final List<AggregateDatum> hourResults = rollupHour(meta, start);

		// THEN
		// @formatter:off
		then(hourResults)
			.as("One aggregate hour generated")
			.hasSize(1)
			.element(0)
			.satisfies(h -> {
				then(h.getProperties().getAccumulating())
					.as("""
						Accumulating is 15/20 of difference crossing start (0.75) and difference \
						within hour (1); nothing crossing end as next reading out of tolerance\
						""")
					.containsExactly(decimalArray("1.75"))
					;
				then(h.getStatistics().getAccumulating())
					.as("Accumulating reading is difference, start, end")
					.isDeepEqualTo(new BigDecimal[][] { decimalArray("2", "100", "102") })
					;
			})
			;
		// @formatter:on
	}

}
