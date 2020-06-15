/* ==================================================================
 * JsonMapTypeHandler.java - 5/06/2020 10:50:29 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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
import java.sql.Types;
import java.util.Map;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import net.solarnetwork.util.JsonUtils;

/**
 * MyBatis {@link TypeHandler} for mapping between a {@link Map} with string
 * keys and a JSON object.
 * 
 * <p>
 * Note that the handler gets/sets string values, so if the database type is
 * something else then casts in SQL might be necessary.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 2.3
 */
public class JsonMapTypeHandler extends BaseTypeHandler<Map<String, Object>> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter,
			JdbcType jdbcType) throws SQLException {
		String json = JsonUtils.getJSONString(parameter, "{}");
		ps.setObject(i, json, (jdbcType != null ? jdbcType.TYPE_CODE : Types.VARCHAR));

	}

	@Override
	public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return JsonUtils.getStringMap(rs.getString(columnName));
	}

	@Override
	public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return JsonUtils.getStringMap(rs.getString(columnIndex));
	}

	@Override
	public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex)
			throws SQLException {
		return JsonUtils.getStringMap(cs.getString(columnIndex));
	}

}
