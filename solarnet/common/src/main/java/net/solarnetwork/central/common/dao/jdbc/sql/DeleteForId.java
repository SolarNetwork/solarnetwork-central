/* ==================================================================
 * DeleteForId.java - 25/06/2024 10:51:03 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;

/**
 * Delete based on a simple primary key.
 * 
 * @author matt
 * @version 1.0
 */
public class DeleteForId implements PreparedStatementCreator, SqlProvider {

	private final Object pk;
	private final String tableName;
	private final String columnName;

	/**
	 * Constructor.
	 * 
	 * @param pk
	 *        the primary key
	 * @param tableName
	 *        the table name
	 * @param columnName
	 *        the column name that corresponds to the key index
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DeleteForId(Object pk, String tableName, String columnName) {
		super();
		this.pk = requireNonNullArgument(pk, tableName);
		this.tableName = requireNonNullArgument(tableName, "tableName");
		this.columnName = requireNonNullArgument(columnName, "columnName");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append(String.format("DELETE FROM %s\n", tableName));
		buf.append("WHERE ").append(columnName).append(" = ?");
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setObject(1, pk);
		return stmt;
	}

}
