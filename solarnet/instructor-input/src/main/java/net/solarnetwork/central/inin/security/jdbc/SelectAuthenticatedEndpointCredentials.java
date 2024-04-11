/* ==================================================================
 * SelectAuthenticatedEndpointCredentials.java - 28/03/2024 3:22:06 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.inin.security.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.inin.domain.CredentialConfiguration;

/**
 * Select {@link CredentialConfiguration} entities.
 *
 * @author matt
 * @version 1.1
 */
public class SelectAuthenticatedEndpointCredentials implements PreparedStatementCreator, SqlProvider {

	private final UUID endpointId;
	private final String username;
	private final boolean oauth;

	/**
	 * Constructor.
	 *
	 * @param endpointId
	 *        the endpoint ID
	 * @param username
	 *        the username
	 * @param oauth
	 *        {@literal true} to lookup OAuth credentials, {@literal false} for
	 *        non-OAuth credentials
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SelectAuthenticatedEndpointCredentials(UUID endpointId, String username, boolean oauth) {
		super();
		this.endpointId = requireNonNullArgument(endpointId, "endpointId");
		this.username = requireNonNullArgument(username, "username");
		this.oauth = oauth;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder(512);
		buf.append("""
				SELECT ic.user_id
					, ieac.endpoint_id
					, ic.username
					, ic.password
					, CASE
						WHEN ieac.enabled AND ie.enabled AND ic.enabled THEN TRUE
						ELSE FALSE
						END AS enabled
					, CASE
						WHEN ic.expires < CURRENT_TIMESTAMP THEN TRUE
						ELSE FALSE
						END AS expired
					, ic.oauth
				FROM solardin.inin_endpoint_auth_cred ieac
				INNER JOIN solardin.inin_endpoint ie ON ie.user_id = ieac.user_id
					AND ie.id = ieac.endpoint_id
				INNER JOIN solardin.inin_credential ic ON ic.user_id = ieac.user_id
				WHERE ieac.endpoint_id = ? AND ic.username = ? AND ic.oauth = ?
				""");
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		stmt.setObject(1, endpointId);
		stmt.setString(2, username);
		stmt.setBoolean(3, oauth);
		return stmt;
	}

}
