/* ==================================================================
 * MigrateHardwareControlDatum.java - Nov 25, 2013 2:21:30 PM
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
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Session;

/**
 * Migrate HardwareControlDatum objects.
 * 
 * @author matt
 * @version 1.0
 */
public class MigrateHardwareControlDatum extends MigrateDatumSupport {

	private static final String SQL = "SELECT node_id, source_id, created, "
			+ "int_val, float_val FROM solarnet.sn_hardware_control_datum "
			+ "WHERE node_id = ? AND created >= ? AND created < ? ORDER BY id ASC";

	private static final String COUNT_SQL = "SELECT count(id) FROM solarnet.sn_hardware_control_datum";

	public MigrateHardwareControlDatum() {
		super();
		setSql(SQL);
		setCountSql(COUNT_SQL);
		setupGroupedDateRangeSql("solarnet.sn_hardware_control_datum", "node_id", "created");
	}

	public MigrateHardwareControlDatum(MigrateDatumSupport other) {
		super(other);
	}

	@Override
	protected int getDatumType() {
		return DatumType.HardwareControlDatum.getCode();
	}

	@Override
	protected String getDatumTypeDescription() {
		return "HardwareControlDatum";
	}

	@Override
	protected void handleInputResultRow(ResultSet rs, Session cSession,
			com.datastax.driver.core.PreparedStatement cStmt) throws SQLException {
		BoundStatement bs = getBoundStatementForResultRowMapping(rs, cStmt);
		Map<String, BigDecimal> rowData = new LinkedHashMap<String, BigDecimal>(3);
		int i = rs.getInt(4);
		if ( !rs.wasNull() ) {
			rowData.put("int_val", new BigDecimal(i));
		}
		float f = rs.getFloat(5);
		if ( !rs.wasNull() ) {
			rowData.put("float_val", new BigDecimal(f));
		}
		bs.setMap(getBoundStatementMapParameterIndex(), rowData);
		cSession.execute(bs);
	}

}
