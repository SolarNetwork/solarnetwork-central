/* ==================================================================
 * MigratePriceDatumAggregateDaily.java - Nov 25, 2013 4:55:32 PM
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
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.central.datum.domain.Aggregation;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

/**
 * Migrate power aggregate tables.
 * 
 * @author matt
 * @version 1.0
 */
public class MigratePowerDatumAggregateDaily extends MigrateDatumAggregateSupport {

	private static final String SQL = "SELECT node_id, source_id, created_day, watts, watt_hours, bat_volts, cost_amt, cost_currency FROM solarrep.rep_power_datum_daily "
			+ "WHERE node_id = ? AND created_day >= ? AND created_day < ? ORDER BY node_id, created_day";

	private static final String COUNT_SQL = "SELECT count(node_id) FROM solarrep.rep_power_datum_daily";

	/**
	 * Default constructor.
	 */
	public MigratePowerDatumAggregateDaily() {
		super();
		setSql(SQL);
		setCountSql(COUNT_SQL);
		setupGroupedDateRangeSql("solarrep.rep_power_datum_daily", "node_id", "created_day");
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 */
	public MigratePowerDatumAggregateDaily(MigrateDatumSupport other) {
		super(other);
	}

	@Override
	protected int getDatumType() {
		return DatumType.PowerDatum.getCode();
	}

	@Override
	protected String getDatumTypeDescription() {
		return "PowerDatumDaily";
	}

	@Override
	protected int getAggregateType() {
		return Aggregation.Day.getLevel();
	}

	@Override
	protected void handleInputResultRow(ResultSet rs, Session cSession, PreparedStatement cStmt)
			throws SQLException {
		BoundStatement bs = getBoundStatementForResultRowMapping(rs, cStmt);
		Map<String, BigDecimal> rowData = new LinkedHashMap<String, BigDecimal>(3);
		int i = rs.getInt(4);
		if ( !rs.wasNull() ) {
			rowData.put("watts", new BigDecimal(i));
		}
		double d = rs.getDouble(5);
		if ( !rs.wasNull() ) {
			rowData.put("watt_hour", getBigDecimal(d, 3));
		}
		float f = rs.getFloat(6);
		if ( !rs.wasNull() ) {
			rowData.put("bat_volts", getBigDecimal(f, 3));
		}
		// TODO: cost, cost_currency
		bs.setMap(getBoundStatementMapParameterIndex(), rowData);
		cSession.execute(bs);
	}

}
