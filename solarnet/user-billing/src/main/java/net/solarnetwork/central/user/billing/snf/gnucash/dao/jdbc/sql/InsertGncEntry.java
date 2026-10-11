/* ==================================================================
 * InsertGncEntry.java - 11/10/2026 2:48:05 pm
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
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncEntry;

/**
 * Insert a GnuCash invoice line.
 *
 * <p>
 * The line has no discount, and its bill fields have the values GnuCash uses
 * for an invoice line.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class InsertGncEntry implements PreparedStatementCreator, SqlProvider {

	private final String schema;
	private final GncEntry entry;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param entry
	 *        the line to insert
	 */
	public InsertGncEntry(String schema, GncEntry entry) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.entry = requireNonNullArgument(entry, "entry");
	}

	@Override
	public String getSql() {
		return """
				INSERT INTO {schema}.entries (guid, date, date_entered, description, action, notes
					, quantity_num, quantity_denom, i_acct, i_price_num, i_price_denom
					, i_discount_num, i_discount_denom, invoice, i_disc_type, i_disc_how
					, i_taxable, i_taxincluded, i_taxtable
					, b_acct, b_price_num, b_price_denom, bill, b_taxable, b_taxincluded, b_taxtable
					, b_paytype, billable, billto_type, billto_guid, order_guid)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 1, ?, 'PERCENT', 'PRETAX', ?, ?, ?
					, NULL, 0, 1, NULL, 1, 0, NULL, 1, 0, NULL, NULL, NULL)
				""".replace("{schema}", schema);
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		stmt.setString(++p, entry.guid());
		setTimestamp(stmt, ++p, entry.date());
		setTimestamp(stmt, ++p, entry.dateEntered());
		stmt.setString(++p, entry.description());
		stmt.setString(++p, entry.action());
		stmt.setString(++p, entry.notes());
		p = setNumeric(stmt, ++p, entry.quantity());
		stmt.setString(++p, entry.accountGuid());
		p = setNumeric(stmt, ++p, entry.price());
		stmt.setString(++p, entry.invoiceGuid());
		stmt.setInt(++p, entry.taxable() ? 1 : 0);
		stmt.setInt(++p, entry.taxIncluded() ? 1 : 0);
		stmt.setString(++p, entry.taxTableGuid());
		return stmt;
	}

}
