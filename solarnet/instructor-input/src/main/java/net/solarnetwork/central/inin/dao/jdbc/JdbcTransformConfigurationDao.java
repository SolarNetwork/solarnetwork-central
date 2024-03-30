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

package net.solarnetwork.central.inin.dao.jdbc;

import static java.time.Instant.now;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.inin.dao.BasicFilter;
import net.solarnetwork.central.inin.dao.TransformConfigurationDao;
import net.solarnetwork.central.inin.dao.TransformFilter;
import net.solarnetwork.central.inin.dao.jdbc.sql.InsertTransformConfiguration;
import net.solarnetwork.central.inin.dao.jdbc.sql.SelectTransformConfiguration;
import net.solarnetwork.central.inin.dao.jdbc.sql.UpdateTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformPhase;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link TransformConfigurationDao}.
 *
 * @param <C>
 *        the transform configuration type
 * @author matt
 * @version 1.0
 */
public abstract sealed class JdbcTransformConfigurationDao<C extends TransformConfiguration<C>>
		implements TransformConfigurationDao<C> {

	/**
	 * JDBC implementation of {@link TransformConfigurationDao} for request
	 * configurations.
	 */
	public static final class JdbcRequestTransformConfigurationDao
			extends JdbcTransformConfigurationDao<RequestTransformConfiguration> {

		private static final String TABLE_NAME = "solardin.inin_req_xform";

		/**
		 * Constructor.
		 *
		 * @param jdbcOps
		 *        the JDBC operations
		 */
		public JdbcRequestTransformConfigurationDao(JdbcOperations jdbcOps) {
			super(jdbcOps, RequestTransformConfiguration.class, TransformPhase.Request);
		}

		@Override
		public RequestTransformConfiguration entityKey(UserLongCompositePK id) {
			return new RequestTransformConfiguration(id, now());
		}

		@Override
		protected RowMapper<RequestTransformConfiguration> rowMapper() {
			return TransformConfigurationRowMapper.REQ_INSTANCE;
		}

		@Override
		protected String tableName() {
			return TABLE_NAME;
		}

	}

	/**
	 * JDBC implementation of {@link TransformConfigurationDao} for response
	 * configurations.
	 */
	public static final class JdbcResponseTransformConfigurationDao
			extends JdbcTransformConfigurationDao<ResponseTransformConfiguration> {

		private static final String TABLE_NAME = "solardin.inin_res_xform";

		/**
		 * Constructor.
		 *
		 * @param jdbcOps
		 *        the JDBC operations
		 */
		public JdbcResponseTransformConfigurationDao(JdbcOperations jdbcOps) {
			super(jdbcOps, ResponseTransformConfiguration.class, TransformPhase.Response);
		}

		@Override
		public ResponseTransformConfiguration entityKey(UserLongCompositePK id) {
			return new ResponseTransformConfiguration(id, now());
		}

		@Override
		protected RowMapper<ResponseTransformConfiguration> rowMapper() {
			return TransformConfigurationRowMapper.RES_INSTANCE;
		}

		@Override
		protected String tableName() {
			return TABLE_NAME;
		}

	}

	private final JdbcOperations jdbcOps;
	private final TransformPhase phase;
	private final Class<C> entityType;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param entityType
	 *        the entity type
	 * @param phase
	 *        the phase
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcTransformConfigurationDao(JdbcOperations jdbcOps, Class<C> entityType,
			TransformPhase phase) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
		this.entityType = requireNonNullArgument(entityType, "entityType");
		this.phase = requireNonNullArgument(phase, "phase");
	}

	@Override
	public Class<? extends C> getObjectType() {
		return entityType;
	}

	private void validatePhase(C entity) {
		if ( entity.getPhase() != phase ) {
			throw new IllegalArgumentException("Invalid phase value: only " + phase + " supported.");
		}
	}

	@Override
	public UserLongCompositePK create(Long userId, C entity) {
		validatePhase(entity);
		final var sql = new InsertTransformConfiguration(userId, entity);

		final Long id = CommonJdbcUtils.updateWithGeneratedLong(jdbcOps, sql, "id");
		var pk = (id != null ? new UserLongCompositePK(userId, id) : null);

		return pk;
	}

	@Override
	public Collection<C> findAll(Long userId, List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectTransformConfiguration(phase, filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, rowMapper());
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public FilterResults<C, UserLongCompositePK> findFiltered(TransformFilter filter,
			List<SortDescriptor> sorts, Integer offset, Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectTransformConfiguration(phase, filter);
		return executeFilterQuery(jdbcOps, filter, sql, rowMapper());
	}

	@Override
	public UserLongCompositePK save(C entity) {
		if ( !entity.getId().entityIdIsAssigned() ) {
			return create(entity.getId().getUserId(), entity);
		}
		validatePhase(entity);
		final UpdateTransformConfiguration sql = new UpdateTransformConfiguration(entity.getId(),
				entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public C get(UserLongCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setTransformId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectTransformConfiguration(phase, filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, rowMapper());
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	/**
	 * Get a row mapper.
	 *
	 * @return the mapper
	 */
	protected abstract RowMapper<C> rowMapper();

	@Override
	public Collection<C> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String ID_COLUMN_NAME = "id";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", ID_COLUMN_NAME };

	/**
	 * Get the table name.
	 *
	 * @return the table name
	 */
	protected abstract String tableName();

	@Override
	public void delete(C entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").getId(), tableName(), PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

}
