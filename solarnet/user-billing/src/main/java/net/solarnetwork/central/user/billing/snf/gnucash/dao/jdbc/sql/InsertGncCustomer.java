/* ==================================================================
 * InsertGncCustomer.java - 11/10/2026 2:47:17 pm
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
import java.sql.Types;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncAddress;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncCustomer;

/**
 * Insert a GnuCash customer.
 *
 * <p>
 * The customer has no discount, credit limit, or billing terms.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class InsertGncCustomer implements PreparedStatementCreator, SqlProvider {

	private final String schema;
	private final GncCustomer customer;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param customer
	 *        the customer to insert
	 */
	public InsertGncCustomer(String schema, GncCustomer customer) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.customer = requireNonNullArgument(customer, "customer");
	}

	@Override
	public String getSql() {
		return """
				INSERT INTO {schema}.customers (guid, name, id, notes, active
					, discount_num, discount_denom, credit_num, credit_denom
					, currency, tax_override
					, addr_name, addr_addr1, addr_addr2, addr_addr3, addr_addr4
					, addr_phone, addr_fax, addr_email
					, shipaddr_name, shipaddr_addr1, shipaddr_addr2, shipaddr_addr3, shipaddr_addr4
					, shipaddr_phone, shipaddr_fax, shipaddr_email
					, terms, tax_included, taxtable)
				VALUES (?, ?, ?, ?, ?, 0, 1, 0, 1, ?, ?
					, ?, ?, ?, ?, ?, ?, ?, ?
					, ?, ?, ?, ?, ?, ?, ?, ?
					, NULL, ?, ?)
				""".replace("{schema}", schema);
	}

	private static int setAddress(PreparedStatement stmt, int p, GncAddress addr) throws SQLException {
		stmt.setString(++p, addr.name());
		stmt.setString(++p, addr.addr1());
		stmt.setString(++p, addr.addr2());
		stmt.setString(++p, addr.addr3());
		stmt.setString(++p, addr.addr4());
		stmt.setString(++p, addr.phone());
		stmt.setString(++p, addr.fax());
		stmt.setString(++p, addr.email());
		return p;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		stmt.setString(++p, customer.guid());
		stmt.setString(++p, customer.name());
		stmt.setString(++p, customer.id());
		stmt.setString(++p, customer.notes());
		stmt.setInt(++p, customer.active() ? 1 : 0);
		stmt.setString(++p, customer.currencyGuid());
		stmt.setInt(++p, customer.taxOverride() ? 1 : 0);
		p = setAddress(stmt, p, customer.address());
		p = setAddress(stmt, p, customer.shipAddress());
		stmt.setInt(++p, customer.taxIncluded());
		if ( customer.taxTableGuid() != null ) {
			stmt.setString(++p, customer.taxTableGuid());
		} else {
			stmt.setNull(++p, Types.VARCHAR);
		}
		return stmt;
	}

}
