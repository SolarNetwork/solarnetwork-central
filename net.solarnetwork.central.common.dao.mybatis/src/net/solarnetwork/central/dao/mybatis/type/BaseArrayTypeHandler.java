/* ==================================================================
 * BaseArrayTypeHandler.java - Nov 8, 2014 11:18:12 AM
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

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * Base {@link org.apache.ibatis.type.TypeHandler} for SQL arrays.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseArrayTypeHandler extends BaseTypeHandler<Object> {

	final protected String elementJdbcType;

	/**
	 * Constructor.
	 * 
	 * @param elementJdbcType
	 *        the element JDBC type
	 */
	public BaseArrayTypeHandler(String elementJdbcType) {
		this.elementJdbcType = elementJdbcType;
	}

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
			throws SQLException {
		if ( parameter == null ) {
			ps.setNull(i, Types.ARRAY);
		} else {
			Connection conn = ps.getConnection();
			Array loc = conn.createArrayOf(elementJdbcType, (Object[]) parameter);
			ps.setArray(i, loc);
		}
	}

	@Override
	public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
		Array result = rs.getArray(columnName);
		return (result == null ? null : result.getArray());
	}

	@Override
	public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		Array result = rs.getArray(columnIndex);
		return (result == null ? null : result.getArray());
	}

	@Override
	public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		Array result = cs.getArray(columnIndex);
		return (result == null ? null : result.getArray());
	}

}
