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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
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

	@Test
	public void fo() throws IOException {
		List<GeneralNodeDatum> datums = loadDatumResource("test-datum-01.txt");
		log.debug("Got test data: {}", datums);
		Map<NodeSourcePK, NodeDatumStreamMetadata> meta = DatumTestUtils.insertDatum(log, jdbcTemplate,
				datums);
		UUID streamId = meta.values().iterator().next().getStreamId();
		List<Map<String, Object>> results = jdbcTemplate.queryForList(
				"select * from solardatm.rollup_datm_for_time_span(?::uuid,?,?)", streamId.toString(),
				Timestamp.from(ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant()),
				Timestamp.from(ZonedDateTime.of(2020, 6, 1, 13, 0, 0, 0, ZoneOffset.UTC).toInstant()));
		log.debug("Got rollup: {}", results);
	}

}
