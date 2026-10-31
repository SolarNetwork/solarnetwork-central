/* ==================================================================
 * JdbcObjectDatumStreamAliasGrantEntityDao.java - 31/10/2026 4:27:30 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc;

import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.updateWithGeneratedLong;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.common.dao.jdbc.sql.UpdateEnabledIdFilter;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamAliasGrantFilter;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasGrantEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasGrantFilter;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.InsertObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectObjectDatumStreamAliasGrantStream;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.UpdateObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantStream;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link ObjectDatumStreamAliasGrantEntityDao}.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcObjectDatumStreamAliasGrantEntityDao implements ObjectDatumStreamAliasGrantEntityDao {

	private static final String TABLE_NAME = "solaruser.da_datm_alias_grant";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", "id" };

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcObjectDatumStreamAliasGrantEntityDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends ObjectDatumStreamAliasGrantEntity> getObjectType() {
		return ObjectDatumStreamAliasGrantEntity.class;
	}

	@Override
	public ObjectDatumStreamAliasGrantEntity entityKey(UserLongCompositePK id) {
		return new ObjectDatumStreamAliasGrantEntity(id, Instant.EPOCH, id.getUserId());
	}

	@Override
	public UserLongCompositePK create(Long userId, ObjectDatumStreamAliasGrantEntity entity) {
		final var sql = new InsertObjectDatumStreamAliasGrantEntity(userId, entity);
		final Long id = updateWithGeneratedLong(jdbcOps, sql, "id");
		return new UserLongCompositePK(userId, id);
	}

	@Override
	public Collection<ObjectDatumStreamAliasGrantEntity> findAll(Long userId,
			@Nullable List<SortDescriptor> sorts) {
		var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectObjectDatumStreamAliasGrantEntity(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				ObjectDatumStreamAliasGrantEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public FilterResults<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK> findFiltered(
			ObjectDatumStreamAliasGrantFilter filter, @Nullable List<SortDescriptor> sorts,
			@Nullable Long offset, @Nullable Integer max) {
		if ( requireNonNullArgument(filter, "filter").getUserId() == null ) {
			requireNonNullArgument(filter.getGranteeId(), "filter.granteeId");
		}
		var sql = new SelectObjectDatumStreamAliasGrantEntity(filter);
		return executeFilterQuery(jdbcOps, filter, sql,
				ObjectDatumStreamAliasGrantEntityRowMapper.INSTANCE);
	}

	@Override
	public FilterResults<ObjectDatumStreamAliasGrantStream, UserLongStringCompositePK> findAvailableStreams(
			Long granteeId, Long aliasNodeId, @Nullable ObjectDatumStreamAliasGrantFilter filter) {
		var f = (filter != null ? filter : new BasicObjectDatumStreamAliasGrantFilter());
		var sql = new SelectObjectDatumStreamAliasGrantStream(granteeId, aliasNodeId, f);
		return executeFilterQuery(jdbcOps, f, sql, ObjectDatumStreamAliasGrantStreamRowMapper.INSTANCE);
	}

	@Override
	public UserLongCompositePK save(ObjectDatumStreamAliasGrantEntity entity) {
		UserLongCompositePK pk = requireNonNullArgument(requireNonNullArgument(entity, "entity").getId(),
				"entity.id");
		if ( !pk.entityIdIsAssigned() ) {
			return create(pk.getUserId(), entity);
		}
		jdbcOps.update(new UpdateObjectDatumStreamAliasGrantEntity(pk, entity));
		return pk;
	}

	@Override
	public @Nullable ObjectDatumStreamAliasGrantEntity get(UserLongCompositePK id) {
		var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setGrantId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectObjectDatumStreamAliasGrantEntity(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				ObjectDatumStreamAliasGrantEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<ObjectDatumStreamAliasGrantEntity> getAll(@Nullable List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(ObjectDatumStreamAliasGrantEntity entity) {
		UserLongCompositePK pk = requireNonNullArgument(requireNonNullArgument(entity, "entity").getId(),
				"entity.id");
		jdbcOps.update(new DeleteForCompositeKey(pk, TABLE_NAME, PK_COLUMN_NAMES));
	}

	@Override
	public int updateEnabledStatus(Long userId, @Nullable ObjectDatumStreamAliasGrantFilter filter,
			boolean enabled) {
		UserLongCompositePK key = filter != null && filter.hasGrantCriteria()
				? new UserLongCompositePK(userId, requireNonNullArgument(filter.getGrantId(), "grantId"))
				: UserLongCompositePK.unassignedEntityIdKey(userId);
		return jdbcOps.update(new UpdateEnabledIdFilter(TABLE_NAME, PK_COLUMN_NAMES, key, enabled));
	}

}
