/* ==================================================================
 * DataSourcePingTest.java - 25/05/2015 11:30:49 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import javax.sql.DataSource;
import net.solarnetwork.central.domain.PingTest;
import net.solarnetwork.central.domain.PingTestResult;

/**
 * {@link PingTest} to verify a {@link DataSource} connection is available. This
 * test expects the configured {@code query} to return a
 * {@link java.sql.Timestamp} as the first column of the query result.
 * 
 * @author matt
 * @version 1.1
 * @deprecated use {@link net.solarnetwork.dao.jdbc.DataSourcePingTest} instead
 */
@Deprecated
public class DataSourcePingTest implements PingTest {

	private final DataSource dataSource;
	private final String query;

	public DataSourcePingTest(DataSource dataSource, String query) {
		super();
		this.dataSource = dataSource;
		this.query = query;
	}

	@Override
	public String getPingTestId() {
		return getClass().getName();
	}

	@Override
	public String getPingTestName() {
		return "JDBC Pool DataSource Connection";
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 500;
	}

	@Override
	public PingTestResult performPingTest() throws Exception {
		if ( dataSource == null ) {
			return new PingTestResult(false, "No DataSource configured.");
		}
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		Timestamp ts = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
			rs = stmt.executeQuery(query);
			while ( rs.next() ) {
				ts = rs.getTimestamp(1);
				break;
			}
		} finally {
			if ( rs != null ) {
				rs.close();
			}
			if ( stmt != null ) {
				stmt.close();
			}
			if ( conn != null ) {
				conn.close();
			}
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return new PingTestResult(ts != null, ts != null ? sdf.format(ts) : "No timestamp available.");
	}

}
