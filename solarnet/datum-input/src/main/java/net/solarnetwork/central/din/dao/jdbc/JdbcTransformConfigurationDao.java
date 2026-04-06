/* ==================================================================
 * JdbcTransformConfigurationDao.java - 21/02/2024 1:35:26 pm
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

package net.solarnetwork.central.din.dao.jdbc;

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
import net.solarnetwork.central.din.dao.BasicFilter;
import net.solarnetwork.central.din.dao.TransformConfigurationDao;
import net.solarnetwork.central.din.dao.TransformFilter;
import net.solarnetwork.central.din.dao.jdbc.sql.InsertTransformConfiguration;
import net.solarnetwork.central.din.dao.jdbc.sql.SelectTransformConfiguration;
import net.solarnetwork.central.din.dao.jdbc.sql.UpdateTransformConfiguration;
import net.solarnetwork.central.din.domain.TransformConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link TransformConfigurationDao}.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcTransformConfigurationDao implements TransformConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcTransformConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends TransformConfiguration> getObjectType() {
		return TransformConfiguration.class;
	}

	@Override
	public TransformConfiguration entityKey(UserLongCompositePK id) {
		return new TransformConfiguration(id, Instant.EPOCH, "", "");
	}

	@Override
	public UserLongCompositePK create(Long userId, TransformConfiguration entity) {
		final var sql = new InsertTransformConfiguration(userId, entity);

		final Long id = updateWithGeneratedLong(jdbcOps, sql, "id");

		return new UserLongCompositePK(userId, id);
	}

	@Override
	public Collection<TransformConfiguration> findAll(Long userId,
			@Nullable List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectTransformConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, TransformConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public FilterResults<TransformConfiguration, UserLongCompositePK> findFiltered(
			TransformFilter filter, @Nullable List<SortDescriptor> sorts, @Nullable Long offset,
			@Nullable Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectTransformConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql, TransformConfigurationRowMapper.INSTANCE);
	}

	@Override
	public UserLongCompositePK save(TransformConfiguration entity) {
		if ( !entity.id().entityIdIsAssigned() ) {
			return create(entity.getUserId(), entity);
		}
		final var sql = new UpdateTransformConfiguration(entity.id(), entity);
		jdbcOps.update(sql);
		return entity.id();
	}

	@Override
	public @Nullable TransformConfiguration get(UserLongCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setTransformId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectTransformConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, TransformConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<TransformConfiguration> getAll(@Nullable List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardin.din_xform";
	private static final String ID_COLUMN_NAME = "id";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", ID_COLUMN_NAME };

	@Override
	public void delete(TransformConfiguration entity) {
		var sql = new DeleteForCompositeKey(requireNonNullArgument(entity, "entity").id(), TABLE_NAME,
				PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

}
