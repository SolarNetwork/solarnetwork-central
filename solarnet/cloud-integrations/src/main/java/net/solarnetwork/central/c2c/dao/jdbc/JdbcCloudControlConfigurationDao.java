/* ==================================================================
 * JdbcCloudControlConfigurationDao.java - 3/11/2025 8:08:41 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.updateWithGeneratedLong;
import static net.solarnetwork.central.domain.UserLongCompositePK.UNASSIGNED_ENTITY_ID;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.CloudControlConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudControlFilter;
import net.solarnetwork.central.c2c.dao.jdbc.sql.InsertCloudControlConfiguration;
import net.solarnetwork.central.c2c.dao.jdbc.sql.SelectCloudControlConfiguration;
import net.solarnetwork.central.c2c.dao.jdbc.sql.UpdateCloudControlConfiguration;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.common.dao.jdbc.sql.UpdateEnabledIdFilter;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link CloudControlConfigurationDao}.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcCloudControlConfigurationDao implements CloudControlConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcCloudControlConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends CloudControlConfiguration> getObjectType() {
		return CloudControlConfiguration.class;
	}

	@Override
	public CloudControlConfiguration entityKey(UserLongCompositePK id) {
		return new CloudControlConfiguration(id, Instant.EPOCH, "", "", UNASSIGNED_ENTITY_ID,
				UNASSIGNED_ENTITY_ID, "");
	}

	@Override
	public UserLongCompositePK create(Long userId, CloudControlConfiguration entity) {
		final var sql = new InsertCloudControlConfiguration(userId, entity);
		final Long id = nonnull(updateWithGeneratedLong(jdbcOps, sql, "id"), "Generated ID");
		return new UserLongCompositePK(userId, id);
	}

	@Override
	public Collection<CloudControlConfiguration> findAll(Long userId,
			@Nullable List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectCloudControlConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CloudControlConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public FilterResults<CloudControlConfiguration, UserLongCompositePK> findFiltered(
			CloudControlFilter filter, @Nullable List<SortDescriptor> sorts, @Nullable Long offset,
			@Nullable Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectCloudControlConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql, CloudControlConfigurationRowMapper.INSTANCE);
	}

	@Override
	public UserLongCompositePK save(CloudControlConfiguration entity) {
		UserLongCompositePK pk = requireNonNullArgument(entity.getId(), "entity.id");
		if ( !pk.entityIdIsAssigned() ) {
			return create(pk.getUserId(), entity);
		}
		final UpdateCloudControlConfiguration sql = new UpdateCloudControlConfiguration(pk, entity);
		jdbcOps.update(sql);
		return pk;
	}

	@Override
	public @Nullable CloudControlConfiguration get(UserLongCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(id, "id").getUserId());
		filter.setCloudControlId(id.getEntityId());
		var sql = new SelectCloudControlConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CloudControlConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<CloudControlConfiguration> getAll(@Nullable List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardin.cin_control";
	private static final String ID_COLUMN_NAME = "id";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", ID_COLUMN_NAME };

	@Override
	public void delete(CloudControlConfiguration entity) {
		final UserLongCompositePK pk = requireNonNullArgument(
				requireNonNullArgument(entity, "entity").getId(), "entity.id");
		DeleteForCompositeKey sql = new DeleteForCompositeKey(pk, TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	@Override
	public int updateEnabledStatus(Long userId, @Nullable CloudControlFilter filter, boolean enabled) {
		UserLongCompositePK key = filter != null && filter.hasCloudControlCriteria()
				? new UserLongCompositePK(userId, nonnull(filter.getCloudControlId(), "cloudControlId"))
				: UserLongCompositePK.unassignedEntityIdKey(userId);
		var sql = new UpdateEnabledIdFilter(TABLE_NAME, PK_COLUMN_NAMES, key, enabled);
		return jdbcOps.update(sql);
	}

}
