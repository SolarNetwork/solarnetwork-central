/* ==================================================================
 * SelectAuthenticatedEndpointCredentials.java - 23/02/2024 1:43:20 pm
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

package net.solarnetwork.central.din.security.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.din.domain.CredentialConfiguration;

/**
 * Select {@link CredentialConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public class SelectAuthenticatedEndpointCredentials implements PreparedStatementCreator, SqlProvider {

	private final UUID endpointId;
	private final String username;

	/**
	 * Constructor.
	 *
	 * @param endpointId
	 *        the endpoint ID
	 * @param username
	 *        the username
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SelectAuthenticatedEndpointCredentials(UUID endpointId, String username) {
		super();
		this.endpointId = requireNonNullArgument(endpointId, "endpointId");
		this.username = requireNonNullArgument(username, "username");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder(512);
		buf.append("""
				SELECT dc.user_id
					, deac.endpoint_id
					, dc.username
					, dc.password
					, CASE
						WHEN deac.enabled AND de.enabled AND dc.enabled THEN TRUE
						ELSE FALSE
						END AS enabled
					, CASE
						WHEN dc.expires < CURRENT_TIMESTAMP THEN TRUE
						ELSE FALSE
						END AS expired
				FROM solardin.din_endpoint_auth_cred deac
				INNER JOIN solardin.din_endpoint de ON de.user_id = deac.user_id
					AND de.id = deac.endpoint_id
				INNER JOIN solardin.din_credential dc ON dc.user_id = deac.user_id
				WHERE deac.endpoint_id = ? AND dc.username = ?
				""");
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		stmt.setObject(1, endpointId);
		stmt.setString(2, username);
		return stmt;
	}

}
