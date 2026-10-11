/* ==================================================================
 * InsertGncInvoice.java - 11/10/2026 2:48:05 pm
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
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncInvoice;

/**
 * Insert a GnuCash business document.
 *
 * <p>
 * The document has no billing terms, separate bill-to owner, or amount to
 * charge to a card.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class InsertGncInvoice implements PreparedStatementCreator, SqlProvider {

	private final String schema;
	private final GncInvoice invoice;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param invoice
	 *        the document to insert
	 */
	public InsertGncInvoice(String schema, GncInvoice invoice) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.invoice = requireNonNullArgument(invoice, "invoice");
	}

	@Override
	public String getSql() {
		return """
				INSERT INTO {schema}.invoices (guid, id, date_opened, date_posted, notes, active
					, currency, owner_type, owner_guid, terms, billing_id
					, post_txn, post_lot, post_acc, billto_type, billto_guid
					, charge_amt_num, charge_amt_denom)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, ?, NULL, NULL, 0, 1)
				""".replace("{schema}", schema);
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		stmt.setString(++p, invoice.guid());
		stmt.setString(++p, invoice.id());
		setTimestamp(stmt, ++p, invoice.dateOpened());
		setTimestamp(stmt, ++p, invoice.datePosted());
		stmt.setString(++p, invoice.notes());
		stmt.setInt(++p, invoice.active() ? 1 : 0);
		stmt.setString(++p, invoice.currencyGuid());
		stmt.setInt(++p, invoice.ownerType());
		stmt.setString(++p, invoice.ownerGuid());
		stmt.setString(++p, invoice.billingId());
		stmt.setString(++p, invoice.postTxnGuid());
		stmt.setString(++p, invoice.postLotGuid());
		stmt.setString(++p, invoice.postAccountGuid());
		return stmt;
	}

}
