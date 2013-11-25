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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.solarnetwork.central.datum.domain.SkyCondition;
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

	private static final String SQL = "SELECT loc_id, info_date, temperature, sky, humidity, bar, bar_dir, visibility, dew "
			+ "FROM solarnet.sn_weather_datum "
			+ "WHERE loc_id = ? AND info_date >= ? AND info_date < ? ORDER BY id ASC";

	private static final String COUNT_SQL = "SELECT count(id) FROM solarnet.sn_weather_datum";

	private final Map<String, Integer> barDirationMapping = getDefaultBarDirectionMapping();
	private Map<Pattern, SkyCondition> skyConditionMapping;

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
		// input: loc_id, info_date, temperature, sky, humidity, bar, bar_dir, visibility, dew
		BoundStatement bs = getBoundStatementForResultRowMapping(rs, cStmt);
		Map<String, BigDecimal> rowData = new LinkedHashMap<String, BigDecimal>(3);
		float f = rs.getFloat(3);
		if ( !rs.wasNull() ) {
			rowData.put("temp", getBigDecimal(f, 1));
		}
		String s = rs.getString(4);
		BigDecimal sky = getSkyConditionValue(s);
		if ( sky != null ) {
			rowData.put("sky", sky);
		}
		f = rs.getFloat(5);
		if ( !rs.wasNull() ) {
			rowData.put("humi", getBigDecimal(f, 0));
		}
		f = rs.getFloat(6);
		if ( !rs.wasNull() ) {
			rowData.put("bar", getBigDecimal(f, 1));
		}
		s = rs.getString(7);
		if ( !rs.wasNull() ) {
			Integer val = barDirationMapping.get(s);
			if ( val != null ) {
				rowData.put("bard", new BigDecimal(val));
			}
		}
		s = rs.getString(8);
		if ( s != null && s.length() > 0 ) {
			try {
				// convert to meters; trying to be sensible with numbers like 
				// 2.3999999999999999 => 2400; 0.20000000000000001 => 200
				double d = Double.parseDouble(s);
				if ( d > Float.MAX_VALUE ) {
					d = java.lang.Double.POSITIVE_INFINITY;
				} else {
					d = Math.round((((long) (d * 10000L)) / 10.0));
				}
				rowData.put("vis", new BigDecimal((long) d));
			} catch ( NumberFormatException e ) {
				log.warn("Unable to parse visibility string [{}]", s);
			}
		}
		f = rs.getFloat(9);
		if ( !rs.wasNull() ) {
			rowData.put("dew", getBigDecimal(f, 0));
		}
		bs.setMap(getBoundStatementMapParameterIndex(), rowData);
		cSession.execute(bs);
	}

	private BigDecimal getSkyConditionValue(String condition) {
		if ( condition == null || condition.length() < 1 ) {
			return null;
		}
		SkyCondition sky = SkyCondition.mapStringValue(condition, this.skyConditionMapping);
		if ( sky != null ) {
			return new BigDecimal(sky.getCode());
		}
		log.warn("Unsupported sky condition value [" + condition + "]");
		return null;
	}

	public Map<Pattern, SkyCondition> getSkyConditionMapping() {
		return skyConditionMapping;
	}

	public void setSkyConditionMapping(Map<Pattern, SkyCondition> conditionMapping) {
		this.skyConditionMapping = conditionMapping;
	}

}
