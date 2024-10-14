/* ==================================================================
 * JdbcCloudIntegrationConfigurationDao.java - 1/10/2024 7:56:14 am
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

package net.solarnetwork.central.c2c.dao.jdbc;

import static java.time.Instant.now;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationFilter;
import net.solarnetwork.central.c2c.dao.jdbc.sql.InsertCloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.dao.jdbc.sql.SelectCloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.dao.jdbc.sql.UpdateCloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.common.dao.jdbc.sql.UpdateEnabledIdFilter;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link CloudIntegrationConfigurationDao}.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcCloudIntegrationConfigurationDao implements CloudIntegrationConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcCloudIntegrationConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends CloudIntegrationConfiguration> getObjectType() {
		return CloudIntegrationConfiguration.class;
	}

	@Override
	public CloudIntegrationConfiguration entityKey(UserLongCompositePK id) {
		return new CloudIntegrationConfiguration(id, now());
	}

	@Override
	public UserLongCompositePK create(Long userId, CloudIntegrationConfiguration entity) {
		final var sql = new InsertCloudIntegrationConfiguration(userId, entity);
		final Long id = CommonJdbcUtils.updateWithGeneratedLong(jdbcOps, sql, "id");
		return (id != null ? new UserLongCompositePK(userId, id) : null);
	}

	@Override
	public Collection<CloudIntegrationConfiguration> findAll(Long userId, List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectCloudIntegrationConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CloudIntegrationConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public FilterResults<CloudIntegrationConfiguration, UserLongCompositePK> findFiltered(
			CloudIntegrationFilter filter, List<SortDescriptor> sorts, Integer offset, Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectCloudIntegrationConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql, CloudIntegrationConfigurationRowMapper.INSTANCE);
	}

	@Override
	public UserLongCompositePK save(CloudIntegrationConfiguration entity) {
		if ( !entity.getId().entityIdIsAssigned() ) {
			return create(entity.getId().getUserId(), entity);
		}
		final UpdateCloudIntegrationConfiguration sql = new UpdateCloudIntegrationConfiguration(
				entity.getId(), entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public CloudIntegrationConfiguration get(UserLongCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setIntegrationId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectCloudIntegrationConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CloudIntegrationConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<CloudIntegrationConfiguration> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardin.cin_integration";
	private static final String ID_COLUMN_NAME = "id";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", ID_COLUMN_NAME };

	@Override
	public void delete(CloudIntegrationConfiguration entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").getId(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	@Override
	public int updateEnabledStatus(Long userId, CloudIntegrationFilter filter, boolean enabled) {
		UserLongCompositePK key = filter != null && filter.hasIntegrationCriteria()
				? new UserLongCompositePK(userId, filter.getIntegrationId())
				: UserLongCompositePK.unassignedEntityIdKey(userId);
		var sql = new UpdateEnabledIdFilter(TABLE_NAME, PK_COLUMN_NAMES, key, enabled);
		return jdbcOps.update(sql);
	}

}
