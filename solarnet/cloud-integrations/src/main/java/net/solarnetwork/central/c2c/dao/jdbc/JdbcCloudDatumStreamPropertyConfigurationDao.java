/* ==================================================================
 * JdbcCloudDatumStreamPropertyConfigurationDao.java - 4/10/2024 8:30:39 am
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

import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType.Reference;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.central.domain.UserLongIntegerCompositePK.UNASSIGNED_GROUP_ID;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyFilter;
import net.solarnetwork.central.c2c.dao.jdbc.sql.SelectCloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.dao.jdbc.sql.UpsertCloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.common.dao.jdbc.sql.UpdateEnabledIdFilter;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link CloudDatumStreamPropertyConfigurationDao}.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcCloudDatumStreamPropertyConfigurationDao
		implements CloudDatumStreamPropertyConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcCloudDatumStreamPropertyConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends CloudDatumStreamPropertyConfiguration> getObjectType() {
		return CloudDatumStreamPropertyConfiguration.class;
	}

	@Override
	public CloudDatumStreamPropertyConfiguration entityKey(UserLongIntegerCompositePK id) {
		return new CloudDatumStreamPropertyConfiguration(id, Instant.EPOCH, Instantaneous, "", Reference,
				"");
	}

	@Override
	public UserLongIntegerCompositePK create(Long userId, Long datumStreamId,
			CloudDatumStreamPropertyConfiguration entity) {
		final var sql = new UpsertCloudDatumStreamPropertyConfiguration(userId, datumStreamId, entity);
		jdbcOps.update(sql);
		return new UserLongIntegerCompositePK(userId, entity.getDatumStreamMappingId(),
				entity.getIndex());
	}

	@Override
	public Collection<CloudDatumStreamPropertyConfiguration> findAll(Long userId,
			Long datumStreamMappingId, @Nullable List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		filter.setDatumStreamMappingId(
				requireNonNullArgument(datumStreamMappingId, "datumStreamMappingId"));
		var sql = new SelectCloudDatumStreamPropertyConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CloudDatumStreamPropertyConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public FilterResults<CloudDatumStreamPropertyConfiguration, UserLongIntegerCompositePK> findFiltered(
			CloudDatumStreamPropertyFilter filter, @Nullable List<SortDescriptor> sorts,
			@Nullable Long offset, @Nullable Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectCloudDatumStreamPropertyConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql,
				CloudDatumStreamPropertyConfigurationRowMapper.INSTANCE);
	}

	@Override
	public UserLongIntegerCompositePK save(CloudDatumStreamPropertyConfiguration entity) {
		return create(entity.getUserId(), entity.getDatumStreamMappingId(), entity);
	}

	@Override
	public @Nullable CloudDatumStreamPropertyConfiguration get(UserLongIntegerCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setDatumStreamMappingId(requireNonNullArgument(id.getGroupId(), "id.groupId"));
		filter.setIndex(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectCloudDatumStreamPropertyConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CloudDatumStreamPropertyConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<CloudDatumStreamPropertyConfiguration> getAll(
			@Nullable List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardin.cin_datum_stream_prop";
	private static final String DATASOURCE_ID_COLUMN_NAME = "map_id";
	private static final String INDEX_COLUMN_NAME = "idx";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", DATASOURCE_ID_COLUMN_NAME,
			INDEX_COLUMN_NAME };

	@Override
	public void delete(CloudDatumStreamPropertyConfiguration entity) {
		var pk = requireNonNullArgument(requireNonNullArgument(entity, "entity").getId(), "entity.id");
		var sql = new DeleteForCompositeKey(pk, TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	@Override
	public int updateEnabledStatus(Long userId, CloudDatumStreamPropertyFilter filter, boolean enabled) {
		UserLongIntegerCompositePK key = filter != null && filter.hasIndexCriteria()
				? new UserLongIntegerCompositePK(
						userId, requireNonNullArgument(filter.getDatumStreamMappingId(), "mappingId"),
						nonnull(filter.getIndex(), "index"))
				: filter != null && filter.hasDatumStreamMappingCriteria()
						? UserLongIntegerCompositePK.unassignedEntityIdKey(userId,
								nonnull(filter.getDatumStreamMappingId(), "mappingId"))
						: UserLongIntegerCompositePK.unassignedEntityIdKey(userId, UNASSIGNED_GROUP_ID);
		var sql = new UpdateEnabledIdFilter(TABLE_NAME, PK_COLUMN_NAMES, key, enabled);
		return jdbcOps.update(sql);
	}

}
