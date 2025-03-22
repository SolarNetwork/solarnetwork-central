/* ==================================================================
 * JdbcUserKeyPairEntityDao.java - 22/03/2025 7:40:01 am
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
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.user.dao.BasicUserSecretFilter;
import net.solarnetwork.central.user.dao.UserKeyPairEntityDao;
import net.solarnetwork.central.user.dao.UserKeyPairFilter;
import net.solarnetwork.central.user.dao.jdbc.sql.SelectUserKeyPairEntity;
import net.solarnetwork.central.user.dao.jdbc.sql.UpsertUserKeyPairEntity;
import net.solarnetwork.central.user.domain.UserKeyPairEntity;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link UserKeyPairEntityDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcUserKeyPairEntityDao implements UserKeyPairEntityDao {

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
	public JdbcUserKeyPairEntityDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends UserKeyPairEntity> getObjectType() {
		return UserKeyPairEntity.class;
	}

	@Override
	public UserKeyPairEntity entityKey(UserStringCompositePK id) {
		return new UserKeyPairEntity(id, EMPTY_SECRET_VALUE);
	}

	@Override
	public UserStringCompositePK create(Long userId, UserKeyPairEntity entity) {
		final var sql = new UpsertUserKeyPairEntity(userId, entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? new UserStringCompositePK(userId, entity.getKey()) : null);
	}

	@Override
	public Collection<UserKeyPairEntity> findAll(Long userId, List<SortDescriptor> sorts) {
		var filter = new BasicUserSecretFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectUserKeyPairEntity(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, UserKeyPairEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public FilterResults<UserKeyPairEntity, UserStringCompositePK> findFiltered(UserKeyPairFilter filter,
			List<SortDescriptor> sorts, Long offset, Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectUserKeyPairEntity(filter);
		return executeFilterQuery(jdbcOps, filter, sql, UserKeyPairEntityRowMapper.INSTANCE);
	}

	@Override
	public UserStringCompositePK save(UserKeyPairEntity entity) {
		return create(entity.getUserId(), entity);
	}

	@Override
	public UserKeyPairEntity get(UserStringCompositePK id) {
		var filter = new BasicUserSecretFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setKey(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectUserKeyPairEntity(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, UserKeyPairEntityRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<UserKeyPairEntity> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solaruser.user_keypair";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", "skey" };

	@Override
	public void delete(UserKeyPairEntity entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").getId(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

}
