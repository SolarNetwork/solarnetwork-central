/* ==================================================================
 * DeleteForGroupMinimumIndex.java - 12/08/2023 2:57:34 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.CompositeKey;

/**
 * Delete items within a composite key group, having the highest key component
 * index equal to or higher than a given value.
 * 
 * @author matt
 * @version 1.0
 */
public final class DeleteForGroupMinimumIndex implements PreparedStatementCreator, SqlProvider {

	private final CompositeKey pk;
	private final String tableName;
	private final String[] columnNames;

	/**
	 * Constructor.
	 * 
	 * @param pk
	 *        the primary key
	 * @param tableName
	 *        the table name
	 * @param columnNames
	 *        the column names that correspond to key indexes
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DeleteForGroupMinimumIndex(CompositeKey pk, String tableName, String[] columnNames) {
		super();
		this.pk = requireNonNullArgument(pk, tableName);
		this.tableName = requireNonNullArgument(tableName, "tableName");
		this.columnNames = requireNonEmptyArgument(columnNames, "columnNames");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlWhere(buf);
		return buf.toString();
	}

	private void sqlCore(StringBuilder buf) {
		buf.append(String.format("DELETE FROM %s\n", tableName));
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int i, len;
		for ( i = 0, len = pk.keyComponentLength() - 1; i < len; i++ ) {
			if ( pk.keyComponentIsAssigned(i) ) {
				where.append("\tAND ").append(columnNames[i]).append(" = ?\n");
			}
		}
		where.append("\tAND ").append(columnNames[i]).append(" >= ?");
		buf.append("WHERE").append(where.substring(4));
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		int i, len;
		for ( i = 0, len = pk.keyComponentLength() - 1; i < len; i++ ) {
			if ( pk.keyComponentIsAssigned(i) ) {
				stmt.setObject(++p, pk.keyComponent(i));
			}
		}
		stmt.setObject(++p, pk.keyComponent(i));
		return stmt;
	}

}
