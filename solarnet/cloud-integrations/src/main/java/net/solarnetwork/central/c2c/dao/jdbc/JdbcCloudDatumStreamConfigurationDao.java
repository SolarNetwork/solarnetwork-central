/* ==================================================================
 * JdbcCloudDatumStreamConfigurationDao.java - 3/10/2024 1:21:57 pm
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
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamFilter;
import net.solarnetwork.central.c2c.dao.jdbc.sql.InsertCloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.dao.jdbc.sql.SelectCloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.dao.jdbc.sql.UpdateCloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.common.dao.jdbc.sql.UpdateEnabledIdFilter;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link CloudDatumStreamConfigurationDao}.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcCloudDatumStreamConfigurationDao implements CloudDatumStreamConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcCloudDatumStreamConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends CloudDatumStreamConfiguration> getObjectType() {
		return CloudDatumStreamConfiguration.class;
	}

	@Override
	public CloudDatumStreamConfiguration entityKey(UserLongCompositePK id) {
		return new CloudDatumStreamConfiguration(id, now());
	}

	@Override
	public UserLongCompositePK create(Long userId, CloudDatumStreamConfiguration entity) {
		final var sql = new InsertCloudDatumStreamConfiguration(userId, entity);
		final Long id = CommonJdbcUtils.updateWithGeneratedLong(jdbcOps, sql, "id");
		return (id != null ? new UserLongCompositePK(userId, id) : null);
	}

	@Override
	public Collection<CloudDatumStreamConfiguration> findAll(Long userId, List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectCloudDatumStreamConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CloudDatumStreamConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> findFiltered(
			CloudDatumStreamFilter filter, List<SortDescriptor> sorts, Long offset, Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectCloudDatumStreamConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql, CloudDatumStreamConfigurationRowMapper.INSTANCE);
	}

	@Override
	public UserLongCompositePK save(CloudDatumStreamConfiguration entity) {
		if ( !entity.getId().entityIdIsAssigned() ) {
			return create(entity.getId().getUserId(), entity);
		}
		final UpdateCloudDatumStreamConfiguration sql = new UpdateCloudDatumStreamConfiguration(
				entity.getId(), entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public CloudDatumStreamConfiguration get(UserLongCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setDatumStreamId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectCloudDatumStreamConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CloudDatumStreamConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<CloudDatumStreamConfiguration> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardin.cin_datum_stream";
	private static final String ID_COLUMN_NAME = "id";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", ID_COLUMN_NAME };

	@Override
	public void delete(CloudDatumStreamConfiguration entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").getId(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	@Override
	public int updateEnabledStatus(Long userId, CloudDatumStreamFilter filter, boolean enabled) {
		UserLongCompositePK key = filter != null && filter.hasDatumStreamCriteria()
				? new UserLongCompositePK(userId, filter.getDatumStreamId())
				: UserLongCompositePK.unassignedEntityIdKey(userId);
		var sql = new UpdateEnabledIdFilter(TABLE_NAME, PK_COLUMN_NAMES, key, enabled);
		return jdbcOps.update(sql);
	}

}
