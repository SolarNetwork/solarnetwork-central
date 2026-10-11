/* ==================================================================
 * UpdateGncTaxTableRefcount.java - 11/10/2026 2:47:17 pm
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;

/**
 * Add to the reference count of a GnuCash tax table.
 *
 * @author matt
 * @version 1.0
 */
public final class UpdateGncTaxTableRefcount implements PreparedStatementCreator, SqlProvider {

	private final String schema;
	private final String guid;
	private final int delta;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param guid
	 *        the tax table GUID
	 * @param delta
	 *        the amount to add to the reference count
	 */
	public UpdateGncTaxTableRefcount(String schema, String guid, int delta) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.guid = requireNonNullArgument(guid, "guid");
		this.delta = delta;
	}

	@Override
	public String getSql() {
		return "UPDATE " + schema + ".taxtables SET refcount = refcount + ? WHERE guid = ?";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setInt(1, delta);
		stmt.setString(2, guid);
		return stmt;
	}

}
