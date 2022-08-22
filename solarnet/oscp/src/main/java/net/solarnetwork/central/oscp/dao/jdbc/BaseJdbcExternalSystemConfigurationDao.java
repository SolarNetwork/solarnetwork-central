/* ==================================================================
 * BaseJdbcExternalSystemConfigurationDao.java - 18/08/2022 3:35:37 pm
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
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.BasicConfigurationFilter;
import net.solarnetwork.central.oscp.dao.ExternalSystemConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertAuthToken;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertHeartbeatDate;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectAuthToken;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateHeartbeatDate;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateOfflineDate;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateSystemSettings;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Base implementation of ExternalSystemConfigurationDao.
 * 
 * @param <C>
 *        the configuration type
 * @author matt
 * @version 1.0
 */
public abstract class BaseJdbcExternalSystemConfigurationDao<C extends BaseOscpExternalSystemConfiguration<C>>
		implements ExternalSystemConfigurationDao<C> {

	/** The JDBC template to use. */
	protected final JdbcOperations jdbcOps;

	/** The role supported by the DAO. */
	protected final OscpRole role;

	protected final Class<C> clazz;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param role
	 *        the role
	 * @param clazz
	 *        the configuration class
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseJdbcExternalSystemConfigurationDao(JdbcOperations jdbcOps, OscpRole role,
			Class<C> clazz) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
		this.role = requireNonNullArgument(role, "role");
		this.clazz = requireNonNullArgument(clazz, "clazz");
	}

	@Override
	public Class<? extends C> getObjectType() {
		return clazz;
	}

	/**
	 * Create a new filter instance for a specific configuration entity.
	 * 
	 * @param configId
	 *        the configuration ID
	 * @return the filter
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	protected BasicConfigurationFilter filterForGet(UserLongCompositePK configId) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(requireNonNullArgument(requireNonNullArgument(configId, "configId").getUserId(),
				"configId.userId"));
		filter.setConfigurationId(requireNonNullArgument(configId.getEntityId(), "configId.entityId"));
		return filter;
	}

	@Override
	public final UserLongCompositePK create(Long userId, C entity) {
		final var sql = createSql(userId, entity);
		final Long id = CommonJdbcUtils.updateWithGeneratedLong(jdbcOps, sql, "id");
		if ( id == null ) {
			return null;
		}
		UserLongCompositePK pk = new UserLongCompositePK(userId, id);
		// make sure heartbeat row created at same time
		jdbcOps.update(new InsertHeartbeatDate(role, pk, null));
		return pk;
	}

	/**
	 * Create the SQL to create a new entity.
	 * 
	 * @param userId
	 *        the user ID
	 * @param entity
	 *        the new entity to insert
	 * @return the SQL
	 */
	protected abstract PreparedStatementCreator createSql(Long userId, C entity);

	@Override
	public Collection<C> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveSettings(UserLongCompositePK id, SystemSettings settings) {
		final var sql = new UpdateSystemSettings(role, id, settings);
		jdbcOps.update(sql);
	}

	@Override
	public void saveExternalSystemAuthToken(UserLongCompositePK id, String token) {
		final var sql = new InsertAuthToken(role, id, token);
		jdbcOps.execute(sql, (cs) -> {
			cs.execute();
			return null;
		});
	}

	@Override
	public String getExternalSystemAuthToken(UserLongCompositePK configurationId) {
		var sql = new SelectAuthToken(role, configurationId);
		return jdbcOps.execute(sql, (stmt) -> {
			stmt.execute();
			return stmt.getString(1);
		});
	}

	@Override
	public boolean compareAndSetHeartbeat(UserLongCompositePK id, Instant expected, Instant ts) {
		int count = jdbcOps.update(new UpdateHeartbeatDate(role, id, expected, ts));
		return (count > 0);
	}

	@Override
	public void updateOfflineDate(UserLongCompositePK id, Instant ts) {
		jdbcOps.update(new UpdateOfflineDate(role, id, ts));
	}

}
