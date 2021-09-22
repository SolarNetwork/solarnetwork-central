/* ==================================================================
 * JodaLocalDateTypeHandler.java - Nov 8, 2014 11:48:42 AM
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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.joda.time.LocalDate;

/**
 * Implementation of {@link TypeHandler} for dealing with Joda Time
 * {@link LocalDate} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class JodaLocalDateTypeHandler extends BaseTypeHandler<LocalDate> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, LocalDate parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setDate(i, new java.sql.Date(parameter.toDate().getTime()));
	}

	@Override
	public LocalDate getNullableResult(ResultSet rs, String columnName) throws SQLException {
		Date ts = rs.getDate(columnName);
		return getResult(ts);
	}

	@Override
	public LocalDate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		Date ts = rs.getDate(columnIndex);
		return getResult(ts);
	}

	@Override
	public LocalDate getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		Date ts = cs.getDate(columnIndex);
		return getResult(ts);
	}

	private LocalDate getResult(java.sql.Date d) {
		if ( d == null ) {
			return null;
		}
		return new LocalDate(d);
	}

}
