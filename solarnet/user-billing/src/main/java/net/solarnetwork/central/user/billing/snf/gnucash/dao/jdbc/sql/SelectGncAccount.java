/* ==================================================================
 * SelectGncAccount.java - 11/10/2026 2:46:24 pm
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
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncAccount;

/**
 * Select a GnuCash account by its full name, or GUID.
 *
 * <p>
 * The full name is built by walking down from the book's root account, so the
 * accounts of the template root used by scheduled transactions are never
 * matched.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectGncAccount implements PreparedStatementCreator, SqlProvider {

	/** A row mapper for the results. */
	public static final RowMapper<GncAccount> ROW_MAPPER = SelectGncAccount::mapRow;

	private final String schema;
	private final boolean byGuid;
	private final String value;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param byGuid
	 *        {@code true} to select by GUID, or {@code false} to select by
	 *        full name
	 * @param value
	 *        the GUID, or full account name such as
	 *        {@code Assets:Accounts Receivable}
	 */
	public SelectGncAccount(String schema, boolean byGuid, String value) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.byGuid = byGuid;
		this.value = requireNonNullArgument(value, "value");
	}

	@Override
	public String getSql() {
		return """
				WITH RECURSIVE acct AS (
					SELECT a.guid, a.account_type, a.commodity_guid, a.commodity_scu
						, a.name::TEXT AS path
					FROM {schema}.accounts a
					WHERE a.parent_guid = (SELECT root_account_guid FROM {schema}.books LIMIT 1)
					UNION ALL
					SELECT a.guid, a.account_type, a.commodity_guid, a.commodity_scu
						, acct.path || ':' || a.name
					FROM {schema}.accounts a
					INNER JOIN acct ON a.parent_guid = acct.guid
				)
				SELECT guid, path, account_type, commodity_guid, commodity_scu
				FROM acct
				WHERE {col} = ?
				""".replace("{schema}", schema).replace("{col}", byGuid ? "guid" : "path");
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setString(1, value);
		return stmt;
	}

	private static GncAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new GncAccount(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
				rs.getInt(5));
	}

}
