/* ==================================================================
 * SelectGncCommodity.java - 11/10/2026 2:46:24 pm
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
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncCommodity;

/**
 * Select a GnuCash commodity by namespace and mnemonic.
 *
 * @author matt
 * @version 1.0
 */
public final class SelectGncCommodity implements PreparedStatementCreator, SqlProvider {

	/** A row mapper for the results. */
	public static final RowMapper<GncCommodity> ROW_MAPPER = SelectGncCommodity::mapRow;

	private final String schema;
	private final String namespace;
	private final String mnemonic;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param namespace
	 *        the namespace, such as {@link GncCommodity#CURRENCY_NAMESPACE}
	 * @param mnemonic
	 *        the mnemonic, such as {@code NZD}
	 */
	public SelectGncCommodity(String schema, String namespace, String mnemonic) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.namespace = requireNonNullArgument(namespace, "namespace");
		this.mnemonic = requireNonNullArgument(mnemonic, "mnemonic");
	}

	@Override
	public String getSql() {
		return "SELECT guid, namespace, mnemonic, fraction FROM " + schema
				+ ".commodities WHERE namespace = ? AND mnemonic = ?";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setString(1, namespace);
		stmt.setString(2, mnemonic);
		return stmt;
	}

	private static GncCommodity mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new GncCommodity(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4));
	}

}
