/* ==================================================================
 * JdbcCapacityOptimizerConfigurationDao.java - 14/08/2022 7:33:47 am
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

import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.BasicConfigurationFilter;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.ConfigurationFilter;
import net.solarnetwork.central.oscp.dao.jdbc.sql.DeleteCapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertCapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectCapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateCapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link JdbcCapacityOptimizerConfigurationDao}.
 *
 * @author matt
 * @version 1.2
 */
public class JdbcCapacityOptimizerConfigurationDao
		extends BaseJdbcExternalSystemConfigurationDao<CapacityOptimizerConfiguration>
		implements CapacityOptimizerConfigurationDao {

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcCapacityOptimizerConfigurationDao(JdbcOperations jdbcOps) {
		super(jdbcOps, OscpRole.CapacityOptimizer, CapacityOptimizerConfiguration.class);
	}

	@Override
	protected RowMapper<CapacityOptimizerConfiguration> rowMapperForEntity() {
		return CapacityOptimizerConfigurationRowMapper.INSTANCE;
	}

	@Override
	protected List<String> expiredHeartbeatEventSuccessTags() {
		return CAPACITY_OPTIMIZER_HEARTBEAT_TAGS;
	}

	@Override
	protected List<String> expiredHeartbeatEventErrorTags() {
		return CAPACITY_OPTIMIZER_HEARTBEAT_ERROR_TAGS;
	}

	@Override
	protected List<String> expiredMeasurementEventSuccessTags() {
		return CAPACITY_OPTIMIZER_MEASUREMENT_TAGS;
	}

	@Override
	protected List<String> expiredMeasurementEventErrorTags() {
		return CAPACITY_OPTIMIZER_MEASUREMENT_ERROR_TAGS;
	}

	@Override
	protected PreparedStatementCreator createSql(Long userId, CapacityOptimizerConfiguration entity) {
		return new InsertCapacityOptimizerConfiguration(userId, entity);
	}

	@Override
	public UserLongCompositePK save(CapacityOptimizerConfiguration entity) {
		final var id = requireNonNullArgument(requireNonNullArgument(entity, "entity").getId(),
				"entity.id");
		if ( !id.entityIdIsAssigned() ) {
			return create(id.getUserId(), entity);
		}
		final var sql = new UpdateCapacityOptimizerConfiguration(id, entity);
		jdbcOps.update(sql);
		return id;
	}

	@Override
	public Collection<CapacityOptimizerConfiguration> findAll(Long userId,
			@Nullable List<SortDescriptor> sorts) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var results = findFiltered(filter);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public @Nullable CapacityOptimizerConfiguration get(UserLongCompositePK id) {
		BasicConfigurationFilter filter = filterForGet(id);
		var results = findFiltered(filter);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public @Nullable CapacityOptimizerConfiguration getForUpdate(UserLongCompositePK id) {
		BasicConfigurationFilter filter = filterForGet(id);
		filter.setLockResults(true);
		var results = findFiltered(filter);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public FilterResults<CapacityOptimizerConfiguration, UserLongCompositePK> findFiltered(
			ConfigurationFilter filter, @Nullable List<SortDescriptor> sorts, @Nullable Long offset,
			@Nullable Integer max) {
		var sql = new SelectCapacityOptimizerConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql, rowMapperForEntity());
	}

	@Override
	public void delete(CapacityOptimizerConfiguration entity) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(requireNonNullArgument(requireNonNullArgument(entity, "entity").getUserId(),
				"entity.userId"));
		filter.setConfigurationId(requireNonNullArgument(entity.getEntityId(), "entity.entityId"));
		var sql = new DeleteCapacityOptimizerConfiguration(filter);
		jdbcOps.update(sql);
	}

}
