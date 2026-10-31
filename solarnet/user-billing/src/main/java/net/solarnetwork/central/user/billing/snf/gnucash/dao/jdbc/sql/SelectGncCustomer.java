/* ==================================================================
 * SelectGncCustomer.java - 11/10/2026 2:47:17 pm
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

import static net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.GnucashSqlUtils.getBoolean;
import static net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.GnucashSqlUtils.getString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncAddress;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncCustomer;

/**
 * Select GnuCash customers by customer ID.
 *
 * @author matt
 * @version 1.0
 */
public final class SelectGncCustomer implements PreparedStatementCreator, SqlProvider {

	/** A row mapper for the results. */
	public static final RowMapper<GncCustomer> ROW_MAPPER = SelectGncCustomer::mapRow;

	private final String schema;
	private final String id;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param id
	 *        the customer ID
	 */
	public SelectGncCustomer(String schema, String id) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.id = requireNonNullArgument(id, "id");
	}

	@Override
	public String getSql() {
		return """
				SELECT guid, id, name, notes, active, currency, tax_override, tax_included, taxtable
					, addr_name, addr_addr1, addr_addr2, addr_addr3, addr_addr4
					, addr_phone, addr_fax, addr_email
					, shipaddr_name, shipaddr_addr1, shipaddr_addr2, shipaddr_addr3, shipaddr_addr4
					, shipaddr_phone, shipaddr_fax, shipaddr_email
				FROM {schema}.customers
				WHERE id = ?
				ORDER BY guid
				""".replace("{schema}", schema);
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setString(1, id);
		return stmt;
	}

	private static GncAddress address(ResultSet rs, int col) throws SQLException {
		return new GncAddress(getString(rs, col), getString(rs, col + 1), getString(rs, col + 2),
				getString(rs, col + 3), getString(rs, col + 4), getString(rs, col + 5),
				getString(rs, col + 6), getString(rs, col + 7));
	}

	private static GncCustomer mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new GncCustomer(rs.getString(1), rs.getString(2), rs.getString(3), getString(rs, 4),
				getBoolean(rs, 5), rs.getString(6), getBoolean(rs, 7), rs.getInt(8), rs.getString(9),
				address(rs, 10), address(rs, 18));
	}

}
