/* ==================================================================
 * InsertGncTransaction.java - 11/10/2026 2:48:05 pm
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

import static net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.GnucashSqlUtils.setTimestamp;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncTransaction;

/**
 * Insert a GnuCash transaction.
 *
 * @author matt
 * @version 1.0
 */
public final class InsertGncTransaction implements PreparedStatementCreator, SqlProvider {

	private final String schema;
	private final GncTransaction transaction;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param transaction
	 *        the transaction to insert
	 */
	public InsertGncTransaction(String schema, GncTransaction transaction) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.transaction = requireNonNullArgument(transaction, "transaction");
	}

	@Override
	public String getSql() {
		return "INSERT INTO " + schema
				+ ".transactions (guid, currency_guid, num, post_date, enter_date, description)"
				+ " VALUES (?, ?, ?, ?, ?, ?)";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setString(1, transaction.guid());
		stmt.setString(2, transaction.currencyGuid());
		stmt.setString(3, transaction.num());
		setTimestamp(stmt, 4, transaction.postDate());
		setTimestamp(stmt, 5, transaction.enterDate());
		stmt.setString(6, transaction.description());
		return stmt;
	}

}
