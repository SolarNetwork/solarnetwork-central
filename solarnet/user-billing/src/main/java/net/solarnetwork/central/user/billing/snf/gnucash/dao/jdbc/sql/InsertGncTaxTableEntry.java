/* ==================================================================
 * InsertGncTaxTableEntry.java - 11/10/2026 2:47:17 pm
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
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncTaxTableEntry;

/**
 * Insert a GnuCash tax table entry.
 *
 * @author matt
 * @version 1.0
 */
public final class InsertGncTaxTableEntry implements PreparedStatementCreator, SqlProvider {

	private final String schema;
	private final String tableGuid;
	private final GncTaxTableEntry entry;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param tableGuid
	 *        the GUID of the entry's tax table
	 * @param entry
	 *        the entry to insert
	 */
	public InsertGncTaxTableEntry(String schema, String tableGuid, GncTaxTableEntry entry) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.tableGuid = requireNonNullArgument(tableGuid, "tableGuid");
		this.entry = requireNonNullArgument(entry, "entry");
	}

	@Override
	public String getSql() {
		return "INSERT INTO " + schema
				+ ".taxtable_entries (taxtable, account, amount_num, amount_denom, type)"
				+ " VALUES (?, ?, ?, ?, ?)";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setString(1, tableGuid);
		stmt.setString(2, entry.accountGuid());
		GnucashSqlUtils.setNumeric(stmt, 3, entry.amount());
		stmt.setInt(5, entry.type());
		return stmt;
	}

}
