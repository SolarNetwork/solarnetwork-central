/* ==================================================================
 * SelectGnucashTableVersions.java - 11/10/2026 2:46:24 pm
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

package net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;

/**
 * Select the versions of the GnuCash book's tables.
 *
 * <p>
 * The result columns are {@code table_name} and {@code table_version}.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectGnucashTableVersions implements PreparedStatementCreator, SqlProvider {

	private final String schema;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 */
	public SelectGnucashTableVersions(String schema) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
	}

	@Override
	public String getSql() {
		return "SELECT table_name, table_version FROM " + schema + ".versions";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		return con.prepareStatement(getSql());
	}

}
