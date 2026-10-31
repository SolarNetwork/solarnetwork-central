/* ==================================================================
 * SelectGncInvoiceRef.java - 11/10/2026 2:47:17 pm
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
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncInvoiceRef;

/**
 * Select references to GnuCash business documents by document ID.
 *
 * @author matt
 * @version 1.0
 */
public final class SelectGncInvoiceRef implements PreparedStatementCreator, SqlProvider {

	/** A row mapper for the results. */
	public static final RowMapper<GncInvoiceRef> ROW_MAPPER = SelectGncInvoiceRef::mapRow;

	private final String schema;
	private final String id;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param id
	 *        the document ID
	 */
	public SelectGncInvoiceRef(String schema, String id) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.id = requireNonNullArgument(id, "id");
	}

	@Override
	public String getSql() {
		return """
				SELECT i.guid, i.id, i.owner_guid, i.post_lot, i.post_acc
					, (SELECT SUM(s.quantity_num::NUMERIC / s.quantity_denom)
						FROM {schema}.splits s
						WHERE s.lot_guid = i.post_lot) AS balance
				FROM {schema}.invoices i
				WHERE i.id = ?
				ORDER BY i.guid
				""".replace("{schema}", schema);
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setString(1, id);
		return stmt;
	}

	private static GncInvoiceRef mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new GncInvoiceRef(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
				rs.getString(5), rs.getBigDecimal(6));
	}

}
