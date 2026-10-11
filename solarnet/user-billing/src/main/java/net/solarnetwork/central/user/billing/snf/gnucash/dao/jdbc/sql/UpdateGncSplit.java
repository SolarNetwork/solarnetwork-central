/* ==================================================================
 * UpdateGncSplit.java - 11/10/2026 2:48:41 pm
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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncNumeric;

/**
 * Update a GnuCash split's value, amount, and lot.
 *
 * @author matt
 * @version 1.0
 */
public final class UpdateGncSplit implements PreparedStatementCreator, SqlProvider {

	private final String schema;
	private final String guid;
	private final GncNumeric value;
	private final GncNumeric quantity;
	private final @Nullable String lotGuid;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param guid
	 *        the split GUID
	 * @param value
	 *        the value to set
	 * @param quantity
	 *        the amount to set
	 * @param lotGuid
	 *        the GUID of the lot to move the split to
	 */
	public UpdateGncSplit(String schema, String guid, GncNumeric value, GncNumeric quantity,
			@Nullable String lotGuid) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.guid = requireNonNullArgument(guid, "guid");
		this.value = requireNonNullArgument(value, "value");
		this.quantity = requireNonNullArgument(quantity, "quantity");
		this.lotGuid = lotGuid;
	}

	@Override
	public String getSql() {
		return """
				UPDATE {schema}.splits
				SET value_num = ?, value_denom = ?, quantity_num = ?, quantity_denom = ?, lot_guid = ?
				WHERE guid = ?
				""".replace("{schema}", schema);
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		int p = setNumeric(stmt, 1, value);
		p = setNumeric(stmt, ++p, quantity);
		stmt.setString(++p, lotGuid);
		stmt.setString(++p, guid);
		return stmt;
	}

}
