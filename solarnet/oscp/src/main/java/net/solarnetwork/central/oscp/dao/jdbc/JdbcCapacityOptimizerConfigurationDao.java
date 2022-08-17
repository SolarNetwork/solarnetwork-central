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

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.BasicConfigurationFilter;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.ConfigurationFilter;
import net.solarnetwork.central.oscp.dao.jdbc.sql.DeleteCapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertAuthToken;
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
 * @version 1.0
 */
public class JdbcCapacityOptimizerConfigurationDao implements CapacityOptimizerConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcCapacityOptimizerConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends CapacityOptimizerConfiguration> getObjectType() {
		return CapacityOptimizerConfiguration.class;
	}

	@Override
	public UserLongCompositePK create(Long userId, CapacityOptimizerConfiguration entity) {
		final var sql = new InsertCapacityOptimizerConfiguration(userId, entity);
		final Long id = CommonJdbcUtils.updateWithGeneratedLong(jdbcOps, sql, "id");
		return (id != null ? new UserLongCompositePK(userId, id) : null);
	}

	@Override
	public void saveExternalSystemAuthToken(UserLongCompositePK id, String token) {
		final var sql = new InsertAuthToken(OscpRole.CapacityOptimizer, id, token);
		jdbcOps.execute(sql, (cs) -> {
			cs.execute();
			return null;
		});
	}

	@Override
	public UserLongCompositePK save(CapacityOptimizerConfiguration entity) {
		if ( !entity.getId().entityIdIsAssigned() ) {
			return create(entity.getId().getUserId(), entity);
		}
		final UpdateCapacityOptimizerConfiguration sql = new UpdateCapacityOptimizerConfiguration(
				entity.getId(), entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public Collection<CapacityOptimizerConfiguration> findAll(Long userId, List<SortDescriptor> sorts) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var results = findFiltered(filter);
		return stream(results.spliterator(), false).collect(toList());
	}

	@Override
	public CapacityOptimizerConfiguration get(UserLongCompositePK id) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setConfigurationId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var results = findFiltered(filter);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public FilterResults<CapacityOptimizerConfiguration, UserLongCompositePK> findFiltered(
			ConfigurationFilter filter, List<SortDescriptor> sorts, Integer offset, Integer max) {
		SelectCapacityOptimizerConfiguration sql = new SelectCapacityOptimizerConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql,
				CapacityOptimizerConfigurationRowMapper.INSTANCE);
	}

	@Override
	public Collection<CapacityOptimizerConfiguration> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(CapacityOptimizerConfiguration entity) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(requireNonNullArgument(requireNonNullArgument(entity, "entity").getUserId(),
				"entity.userId"));
		filter.setConfigurationId(requireNonNullArgument(entity.getEntityId(), "entity.entityId"));
		DeleteCapacityOptimizerConfiguration sql = new DeleteCapacityOptimizerConfiguration(filter);
		jdbcOps.update(sql);
	}

}
