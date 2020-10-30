/* ==================================================================
 * DbDatumRollupTests.java - 30/10/2020 3:12:24 pm
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

package net.solarnetwork.central.datum.v2.dao.mybatis.test;

import static java.lang.String.format;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.util.JsonUtils;

/**
 * Tests for the database rollup stored procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class DbDatumRollupTests extends BaseDatumJdbcTestSupport {

	private static final Pattern COMMENT = Pattern.compile("\\s*#");

	private final List<GeneralNodeDatum> loadDatumResource(String resource) throws IOException {
		List<GeneralNodeDatum> result = new ArrayList<>();
		int row = 0;
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
				getClass().getResourceAsStream(resource), Charset.forName("UTF-8")))) {
			while ( true ) {
				String line = r.readLine();
				if ( line == null ) {
					break;
				}
				row++;
				if ( line.isEmpty() || COMMENT.matcher(line).find() ) {
					// skip empty/comment line
					continue;
				}
				GeneralNodeDatum d = JsonUtils.getObjectFromJSON(line, GeneralNodeDatum.class);
				assertThat(format("Parsed JSON datum in row %d", row), d, notNullValue());
				result.add(d);
			}
		}
		return result;
	}

	private static Matcher<BigDecimal[]> arrayOfDecimals(String... nums) {
		BigDecimal[] vals = new BigDecimal[nums.length];
		for ( int i = 0; i < nums.length; i++ ) {
			vals[i] = new BigDecimal(nums[i]);
		}
		return Matchers.arrayContaining(vals);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void rollup_regularHour() throws IOException {
		List<GeneralNodeDatum> datums = loadDatumResource("test-datum-01.txt");
		log.debug("Got test data: {}", datums);
		Map<NodeSourcePK, NodeDatumStreamMetadata> meta = DatumTestUtils.insertDatum(log, jdbcTemplate,
				datums);
		UUID streamId = meta.values().iterator().next().getStreamId();
		Instant aggStartDate = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant();
		List<AggregateDatumEntity> results = jdbcTemplate.query(
				"select * from solardatm.rollup_datm_for_time_span(?::uuid,?,?)",
				AggregateDatumEntityRowMapper.INSTANCE, streamId.toString(),
				Timestamp.from(aggStartDate), Timestamp.from(aggStartDate.plus(1, ChronoUnit.HOURS)));
		assertThat("Agg result returned", results, hasSize(1));

		AggregateDatumEntity result = results.get(0);
		log.debug("Got result: {}", result);
		assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
		assertThat("Agg timestamp", result.getTimestamp(), equalTo(aggStartDate));
		assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
				arrayOfDecimals("1.45", "4.6"));
		assertThat("Agg accumulating", result.getProperties().getAccumulating(), arrayOfDecimals("25"));
		assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
				arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
						arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
		assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
				arrayContaining(arrayOfDecimals(new String[] { "20", "100", "120" })));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void rollup_imperfectHour() throws IOException {
		List<GeneralNodeDatum> datums = loadDatumResource("test-datum-02.txt");
		log.debug("Got test data: {}", datums);
		Map<NodeSourcePK, NodeDatumStreamMetadata> meta = DatumTestUtils.insertDatum(log, jdbcTemplate,
				datums);
		UUID streamId = meta.values().iterator().next().getStreamId();
		Instant aggStartDate = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant();
		List<AggregateDatumEntity> results = jdbcTemplate.query(
				"select * from solardatm.rollup_datm_for_time_span(?::uuid,?,?)",
				AggregateDatumEntityRowMapper.INSTANCE, streamId.toString(),
				Timestamp.from(aggStartDate), Timestamp.from(aggStartDate.plus(1, ChronoUnit.HOURS)));
		assertThat("Agg result returned", results, hasSize(1));

		AggregateDatumEntity result = results.get(0);
		log.debug("Got result: {}", result);
		assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
		assertThat("Agg timestamp", result.getTimestamp(), equalTo(aggStartDate));
		assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
				arrayOfDecimals("1.45", "4.6"));
		assertThat("Agg accumulating", result.getProperties().getAccumulating(), arrayOfDecimals("30"));
		assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
				arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
						arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
		assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
				arrayContaining(arrayOfDecimals(new String[] { "30", "100", "130" })));
	}

}
