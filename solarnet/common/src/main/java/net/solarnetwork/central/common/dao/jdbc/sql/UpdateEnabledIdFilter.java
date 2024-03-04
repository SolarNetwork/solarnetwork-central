/* ==================================================================
 * UpdateEnabledIdFilter.java - 21/02/2024 9:54:06 am
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

import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.CompositeKey;

/**
 * Update the enabled status based on a primary key filter.
 *
 * @author matt
 * @version 1.0
 */
public class UpdateEnabledIdFilter implements PreparedStatementCreator, SqlProvider {

	private String tableName;
	private String[] idColumnNames;
	private CompositeKey filter;
	private boolean enabled;

	/**
	 * Constructor.
	 *
	 * @param tableName
	 *        the table name to update
	 * @param idColumnNames
	 *        the ID column names
	 * @param enabled
	 *        the desired enabled state
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code filter} is {@literal null}
	 */
	public UpdateEnabledIdFilter(String tableName, String[] idColumnNames, CompositeKey filter,
			boolean enabled) {
		super();
		this.tableName = requireNonNullArgument(tableName, "tableName");
		this.idColumnNames = requireNonEmptyArgument(idColumnNames, "idColumnNames");
		this.filter = requireNonNullArgument(filter, "filter");
		this.enabled = enabled;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlWhere(buf);
		return buf.toString();
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("""
				UPDATE %s
				SET enabled = ?, modified = CURRENT_TIMESTAMP
				""".formatted(tableName));
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = null;
		final int componentCount = filter.keyComponentLength();
		for ( int i = 0; i < componentCount && i < idColumnNames.length; i++ ) {
			if ( filter.keyComponentIsAssigned(i) ) {
				if ( where == null ) {
					where = new StringBuilder();
				}
				where.append("\tAND ").append(idColumnNames[i]).append(" = ?\n");
			}
		}
		if ( where != null ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		stmt.setBoolean(++p, enabled);

		p = prepareWhere(con, stmt, p);
		return stmt;
	}

	private int prepareWhere(Connection con, PreparedStatement stmt, int p) throws SQLException {
		final int componentCount = filter.keyComponentLength();
		for ( int i = 0; i < componentCount && i < idColumnNames.length; i++ ) {
			if ( filter.keyComponentIsAssigned(i) ) {
				stmt.setObject(++p, filter.keyComponent(i));
			}
		}
		return p;
	}

}
