/* ==================================================================
 * ArrayTypeHandler.java - Jun 4, 2011 4:26:23 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dao.ibatis;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.ibatis.sqlmap.engine.type.TypeHandler;

/**
 * {@link TypeHandler} for SQL arrays.
 * 
 * @author matt
 * @version $Revision$
 */
public abstract class ArrayTypeHandler implements TypeHandler {
	
	final protected String elementJdbcType;
	
	/**
	 * Text array type hanlder.
	 * 
	 * @author matt
	 * @version $Revision$
	 */
	public static final class TextArrayTypeHandler extends ArrayTypeHandler {
		
		/**
		 * Default constructor.
		 */
		public TextArrayTypeHandler() {
			super("text");
		}
		
	}
	
	/**
	 * Constructor.
	 * 
	 * @param elementJdbcType the element JDBC type
	 */
	public ArrayTypeHandler(String elementJdbcType) {
		this.elementJdbcType = elementJdbcType;
	}

	@Override
	public void setParameter(PreparedStatement ps, int i, Object parameter,
			String jdbcType) throws SQLException {
		if ( parameter == null ) {
            ps.setNull(i, Types.ARRAY);
        } else {
            Connection conn = ps.getConnection();
            Array loc = conn.createArrayOf(elementJdbcType, (Object[]) parameter);
            ps.setArray(i, loc);
        }
	}

	@Override
	public Object getResult(ResultSet rs, String columnName)
			throws SQLException {
		Array result = rs.getArray(columnName);
		if ( result == null ) {
			return null;
		}
		 return result.getArray();
	}

	@Override
	public Object getResult(ResultSet rs, int columnIndex) throws SQLException {
		 return rs.getArray(columnIndex).getArray();

	}

	@Override
	public Object getResult(CallableStatement cs, int columnIndex)
			throws SQLException {
		 return cs.getArray(columnIndex).getArray();

	}

	@Override
	public Object valueOf(String s) {
		return s;
	}

	@Override
	public boolean equals(Object object, String string) {
		return false;
	}

}
