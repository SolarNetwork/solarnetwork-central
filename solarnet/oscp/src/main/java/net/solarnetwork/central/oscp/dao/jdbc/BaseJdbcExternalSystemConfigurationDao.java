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

import static net.solarnetwork.central.oscp.dao.BasicLockingFilter.ONE_FOR_UPDATE_SKIP;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.common.dao.jdbc.ColumnCountProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.BasicConfigurationFilter;
import net.solarnetwork.central.oscp.dao.ExternalSystemConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertAuthToken;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertHeartbeatDate;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectAuthToken;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectExternalSystemForHeartbeat;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectExternalSystemForMeasurement;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateCapacityGroupMeasurementDate;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateHeartbeatDate;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateOfflineDate;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateSystemSettings;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.ExternalSystemAndGroup;
import net.solarnetwork.central.oscp.domain.MeasurementPeriod;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.central.oscp.util.CapacityGroupSystemTaskContext;
import net.solarnetwork.central.oscp.util.CapacityGroupTaskContext;
import net.solarnetwork.central.oscp.util.SystemTaskContext;
import net.solarnetwork.central.oscp.util.TaskContext;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Base implementation of ExternalSystemConfigurationDao.
 *
 * @param <C>
 *        the configuration type
 * @author matt
 * @version 1.1
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
	public boolean compareAndSetMeasurement(UserLongCompositePK groupId, Instant expected, Instant ts) {
		int count = jdbcOps.update(new UpdateCapacityGroupMeasurementDate(role, groupId, expected, ts));
		return (count > 0);
	}

	@Override
	public void updateOfflineDate(UserLongCompositePK id, Instant ts) {
		jdbcOps.update(new UpdateOfflineDate(role, id, ts));
	}

	/**
	 * Get the {@link RowMapper} to use for mapping full entity result objects.
	 *
	 * @return the mapper, never {@literal null}
	 */
	protected abstract RowMapper<C> rowMapperForEntity();

	/**
	 * Get the success event tags to use within
	 * {@link #processExternalSystemWithExpiredHeartbeat(Function)}.
	 *
	 * @return the tags, never {@literal null}
	 */
	protected abstract List<String> expiredHeartbeatEventSuccessTags();

	/**
	 * Get the error event tags to use within
	 * {@link #processExternalSystemWithExpiredHeartbeat(Function)}.
	 *
	 * @return the tags, never {@literal null}
	 */
	protected abstract List<String> expiredHeartbeatEventErrorTags();

	/**
	 * Get the success event tags to use within
	 * {@link #processExternalSystemWithExpiredMeasurement(Function)}.
	 *
	 * @return the tags, never {@literal null}
	 */
	protected abstract List<String> expiredMeasurementEventSuccessTags();

	/**
	 * Get the error event tags to use within
	 * {@link #processExternalSystemWithExpiredMeasurement(Function)}.
	 *
	 * @return the tags, never {@literal null}
	 */
	protected abstract List<String> expiredMeasurementEventErrorTags();

	@Override
	public boolean processExternalSystemWithExpiredHeartbeat(Function<TaskContext<C>, Instant> handler) {
		PreparedStatementCreator sql = new SelectExternalSystemForHeartbeat(role, ONE_FOR_UPDATE_SKIP);
		List<C> rows = jdbcOps.query(sql, rowMapperForEntity());
		if ( !rows.isEmpty() ) {
			C row = rows.getFirst();
			SystemTaskContext<C> context = new SystemTaskContext<>("Heartbeat", role, row,
					expiredHeartbeatEventErrorTags(), expiredHeartbeatEventSuccessTags(), this,
					Collections.emptyMap());
			Instant ts = handler.apply(context);
			if ( ts != null ) {
				compareAndSetHeartbeat(row.getId(), row.getHeartbeatDate(), ts);
			}
			return (ts != null);
		}
		return false;
	}

	@Override
	public boolean processExternalSystemWithExpiredMeasurement(
			Function<CapacityGroupTaskContext<C>, Instant> handler) {
		PreparedStatementCreator sql = new SelectExternalSystemForMeasurement(role, ONE_FOR_UPDATE_SKIP);
		RowMapper<C> entityMapper = rowMapperForEntity();
		if ( !(entityMapper instanceof ColumnCountProvider ccp) ) {
			throw new RuntimeException(
					"The configured entity RowMapper must implement ColumnCountProvider: "
							+ entityMapper);
		}
		List<ExternalSystemAndGroup<C>> rows = jdbcOps.query(sql,
				new ExternalSystemAndGroupConfigurationRowMapper<>(rowMapperForEntity(),
						new CapacityGroupConfigurationRowMapper(ccp.getColumnCount())));
		if ( !rows.isEmpty() ) {
			ExternalSystemAndGroup<C> row = rows.getFirst();
			Instant measurementDate = (role == OscpRole.CapacityProvider
					? row.group().getCapacityProviderMeasurementDate()
					: row.group().getCapacityOptimizerMeasurementDate());
			Instant taskDate;
			if ( measurementDate != null ) {
				taskDate = measurementDate;
			} else {
				// no previous task date; set the date to the start of the _previous_ period
				MeasurementPeriod p = (role == OscpRole.CapacityProvider
						? row.group().getCapacityProviderMeasurementPeriod()
						: row.group().getCapacityOptimizerMeasurementPeriod());
				taskDate = p.previousPeriodStart(Instant.now());
			}
			CapacityGroupSystemTaskContext<C> context = new CapacityGroupSystemTaskContext<>(
					"Measurement", role, row.conf(), row.group(), taskDate,
					expiredMeasurementEventErrorTags(), expiredMeasurementEventSuccessTags(), this,
					Collections.emptyMap());
			Instant ts = handler.apply(context);
			if ( ts != null ) {
				compareAndSetMeasurement(row.group().getId(), measurementDate, ts);
			}
			return (ts != null);
		}
		return false;
	}

}
