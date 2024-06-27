/* ==================================================================
 * JdbcUserFluxDefaultAggregatePublishConfigurationDao.java - 25/06/2024 10:47:54 am
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

package net.solarnetwork.central.user.flux.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForId;
import net.solarnetwork.central.user.flux.dao.UserFluxDefaultAggregatePublishConfigurationDao;
import net.solarnetwork.central.user.flux.dao.jdbc.sql.SelectUserFluxDefaultAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.dao.jdbc.sql.UpsertUserFluxDefaultAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfiguration;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of
 * {@link UserFluxDefaultAggregatePublishConfigurationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcUserFluxDefaultAggregatePublishConfigurationDao
		implements UserFluxDefaultAggregatePublishConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcUserFluxDefaultAggregatePublishConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends UserFluxDefaultAggregatePublishConfiguration> getObjectType() {
		return UserFluxDefaultAggregatePublishConfiguration.class;
	}

	@Override
	public Long save(UserFluxDefaultAggregatePublishConfiguration entity) {
		var sql = new UpsertUserFluxDefaultAggregatePublishConfiguration(
				requireNonNullArgument(entity, "entity").getId(), entity);
		jdbcOps.update(sql);
		return entity.getId();
	}

	@Override
	public UserFluxDefaultAggregatePublishConfiguration get(Long id) {
		var sql = new SelectUserFluxDefaultAggregatePublishConfiguration(id);
		var results = jdbcOps.query(sql, UserFluxDefaultAggregatePublishConfigurationRowMapper.INSTANCE);
		return (!results.isEmpty() ? results.get(0) : null);
	}

	@Override
	public Collection<UserFluxDefaultAggregatePublishConfiguration> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solaruser.user_flux_default_agg_pub_settings";
	private static final String PRIMARY_KEY_COLUMN_NAME = "user_id";

	@Override
	public void delete(UserFluxDefaultAggregatePublishConfiguration entity) {
		var sql = new DeleteForId(requireNonNullArgument(entity, "entity").getUserId(), TABLE_NAME,
				PRIMARY_KEY_COLUMN_NAME);
		jdbcOps.update(sql);
	}

}
