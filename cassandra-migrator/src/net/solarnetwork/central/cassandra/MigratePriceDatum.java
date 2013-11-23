/* ==================================================================
 * MigratePriceDatum.java - Nov 23, 2013 5:49:50 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.cassandra;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

/**
 * Migrate PriceDatum.
 * 
 * @author matt
 * @version 1.0
 */
public class MigratePriceDatum extends MigrateLocationDatumSupport {

	private static final String SQL = "SELECT id, created, loc_id, price "
			+ "FROM solarnet.sn_price_datum ORDER BY id ASC";

	private static final String COUNT_SQL = "SELECT count(id) FROM solarnet.sn_price_datum";

	public MigratePriceDatum() {
		super();
		setSql(SQL);
		setCountSql(COUNT_SQL);
	}

	@Override
	protected int getDatumType() {
		return 3;
	}

	@Override
	protected String getDatumTypeDescription() {
		return "PriceDatum";
	}

	@Override
	protected void handleInputResultRow(ResultSet rs, Session cSession, PreparedStatement cStmt)
			throws SQLException {
		// output: loc_id, ltype, year, ts, data_num

		BoundStatement bs = new BoundStatement(cStmt);
		bs.setString(0, rs.getObject(3).toString());
		bs.setInt(1, getDatumType());

		Timestamp created = rs.getTimestamp(2);
		gmtCalendar.setTimeInMillis(created.getTime());
		bs.setInt(2, gmtCalendar.get(Calendar.YEAR));

		bs.setDate(3, created);

		Map<String, BigDecimal> rowData = new LinkedHashMap<String, BigDecimal>(3);
		float f = rs.getFloat(4);
		if ( !rs.wasNull() ) {
			rowData.put("price", new BigDecimal(f));
		}
		bs.setMap(4, rowData);
		cSession.execute(bs);
	}

}
