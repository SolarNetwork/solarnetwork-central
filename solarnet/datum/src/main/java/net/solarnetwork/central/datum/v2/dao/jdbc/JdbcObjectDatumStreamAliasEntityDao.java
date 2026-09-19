/* ==================================================================
 * JdbcObjectDatumStreamAliasEntityDao.java - 28/03/2026 6:32:17 am
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

import static java.time.Instant.EPOCH;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.central.domain.EntityConstants.UNASSIGNED_LONG_ID;
import static net.solarnetwork.central.domain.EntityConstants.UNASSIGNED_STRING_ID;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasFilter;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.DeleteObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.UpsertObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.domain.EntityConstants;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.util.UuidGenerator;

/**
 * JDBC implementation of {@link ObjectDatumStreamAliasEntityDao}.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcObjectDatumStreamAliasEntityDao implements ObjectDatumStreamAliasEntityDao {

	private final UuidGenerator uuidGenerator;
	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcObjectDatumStreamAliasEntityDao(JdbcOperations jdbcOps) {
		this(UUID::randomUUID, jdbcOps);
	}

	/**
	 * Constructor.
	 *
	 * @param uuidGenerator
	 *        the UUID generator
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcObjectDatumStreamAliasEntityDao(UuidGenerator uuidGenerator, JdbcOperations jdbcOps) {
		super();
		this.uuidGenerator = requireNonNullArgument(uuidGenerator, "uuidGenerator");
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends ObjectDatumStreamAliasEntity> getObjectType() {
		return ObjectDatumStreamAliasEntity.class;
	}

	@Override
	public ObjectDatumStreamAliasEntity entityKey(final UUID id) {
		return new ObjectDatumStreamAliasEntity(id, EPOCH, null, ObjectDatumKind.Node,
				UNASSIGNED_LONG_ID, UNASSIGNED_STRING_ID, UNASSIGNED_LONG_ID, UNASSIGNED_STRING_ID);
	}

	@Override
	public FilterResults<ObjectDatumStreamAliasEntity, UUID> findFiltered(
			final ObjectDatumStreamAliasFilter filter, final @Nullable List<SortDescriptor> sorts,
			final @Nullable Long offset, final @Nullable Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectObjectDatumStreamAliasEntity(filter);
		return executeFilterQuery(jdbcOps, filter, sql, ObjectDatumStreamAliasEntityRowMapper.INSTANCE);
	}

	@Override
	public UUID save(final ObjectDatumStreamAliasEntity entity) {
		UUID pk = requireNonNullArgument(entity, "entity").id();
		if ( !EntityConstants.isAssigned(pk) ) {
			pk = uuidGenerator.generate();
		}
		final var sql = new UpsertObjectDatumStreamAliasEntity(pk, entity);
		jdbcOps.update(sql);
		return pk;
	}

	@Override
	public @Nullable ObjectDatumStreamAliasEntity get(final UUID id) {
		final var filter = new BasicDatumCriteria();
		filter.setStreamId(requireNonNullArgument(id, "id"));
		final var sql = new SelectObjectDatumStreamAliasEntity(filter, true);
		final var results = executeFilterQuery(jdbcOps, filter, sql,
				ObjectDatumStreamAliasEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<ObjectDatumStreamAliasEntity> getAll(final @Nullable List<SortDescriptor> sorts) {
		final var filter = new BasicDatumCriteria();
		final var sql = new SelectObjectDatumStreamAliasEntity(filter);
		return jdbcOps.query(sql, ObjectDatumStreamAliasEntityRowMapper.INSTANCE);
	}

	@Override
	public void delete(final ObjectDatumStreamAliasEntity entity) {
		final var filter = new BasicDatumCriteria();
		filter.setStreamId(entity.id());
		var sql = new DeleteObjectDatumStreamAliasEntity(filter, true);
		jdbcOps.update(sql);
	}

	@Override
	public int delete(final ObjectDatumStreamAliasFilter filter) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new DeleteObjectDatumStreamAliasEntity(filter);
		return jdbcOps.update(sql);
	}

}
