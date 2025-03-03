/* ==================================================================
 * JdbcCloudDatumStreamSettingsEntityDao.java - 28/10/2024 10:06:28 am
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
import net.solarnetwork.central.c2c.dao.CloudDatumStreamSettingsEntityDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamSettingsFilter;
import net.solarnetwork.central.c2c.dao.jdbc.sql.SelectCloudDatumStreamSettingsEntity;
import net.solarnetwork.central.c2c.dao.jdbc.sql.UpsertCloudDatumStreamSettingsEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettings;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettingsEntity;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link CloudDatumStreamSettingsEntityDao}.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcCloudDatumStreamSettingsEntityDao implements CloudDatumStreamSettingsEntityDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcCloudDatumStreamSettingsEntityDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends CloudDatumStreamSettingsEntity> getObjectType() {
		return CloudDatumStreamSettingsEntity.class;
	}

	@Override
	public CloudDatumStreamSettingsEntity entityKey(UserLongCompositePK id) {
		return new CloudDatumStreamSettingsEntity(id, now());
	}

	@Override
	public UserLongCompositePK create(Long userId, CloudDatumStreamSettingsEntity entity) {
		requireNonNullArgument(entity, "entity");
		final var sql = new UpsertCloudDatumStreamSettingsEntity(userId, entity.getDatumStreamId(),
				entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? new UserLongCompositePK(userId, entity.getDatumStreamId()) : null);
	}

	@Override
	public Collection<CloudDatumStreamSettingsEntity> findAll(Long userId, List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var results = findFiltered(filter, sorts, null, null);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public FilterResults<CloudDatumStreamSettingsEntity, UserLongCompositePK> findFiltered(
			CloudDatumStreamSettingsFilter filter, List<SortDescriptor> sorts, Long offset,
			Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectCloudDatumStreamSettingsEntity(filter);
		return executeFilterQuery(jdbcOps, filter, sql,
				CloudDatumStreamSettingsEntityRowMapper.INSTANCE);
	}

	@Override
	public UserLongCompositePK save(CloudDatumStreamSettingsEntity entity) {
		requireNonNullArgument(entity, "entity");
		return create(entity.getUserId(), entity);
	}

	@Override
	public CloudDatumStreamSettingsEntity get(UserLongCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setDatumStreamId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectCloudDatumStreamSettingsEntity(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CloudDatumStreamSettingsEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<CloudDatumStreamSettingsEntity> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardin.cin_datum_stream_settings";
	private static final String ID_COLUMN_NAME = "ds_id";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", ID_COLUMN_NAME };

	@Override
	public void delete(CloudDatumStreamSettingsEntity entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").getId(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	@Override
	public CloudDatumStreamSettings resolveSettings(Long userId, Long datumStreamId,
			CloudDatumStreamSettings defaultSettings) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		filter.setDatumStreamId(requireNonNullArgument(datumStreamId, "datumStreamId"));
		var sql = new SelectCloudDatumStreamSettingsEntity(filter, true);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CloudDatumStreamSettingsEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().map(CloudDatumStreamSettings.class::cast)
				.orElse(defaultSettings);
	}

}
