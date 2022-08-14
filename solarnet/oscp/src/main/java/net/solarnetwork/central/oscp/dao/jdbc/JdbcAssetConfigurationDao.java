/* ==================================================================
 * JdbcAssetConfigurationDao.java - 14/08/2022 7:33:47 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao.jdbc;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.domain.UserLongPK;
import net.solarnetwork.central.oscp.dao.AssetConfigurationDao;
import net.solarnetwork.central.oscp.dao.BasicConfigurationFilter;
import net.solarnetwork.central.oscp.dao.jdbc.sql.DeleteAssetConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertAssetConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectAssetConfiguration;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateAssetConfiguration;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link JdbcAssetConfigurationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcAssetConfigurationDao implements AssetConfigurationDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcAssetConfigurationDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends AssetConfiguration> getObjectType() {
		return AssetConfiguration.class;
	}

	@Override
	public UserLongPK create(Long userId, AssetConfiguration entity) {
		final InsertAssetConfiguration sql = new InsertAssetConfiguration(userId, entity);
		final Long id = CommonJdbcUtils.updateWithGeneratedLong(jdbcOps, sql, "id");
		return (id != null ? new UserLongPK(userId, id) : null);
	}

	@Override
	public UserLongPK save(AssetConfiguration entity) {
		if ( !entity.getId().entityIdIsAssigned() ) {
			return create(entity.getId().getUserId(), entity);
		}
		final UpdateAssetConfiguration sql = new UpdateAssetConfiguration(entity.getId(), entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public Collection<AssetConfiguration> findAll(Long userId, List<SortDescriptor> sorts) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		SelectAssetConfiguration sql = new SelectAssetConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, AssetConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).collect(toList());
	}

	@Override
	public Collection<AssetConfiguration> findAllForCapacityGroup(Long userId, Long capacityGroupId,
			List<SortDescriptor> sorts) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(requireNonNullArgument(userId, "userId"));
		filter.setGroupId(requireNonNullArgument(capacityGroupId, "capacityGroupId"));
		SelectAssetConfiguration sql = new SelectAssetConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, AssetConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).collect(toList());
	}

	@Override
	public AssetConfiguration get(UserLongPK id) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(
				requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId"));
		filter.setConfigurationId(requireNonNullArgument(id.getEntityId(), "id.entityId"));
		SelectAssetConfiguration sql = new SelectAssetConfiguration(filter);
		var results = executeFilterQuery(jdbcOps, filter, sql, AssetConfigurationRowMapper.INSTANCE);
		return stream(results.spliterator(), false).findFirst().orElse(null);
	}

	@Override
	public Collection<AssetConfiguration> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(AssetConfiguration entity) {
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(requireNonNullArgument(requireNonNullArgument(entity, "entity").getUserId(),
				"entity.userId"));
		filter.setConfigurationId(requireNonNullArgument(entity.getEntityId(), "entity.entityId"));
		DeleteAssetConfiguration sql = new DeleteAssetConfiguration(filter);
		jdbcOps.update(sql);
	}

}
