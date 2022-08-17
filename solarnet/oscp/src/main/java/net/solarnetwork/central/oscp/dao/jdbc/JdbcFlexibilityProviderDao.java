/* ==================================================================
 * JdbcFlexibilityProviderDao.java - 16/08/2022 5:45:44 pm
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

package net.solarnetwork.central.oscp.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.FlexibilityProviderDao;
import net.solarnetwork.central.oscp.dao.jdbc.sql.CreateAuthToken;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectAuthRoleInfo;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectAuthTokenId;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateAuthToken;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * JDBC implementation of {@link FlexibilityProviderDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcFlexibilityProviderDao implements FlexibilityProviderDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcFlexibilityProviderDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public String createAuthToken(UserLongCompositePK id) {
		final CallableStatementCreator sql;
		if ( id.entityIdIsAssigned() ) {
			sql = new UpdateAuthToken(OscpRole.FlexibilityProvider, id);
		} else {
			sql = new CreateAuthToken(OscpRole.FlexibilityProvider, id);
		}
		return jdbcOps.execute(sql, (cs) -> {
			cs.execute();
			return cs.getString(1);
		});
	}

	@Override
	public UserLongCompositePK idForToken(String token) {
		final var sql = new SelectAuthTokenId(OscpRole.FlexibilityProvider, token);
		Collection<UserLongCompositePK> results = jdbcOps.query(sql, UserLongKeyRowMapper.INSTANCE);
		return results.stream().findFirst().orElse(null);
	}

	@Override
	public AuthRoleInfo roleForAuthorization(UserLongCompositePK authId) {
		final var sql = new SelectAuthRoleInfo(OscpRole.FlexibilityProvider, authId);
		Collection<AuthRoleInfo> results = jdbcOps.query(sql, AuthRoleInfoRowMapper.INSTANCE);
		return results.stream().findFirst().orElse(null);
	}

}
