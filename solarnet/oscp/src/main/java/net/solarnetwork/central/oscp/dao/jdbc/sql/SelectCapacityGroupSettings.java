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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;

/**
 * Select for capacity group settings rows.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectCapacityGroupSettings implements PreparedStatementCreator, SqlProvider {

	private final Long userId;
	private final Long groupId;
	private final String groupIdentifier;
	private final boolean resolve;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the ID of the user to select
	 * @param groupId
	 *        the capacity group ID
	 */
	public SelectCapacityGroupSettings(Long userId, Long groupId) {
		this(userId, groupId, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the ID of the user to select
	 * @param groupId
	 *        the capacity group ID
	 * @param resolve
	 *        {@literal true} to resolve user settings as defaults when no group
	 *        settings are available
	 * @throws IllegalArgumentException
	 *         if {@code userId} is {@literal null}
	 */
	public SelectCapacityGroupSettings(Long userId, Long groupId, boolean resolve) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.groupId = groupId;
		this.groupIdentifier = null;
		this.resolve = resolve;
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the ID of the user to select
	 * @param groupIdentifier
	 *        the capacity group identifier
	 */
	public SelectCapacityGroupSettings(Long userId, String groupIdentifier) {
		this(userId, groupIdentifier, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the ID of the user to select
	 * @param groupIdentifier
	 *        the capacity group identifier
	 * @param resolve
	 *        {@literal true} to resolve user settings as defaults when no group
	 *        settings are available
	 * @throws IllegalArgumentException
	 *         if {@code userId} is {@literal null}
	 */
	public SelectCapacityGroupSettings(Long userId, String groupIdentifier, boolean resolve) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.groupId = null;
		this.groupIdentifier = groupIdentifier;
		this.resolve = resolve;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		if ( resolve ) {
			buf.append("""
					SELECT
						  cg.user_id
						, cg.id AS cg_id
						, COALESCE(gs.created, us.created) AS created
						, COALESCE(gs.modified, us.modified) AS modified
						, COALESCE(gs.pub_in, us.pub_in) AS pub_in
						, COALESCE(gs.pub_flux, us.pub_flux) AS pub_flux
						, COALESCE(gs.node_id, us.node_id) AS node_id
						, COALESCE(gs.source_id_tmpl, us.source_id_tmpl) AS source_id_tmpl
					FROM solaroscp.oscp_cg_conf cg
					LEFT OUTER JOIN solaroscp.oscp_user_settings us ON us.user_id = cg.user_id
					LEFT OUTER JOIN solaroscp.oscp_cg_settings gs
						ON gs.user_id = cg.user_id AND gs.cg_id = cg.id
					WHERE cg.user_id = ? AND COALESCE(gs.created, us.created) IS NOT NULL
					""");
			if ( groupId != null ) {
				buf.append(" AND cg.id = ?");
			} else if ( groupIdentifier != null ) {
				buf.append(" AND cg.ident = ?");
			}
		} else {
			buf.append("""
					SELECT
						  user_id
						, cg_id
						, created
						, modified
						, pub_in
						, pub_flux
						, node_id
						, source_id_tmpl
					FROM solaroscp.oscp_cg_settings
					WHERE user_id = ?
					""");
			if ( groupId != null ) {
				buf.append(" AND cg_id = ?");
			} else if ( groupIdentifier != null ) {
				buf.append(" AND ident = ?");
			}
		}
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		stmt.setObject(1, userId, Types.BIGINT);
		if ( groupId != null ) {
			stmt.setObject(2, groupId, Types.BIGINT);
		} else if ( groupIdentifier != null ) {
			stmt.setString(2, groupIdentifier);
		}
		return stmt;
	}

}
