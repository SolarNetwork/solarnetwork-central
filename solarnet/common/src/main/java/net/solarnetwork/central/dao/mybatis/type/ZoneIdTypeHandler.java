/* ==================================================================
 * ZoneIdTypeHandler.java - 2 Aug 2026 10:56:12 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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
import java.time.ZoneId;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.jspecify.annotations.Nullable;

/**
 * MyBatis {@link TypeHandler} for {@link ZoneId} support, stored as text column
 * time zone ID value.
 * 
 * @author matt
 * @version 1.0
 */
public class ZoneIdTypeHandler extends BaseTypeHandler<ZoneId> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, ZoneId parameter,
			@Nullable JdbcType jdbcType) throws SQLException {
		ps.setString(i, parameter.getId());
	}

	@Override
	public @Nullable ZoneId getNullableResult(ResultSet rs, String columnName) throws SQLException {
		final String id = rs.getString(columnName);
		return (id != null ? ZoneId.of(id) : null);
	}

	@Override
	public @Nullable ZoneId getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		final String id = rs.getString(columnIndex);
		return (id != null ? ZoneId.of(id) : null);
	}

	@Override
	public @Nullable ZoneId getNullableResult(CallableStatement cs, int columnIndex)
			throws SQLException {
		final String id = cs.getString(columnIndex);
		return (id != null ? ZoneId.of(id) : null);
	}

}
