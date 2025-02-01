/* ==================================================================
 * JdbcCapacityProviderConfigurationDao.java - 11/08/2022 11:10:53 am
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
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.BasicConfigurationFilter;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.ConfigurationFilter;
import net.solarnetwork.central.oscp.dao.jdbc.sql.DeleteCapacityProviderConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertCapacityProviderConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectCapacityProviderConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateCapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link JdbcCapacityProviderConfigurationDao}.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcCapacityProviderConfigurationDao
		extends BaseJdbcExternalSystemConfigurationDao<CapacityProviderConfiguration>
		implements CapacityProviderConfigurationDao {

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcCapacityProviderConfigurationDao(JdbcOperations jdbcOps) {
		super(jdbcOps, OscpRole.CapacityProvider, CapacityProviderConfiguration.class);
	}

	@Override
	protected RowMapper<CapacityProviderConfiguration> rowMapperForEntity() {
		return CapacityProviderConfigurationRowMapper.INSTANCE;
	}

	@Override
	protected String[] expiredHeartbeatEventSuccessTags() {
		return CAPACITY_PROVIDER_HEARTBEAT_TAGS;
	}

	@Override
	protected String[] expiredHeartbeatEventErrorTags() {
		return CAPACITY_PROVIDER_HEARTBEAT_ERROR_TAGS;
	}

	@Override
	protected String[] expiredMeasurementEventSuccessTags() {
		return CAPACITY_PROVIDER_MEASUREMENT_TAGS;
	}

	@Override
	protected String[] expiredMeasurementEventErrorTags() {
		return CAPACITY_PROVIDER_MEASUREMENT_ERROR_TAGS;
	}

	@Override
	protected PreparedStatementCreator createSql(Long userId, CapacityProviderConfiguration entity) {
		return new InsertCapacityProviderConfiguration(userId, entity);
	}

	@Override
	public UserLongCompositePK save(CapacityProviderConfiguration entity) {
		if ( !entity.getId().entityIdIsAssigned() ) {
			return create(entity.getId().getUserId(), entity);
		}
		final var sql = new UpdateCapacityProviderConfiguration(entity.getId(), entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public Collection<CapacityProviderConfiguration> findAll(Long userId, List<SortDescriptor> sorts) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var results = findFiltered(filter);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public CapacityProviderConfiguration get(UserLongCompositePK id) {
		BasicConfigurationFilter filter = filterForGet(id);
		var results = findFiltered(filter);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public CapacityProviderConfiguration getForUpdate(UserLongCompositePK id) {
		BasicConfigurationFilter filter = filterForGet(id);
		filter.setLockResults(true);
		var results = findFiltered(filter);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public FilterResults<CapacityProviderConfiguration, UserLongCompositePK> findFiltered(
			ConfigurationFilter filter, List<SortDescriptor> sorts, Long offset, Integer max) {
		var sql = new SelectCapacityProviderConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql, rowMapperForEntity());
	}

	@Override
	public void delete(CapacityProviderConfiguration entity) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(requireNonNullArgument(requireNonNullArgument(entity, "entity").getUserId(),
				"entity.userId"));
		filter.setConfigurationId(requireNonNullArgument(entity.getEntityId(), "entity.entityId"));
		var sql = new DeleteCapacityProviderConfiguration(filter);
		jdbcOps.update(sql);
	}

}
