/* ==================================================================
 * UpdateCloudIntegrationOAuthAuthorizationState.java - 10/03/2025 2:46:27 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.dao.jdbc.sql;

import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_STATE_SETTING;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.util.ObjectUtils;

/**
 * Save an OAuth authorization state value on an integration configuration
 * entity.
 *
 * @author matt
 * @version 1.0
 */
public class UpdateCloudIntegrationOAuthAuthorizationState
		implements PreparedStatementCreator, SqlProvider {

	private final UserLongCompositePK id;
	private final String state;
	private final String expectedState;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param state
	 *        the state value to save, or {@code null} to remove
	 * @param expectedState
	 *        the optional expected state
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@code null}
	 */
	public UpdateCloudIntegrationOAuthAuthorizationState(UserLongCompositePK id, String state,
			String expectedState) {
		this.id = ObjectUtils.requireNonNullArgument(id, "id");
		this.state = state;
		this.expectedState = expectedState;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append("""
				UPDATE solardin.cin_integration
				SET sprops =
				""");
		if ( state == null ) {
			buf.append("sprops - '").append(OAUTH_STATE_SETTING).append("'\n");
		} else {
			buf.append("jsonb_set(COALESCE(sprops, '{}'::jsonb), '{").append(OAUTH_STATE_SETTING)
					.append("}', to_jsonb(?::TEXT), TRUE)\n");
		}
		buf.append("""
				WHERE user_id = ?
				AND id = ?
				""");
		if ( expectedState != null ) {
			buf.append("AND sprops ->> '").append(OAUTH_STATE_SETTING).append("' = ?");
		}
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		if ( state != null ) {
			stmt.setString(++p, state);
		}
		stmt.setObject(++p, id.getUserId());
		stmt.setObject(++p, id.getEntityId());
		if ( expectedState != null ) {
			stmt.setString(++p, expectedState);
		}
		return stmt;
	}

}
