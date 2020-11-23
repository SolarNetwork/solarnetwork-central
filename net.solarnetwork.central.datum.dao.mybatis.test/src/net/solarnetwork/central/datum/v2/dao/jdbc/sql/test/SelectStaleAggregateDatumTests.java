/* ==================================================================
 * SelectStaleAggregateDatumTests.java - 23/11/2020 9:17:40 am
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.SQL_COMMENT;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.equalToTextResource;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectStaleAggregateDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the {@link SelectStaleAggregateDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectStaleAggregateDatumTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_find_all() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();

		// WHEN
		String sql = new SelectStaleAggregateDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-stale-agg-datum-all.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_hour_all() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);

		// WHEN
		String sql = new SelectStaleAggregateDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-stale-agg-datum-hour-all.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_hour_all() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		stmt.setString(1, Aggregation.Hour.getKey());

		// WHEN
		replay(con, stmt);
		PreparedStatement result = new SelectStaleAggregateDatum(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-stale-agg-datum-hour-all.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt);
	}

}
