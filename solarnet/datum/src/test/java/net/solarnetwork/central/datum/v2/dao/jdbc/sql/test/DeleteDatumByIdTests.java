/* ==================================================================
 * DeleteDatumByIdTests.java - 12/02/2025 11:11:57 am
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
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.DeleteDatumById;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Test cases for the {@link DeleteDatumById} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DeleteDatumByIdTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any())).willReturn(stmt);
	}

	@Test
	public void prep_streamIds() throws SQLException {
		// GIVEN
		final Long userId = randomLong();

		final List<ObjectDatumId> ids = List.of(
				ObjectDatumId.nodeId(randomUUID(), null, null, now(), Aggregation.None),
				ObjectDatumId.nodeId(randomUUID(), null, null, now(), Aggregation.None),
				ObjectDatumId.nodeId(randomUUID(), null, null, now(), Aggregation.None));

		givenPrepStatement();

		// WHEN
		var sql = new DeleteDatumById(userId, ids, true);
		PreparedStatement result = sql.createPreparedStatement(con);
		for ( int i = 0; i < ids.size(); i++ ) {
			sql.setValues(result, i);
			result.execute();
		}

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());

		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue()).as("SQL generated").isEqualTo(DeleteDatumById.SQL);
		and.then(result).as("Connection statement returned").isSameAs(stmt);

		then(result).should().setObject(1, userId);
		then(result).should().setBoolean(6, true);
		then(result).should(times(ids.size())).setNull(4, Types.BIGINT);
		then(result).should(times(ids.size())).setNull(5, Types.VARCHAR);
		then(result).should(times(ids.size())).execute();

		for ( int i = 0; i < ids.size(); i++ ) {
			ObjectDatumId id = ids.get(i);
			thenSetBatchParametersStream(result, id);
		}
	}

	private void thenSetBatchParametersStream(PreparedStatement result, ObjectDatumId id)
			throws SQLException {
		then(result).should().setTimestamp(2, Timestamp.from(id.getTimestamp()));
		then(result).should().setString(3, id.getStreamId().toString());
	}

	private void thenSetBatchParametersNodeSource(PreparedStatement result, ObjectDatumId id)
			throws SQLException {
		then(result).should().setTimestamp(2, Timestamp.from(id.getTimestamp()));
		then(result).should().setObject(4, id.getObjectId());
		then(result).should().setString(5, id.getSourceId());
	}

	@Test
	public void prep_nodeSourceIds() throws SQLException {
		// GIVEN
		final Long userId = randomLong();

		final List<ObjectDatumId> ids = List.of(
				ObjectDatumId.nodeId(null, randomLong(), randomString(), now(), Aggregation.None),
				ObjectDatumId.nodeId(null, randomLong(), randomString(), now(), Aggregation.None),
				ObjectDatumId.nodeId(null, randomLong(), randomString(), now(), Aggregation.None));

		givenPrepStatement();

		// WHEN
		var sql = new DeleteDatumById(userId, ids, true);
		PreparedStatement result = sql.createPreparedStatement(con);
		for ( int i = 0; i < ids.size(); i++ ) {
			sql.setValues(result, i);
			result.execute();
		}

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());

		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue()).as("SQL generated").isEqualTo(DeleteDatumById.SQL);
		and.then(result).as("Connection statement returned").isSameAs(stmt);

		then(result).should().setObject(1, userId);
		then(result).should().setBoolean(6, true);
		then(result).should(times(ids.size())).setNull(3, Types.VARCHAR);
		then(result).should(times(ids.size())).execute();

		for ( int i = 0; i < ids.size(); i++ ) {
			ObjectDatumId id = ids.get(i);
			thenSetBatchParametersNodeSource(result, id);
		}
	}

}
