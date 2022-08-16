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
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.sql.CreateAuthToken;
import net.solarnetwork.central.oscp.dao.jdbc.sql.DeleteCapacityProviderConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertCapacityProviderConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectCapacityProviderConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateCapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link JdbcCapacityProviderConfigurationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcCapacityProviderConfigurationDao implements CapacityProviderConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcCapacityProviderConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends CapacityProviderConfiguration> getObjectType() {
		return CapacityProviderConfiguration.class;
	}

	@Override
	public UserLongCompositePK create(Long userId, CapacityProviderConfiguration entity) {
		final var sql = new InsertCapacityProviderConfiguration(userId, entity);
		final Long id = CommonJdbcUtils.updateWithGeneratedLong(jdbcOps, sql, "id");
		return (id != null ? new UserLongCompositePK(userId, id) : null);
	}

	@Override
	public String createAuthToken(UserLongCompositePK id) {
		final var sql = new CreateAuthToken(CreateAuthToken.TokenType.CapacityProvider, id);
		return jdbcOps.execute(sql, (cs) -> {
			cs.execute();
			return cs.getString(1);
		});
	}

	@Override
	public UserLongCompositePK save(CapacityProviderConfiguration entity) {
		if ( !entity.getId().entityIdIsAssigned() ) {
			return create(entity.getId().getUserId(), entity);
		}
		final UpdateCapacityProviderConfiguration sql = new UpdateCapacityProviderConfiguration(
				entity.getId(), entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public Collection<CapacityProviderConfiguration> findAll(Long userId, List<SortDescriptor> sorts) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		SelectCapacityProviderConfiguration sql = new SelectCapacityProviderConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CapacityProviderConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).collect(toList());
	}

	@Override
	public CapacityProviderConfiguration get(UserLongCompositePK id) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setConfigurationId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		SelectCapacityProviderConfiguration sql = new SelectCapacityProviderConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CapacityProviderConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<CapacityProviderConfiguration> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(CapacityProviderConfiguration entity) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(requireNonNullArgument(requireNonNullArgument(entity, "entity").getUserId(),
				"entity.userId"));
		filter.setConfigurationId(requireNonNullArgument(entity.getEntityId(), "entity.entityId"));
		DeleteCapacityProviderConfiguration sql = new DeleteCapacityProviderConfiguration(filter);
		jdbcOps.update(sql);
	}

}
