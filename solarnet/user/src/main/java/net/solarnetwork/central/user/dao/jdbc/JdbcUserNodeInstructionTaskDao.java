/* ==================================================================
 * JdbcUserNodeInstructionTaskDao.java - 10/11/2025 5:02:20 pm
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

package net.solarnetwork.central.user.dao.jdbc;

import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.CallableStatement;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.dao.BasicUserNodeInstructionTaskFilter;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskDao;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskFilter;
import net.solarnetwork.central.user.dao.jdbc.sql.DeleteUserNodeInstructionTaskEntity;
import net.solarnetwork.central.user.dao.jdbc.sql.InsertUserNodeInstructionTaskEntity;
import net.solarnetwork.central.user.dao.jdbc.sql.SelectUserNodeInstructionTaskEntity;
import net.solarnetwork.central.user.dao.jdbc.sql.UpdateUserNodeInstructionTaskEntity;
import net.solarnetwork.central.user.dao.jdbc.sql.UpdateUserNodeInstructionTaskEntityState;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link UserNodeInstructionTaskDao}.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcUserNodeInstructionTaskDao implements UserNodeInstructionTaskDao {

	public static String DEFAULT_CLAIM_JOB_SQL = "{call solaruser.claim_node_instr_task()}";

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
	public JdbcUserNodeInstructionTaskDao(JdbcOperations jdbcOps) {
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
	public JdbcUserNodeInstructionTaskDao(JdbcOperations jdbcOps, String claimTaskSql) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
		this.claimTaskSql = requireNonNullArgument(claimTaskSql, "claimTaskSql");
	}

	@Override
	public Class<? extends UserNodeInstructionTaskEntity> getObjectType() {
		return UserNodeInstructionTaskEntity.class;
	}

	@Override
	public UserNodeInstructionTaskEntity entityKey(UserLongCompositePK id) {
		return new UserNodeInstructionTaskEntity(id);
	}

	@Override
	public UserLongCompositePK create(Long userId, UserNodeInstructionTaskEntity entity) {
		final var sql = new InsertUserNodeInstructionTaskEntity(userId, entity);
		final Long id = CommonJdbcUtils.updateWithGeneratedLong(jdbcOps, sql, "id");
		return (id != null ? new UserLongCompositePK(userId, id) : null);
	}

	@Override
	public FilterResults<UserNodeInstructionTaskEntity, UserLongCompositePK> findFiltered(
			UserNodeInstructionTaskFilter filter, List<SortDescriptor> sorts, Long offset, Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectUserNodeInstructionTaskEntity(filter);
		return executeFilterQuery(jdbcOps, filter, sql, UserInstructionTaskEntityRowMapper.INSTANCE);
	}

	@Override
	public Collection<UserNodeInstructionTaskEntity> findAll(Long userId, List<SortDescriptor> sorts) {
		var filter = new BasicUserNodeInstructionTaskFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectUserNodeInstructionTaskEntity(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				UserInstructionTaskEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public UserLongCompositePK save(UserNodeInstructionTaskEntity entity) {
		if ( !entity.getId().entityIdIsAssigned() ) {
			return create(entity.getId().getUserId(), entity);
		}
		final UpdateUserNodeInstructionTaskEntity sql = new UpdateUserNodeInstructionTaskEntity(entity.getId(),
				entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public UserNodeInstructionTaskEntity get(UserLongCompositePK id) {
		var filter = new BasicUserNodeInstructionTaskFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setTaskId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectUserNodeInstructionTaskEntity(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				UserInstructionTaskEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<UserNodeInstructionTaskEntity> getAll(List<SortDescriptor> sorts) {
		var filter = new BasicUserNodeInstructionTaskFilter();
		var sql = new SelectUserNodeInstructionTaskEntity(filter);
		return jdbcOps.query(sql, UserInstructionTaskEntityRowMapper.INSTANCE);
	}

	@Override
	public void delete(UserNodeInstructionTaskEntity entity) {
		var filter = new BasicUserNodeInstructionTaskFilter();
		filter.setUserId(entity.getUserId());
		filter.setTaskId(entity.getConfigId());
		delete(filter);
	}

	@Override
	public int delete(UserNodeInstructionTaskFilter filter) {
		var sql = new DeleteUserNodeInstructionTaskEntity(filter);
		return jdbcOps.update(sql);
	}

	@Override
	public UserNodeInstructionTaskEntity claimQueuedTask() {
		return jdbcOps.execute(claimTaskSql, (CallableStatement cs) -> {
			if ( cs.execute() ) {
				try (var rs = cs.getResultSet()) {
					if ( rs != null ) {
						if ( rs.next() ) {
							return UserInstructionTaskEntityRowMapper.INSTANCE.mapRow(rs, 1);
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
		var filter = new BasicUserNodeInstructionTaskFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setTaskId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		if ( expectedStates != null ) {
			filter.setClaimableJobStates(expectedStates);
		}
		var sql = new UpdateUserNodeInstructionTaskEntityState(desiredState, filter);
		return jdbcOps.update(sql) != 0;
	}

	@Override
	public boolean updateTask(UserNodeInstructionTaskEntity info, BasicClaimableJobState... expectedStates) {
		var filter = new BasicUserNodeInstructionTaskFilter();
		filter.setUserId(info.getUserId());
		filter.setTaskId(filter.getTaskId());
		filter.setClaimableJobStates(expectedStates);
		var sql = new UpdateUserNodeInstructionTaskEntityState(info.getState(), filter, info);
		return jdbcOps.update(sql) != 0;
	}

	@Override
	public int resetAbandondedExecutingTasks(Instant olderThan) {
		// reset tasks in Executing or Claimed older than olderThan to Queued
		var filter = new BasicUserNodeInstructionTaskFilter();
		filter.setClaimableJobStates(new BasicClaimableJobState[] { BasicClaimableJobState.Executing,
				BasicClaimableJobState.Claimed });
		filter.setEndDate(olderThan);
		var sql = new UpdateUserNodeInstructionTaskEntityState(BasicClaimableJobState.Queued, filter);
		return jdbcOps.update(sql);
	}

}
