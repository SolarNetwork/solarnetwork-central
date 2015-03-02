/* ==================================================================
 * JodaLocalDateTimeTypeHandler.java - Nov 8, 2014 11:39:42 AM
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
import java.sql.Timestamp;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

/**
 * Implementation of {@link TypeHandler} for dealing with Joda Time
 * {@link LocalDateTime} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class JodaLocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter,
			JdbcType jdbcType) throws SQLException {
		switch (jdbcType) {
			case DATE:
				ps.setDate(i, new java.sql.Date(parameter.toDateTime(DateTimeZone.UTC).getMillis()));
				break;

			default:
				ps.setTimestamp(i, new Timestamp(parameter.toDateTime(DateTimeZone.UTC).getMillis()));
				break;
		}
	}

	@Override
	public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
		Timestamp ts = rs.getTimestamp(columnName);
		return getResult(ts);
	}

	@Override
	public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		Timestamp ts = rs.getTimestamp(columnIndex);
		return getResult(ts);
	}

	@Override
	public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		Timestamp ts = cs.getTimestamp(columnIndex);
		return getResult(ts);
	}

	private LocalDateTime getResult(Timestamp ts) {
		if ( ts == null ) {
			return null;
		}
		return new LocalDateTime(ts.getTime());
	}

}
