/* ==================================================================
 * MigratePriceDatumAggregateHourly.java - Nov 25, 2013 4:55:32 PM
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
 * Migrate price aggregate tables.
 * 
 * @author matt
 * @version 1.0
 */
public class MigratePriceDatumAggregateHourly extends MigrateLocationDatumAggregateSupport {

	private static final String SQL = "SELECT loc_id, created_hour, price FROM solarrep.rep_price_datum_hourly "
			+ "WHERE loc_id = ? AND created_hour >= ? AND created_hour < ? ORDER BY loc_id, created_hour";

	private static final String COUNT_SQL = "SELECT count(loc_id) FROM solarrep.rep_price_datum_hourly";

	/**
	 * Default constructor.
	 */
	public MigratePriceDatumAggregateHourly() {
		super();
		setSql(SQL);
		setCountSql(COUNT_SQL);
		setupGroupedDateRangeSql("solarrep.rep_price_datum_hourly", "loc_id", "created_hour");
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 */
	public MigratePriceDatumAggregateHourly(MigrateDatumSupport other) {
		super(other);
	}

	@Override
	protected int getDatumType() {
		return DatumType.PriceDatum.getCode();
	}

	@Override
	protected String getDatumTypeDescription() {
		return "PriceDatumHourly";
	}

	@Override
	protected int getAggregateType() {
		return Aggregation.Hour.getLevel();
	}

	@Override
	protected void handleInputResultRow(ResultSet rs, Session cSession, PreparedStatement cStmt)
			throws SQLException {
		BoundStatement bs = getBoundStatementForResultRowMapping(rs, cStmt);
		Map<String, BigDecimal> rowData = new LinkedHashMap<String, BigDecimal>(3);
		float f = rs.getFloat(3);
		if ( !rs.wasNull() ) {
			rowData.put("price", getBigDecimal(f, 5));
		}
		bs.setMap(getBoundStatementMapParameterIndex(), rowData);
		cSession.execute(bs);
	}

}
