/* ==================================================================
 * StoreDatumTests.java - 22/04/2022 5:30:13 PM
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql.test;

import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.StoreDatum;
import net.solarnetwork.domain.datum.DatumProperties;

/**
 * Test cases for the {@link StoreDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class StoreDatumTests {

	private static final Logger log = LoggerFactory.getLogger(StoreDatumTests.class);

	private static DatumEntity testDatum() {
		DatumProperties p = DatumProperties.propertiesOf(decimalArray("1.23", "2.34"),
				decimalArray("3.45"), new String[] { "a", "b" }, new String[] { "t" });
		return new DatumEntity(UUID.randomUUID(), Instant.now().truncatedTo(ChronoUnit.MILLIS),
				Instant.now(), p);
	}

	@Test
	public void sql_store() {
		// GIVEN
		DatumEntity d = testDatum();

		// WHEN
		String sql = new StoreDatum(d).getSql();

		// THEN
		assertThat("SQL matches", sql, is("{call solardatm.store_stream_datum(?,?,?,?,?,?,?)}"));
	}

	@Test
	public void prep_store() throws SQLException {
		// GIVEN
		DatumEntity d = testDatum();

		Connection con = EasyMock.createMock(Connection.class);
		CallableStatement stmt = EasyMock.createMock(CallableStatement.class);

		// stream_id, ts, atype, notes, jdata_af, jdata_as, jmeta

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareCall(capture(sqlCaptor))).andReturn(stmt);

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
		CallableStatement result = new StoreDatum(d).createCallableStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(),
				is("{call solardatm.store_stream_datum(?,?,?,?,?,?,?)}"));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		assertThat("Timestamp prarameter", tsCaptor.getValue(),
				equalTo(Timestamp.from(d.getTimestamp())));
		assertThat("Received prarameter", recvCaptor.getValue(),
				equalTo(Timestamp.from(d.getReceived())));
		verify(con, stmt, iArray, aArray, sArray, tArray);
	}

}
