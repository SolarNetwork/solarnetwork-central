/* ==================================================================
 * StoreGeneralObjectDatumTests.java - 25/07/2025 10:15:09 am
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql.test;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.StoreGeneralObjectDatum;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * Test cases for the {@link StoreGeneralObjectDatum} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class StoreGeneralObjectDatumTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private CallableStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	@Captor
	private ArgumentCaptor<String> jsonCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareCall(any())).willReturn(stmt);
	}

	@Test
	public void prep() throws SQLException {
		// GIVEN
		final Instant now = now().truncatedTo(MILLIS);

		final GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setNodeId(randomLong());
		datum.setSourceId(randomString());
		datum.setCreated(now);

		DatumSamples samples = new DatumSamples();
		samples.setInstantaneous(Map.of("a", randomInt()));
		samples.setAccumulating(Map.of("b", randomInt()));
		samples.setStatus(Map.of("c", randomString()));
		samples.setTags(Set.of(randomString()));
		datum.setSamples(samples);

		givenPrepStatement();

		// WHEN
		var sql = new StoreGeneralObjectDatum(datum);
		CallableStatement result = sql.createCallableStatement(con);

		// THEN
		then(con).should().prepareCall(sqlCaptor.capture());

		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue()).as("SQL generated")
				.isEqualTo(StoreGeneralObjectDatum.STORE_NODE_DATUM_SQL);
		and.then(result).as("Connection statement returned").isSameAs(stmt);

		then(result).should().registerOutParameter(1, Types.OTHER);
		then(result).should().setTimestamp(2, Timestamp.from(datum.getCreated()));
		then(result).should().setObject(3, datum.getNodeId());
		then(result).should().setString(4, datum.getSourceId());
		then(result).should().setTimestamp(eq(5), any());

		// @formatter:off
		then(result).should().setString(eq( 6), jsonCaptor.capture());
		and.then(jsonCaptor.getValue())
			.as("JSON provided")
			.isNotNull()
			.as("JSON serialized")
			.isEqualToIgnoringWhitespace("""
					{
						"i": {
							"a": %d
						},
						"a": {
							"b": %d
						},
						"s": {
							"c": "%s"
						},
						"t": ["%s"]
					}
					""".formatted(
							  samples.getInstantaneousSampleInteger("a")
							, samples.getAccumulatingSampleInteger("b")
							, samples.getStatusSampleString("c")
							, samples.getTags().iterator().next()))
			;
		// @formatter:on

	}

	@Test
	public void prep_Infinity() throws SQLException {
		// GIVEN
		final Instant now = now().truncatedTo(MILLIS);

		final GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setNodeId(randomLong());
		datum.setSourceId(randomString());
		datum.setCreated(now);

		DatumSamples samples = new DatumSamples();
		samples.setInstantaneous(Map.of("a", Double.POSITIVE_INFINITY));
		samples.setInstantaneous(Map.of("b", 12345));
		samples.setAccumulating(Map.of("c", Double.POSITIVE_INFINITY));
		samples.setStatus(Map.of("d", Double.POSITIVE_INFINITY));
		samples.setStatus(Map.of("e", "eek"));
		datum.setSamples(samples);

		givenPrepStatement();

		// WHEN
		var sql = new StoreGeneralObjectDatum(datum);
		CallableStatement result = sql.createCallableStatement(con);

		// THEN
		then(con).should().prepareCall(sqlCaptor.capture());

		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue()).as("SQL generated")
				.isEqualTo(StoreGeneralObjectDatum.STORE_NODE_DATUM_SQL);
		and.then(result).as("Connection statement returned").isSameAs(stmt);

		then(result).should().registerOutParameter(1, Types.OTHER);
		then(result).should().setTimestamp(2, Timestamp.from(datum.getCreated()));
		then(result).should().setObject(3, datum.getNodeId());
		then(result).should().setString(4, datum.getSourceId());
		then(result).should().setTimestamp(eq(5), any());

		// @formatter:off
		then(result).should().setString(eq( 6), jsonCaptor.capture());
		and.then(jsonCaptor.getValue())
			.as("JSON provided")
			.isNotNull()
			.as("JSON serialized with Infinity removed")
			.isEqualToIgnoringWhitespace("""
					{
						"i": {
							"b": %d
						},
						"s": {
							"e": "%s"
						}
					}
					""".formatted(
							  samples.getInstantaneousSampleInteger("b")
							, samples.getStatusSampleString("e")))
			;
		// @formatter:on

	}

}
