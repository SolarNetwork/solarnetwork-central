/* ==================================================================
 * SelectAppSetting.java - 10/11/2021 9:28:15 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.sql;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.AppSetting;

/**
 * Delete {@link AppSetting} instances.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class DeleteAppSetting implements PreparedStatementCreator, SqlProvider {

	private final String[] keys;
	private final String[] types;

	/**
	 * Select for a single key.
	 * 
	 * @param key
	 *        the key
	 * @return the select statement
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public static DeleteAppSetting deleteForKey(String key) {
		return new DeleteAppSetting(new String[] { requireNonNullArgument(key, "key") }, null);
	}

	/**
	 * Select for a single key and type.
	 * 
	 * @param key
	 *        the key
	 * @param type
	 *        the type
	 * @return the select statement
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public static DeleteAppSetting deleteForKeyType(String key, String type) {
		return new DeleteAppSetting(new String[] { requireNonNullArgument(key, "key") },
				new String[] { requireNonNullArgument(type, "type") });
	}

	/**
	 * Constructor.
	 * 
	 * @param keys
	 *        the optional keys to filter on
	 * @param types
	 *        the optional types to filter on
	 */
	public DeleteAppSetting(String[] keys, String[] types) {
		super();
		this.keys = keys;
		this.types = types;
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("DELETE FROM solarcommon.app_setting");
		sqlWhere(buf);
	}

	private void sqlWhere(StringBuilder buf) {
		boolean haveWhere = false;
		if ( keys != null && keys.length > 0 ) {
			haveWhere = true;
			buf.append("\nWHERE skey = ");
			if ( keys.length > 1 ) {
				buf.append("ANY(?)");
			} else {
				buf.append("?");
			}
		}
		if ( types != null && types.length > 0 ) {
			if ( !haveWhere ) {
				buf.append("\nWHERE ");
				haveWhere = true;
			} else {
				buf.append("\nAND ");
			}
			buf.append("stype = ");
			if ( types.length > 1 ) {
				buf.append("ANY(?)");
			} else {
				buf.append("?");
			}
		}
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		p = prepareOptimizedArrayParameter(con, stmt, p, keys);
		p = prepareOptimizedArrayParameter(con, stmt, p, types);
		return stmt;
	}

}
