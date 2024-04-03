/* ==================================================================
 * JdbcEndpointConfigurationDao.java - 21/02/2024 3:29:48 pm
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
import java.util.UUID;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.common.dao.jdbc.sql.UpdateEnabledIdFilter;
import net.solarnetwork.central.inin.dao.BasicFilter;
import net.solarnetwork.central.inin.dao.EndpointConfigurationDao;
import net.solarnetwork.central.inin.dao.EndpointFilter;
import net.solarnetwork.central.inin.dao.jdbc.sql.SelectEndpointConfiguration;
import net.solarnetwork.central.inin.dao.jdbc.sql.UpsertEndpointConfiguration;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link EndpointConfigurationDao}.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcEndpointConfigurationDao implements EndpointConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcEndpointConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends EndpointConfiguration> getObjectType() {
		return EndpointConfiguration.class;
	}

	@Override
	public EndpointConfiguration entityKey(UserUuidPK id) {
		return new EndpointConfiguration(id, now());
	}

	@Override
	public UserUuidPK create(Long userId, EndpointConfiguration entity) {
		final var sql = new UpsertEndpointConfiguration(userId, entity);

		int count = jdbcOps.update(sql);
		return (count > 0 ? new UserUuidPK(userId, sql.getEndpointId()) : null);
	}

	@Override
	public Collection<EndpointConfiguration> findAll(Long userId, List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectEndpointConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, EndpointConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public FilterResults<EndpointConfiguration, UserUuidPK> findFiltered(EndpointFilter filter,
			List<SortDescriptor> sorts, Integer offset, Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectEndpointConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql, EndpointConfigurationRowMapper.INSTANCE);
	}

	@Override
	public UserUuidPK save(EndpointConfiguration entity) {
		final var sql = new UpsertEndpointConfiguration(entity.getUserId(), entity);

		int count = jdbcOps.update(sql);
		return (count > 0 ? new UserUuidPK(entity.getUserId(), sql.getEndpointId()) : null);
	}

	@Override
	public EndpointConfiguration get(UserUuidPK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setEndpointId(requireNonNullArgument(id.getUuid(), "id.uuid"));
		var sql = new SelectEndpointConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, EndpointConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public EndpointConfiguration getForEndpointId(UUID endpointId) {
		var filter = new BasicFilter();
		filter.setEndpointId(requireNonNullArgument(endpointId, "endpointId"));
		var sql = new SelectEndpointConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, EndpointConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<EndpointConfiguration> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardin.inin_endpoint";
	private static final String ID_COLUMN_NAME = "id";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", ID_COLUMN_NAME };

	@Override
	public void delete(EndpointConfiguration entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").getId(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	@Override
	public int updateEnabledStatus(Long userId, EndpointFilter filter, boolean enabled) {
		UserUuidPK key = filter != null && filter.hasEndpointCriteria()
				? new UserUuidPK(userId, filter.getEndpointId())
				: UserUuidPK.unassignedUuidKey(userId);
		var sql = new UpdateEnabledIdFilter(TABLE_NAME, PK_COLUMN_NAMES, key, enabled);
		return jdbcOps.update(sql);
	}

}
