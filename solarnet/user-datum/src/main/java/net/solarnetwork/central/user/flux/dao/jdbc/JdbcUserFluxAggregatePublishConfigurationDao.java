/* ==================================================================
 * JdbcUserFluxAggregatePublishConfigurationDao.java - 24/06/2024 9:10:58 am
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

package net.solarnetwork.central.user.flux.dao.jdbc;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettings;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettingsInfo;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.flux.dao.UserFluxAggregatePublishConfigurationDao;
import net.solarnetwork.central.user.flux.dao.UserFluxAggregatePublishConfigurationFilter;
import net.solarnetwork.central.user.flux.dao.jdbc.sql.SelectPublishConfigurationForNodeSource;
import net.solarnetwork.central.user.flux.dao.jdbc.sql.SelectUserFluxAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.dao.jdbc.sql.UpsertUserFluxAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfiguration;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.util.ObjectUtils;

/**
 * JDBC implementation of {@link UserFluxAggregatePublishConfigurationDao}.
 * 
 * @author matt
 * @version 1.1
 */
public class JdbcUserFluxAggregatePublishConfigurationDao
		implements UserFluxAggregatePublishConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcUserFluxAggregatePublishConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends UserFluxAggregatePublishConfiguration> getObjectType() {
		return UserFluxAggregatePublishConfiguration.class;
	}

	@Override
	public UserLongCompositePK create(Long userId, UserFluxAggregatePublishConfiguration entity) {
		final UpsertUserFluxAggregatePublishConfiguration sql = new UpsertUserFluxAggregatePublishConfiguration(
				userId, entity);
		final Long id = CommonJdbcUtils.updateWithGeneratedLong(jdbcOps, sql, "id");
		return (id != null ? new UserLongCompositePK(userId, id) : null);
	}

	@Override
	public Collection<UserFluxAggregatePublishConfiguration> findAll(Long keyComponent1,
			List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public UserFluxAggregatePublishConfiguration entityKey(UserLongCompositePK id) {
		return new UserFluxAggregatePublishConfiguration(id, Instant.now());
	}

	@Override
	public UserLongCompositePK save(UserFluxAggregatePublishConfiguration entity) {
		if ( !ObjectUtils.requireNonNullArgument(entity, "entity").getId().userIdIsAssigned() ) {
			throw new IllegalArgumentException("The user ID must be assigned.");
		}
		return create(entity.getUserId(), entity);
	}

	@Override
	public UserFluxAggregatePublishConfiguration get(UserLongCompositePK id) {
		var sql = new SelectUserFluxAggregatePublishConfiguration(id);
		List<UserFluxAggregatePublishConfiguration> results = jdbcOps.query(sql,
				UserFluxAggregatePublishConfigurationRowMapper.INSTANCE);
		return (!results.isEmpty() ? results.get(0) : null);
	}

	@Override
	public Collection<UserFluxAggregatePublishConfiguration> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solaruser.user_flux_agg_pub_settings";
	private static final String[] PRIMARY_KEY_COLUMN_NAMES = new String[] { "user_id", "id" };

	@Override
	public void delete(UserFluxAggregatePublishConfiguration entity) {
		var sql = new DeleteForCompositeKey(requireNonNullArgument(entity, "entity").getId(), TABLE_NAME,
				PRIMARY_KEY_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	@Override
	public FluxPublishSettings nodeSourcePublishConfiguration(Long userId, Long nodeId,
			String sourceId) {
		final var sql = SelectPublishConfigurationForNodeSource.forUserNodeSource(userId, nodeId,
				sourceId);
		final List<FluxPublishSettings> results = jdbcOps.query(sql,
				FluxPublishSettingsInfoRowMapper.INSTANCE);
		return (!results.isEmpty() ? results.get(0) : FluxPublishSettingsInfo.NOT_PUBLISHED);
	}

	@Override
	public FilterResults<UserFluxAggregatePublishConfiguration, UserLongCompositePK> findFiltered(
			UserFluxAggregatePublishConfigurationFilter filter, List<SortDescriptor> sorts, Long offset,
			Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectUserFluxAggregatePublishConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql,
				UserFluxAggregatePublishConfigurationRowMapper.INSTANCE);
	}

}
