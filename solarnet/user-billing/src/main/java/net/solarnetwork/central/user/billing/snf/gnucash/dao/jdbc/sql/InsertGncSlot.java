/* ==================================================================
 * InsertGncSlot.java - 11/10/2026 2:48:05 pm
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
import java.sql.Types;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncSlot;

/**
 * Insert a GnuCash slot.
 *
 * @author matt
 * @version 1.0
 */
public final class InsertGncSlot implements PreparedStatementCreator, SqlProvider {

	private final String schema;
	private final GncSlot slot;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param slot
	 *        the slot to insert
	 */
	public InsertGncSlot(String schema, GncSlot slot) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.slot = requireNonNullArgument(slot, "slot");
	}

	@Override
	public String getSql() {
		return """
				INSERT INTO {schema}.slots (obj_guid, name, slot_type, int64_val, string_val
					, double_val, timespec_val, guid_val, numeric_val_num, numeric_val_denom
					, gdate_val)
				VALUES (?, ?, ?, ?, ?, NULL, ?, ?, 0, 1, ?)
				""".replace("{schema}", schema);
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		stmt.setString(++p, slot.objGuid());
		stmt.setString(++p, slot.name());
		stmt.setInt(++p, slot.type());
		stmt.setLong(++p, slot.int64Value());
		stmt.setString(++p, slot.stringValue());
		setTimestamp(stmt, ++p, slot.timespecValue());
		stmt.setString(++p, slot.guidValue());
		if ( slot.gdateValue() != null ) {
			stmt.setObject(++p, slot.gdateValue());
		} else {
			stmt.setNull(++p, Types.DATE);
		}
		return stmt;
	}

}
