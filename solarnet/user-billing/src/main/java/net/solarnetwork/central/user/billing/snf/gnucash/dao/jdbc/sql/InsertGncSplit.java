/* ==================================================================
 * InsertGncSplit.java - 11/10/2026 2:48:05 pm
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

import static net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.GnucashSqlUtils.setNumeric;
import static net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.GnucashSqlUtils.setTimestamp;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncSplit;

/**
 * Insert a GnuCash split.
 *
 * @author matt
 * @version 1.0
 */
public final class InsertGncSplit implements PreparedStatementCreator, SqlProvider {

	private final String schema;
	private final GncSplit split;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param split
	 *        the split to insert
	 */
	public InsertGncSplit(String schema, GncSplit split) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.split = requireNonNullArgument(split, "split");
	}

	@Override
	public String getSql() {
		return """
				INSERT INTO {schema}.splits (guid, tx_guid, account_guid, memo, action
					, reconcile_state, reconcile_date, value_num, value_denom
					, quantity_num, quantity_denom, lot_guid)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""".replace("{schema}", schema);
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		stmt.setString(++p, split.guid());
		stmt.setString(++p, split.txGuid());
		stmt.setString(++p, split.accountGuid());
		stmt.setString(++p, split.memo());
		stmt.setString(++p, split.action());
		stmt.setString(++p, split.reconcileState());
		setTimestamp(stmt, ++p, split.reconcileDate());
		p = setNumeric(stmt, ++p, split.value());
		p = setNumeric(stmt, ++p, split.quantity());
		stmt.setString(++p, split.lotGuid());
		return stmt;
	}

}
