/* ==================================================================
 * JdbcCloudDatumStreamPollTaskDao.java - 10/10/2024 6:44:55 am
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
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.CallableStatement;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskFilter;
import net.solarnetwork.central.c2c.dao.jdbc.sql.SelectCloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.dao.jdbc.sql.UpdateCloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.dao.jdbc.sql.UpdateCloudDatumStreamPollTaskEntityState;
import net.solarnetwork.central.c2c.dao.jdbc.sql.UpsertCloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link CloudDatumStreamPollTaskDao}.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcCloudDatumStreamPollTaskDao implements CloudDatumStreamPollTaskDao {

	public static String DEFAULT_CLAIM_JOB_SQL = "{call solarcin.claim_datum_stream_poll_task()}";

	private final JdbcOperations jdbcOps;
	private final String claimTaskSql;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcCloudDatumStreamPollTaskDao(JdbcOperations jdbcOps) {
		this(jdbcOps, DEFAULT_CLAIM_JOB_SQL);
	}

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param claimTaskSql
	 *        the claim task SQL
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcCloudDatumStreamPollTaskDao(JdbcOperations jdbcOps, String claimTaskSql) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
		this.claimTaskSql = requireNonNullArgument(claimTaskSql, "claimTaskSql");
	}

	@Override
	public Class<? extends CloudDatumStreamPollTaskEntity> getObjectType() {
		return CloudDatumStreamPollTaskEntity.class;
	}

	@Override
	public CloudDatumStreamPollTaskEntity entityKey(UserLongCompositePK id) {
		return new CloudDatumStreamPollTaskEntity(id);
	}

	@Override
	public UserLongCompositePK create(Long userId, CloudDatumStreamPollTaskEntity entity) {
		final var sql = new UpsertCloudDatumStreamPollTaskEntity(userId, entity.getDatumStreamId(),
				entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? new UserLongCompositePK(userId, entity.getDatumStreamId()) : null);
	}

	@Override
	public FilterResults<CloudDatumStreamPollTaskEntity, UserLongCompositePK> findFiltered(
			CloudDatumStreamPollTaskFilter filter, List<SortDescriptor> sorts, Integer offset,
			Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectCloudDatumStreamPollTaskEntity(filter);
		return executeFilterQuery(jdbcOps, filter, sql,
				CloudDatumStreamPollTaskEntityRowMapper.INSTANCE);
	}

	@Override
	public Collection<CloudDatumStreamPollTaskEntity> findAll(Long userId, List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectCloudDatumStreamPollTaskEntity(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CloudDatumStreamPollTaskEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public UserLongCompositePK save(CloudDatumStreamPollTaskEntity entity) {
		return create(entity.getUserId(), entity);
	}

	@Override
	public CloudDatumStreamPollTaskEntity get(UserLongCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setDatumStreamId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectCloudDatumStreamPollTaskEntity(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CloudDatumStreamPollTaskEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<CloudDatumStreamPollTaskEntity> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solarcin.cin_datum_stream_poll_task";
	private static final String ID_COLUMN_NAME = "ds_id";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", ID_COLUMN_NAME };

	@Override
	public void delete(CloudDatumStreamPollTaskEntity entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").getId(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	@Override
	public CloudDatumStreamPollTaskEntity claimQueuedTask() {
		return jdbcOps.execute(claimTaskSql, (CallableStatement cs) -> {
			if ( cs.execute() ) {
				try (var rs = cs.getResultSet()) {
					if ( rs != null ) {
						while ( rs.next() ) {
							return CloudDatumStreamPollTaskEntityRowMapper.INSTANCE.mapRow(rs, 1);
						}
					}
				}
			}
			return null;
		});
	}

	@Override
	public boolean updateTaskState(UserLongCompositePK id, BasicClaimableJobState desiredState,
			BasicClaimableJobState... expectedStates) {
		BasicFilter filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setDatumStreamId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		if ( expectedStates != null ) {
			filter.setClaimableJobStates(expectedStates);
		}
		var sql = new UpdateCloudDatumStreamPollTaskEntityState(desiredState, filter);
		return jdbcOps.update(sql) != 0;
	}

	@Override
	public boolean updateTask(CloudDatumStreamPollTaskEntity info,
			BasicClaimableJobState... expectedStates) {
		BasicFilter filter = null;
		if ( expectedStates != null ) {
			filter = new BasicFilter();
			filter.setClaimableJobStates(expectedStates);
		}
		var sql = new UpdateCloudDatumStreamPollTaskEntity(info.getId(), info, filter);
		return jdbcOps.update(sql) != 0;
	}

	@Override
	public int resetAbandondedExecutingTasks(Instant olderThan) {
		BasicFilter filter = new BasicFilter();
		filter.setClaimableJobStates(new BasicClaimableJobState[] { BasicClaimableJobState.Executing });
		filter.setEndDate(olderThan);
		var sql = new UpdateCloudDatumStreamPollTaskEntityState(BasicClaimableJobState.Queued, filter);
		return jdbcOps.update(sql);
	}

}
