/* ==================================================================
 * JodaDurationArrayTypeHandler.java - Nov 8, 2014 10:36:04 AM
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.joda.time.DateTime;

/**
 * {@link TypeHandler} for {@link DateTime} objects mapped to {@code TIMESTAMP}
 * columns.
 * 
 * @author matt
 * @version 1.0
 */
public class JodaDurationArrayTypeHandler extends BaseArrayTypeHandler {

	/**
	 * Default constructor.
	 */
	public JodaDurationArrayTypeHandler() {
		super("interval");
	}

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
			throws SQLException {
		if ( parameter == null ) {
			ps.setNull(i, Types.ARRAY);
		} else {
			Object[] input = (Object[]) parameter;
			String[] output = new String[input.length];
			for ( int j = 0, len = input.length; j < len; j++ ) {
				output[j] = JodaDurationTypeHandler.getDuration(input[j]);
			}

			Connection conn = ps.getConnection();
			Array loc = conn.createArrayOf(elementJdbcType, output);
			ps.setArray(i, loc);
		}
	}

}
