/* ==================================================================
 * JodaLocalTimeTypeHandler.java - Nov 8, 2014 12:01:46 PM
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
import java.sql.Time;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePartial;

/**
 * Implementation of {@link TypeHandler} for dealing with Joda Time
 * {@link LocalTime} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class JodaLocalTimeTypeHandler extends BaseTypeHandler<LocalTime> {

	/**
	 * Get a SQL Time object from a Joda Time object.
	 * 
	 * @param parameter
	 *        the Joda Time object, implementing {@link ReadablePartial} or
	 *        {@link ReadableInstant}
	 * @return the Time instance
	 * @throws IllegalArgumentException
	 *         if the parameter type is not supported
	 */
	public static Time getTime(Object parameter) {
		if ( parameter instanceof ReadablePartial ) {
			ReadablePartial p = (ReadablePartial) parameter;
			DateTime dt = p.toDateTime(new DateTime());
			Time t = new Time(dt.getMillis());
			return t;
		} else if ( parameter instanceof ReadableInstant ) {
			ReadableInstant p = (ReadableInstant) parameter;
			Time t = new Time(p.getMillis());
			return t;
		} else {
			throw new IllegalArgumentException("Unknown time object type [" + parameter.getClass()
					+ "]: " + parameter);
		}
	}

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, LocalTime parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setTime(i, getTime(parameter));
	}

	@Override
	public LocalTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
		Time ts = rs.getTime(columnName);
		return getResult(ts);
	}

	@Override
	public LocalTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		Time ts = rs.getTime(columnIndex);
		return getResult(ts);
	}

	@Override
	public LocalTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		Time ts = cs.getTime(columnIndex);
		return getResult(ts);
	}

	private LocalTime getResult(java.sql.Time t) {
		if ( t == null ) {
			return null;
		}
		return new LocalTime(t);
	}

}
