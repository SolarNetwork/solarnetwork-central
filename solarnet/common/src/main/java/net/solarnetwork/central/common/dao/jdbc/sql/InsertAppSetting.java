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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.AppSetting;

/**
 * Insert an {@link AppSetting} instance.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class InsertAppSetting implements PreparedStatementCreator, SqlProvider {

	private final AppSetting setting;
	private final boolean upsert;

	/**
	 * Constructor.
	 * 
	 * @param setting
	 *        the setting to insert
	 * @param upsert
	 *        use an "upsert" style query
	 * @throws IllegalArgumentException
	 *         if {@code setting} is {@literal null}
	 */
	public InsertAppSetting(AppSetting setting, boolean upsert) {
		super();
		this.setting = requireNonNullArgument(setting, "setting");
		this.upsert = upsert;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append("INSERT INTO solarcommon.app_setting (");
		if ( setting.getCreated() != null ) {
			buf.append("created, ");
		}
		if ( setting.getModified() != null ) {
			buf.append("modified, ");
		}
		buf.append("skey, stype, svalue)\n");
		buf.append("VALUES (");
		if ( setting.getCreated() != null ) {
			buf.append("?,");
		}
		if ( setting.getModified() != null ) {
			buf.append("?,");
		}
		buf.append("?,?,?)\n");
		if ( upsert ) {
			buf.append("ON CONFLICT (skey, stype) DO UPDATE\nSET ");
			if ( setting.getModified() != null ) {
				buf.append("modified = EXCLUDED.modified\n\t, ");
			}
			buf.append("svalue = EXCLUDED.svalue");
		}
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		if ( setting.getCreated() != null ) {
			stmt.setTimestamp(++p, Timestamp.from(setting.getCreated()));
		}
		if ( setting.getModified() != null ) {
			stmt.setTimestamp(++p, Timestamp.from(setting.getModified()));
		}
		stmt.setString(++p, setting.getKey());
		stmt.setString(++p, setting.getType());
		stmt.setString(++p, setting.getValue());
		return stmt;
	}

}
