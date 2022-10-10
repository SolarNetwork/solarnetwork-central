/* ==================================================================
 * SelectUserSettings.java - 10/10/2022 9:04:19 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao.jdbc.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.util.ObjectUtils;

/**
 * Select for user settings rows.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectUserSettings implements PreparedStatementCreator, SqlProvider {

	private final Long userId;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the ID of the user to select
	 */
	public SelectUserSettings(Long userId) {
		super();
		this.userId = ObjectUtils.requireNonNullArgument(userId, "userId");
	}

	@Override
	public String getSql() {
		return """
				SELECT user_id,created,modified,pub_in,pub_flux,node_id,source_id_tmpl
				FROM solaroscp.oscp_user_settings
				WHERE user_id = ?
				""";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		stmt.setObject(1, userId, Types.BIGINT);
		return stmt;
	}

}
