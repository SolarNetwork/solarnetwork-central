/* ==================================================================
 * SelectGncSlotObjects.java - 14/10/2026 10:21:37 am
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
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncSlot;

/**
 * Select the GUIDs of the GnuCash objects that have a string slot with a
 * given value.
 *
 * <p>
 * The result column is the object GUID.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectGncSlotObjects implements PreparedStatementCreator, SqlProvider {

	private final String schema;
	private final String name;
	private final String value;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param name
	 *        the slot name
	 * @param value
	 *        the slot value
	 */
	public SelectGncSlotObjects(String schema, String name, String value) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.name = requireNonNullArgument(name, "name");
		this.value = requireNonNullArgument(value, "value");
	}

	@Override
	public String getSql() {
		return "SELECT obj_guid FROM " + schema
				+ ".slots WHERE name = ? AND slot_type = ? AND string_val = ? ORDER BY obj_guid";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setString(1, name);
		stmt.setInt(2, GncSlot.TYPE_STRING);
		stmt.setString(3, value);
		return stmt;
	}

}
