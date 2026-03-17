/* ==================================================================
 * JdbcCredentialConfigurationDao.java - 21/02/2024 7:28:29 am
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

package net.solarnetwork.central.din.dao.jdbc;

import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.updateWithGeneratedLong;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.common.dao.jdbc.sql.UpdateEnabledIdFilter;
import net.solarnetwork.central.din.dao.BasicFilter;
import net.solarnetwork.central.din.dao.CredentialConfigurationDao;
import net.solarnetwork.central.din.dao.CredentialFilter;
import net.solarnetwork.central.din.dao.jdbc.sql.InsertCredentialConfiguration;
import net.solarnetwork.central.din.dao.jdbc.sql.SelectCredentialConfiguration;
import net.solarnetwork.central.din.dao.jdbc.sql.UpdateCredentialConfiguration;
import net.solarnetwork.central.din.domain.CredentialConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link CredentialConfigurationDao}.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcCredentialConfigurationDao implements CredentialConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcCredentialConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends CredentialConfiguration> getObjectType() {
		return CredentialConfiguration.class;
	}

	@Override
	public CredentialConfiguration entityKey(UserLongCompositePK id) {
		return new CredentialConfiguration(id, Instant.EPOCH, "");
	}

	@Override
	public UserLongCompositePK create(Long userId, CredentialConfiguration entity) {
		final var sql = new InsertCredentialConfiguration(userId, entity);

		final Long id = updateWithGeneratedLong(jdbcOps, sql, "id");

		return new UserLongCompositePK(userId, id);
	}

	@Override
	public Collection<CredentialConfiguration> findAll(Long userId,
			@Nullable List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectCredentialConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CredentialConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public FilterResults<CredentialConfiguration, UserLongCompositePK> findFiltered(
			CredentialFilter filter, @Nullable List<SortDescriptor> sorts, @Nullable Long offset,
			@Nullable Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectCredentialConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql, CredentialConfigurationRowMapper.INSTANCE);
	}

	@Override
	public UserLongCompositePK save(CredentialConfiguration entity) {
		if ( !entity.id().entityIdIsAssigned() ) {
			return create(entity.getUserId(), entity);
		}
		final var sql = new UpdateCredentialConfiguration(entity.id(), entity);
		jdbcOps.update(sql);
		return entity.id();
	}

	@Override
	public @Nullable CredentialConfiguration get(UserLongCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setCredentialId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectCredentialConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CredentialConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<CredentialConfiguration> getAll(@Nullable List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardin.din_credential";
	private static final String ID_COLUMN_NAME = "id";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", ID_COLUMN_NAME };

	@Override
	public void delete(CredentialConfiguration entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").id(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	@Override
	public int updateEnabledStatus(Long userId, @Nullable CredentialFilter filter, boolean enabled) {
		UserLongCompositePK key = filter != null && filter.hasCredentialCriteria()
				? new UserLongCompositePK(userId, filter.credentialId())
				: UserLongCompositePK.unassignedEntityIdKey(userId);
		var sql = new UpdateEnabledIdFilter(TABLE_NAME, PK_COLUMN_NAMES, key, enabled);
		return jdbcOps.update(sql);
	}

}
