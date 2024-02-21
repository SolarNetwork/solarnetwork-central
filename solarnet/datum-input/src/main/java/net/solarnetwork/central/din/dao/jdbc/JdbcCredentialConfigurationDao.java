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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
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
 * @version 1.0
 */
public class JdbcCredentialConfigurationDao implements CredentialConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
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
	public UserLongCompositePK create(Long userId, CredentialConfiguration entity) {
		final var sql = new InsertCredentialConfiguration(userId, entity);

		final Long id = CommonJdbcUtils.updateWithGeneratedLong(jdbcOps, sql, "id");
		var pk = (id != null ? new UserLongCompositePK(userId, id) : null);

		return pk;
	}

	@Override
	public Collection<CredentialConfiguration> findAll(Long userId, List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectCredentialConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				CredentialConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public FilterResults<CredentialConfiguration, UserLongCompositePK> findFiltered(
			CredentialFilter filter, List<SortDescriptor> sorts, Integer offset, Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		var sql = new SelectCredentialConfiguration(filter);
		return executeFilterQuery(jdbcOps, filter, sql, CredentialConfigurationRowMapper.INSTANCE);
	}

	@Override
	public UserLongCompositePK save(CredentialConfiguration entity) {
		if ( !entity.getId().entityIdIsAssigned() ) {
			return create(entity.getId().getUserId(), entity);
		}
		final UpdateCredentialConfiguration sql = new UpdateCredentialConfiguration(entity.getId(),
				entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public CredentialConfiguration get(UserLongCompositePK id) {
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
	public Collection<CredentialConfiguration> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardin.din_credential";
	private static final String ID_COLUMN_NAME = "id";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", ID_COLUMN_NAME };

	@Override
	public void delete(CredentialConfiguration entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").getId(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	@Override
	public int updateEnabledStatus(Long userId, CredentialFilter filter, boolean enabled) {
		UserLongCompositePK key = filter != null && filter.hasCredentialCriteria()
				? new UserLongCompositePK(userId, filter.getCredentialId())
				: UserLongCompositePK.unassignedEntityIdKey(userId);
		var sql = new UpdateEnabledIdFilter(TABLE_NAME, PK_COLUMN_NAMES, key, enabled);
		return jdbcOps.update(sql);
	}

}
