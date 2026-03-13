/* ==================================================================
 * JdbcEndpointAuthAuthConfigurationDao.java - 21/02/2024 4:24:57 pm
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

import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.common.dao.jdbc.sql.UpdateEnabledIdFilter;
import net.solarnetwork.central.domain.UserUuidLongCompositePK;
import net.solarnetwork.central.inin.dao.BasicFilter;
import net.solarnetwork.central.inin.dao.EndpointAuthConfigurationDao;
import net.solarnetwork.central.inin.dao.EndpointAuthFilter;
import net.solarnetwork.central.inin.dao.jdbc.sql.SelectEndpointAuthConfiguration;
import net.solarnetwork.central.inin.dao.jdbc.sql.UpsertEndpointAuthConfiguration;
import net.solarnetwork.central.inin.domain.EndpointAuthConfiguration;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link EndpointAuthConfigurationDao}.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcEndpointAuthConfigurationDao implements EndpointAuthConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcEndpointAuthConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends EndpointAuthConfiguration> getObjectType() {
		return EndpointAuthConfiguration.class;
	}

	@Override
	public EndpointAuthConfiguration entityKey(UserUuidLongCompositePK id) {
		return new EndpointAuthConfiguration(id, Instant.EPOCH);
	}

	@Override
	public UserUuidLongCompositePK create(Long userId, UUID endpointId,
			EndpointAuthConfiguration entity) {
		final var sql = new UpsertEndpointAuthConfiguration(userId, endpointId, entity);

		jdbcOps.update(sql);
		return entity.pk();
	}

	@Override
	public Collection<EndpointAuthConfiguration> findAll(Long userId, UUID endpointId,
			@Nullable List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		filter.setEndpointId(endpointId);
		var sql = new SelectEndpointAuthConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				EndpointAuthConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public FilterResults<EndpointAuthConfiguration, UserUuidLongCompositePK> findFiltered(
			EndpointAuthFilter filter, @Nullable List<SortDescriptor> sorts, @Nullable Long offset,
			@Nullable Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectEndpointAuthConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql, EndpointAuthConfigurationRowMapper.INSTANCE);
	}

	@Override
	public UserUuidLongCompositePK save(EndpointAuthConfiguration entity) {
		final var sql = new UpsertEndpointAuthConfiguration(entity.getUserId(), entity.getEndpointId(),
				entity);

		jdbcOps.update(sql);
		return entity.pk();
	}

	@Override
	public @Nullable EndpointAuthConfiguration get(UserUuidLongCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setEndpointId(requireNonNullArgument(id.getGroupId(), "id.groupId"));
		filter.setCredentialId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectEndpointAuthConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				EndpointAuthConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<EndpointAuthConfiguration> getAll(@Nullable List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardin.inin_endpoint_auth_cred";
	private static final String GROUP_ID_COLUMN_NAME = "endpoint_id";
	private static final String ID_COLUMN_NAME = "cred_id";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", GROUP_ID_COLUMN_NAME,
			ID_COLUMN_NAME };

	@Override
	public void delete(EndpointAuthConfiguration entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").pk(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	@Override
	public int updateEnabledStatus(Long userId, @Nullable EndpointAuthFilter filter, boolean enabled) {
		UserUuidLongCompositePK key = filter != null && filter.hasEndpointCriteria()
				&& filter.hasCredentialCriteria()
						? new UserUuidLongCompositePK(userId,
								nonnull(filter.getEndpointId(), "Endpoint ID"),
								nonnull(filter.getCredentialId(), "Credential ID"))
						: filter != null && filter.hasEndpointCriteria()
								? UserUuidLongCompositePK.unassignedEntityIdKey(userId,
										nonnull(filter.getEndpointId(), "Endpoint ID"))
								: UserUuidLongCompositePK.unassignedEntityIdKey(userId);
		var sql = new UpdateEnabledIdFilter(TABLE_NAME, PK_COLUMN_NAMES, key, enabled);
		return jdbcOps.update(sql);
	}

}
