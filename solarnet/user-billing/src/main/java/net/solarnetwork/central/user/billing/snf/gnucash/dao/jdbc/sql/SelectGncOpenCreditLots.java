/* ==================================================================
 * SelectGncOpenCreditLots.java - 11/10/2026 2:48:41 pm
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
 * Select the open lots that hold credit for a customer in an account.
 *
 * <p>
 * These are lots with a negative balance that either hold a payment, tagged
 * with the customer in {@code gncOwner} slots, or belong to one of the
 * customer's posted business documents, such as a credit note. The lots are
 * ordered by the post date of their earliest transaction.
 * </p>
 *
 * <p>
 * The result columns are {@code lot_guid}, {@code doc_guid}, {@code doc_id},
 * {@code doc_credit_note}, {@code balance}, {@code first_post}, and
 * {@code last_post}. The document columns are {@code null} for a payment lot.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectGncOpenCreditLots implements PreparedStatementCreator, SqlProvider {

	private final String schema;
	private final String ownerGuid;
	private final String accountGuid;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param ownerGuid
	 *        the GUID of the customer
	 * @param accountGuid
	 *        the GUID of the receivable account
	 */
	public SelectGncOpenCreditLots(String schema, String ownerGuid, String accountGuid) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.ownerGuid = requireNonNullArgument(ownerGuid, "ownerGuid");
		this.accountGuid = requireNonNullArgument(accountGuid, "accountGuid");
	}

	@Override
	public String getSql() {
		return """
				WITH owner_lots AS (
					SELECT l.guid AS lot_guid
						, NULL::VARCHAR AS doc_guid
						, NULL::VARCHAR AS doc_id
						, NULL::BIGINT AS doc_credit_note
					FROM {schema}.lots l
					INNER JOIN {schema}.slots f ON f.obj_guid = l.guid
						AND f.name = 'gncOwner'
					INNER JOIN {schema}.slots o ON o.obj_guid = f.guid_val
						AND o.name = 'gncOwner/owner-guid'
					WHERE l.account_guid = ? AND o.guid_val = ?
					UNION ALL
					SELECT l.guid AS lot_guid
						, i.guid AS doc_guid
						, i.id AS doc_id
						, COALESCE(cn.int64_val, 0) AS doc_credit_note
					FROM {schema}.lots l
					INNER JOIN {schema}.invoices i ON i.post_lot = l.guid
					LEFT OUTER JOIN {schema}.slots cn ON cn.obj_guid = i.guid
						AND cn.name = 'credit-note'
					WHERE l.account_guid = ? AND i.owner_guid = ?
				)
				SELECT ol.lot_guid, ol.doc_guid, ol.doc_id, ol.doc_credit_note
					, SUM(s.quantity_num::NUMERIC / s.quantity_denom) AS balance
					, MIN(t.post_date) AS first_post
					, MAX(t.post_date) AS last_post
				FROM owner_lots ol
				INNER JOIN {schema}.splits s ON s.lot_guid = ol.lot_guid
				INNER JOIN {schema}.transactions t ON t.guid = s.tx_guid
				GROUP BY ol.lot_guid, ol.doc_guid, ol.doc_id, ol.doc_credit_note
				HAVING SUM(s.quantity_num::NUMERIC / s.quantity_denom) < 0
				ORDER BY first_post, ol.lot_guid
				""".replace("{schema}", schema);
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setString(1, accountGuid);
		stmt.setString(2, ownerGuid);
		stmt.setString(3, accountGuid);
		stmt.setString(4, ownerGuid);
		return stmt;
	}

}
