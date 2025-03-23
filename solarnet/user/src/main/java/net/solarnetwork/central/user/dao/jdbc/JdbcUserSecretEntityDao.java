/* ==================================================================
 * JdbcUserSecretEntityDao.java - 22/03/2025 7:40:01 am
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
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.user.dao.BasicUserSecretFilter;
import net.solarnetwork.central.user.dao.UserSecretEntityDao;
import net.solarnetwork.central.user.dao.UserSecretFilter;
import net.solarnetwork.central.user.dao.jdbc.sql.SelectUserSecretEntity;
import net.solarnetwork.central.user.dao.jdbc.sql.UpsertUserSecretEntity;
import net.solarnetwork.central.user.domain.UserSecretEntity;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link UserSecretEntityDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcUserSecretEntityDao implements UserSecretEntityDao {

	private static final byte[] EMPTY_SECRET_VALUE = new byte[0];

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcUserSecretEntityDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends UserSecretEntity> getObjectType() {
		return UserSecretEntity.class;
	}

	@Override
	public UserSecretEntity entityKey(UserStringStringCompositePK id) {
		return new UserSecretEntity(id, EMPTY_SECRET_VALUE);
	}

	@Override
	public UserStringStringCompositePK create(Long userId, String topicId, UserSecretEntity entity) {
		final var sql = new UpsertUserSecretEntity(userId, topicId, entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? new UserStringStringCompositePK(userId, entity.getTopicId(), entity.getKey())
				: null);
	}

	@Override
	public Collection<UserSecretEntity> findAll(Long userId, String topicId,
			List<SortDescriptor> sorts) {
		var filter = new BasicUserSecretFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		filter.setTopicId(requireNonNullArgument(topicId, "topicId"));
		var sql = new SelectUserSecretEntity(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, UserSecretEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public FilterResults<UserSecretEntity, UserStringStringCompositePK> findFiltered(
			UserSecretFilter filter, List<SortDescriptor> sorts, Long offset, Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectUserSecretEntity(filter);
		return executeFilterQuery(jdbcOps, filter, sql, UserSecretEntityRowMapper.INSTANCE);
	}

	@Override
	public UserStringStringCompositePK save(UserSecretEntity entity) {
		return create(entity.getUserId(), entity.getTopicId(), entity);
	}

	@Override
	public UserSecretEntity get(UserStringStringCompositePK id) {
		var filter = new BasicUserSecretFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setTopicId(requireNonNullArgument(id.getGroupId(), "id.groupId"));
		filter.setKey(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectUserSecretEntity(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, UserSecretEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<UserSecretEntity> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solaruser.user_secret";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", "topic_id", "skey" };

	@Override
	public void delete(UserSecretEntity entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").getId(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

}
