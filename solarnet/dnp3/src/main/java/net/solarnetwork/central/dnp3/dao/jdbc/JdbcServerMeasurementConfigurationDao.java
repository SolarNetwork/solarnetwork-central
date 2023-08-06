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
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.dnp3.dao.BasicFilter;
import net.solarnetwork.central.dnp3.dao.ServerMeasurementConfigurationDao;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.SelectServerMeasurementConfiguration;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.UpsertServerMeasurementConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link ServerMeasurementConfigurationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcServerMeasurementConfigurationDao implements ServerMeasurementConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcServerMeasurementConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends ServerMeasurementConfiguration> getObjectType() {
		return ServerMeasurementConfiguration.class;
	}

	@Override
	public UserLongIntegerCompositePK create(Long userId, Long serverId,
			ServerMeasurementConfiguration entity) {
		final var sql = new UpsertServerMeasurementConfiguration(userId, serverId, entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public Collection<ServerMeasurementConfiguration> findAll(Long userId, Long serverId,
			List<SortDescriptor> sorts) {
		var filter = new BasicFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		filter.setServerId(serverId);
		var sql = new SelectServerMeasurementConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				ServerMeasurementConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).toList();
	}

	@Override
	public UserLongIntegerCompositePK save(ServerMeasurementConfiguration entity) {
		return create(entity.getUserId(), entity.getServerId(), entity);
	}

	@Override
	public ServerMeasurementConfiguration get(UserLongIntegerCompositePK id) {
		var filter = new BasicFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setServerId(requireNonNullArgument(id.getGroupId(), "id.groupId"));
		filter.setIndex(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		var sql = new SelectServerMeasurementConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql,
				ServerMeasurementConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<ServerMeasurementConfiguration> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardnp3.dnp3_server_meas";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", "server_id", "idx" };

	@Override
	public void delete(ServerMeasurementConfiguration entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").getId(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

}
