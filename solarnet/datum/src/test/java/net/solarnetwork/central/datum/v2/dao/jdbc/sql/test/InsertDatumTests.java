/* ==================================================================
 * InsertDatumTests.java - 19/11/2020 6:11:01 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql.test;

import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static net.solarnetwork.central.datum.v2.domain.DatumProperties.propertiesOf;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.InsertDatum;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;

/**
 * Test cases for the {@link InsertDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class InsertDatumTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static DatumProperties testProps() {
		return propertiesOf(decimalArray("1.1", "1.2", "1.3", "1.4"),
				decimalArray("2.1", "2.2", "2.3", "2.4"), new String[] { "a", "b" },
				new String[] { "c" });
	}

	@Test
	public void sql_insert() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		DatumEntity d = new DatumEntity(UUID.randomUUID(), start.toInstant(), Instant.now(),
				testProps());

		// WHEN
		String sql = new InsertDatum(d).getSql();

		// THEN
		assertThat("SQL matches", sql, equalToTextResource("insert-datum.sql", TestSqlResources.class));
	}

	@Test
	public void sql_insert_noTimestamp() {
		// GIVEN
		DatumEntity d = new DatumEntity(UUID.randomUUID(), null, Instant.now(), testProps());

		// WHEN
		String sql = new InsertDatum(d).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("insert-datum-noTimestamp.sql", TestSqlResources.class));
	}

	@Test
	public void sql_insert_noReceived() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		DatumEntity d = new DatumEntity(UUID.randomUUID(), start.toInstant(), null, testProps());

		// WHEN
		String sql = new InsertDatum(d).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("insert-datum-noReceived.sql", TestSqlResources.class));
	}

	@Test
	public void sql_insert_noTimestampOrReceived() {
		// GIVEN
		DatumEntity d = new DatumEntity(UUID.randomUUID(), null, null, testProps());

		// WHEN
		String sql = new InsertDatum(d).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("insert-datum-noTimestampOrReceived.sql", TestSqlResources.class));
	}

	@Test
	public void prep_insert() throws SQLException {
		// GIVEN
		ZonedDateTime ts = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		DatumEntity d = new DatumEntity(UUID.randomUUID(), ts.toInstant(), Instant.now(), testProps());

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		// stream_id, ts, received, data_i, data_a, data_s, data_t

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor))).andReturn(stmt);

		stmt.setObject(1, d.getStreamId(), Types.OTHER);

		Capture<Timestamp> tsCaptor = new Capture<>();
		stmt.setTimestamp(eq(2), capture(tsCaptor));

		Capture<Timestamp> recvCaptor = new Capture<>();
		stmt.setTimestamp(eq(3), capture(recvCaptor));

		Array iArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("NUMERIC"), aryEq(d.getProperties().getInstantaneous())))
				.andReturn(iArray);
		stmt.setArray(4, iArray);
		iArray.free();

		Array aArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("NUMERIC"), aryEq(d.getProperties().getAccumulating())))
				.andReturn(aArray);
		stmt.setArray(5, aArray);
		aArray.free();

		Array sArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("TEXT"), aryEq(d.getProperties().getStatus()))).andReturn(sArray);
		stmt.setArray(6, sArray);
		sArray.free();

		Array tArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("TEXT"), aryEq(d.getProperties().getTags()))).andReturn(tArray);
		stmt.setArray(7, tArray);
		tArray.free();

		// WHEN
		replay(con, stmt, iArray, aArray, sArray, tArray);
		PreparedStatement result = new InsertDatum(d).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Connection statement returned", result, sameInstance(stmt));
		assertThat("Timestamp prarameter", tsCaptor.getValue(),
				equalTo(Timestamp.from(d.getTimestamp())));
		assertThat("Received prarameter", recvCaptor.getValue(),
				equalTo(Timestamp.from(d.getReceived())));
		verify(con, stmt, iArray, aArray, sArray, tArray);
	}
}
