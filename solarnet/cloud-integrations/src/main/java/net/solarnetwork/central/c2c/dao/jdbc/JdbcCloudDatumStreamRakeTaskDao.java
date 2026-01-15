/* ==================================================================
 * JdbcCloudDatumStreamRakeTaskDao.java - 20/09/2025 6:49:34 pm
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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.CallableStatement;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamRakeTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamRakeTaskFilter;
import net.solarnetwork.central.c2c.dao.jdbc.sql.DeleteCloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.c2c.dao.jdbc.sql.InsertCloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.c2c.dao.jdbc.sql.SelectCloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.c2c.dao.jdbc.sql.UpdateCloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.c2c.dao.jdbc.sql.UpdateCloudDatumStreamRakeTaskEntityState;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link CloudDatumStreamRakeTaskDao}.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcCloudDatumStreamRakeTaskDao implements CloudDatumStreamRakeTaskDao {

	/** The default SQL to use. */
	public static final String DEFAULT_CLAIM_JOB_SQL = "{call solardin.claim_datum_stream_rake_task()}";

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
	public JdbcCloudDatumStreamRakeTaskDao(JdbcOperations jdbcOps) {
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
	public JdbcCloudDatumStreamRakeTaskDao(JdbcOperations jdbcOps, String claimTaskSql) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
		this.claimTaskSql = requireNonNullArgument(claimTaskSql, "claimTaskSql");
	}

	@Override
	public Class<? extends CloudDatumStreamRakeTaskEntity> getObjectType() {
		return CloudDatumStreamRakeTaskEntity.class;
	}

	@Override
	public CloudDatumStreamRakeTaskEntity entityKey(UserLongCompositePK id) {
		return new CloudDatumStreamRakeTaskEntity(id);
	}

	@Override
	public UserLongCompositePK create(Long userId, CloudDatumStreamRakeTaskEntity entity) {
		final var sql = new InsertCloudDatumStreamRakeTaskEntity(userId, entity);
		final Long id = CommonJdbcUtils.updateWithGeneratedLong(jdbcOps, sql, "id");
		return (id != null ? new UserLongCompositePK(userId, id) : null);
	}

	@Override
	public FilterResults<CloudDatumStreamRakeTaskEntity, UserLongCompositePK> findFiltered(
			CloudDatumStreamRakeTaskFilter filter, List<SortDescriptor> sorts, Long offset,
			Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectCloudDatumStreamRakeTaskEntity(filter);
		return executeFilterQuery(jdbcOps, filter, sql,
				CloudDatumStreamRakeTaskEntityRowMapper.INSTANCE);
	}

	@Override
	public Collection<CloudDatumStreamRakeTaskEntity> findAll(Long userId, List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectCloudDatumStreamRakeTaskEntity(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CloudDatumStreamRakeTaskEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public UserLongCompositePK save(CloudDatumStreamRakeTaskEntity entity) {
		if ( !entity.getId().entityIdIsAssigned() ) {
			return create(entity.getId().getUserId(), entity);
		}
		final UpdateCloudDatumStreamRakeTaskEntity sql = new UpdateCloudDatumStreamRakeTaskEntity(
				entity.getId(), entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public CloudDatumStreamRakeTaskEntity get(UserLongCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setTaskId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectCloudDatumStreamRakeTaskEntity(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CloudDatumStreamRakeTaskEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<CloudDatumStreamRakeTaskEntity> getAll(List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		var sql = new SelectCloudDatumStreamRakeTaskEntity(filter);
		return jdbcOps.query(sql, CloudDatumStreamRakeTaskEntityRowMapper.INSTANCE);
	}

	@Override
	public void delete(CloudDatumStreamRakeTaskEntity entity) {
		BasicFilter filter = new BasicFilter();
		filter.setUserId(entity.getUserId());
		filter.setTaskId(entity.getConfigId());
		delete(filter);
	}

	@Override
	public int delete(CloudDatumStreamRakeTaskFilter filter) {
		var sql = new DeleteCloudDatumStreamRakeTaskEntity(filter);
		return jdbcOps.update(sql);
	}

	@Override
	public CloudDatumStreamRakeTaskEntity claimQueuedTask() {
		return jdbcOps.execute(claimTaskSql, (CallableStatement cs) -> {
			if ( cs.execute() ) {
				try (var rs = cs.getResultSet()) {
					if ( rs != null ) {
						if ( rs.next() ) {
							return CloudDatumStreamRakeTaskEntityRowMapper.INSTANCE.mapRow(rs, 1);
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
		filter.setTaskId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		if ( expectedStates != null ) {
			filter.setClaimableJobStates(expectedStates);
		}
		var sql = new UpdateCloudDatumStreamRakeTaskEntityState(desiredState, filter);
		return jdbcOps.update(sql) != 0;
	}

	@Override
	public boolean updateTask(CloudDatumStreamRakeTaskEntity info,
			BasicClaimableJobState... expectedStates) {
		BasicFilter filter = new BasicFilter();
		filter.setUserId(info.getUserId());
		filter.setTaskId(filter.getTaskId());
		filter.setClaimableJobStates(expectedStates);
		var sql = new UpdateCloudDatumStreamRakeTaskEntityState(info.getState(), filter, info);
		return jdbcOps.update(sql) != 0;
	}

	@Override
	public int resetAbandondedExecutingTasks(Instant olderThan) {
		// reset tasks in Executing or Claimed older than olderThan to Queued
		BasicFilter filter = new BasicFilter();
		filter.setClaimableJobStates(new BasicClaimableJobState[] { BasicClaimableJobState.Executing,
				BasicClaimableJobState.Claimed });
		filter.setEndDate(olderThan);
		var sql = new UpdateCloudDatumStreamRakeTaskEntityState(BasicClaimableJobState.Queued, filter);
		return jdbcOps.update(sql);
	}

}
