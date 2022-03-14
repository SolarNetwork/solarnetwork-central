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
 * Select for {@link AppSetting} instances.
 * 
 * <p>
 * The result columns in the SQL are:
 * </p>
 * 
 * <ol>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>skey (VARCHAR)</li>
 * <li>stype (VARCHAR)</li>
 * <li>svalue (VARCHAR)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class SelectAppSetting implements PreparedStatementCreator, SqlProvider {

	private final String[] keys;
	private final String[] types;
	private final boolean forUpdate;

	/**
	 * Select for a single key.
	 * 
	 * @param key
	 *        the key
	 * @return the select statement
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public static SelectAppSetting selectForKey(String key) {
		return selectForKey(key, false);
	}

	/**
	 * Select for a single key for update.
	 * 
	 * @param key
	 *        the key
	 * @param forUpdate
	 *        {@literal true} to use "for update" locking semantics
	 * @return the select statement
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public static SelectAppSetting selectForKey(String key, boolean forUpdate) {
		return new SelectAppSetting(new String[] { requireNonNullArgument(key, "key") }, null,
				forUpdate);
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
	public static SelectAppSetting selectForKeyType(String key, String type) {
		return selectForKeyType(key, type, false);
	}

	/**
	 * Select for a single key and type.
	 * 
	 * @param key
	 *        the key
	 * @param type
	 *        the type
	 * @param forUpdate
	 *        {@literal true} to use "for update" locking semantics
	 * @return the select statement
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public static SelectAppSetting selectForKeyType(String key, String type, boolean forUpdate) {
		return new SelectAppSetting(new String[] { requireNonNullArgument(key, "key") },
				new String[] { requireNonNullArgument(type, "type") }, forUpdate);
	}

	/**
	 * Constructor.
	 * 
	 * @param keys
	 *        the optional keys to filter on
	 * @param types
	 *        the optional types to filter on
	 */
	public SelectAppSetting(String[] keys, String[] types) {
		this(keys, types, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param keys
	 *        the optional keys to filter on
	 * @param types
	 *        the optional types to filter on
	 * @param forUpdate
	 *        {@literal true} to use "for update" locking semantics
	 */
	public SelectAppSetting(String[] keys, String[] types, boolean forUpdate) {
		super();
		this.keys = keys;
		this.types = types;
		this.forUpdate = forUpdate;
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("SELECT s.created, s.modified, s.skey, s.stype, s.svalue\n");
		buf.append("FROM solarcommon.app_setting s");
		sqlWhere(buf);
	}

	private void sqlWhere(StringBuilder buf) {
		boolean haveWhere = false;
		if ( keys != null && keys.length > 0 ) {
			haveWhere = true;
			buf.append("\nWHERE s.skey = ");
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
			buf.append("s.stype = ");
			if ( types.length > 1 ) {
				buf.append("ANY(?)");
			} else {
				buf.append("?");
			}
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		if ( keys != null && keys.length == 1 && types != null && types.length == 1 ) {
			// at most one result, skip order
			return;
		}
		buf.append("\nORDER BY s.skey, s.stype");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlOrderBy(buf);
		if ( forUpdate ) {
			CommonSqlUtils.forUpdate(false, buf);
		}
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = CommonSqlUtils.createPreparedStatement(con, getSql(), forUpdate);
		int p = 0;
		p = prepareOptimizedArrayParameter(con, stmt, p, keys);
		p = prepareOptimizedArrayParameter(con, stmt, p, types);
		return stmt;
	}

}
