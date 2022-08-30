/* ==================================================================
 * JdbcCapacityGroupConfigurationDao.java - 14/08/2022 7:33:47 am
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
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.BasicConfigurationFilter;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.sql.DeleteCapacityGroupConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertCapacityGroupConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectCapacityGroupConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateCapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link JdbcCapacityGroupConfigurationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcCapacityGroupConfigurationDao implements CapacityGroupConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcCapacityGroupConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends CapacityGroupConfiguration> getObjectType() {
		return CapacityGroupConfiguration.class;
	}

	@Override
	public UserLongCompositePK create(Long userId, CapacityGroupConfiguration entity) {
		final InsertCapacityGroupConfiguration sql = new InsertCapacityGroupConfiguration(userId,
				entity);
		final Long id = CommonJdbcUtils.updateWithGeneratedLong(jdbcOps, sql, "id");
		return (id != null ? new UserLongCompositePK(userId, id) : null);
	}

	@Override
	public UserLongCompositePK save(CapacityGroupConfiguration entity) {
		if ( !entity.getId().entityIdIsAssigned() ) {
			return create(entity.getId().getUserId(), entity);
		}
		final UpdateCapacityGroupConfiguration sql = new UpdateCapacityGroupConfiguration(entity.getId(),
				entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public Collection<CapacityGroupConfiguration> findAll(Long userId, List<SortDescriptor> sorts) {
		var filter = new BasicConfigurationFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectCapacityGroupConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CapacityGroupConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public CapacityGroupConfiguration get(UserLongCompositePK id) {
		var filter = new BasicConfigurationFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setConfigurationId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectCapacityGroupConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CapacityGroupConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<CapacityGroupConfiguration> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(CapacityGroupConfiguration entity) {
		var filter = new BasicConfigurationFilter();
		filter.setUserId(requireNonNullArgument(requireNonNullArgument(entity, "entity").getUserId(),
				"entity.userId"));
		filter.setConfigurationId(requireNonNullArgument(entity.getEntityId(), "entity.entityId"));
		var sql = new DeleteCapacityGroupConfiguration(filter);
		jdbcOps.update(sql);
	}

	@Override
	public CapacityGroupConfiguration findForCapacityProvider(Long userId, Long capacityProviderId,
			String groupIdentifier) {
		var filter = BasicConfigurationFilter.filterForUsers(requireNonNullArgument(userId, "userId"));
		filter.setProviderId(capacityProviderId);
		filter.setIdentifier(groupIdentifier);
		var sql = new SelectCapacityGroupConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CapacityGroupConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public CapacityGroupConfiguration findForCapacityOptimizer(Long userId, Long capacityOptimizerId,
			String groupIdentifier) {
		var filter = BasicConfigurationFilter.filterForUsers(requireNonNullArgument(userId, "userId"));
		filter.setOptimizerId(capacityOptimizerId);
		filter.setIdentifier(groupIdentifier);
		var sql = new SelectCapacityGroupConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CapacityGroupConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

}
