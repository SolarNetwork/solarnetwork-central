/* ==================================================================
 * JdbcServerConfigurationDao.java - 6/08/2023 7:14:13 pm
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
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.updateWithGeneratedLong;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.dnp3.dao.BasicFilter;
import net.solarnetwork.central.dnp3.dao.ServerConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerFilter;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.InsertServerConfiguration;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.SelectServerConfiguration;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.UpdateEnabledServerFilter;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.UpdateServerConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link ServerConfigurationDao}.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcServerConfigurationDao implements ServerConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcServerConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends ServerConfiguration> getObjectType() {
		return ServerConfiguration.class;
	}

	@Override
	public UserLongCompositePK create(Long userId, ServerConfiguration entity) {
		final var sql = new InsertServerConfiguration(userId, entity);

		final Long id = updateWithGeneratedLong(jdbcOps, sql, "id");

		return new UserLongCompositePK(userId, id);
	}

	@Override
	public Collection<ServerConfiguration> findAll(Long userId, @Nullable List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectServerConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, ServerConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public UserLongCompositePK save(ServerConfiguration entity) {
		if ( !entity.pk().entityIdIsAssigned() ) {
			return create(entity.getUserId(), entity);
		}
		final var sql = new UpdateServerConfiguration(entity.pk(), entity);
		jdbcOps.update(sql);
		return entity.pk();
	}

	@Override
	public @Nullable ServerConfiguration get(UserLongCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setServerId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectServerConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, ServerConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<ServerConfiguration> getAll(@Nullable List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardnp3.dnp3_server";
	private static final String SERVER_ID_COLUMN_NAME = "id";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", SERVER_ID_COLUMN_NAME };

	@Override
	public void delete(ServerConfiguration entity) {
		var sql = new DeleteForCompositeKey(requireNonNullArgument(entity, "entity").pk(), TABLE_NAME,
				PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	@Override
	public FilterResults<ServerConfiguration, UserLongCompositePK> findFiltered(ServerFilter filter,
			@Nullable List<SortDescriptor> sorts, @Nullable Long offset, @Nullable Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectServerConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql, ServerConfigurationRowMapper.INSTANCE);
	}

	@Override
	public int updateEnabledStatus(Long userId, @Nullable ServerFilter filter, boolean enabled) {
		var sql = new UpdateEnabledServerFilter(TABLE_NAME, SERVER_ID_COLUMN_NAME, userId, filter,
				enabled);
		return jdbcOps.update(sql);
	}

}
