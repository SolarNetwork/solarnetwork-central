/* ==================================================================
 * ColumnRowMapper.java - 6/10/2021 3:29:45 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

/**
 * Row mapper for extracting a single column value.
 * 
 * @author matt
 * @version 1.0
 */
public class ColumnRowMapper<T> implements RowMapper<T> {

	private final int colNum;
	private final Class<? extends T> colType;

	/**
	 * Constructor.
	 * 
	 * @param colNum
	 *        the column number
	 * @param colType
	 *        the output type
	 */
	public ColumnRowMapper(int colNum, Class<? extends T> colType) {
		super();
		this.colNum = colNum;
		this.colType = colType;
	}

	@Override
	public T mapRow(ResultSet rs, int rowNum) throws SQLException {
		return rs.getObject(colNum, colType);
	}

}
