/* ==================================================================
 * AuthenticatedEndpointCredentialsRowMapper.java - 23/02/2024 2:03:10 pm
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.din.domain.CredentialConfiguration;
import net.solarnetwork.central.din.security.AuthenticatedEndpointCredentials;

/**
 * Row mapper for {@link CredentialConfiguration} entities.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>endpoint_id (UUID)</li>
 * <li>username (TEXT)</li>
 * <li>password (TEXT)</li>
 * <li>enabled (BOOLEAN)</li>
 * <li>expired (BOOLEAN)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class AuthenticatedEndpointCredentialsRowMapper implements RowMapper<AuthenticatedEndpointCredentials> {

	/** A default instance. */
	public static final RowMapper<AuthenticatedEndpointCredentials> INSTANCE = new AuthenticatedEndpointCredentialsRowMapper();

	/**
	 * Default constructor.
	 */
	public AuthenticatedEndpointCredentialsRowMapper() {
		super();
	}

	@Override
	public AuthenticatedEndpointCredentials mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = 0;
		Long userId = rs.getObject(++p, Long.class);
		UUID entityId = CommonJdbcUtils.getUuid(rs, ++p);
		String username = rs.getString(++p);
		String password = rs.getString(++p);
		boolean enabled = rs.getBoolean(++p);
		boolean expired = rs.getBoolean(++p);
		return new AuthenticatedEndpointCredentials(userId, entityId, username, password, enabled,
				expired);
	}

}
