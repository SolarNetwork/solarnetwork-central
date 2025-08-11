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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.test.DbDatumRollupTests.RollupCallback;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for datum rollup validating NET-470.
 *
 * @author matt
 * @version 1.0
 */
public class DbDatumRollup_Net470Tests extends BaseDatumJdbcTestSupport {

	private ObjectDatumStreamMetadata load_net469() {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", new String[] { "i" }, new String[] { "val" }, null);
		List<Datum> datum = datumResourceToList(getClass(), "sample-raw-data-09-net470.csv",
				staticProvider(singleton(meta)));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
		log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));

		return meta;
	}

	@Test
	public void net469_rawGapPerfectHourStart() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_net469();

		// WHEN
		final List<AggregateDatum> hourResults = new ArrayList<>();
		final ZonedDateTime start = ZonedDateTime.of(2023, 2, 6, 18, 0, 0, 0, ZoneOffset.UTC);
		final ZonedDateTime end = start.plusHours(1);
		for ( ZonedDateTime hour = start; hour.isBefore(end); hour = hour.plusHours(1) ) {
			final ZonedDateTime curr = hour;
			rollup(log, jdbcTemplate, meta.getStreamId(), hour, hour.plusHours(1), new RollupCallback() {

				@Override
				public void doWithStream(List<GeneralNodeDatum> datums,
						Map<NodeSourcePK, ObjectDatumStreamMetadata> metas, UUID sid,
						List<AggregateDatum> results) {
					if ( results.isEmpty() ) {
						return;
					}
					AggregateDatum result = results.get(0);
					log.debug("Got result: {}", result);
					assertThat("Stream ID matches", result.getStreamId(), equalTo(meta.getStreamId()));
					assertThat("Agg timestamp is hour", result.getTimestamp(),
							equalTo(curr.toInstant()));

					hourResults.add(results.get(0));
				}
			});
		}

		// THEN
		assertThat("1 aggregate hours generated: 18", hourResults, hasSize(1));

		AggregateDatum h1 = hourResults.get(0);
		assertThat("Agg 1 is for hour 18", h1.getTimestamp(), is(equalTo(start.toInstant())));
		assertThat("Agg 1 instantaneous is average (2,3)", h1.getProperties().getInstantaneous(),
				arrayContaining(decimalArray("2.5")));
		assertThat("Agg 1 instantaneous stats", h1.getStatistics().getInstantaneous(),
				arrayContaining(new BigDecimal[][] { decimalArray("2", "2", "3") }));
		assertThat("Agg 1 accumulating is diff", h1.getProperties().getAccumulating(),
				arrayContaining(decimalArray("1.9375")));
		assertThat("Agg 1 accumulating reading", h1.getStatistics().getAccumulating(),
				arrayContaining(new BigDecimal[][] { decimalArray("2", "100", "102") }));
	}

}
