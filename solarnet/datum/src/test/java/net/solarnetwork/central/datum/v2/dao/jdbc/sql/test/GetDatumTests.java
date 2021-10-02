/* ==================================================================
 * GetDatumTests.java - 22/11/2020 3:29:53 pm
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.equalToTextResource;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.GetDatum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;

/**
 * Test cases for the {@link GetDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GetDatumTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_get() {
		// GIVEN
		DatumPK pk = new DatumPK(UUID.randomUUID(), Instant.now());

		// WHEN
		String sql = new GetDatum(pk).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("get-datum.sql", TestSqlResources.class));
	}

	@Test
	public void prep_get() throws SQLException {
		// GIVEN
		DatumPK pk = new DatumPK(UUID.randomUUID(), Instant.now());

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor))).andReturn(stmt);

		stmt.setObject(eq(1), eq(pk.getStreamId()), eq(Types.OTHER));
		stmt.setTimestamp(eq(2), eq(Timestamp.from(pk.getTimestamp())));

		// WHEN
		replay(con, stmt);
		PreparedStatement result = new GetDatum(pk).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt);
	}

}
