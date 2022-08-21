/* ==================================================================
 * JdbcExternalSystemSupportDao.java - 21/08/2022 4:12:29 pm
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

import static net.solarnetwork.central.oscp.dao.BasicLockingFilter.ONE_FOR_UPDATE_SKIP;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.ExternalSystemConfigurationDao;
import net.solarnetwork.central.oscp.dao.ExternalSystemSupportDao;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectExternalSystemForHeartbeat;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.UserLongKeyTimestamp;

/**
 * JDBC implementation of {@link ExternalSystemSupportDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcExternalSystemSupportDao implements ExternalSystemSupportDao {

	private final JdbcOperations jdbcOps;
	private final CapacityProviderConfigurationDao capacityProviderDao;
	private final CapacityOptimizerConfigurationDao capacityOptimizerDao;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param capacityProviderDao
	 *        the capacity provider DAO
	 * @param capacityOptimizerDao
	 *        the capacity optimizer DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcExternalSystemSupportDao(JdbcOperations jdbcOps,
			CapacityProviderConfigurationDao capacityProviderDao,
			CapacityOptimizerConfigurationDao capacityOptimizerDao) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
		this.capacityOptimizerDao = requireNonNullArgument(capacityOptimizerDao, "capacityOptimizerDao");
	}

	@Override
	public boolean processExternalSystemWithExpiredHeartbeat(Function<AuthRoleInfo, Instant> handler) {
		requireNonNullArgument(handler, "handler");

		if ( executeQuery(OscpRole.CapacityProvider, capacityProviderDao, handler) ) {
			return true;
		}
		return executeQuery(OscpRole.CapacityOptimizer, capacityOptimizerDao, handler);
	}

	private boolean executeQuery(OscpRole role, ExternalSystemConfigurationDao<?> dao,
			Function<AuthRoleInfo, Instant> handler) {
		PreparedStatementCreator sql = new SelectExternalSystemForHeartbeat(role, ONE_FOR_UPDATE_SKIP);
		List<UserLongKeyTimestamp> rows = jdbcOps.query(sql, UserLongKeyTimestampRowMapper.INSTANCE);
		if ( !rows.isEmpty() ) {
			UserLongKeyTimestamp row = rows.get(0);
			Instant ts = handler.apply(new AuthRoleInfo(row.id(), OscpRole.CapacityProvider));
			if ( ts != null ) {
				dao.compareAndSetHeartbeat(row.id(), row.timestamp(), ts);
			}
			return (ts != null);
		}
		return false;
	}

}
