/* ==================================================================
 * MigrateWeatherDatum.java - Nov 23, 2013 5:49:50 PM
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

/**
 * Migrate WeatherDatum.
 * 
 * @author matt
 * @version 1.0
 */
public class MigrateWeatherDatum extends MigrateLocationDatumSupport {

	private static final String SQL = "SELECT id, info_date, loc_id, temperature, sky, humidity, bar, bar_dir, visibility, dew "
			+ "FROM solarnet.sn_weather_datum "
			+ "WHERE loc_id = ? AND info_date >= ? AND info_date < ? ORDER BY id ASC";

	private static final String COUNT_SQL = "SELECT count(id) FROM solarnet.sn_weather_datum";

	private final Map<String, Integer> barDirationMapping = getDefaultBarDirectionMapping();

	public MigrateWeatherDatum() {
		super();
		setSql(SQL);
		setCountSql(COUNT_SQL);
		setupGroupedDateRangeSql("solarnet.sn_weather_datum", "loc_id", "info_date");
	}

	public MigrateWeatherDatum(MigrateDatumSupport other) {
		super(other);
	}

	private Map<String, Integer> getDefaultBarDirectionMapping() {
		Map<String, Integer> m = new HashMap<String, Integer>(5);
		m.put("steady", Integer.valueOf(0));
		m.put("rising", Integer.valueOf(1));
		m.put("falling", Integer.valueOf(-1));
		return m;
	}

	@Override
	protected int getDatumType() {
		return DatumType.WeatherDatum.getCode();
	}

	@Override
	protected String getDatumTypeDescription() {
		return "WeatherDatum";
	}

	@Override
	protected void handleInputResultRow(ResultSet rs, Session cSession, PreparedStatement cStmt)
			throws SQLException {
		// input: id, info_date, loc_id, temperature, sky, humidity, bar, bar_dir, visibility, dew
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
			rowData.put("temp", new BigDecimal(f));
		}
		f = rs.getFloat(6);
		if ( !rs.wasNull() ) {
			rowData.put("humi", new BigDecimal(f));
		}
		f = rs.getFloat(7);
		if ( !rs.wasNull() ) {
			rowData.put("bar", new BigDecimal(f));
		}
		String s = rs.getString(8);
		if ( !rs.wasNull() ) {
			Integer val = barDirationMapping.get(s);
			if ( val != null ) {
				rowData.put("bard", new BigDecimal(val));
			}
		}
		f = rs.getFloat(10);
		if ( !rs.wasNull() ) {
			rowData.put("dew", new BigDecimal(f));
		}
		bs.setMap(4, rowData);
		cSession.execute(bs);
	}

}
