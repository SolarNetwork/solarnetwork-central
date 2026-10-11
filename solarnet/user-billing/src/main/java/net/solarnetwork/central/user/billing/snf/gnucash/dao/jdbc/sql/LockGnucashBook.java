/* ==================================================================
 * LockGnucashBook.java - 11/10/2026 2:46:24 pm
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;

/**
 * Lock the GnuCash book's lock table, until the current transaction ends.
 *
 * <p>
 * GnuCash claims a book by inserting a row into its {@code gnclock} table,
 * after checking that the table is empty. An exclusive lock on the table makes
 * GnuCash wait for the lock to be released before it can claim the book, but
 * still lets the table be read.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class LockGnucashBook implements PreparedStatementCreator, SqlProvider {

	private final String schema;
	private final int timeoutSecs;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param timeoutSecs
	 *        the maximum number of seconds to wait for the lock, or
	 *        {@literal 0} for no limit
	 */
	public LockGnucashBook(String schema, int timeoutSecs) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.timeoutSecs = timeoutSecs;
	}

	@Override
	public String getSql() {
		return "LOCK TABLE " + schema + ".gnclock IN EXCLUSIVE MODE";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		if ( timeoutSecs > 0 ) {
			stmt.setQueryTimeout(timeoutSecs);
		}
		return stmt;
	}

}
