/* ==================================================================
 * MigrateConsumptionDatum.java - Nov 22, 2013 3:05:41 PM
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
import com.datastax.driver.core.Session;

/**
 * Migrate the ConsumptionDatum table.
 * 
 * @author matt
 * @version 1.0
 */
public class MigratePowerDatum extends MigrateDatumSupport {

	private static final String SQL = "SELECT id, created, posted, node_id, source_id, "
			+ "price_loc_id, watts, watt_hour, bat_volts, bat_amp_hrs FROM solarnet.sn_power_datum "
			+ "ORDER BY id ASC";

	private static final String COUNT_SQL = "SELECT count(id) FROM solarnet.sn_power_datum";

	public MigratePowerDatum() {
		super();
		setSql(SQL);
		setCountSql(COUNT_SQL);
	}

	@Override
	protected int getDatumType() {
		return 2;
	}

	@Override
	protected String getDatumTypeDescription() {
		return "PowerDatum";
	}

	@Override
	protected void handleInputResultRow(ResultSet rs, Session cSession,
			com.datastax.driver.core.PreparedStatement cStmt) throws SQLException {
		// output: node_id, dtype, source_id, year, ts, data_num

		BoundStatement bs = new BoundStatement(cStmt);
		bs.setString(0, rs.getObject(4).toString());
		bs.setInt(1, getDatumType());
		bs.setString(2, rs.getString(5));

		Timestamp created = rs.getTimestamp(2);
		gmtCalendar.setTimeInMillis(created.getTime());
		bs.setInt(3, gmtCalendar.get(Calendar.YEAR));

		bs.setDate(4, created);

		Map<String, BigDecimal> rowData = new LinkedHashMap<String, BigDecimal>(3);
		long l = rs.getLong(6);
		if ( !rs.wasNull() ) {
			rowData.put("priceLocationId", new BigDecimal(l));
		}
		int i = rs.getInt(7);
		if ( !rs.wasNull() ) {
			rowData.put("watts", new BigDecimal(i));
		}
		l = rs.getLong(8);
		if ( !rs.wasNull() ) {
			rowData.put("watt_hour", new BigDecimal(l));
		}
		float f = rs.getFloat(9);
		if ( !rs.wasNull() ) {
			rowData.put("bat_volts", new BigDecimal(f));
		}
		f = rs.getFloat(10);
		if ( !rs.wasNull() ) {
			rowData.put("bat_amp_hrs", new BigDecimal(f));
		}
		bs.setMap(5, rowData);
		cSession.execute(bs);
	}

}
