/* ==================================================================
 * DbDatumRollup_Raw06Tests.java - 22/12/2022 11:43:59 am
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
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.datumResourceToList;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DbDatumRollupTests.rollup;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
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
 * Test cases for datum rollup on the sample-raw-data-06 data set.
 * 
 * @author matt
 * @version 1.0
 */
public class DbDatumRollup_Raw06Tests extends BaseDatumJdbcTestSupport {

	private ObjectDatumStreamMetadata load_raw06() {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);
		List<Datum> datum = datumResourceToList(getClass(), "sample-raw-data-06-perfect-minutes.csv",
				staticProvider(singleton(meta)));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
		log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));

		return meta;
	}

	@Test
	public void raw06_1109_01() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw06();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 1, 0, 0, 0, ZoneOffset.UTC);
		rollup(jdbcTemplate, meta.getStreamId(), start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> metas, UUID sid,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(meta.getStreamId()));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));

				assertThat("Sparse data:", result.getStatistics().getAccumulating(), arrayContaining(
						decimalArray("1", "45810", "45811"), decimalArray("2", "41007", "41009")));
			}
		});
	}

	@Test
	public void raw06_1109_02() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw06();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 2, 0, 0, 0, ZoneOffset.UTC);
		rollup(jdbcTemplate, meta.getStreamId(), start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> metas, UUID sid,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(meta.getStreamId()));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));

				assertThat("Before mising data:", result.getStatistics().getAccumulating(),
						arrayContaining(decimalArray("2", "45811", "45813"),
								decimalArray("3", "41009", "41012")));
			}
		});
	}

	@Test
	public void raw06_1109_03() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw06();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 3, 0, 0, 0, ZoneOffset.UTC);
		rollup(jdbcTemplate, meta.getStreamId(), start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> metas, UUID sid,
					List<AggregateDatum> results) {
				assertThat("No data in range", results, hasSize(0));
			}
		});
	}

	@Test
	public void raw06_1109_18() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw06();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 18, 0, 0, 0, ZoneOffset.UTC);
		rollup(jdbcTemplate, meta.getStreamId(), start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> metas, UUID sid,
					List<AggregateDatum> results) {
				assertThat("No data in range", results, hasSize(0));
			}
		});
	}

	@Test
	public void raw06_1109_19() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw06();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 19, 0, 0, 0, ZoneOffset.UTC);
		rollup(jdbcTemplate, meta.getStreamId(), start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> metas, UUID sid,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(meta.getStreamId()));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));

				assertThat("Pick up accumulation in >hour gap:",
						result.getStatistics().getAccumulating(),
						arrayContaining(decimalArray("124", "45813", "45937"),
								decimalArray("130", "41012", "41142")));
			}
		});
	}

	@Test
	public void raw06_1109_20() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw06();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 20, 0, 0, 0, ZoneOffset.UTC);
		rollup(jdbcTemplate, meta.getStreamId(), start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> metas, UUID sid,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(meta.getStreamId()));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));

				assertThat("Hour with perfect start:", result.getStatistics().getAccumulating(),
						arrayContaining(decimalArray("1", "45937", "45938"),
								decimalArray("15", "41142", "41157")));
			}
		});
	}

}
