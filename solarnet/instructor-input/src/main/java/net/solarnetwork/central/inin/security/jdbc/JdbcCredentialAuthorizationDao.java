/* ==================================================================
 * JdbcCredentialAuthorizationDao.java - 28/03/2024 3:21:47 pm
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
import java.util.UUID;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.inin.security.CredentialAuthorizationDao;
import net.solarnetwork.central.inin.security.EndpointUserDetails;

/**
 * JDBC implementation of {@link CredentialAuthorizationDao}.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcCredentialAuthorizationDao implements CredentialAuthorizationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcCredentialAuthorizationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public EndpointUserDetails credentialsForEndpoint(UUID endpointId, String username, boolean oauth) {
		var sql = new SelectAuthenticatedEndpointCredentials(endpointId, username, oauth);
		var results = jdbcOps.query(sql, AuthenticatedEndpointCredentialsRowMapper.INSTANCE);
		return (!results.isEmpty() ? results.get(0) : null);
	}

}
