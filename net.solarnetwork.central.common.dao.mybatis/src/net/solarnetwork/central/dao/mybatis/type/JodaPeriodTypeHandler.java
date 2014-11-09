/* ==================================================================
 * JodaPeriodTypeHandler.java - Nov 8, 2014 12:11:23 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao.mybatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

/**
 * Implementation of {@link org.apache.ibatis.type.TypeHandler} for dealing with
 * Joda Time {@link Period} objects.
 * 
 * <p>
 * This implementation works by setting/getting String values of SQL INTERVAL
 * types, which are expected to be in standard ISO 8601 format.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class JodaPeriodTypeHandler extends BaseTypeHandler<Period> {

	/** The ISO PeriodFormatter. */
	protected static final PeriodFormatter PERIOD_FORMAT = ISOPeriodFormat.standard();

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Period parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setString(i, PERIOD_FORMAT.print(parameter));
	}

	@Override
	public Period getNullableResult(ResultSet rs, String columnName) throws SQLException {
		String s = rs.getString(columnName);
		return getPeriod(s);
	}

	@Override
	public Period getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		String s = rs.getString(columnIndex);
		return getPeriod(s);
	}

	@Override
	public Period getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		String s = cs.getString(columnIndex);
		return getPeriod(s);
	}

	private Period getPeriod(String s) {
		if ( s == null ) {
			return null;
		}
		return PERIOD_FORMAT.parsePeriod(s);
	}

}
