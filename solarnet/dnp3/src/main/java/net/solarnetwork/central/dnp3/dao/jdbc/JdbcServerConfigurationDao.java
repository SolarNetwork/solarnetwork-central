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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.dnp3.dao.BasicFilter;
import net.solarnetwork.central.dnp3.dao.ServerConfigurationDao;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.InsertServerConfiguration;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.SelectServerConfiguration;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.UpdateServerConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link ServerConfigurationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcServerConfigurationDao implements ServerConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
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

		final Long id = CommonJdbcUtils.updateWithGeneratedLong(jdbcOps, sql, "id");
		var pk = (id != null ? new UserLongCompositePK(userId, id) : null);

		return pk;
	}

	@Override
	public Collection<ServerConfiguration> findAll(Long userId, List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		var sql = new SelectServerConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, ServerConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public UserLongCompositePK save(ServerConfiguration entity) {
		if ( !entity.getId().entityIdIsAssigned() ) {
			return create(entity.getId().getUserId(), entity);
		}
		final UpdateServerConfiguration sql = new UpdateServerConfiguration(entity.getId(), entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public ServerConfiguration get(UserLongCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setServerId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectServerConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, ServerConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<ServerConfiguration> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardnp3.dnp3_server";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", "id" };

	@Override
	public void delete(ServerConfiguration entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").getId(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

}
