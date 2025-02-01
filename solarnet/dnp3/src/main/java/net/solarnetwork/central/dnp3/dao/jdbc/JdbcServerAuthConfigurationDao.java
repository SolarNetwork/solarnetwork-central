/* ==================================================================
 * JdbcDao.java - 7/08/2023 6:06:12 am
 *
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.dao.jdbc;

import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.dnp3.dao.BasicFilter;
import net.solarnetwork.central.dnp3.dao.ServerAuthConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerFilter;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.SelectServerAuthConfiguration;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.UpdateEnabledServerFilter;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.UpsertServerAuthConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerAuthConfiguration;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link ServerAuthConfigurationDao}.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcServerAuthConfigurationDao implements ServerAuthConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcServerAuthConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends ServerAuthConfiguration> getObjectType() {
		return ServerAuthConfiguration.class;
	}

	@Override
	public UserLongStringCompositePK create(Long userId, Long serverId, ServerAuthConfiguration entity) {
		final var sql = new UpsertServerAuthConfiguration(userId, serverId, entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public Collection<ServerAuthConfiguration> findAll(Long userId, Long serverId,
			List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		filter.setServerId(serverId);
		var sql = new SelectServerAuthConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				ServerAuthConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public UserLongStringCompositePK save(ServerAuthConfiguration entity) {
		return create(entity.getUserId(), entity.getServerId(), entity);
	}

	@Override
	public ServerAuthConfiguration get(UserLongStringCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setServerId(requireNonNullArgument(id.getGroupId(), "id.groupId"));
		filter.setSubjectDn(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectServerAuthConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				ServerAuthConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<ServerAuthConfiguration> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardnp3.dnp3_server_auth";
	private static final String SERVER_ID_COLUMN_NAME = "server_id";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", SERVER_ID_COLUMN_NAME,
			"ident" };

	@Override
	public void delete(ServerAuthConfiguration entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").getId(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	@Override
	public FilterResults<ServerAuthConfiguration, UserLongStringCompositePK> findFiltered(
			ServerFilter filter, List<SortDescriptor> sorts, Long offset, Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectServerAuthConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql, ServerAuthConfigurationRowMapper.INSTANCE);
	}

	@Override
	public int updateEnabledStatus(Long userId, ServerFilter filter, boolean enabled) {
		var sql = new UpdateEnabledServerFilter(TABLE_NAME, SERVER_ID_COLUMN_NAME, userId, filter,
				enabled);
		return jdbcOps.update(sql);
	}

	@Override
	public ServerAuthConfiguration findForIdentifier(String subjectDn) {
		BasicFilter filter = new BasicFilter();
		filter.setIdentifier(subjectDn);
		filter.setEnabled(true);
		var sql = new SelectServerAuthConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				ServerAuthConfigurationRowMapper.INSTANCE);
		return StreamSupport.stream(results.spliterator(), false).findFirst().orElse(null);
	}

}
