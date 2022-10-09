/* ==================================================================
 * JdbcCapacityGroupSettingsDao.java - 10/10/2022 9:48:00 am
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.CapacityGroupSettingsDao;
import net.solarnetwork.central.oscp.dao.jdbc.sql.DeleteCapacityGroupSettings;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertCapacityGroupSettings;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectCapacityGroupSettings;
import net.solarnetwork.central.oscp.domain.CapacityGroupSettings;
import net.solarnetwork.central.oscp.domain.DatumPublishSettings;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link CapacityGroupSettingsDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcCapacityGroupSettingsDao implements CapacityGroupSettingsDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcCapacityGroupSettingsDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public UserLongCompositePK create(Long userId, CapacityGroupSettings entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<CapacityGroupSettings> findAll(Long userId, List<SortDescriptor> sorts) {
		final var sql = new SelectCapacityGroupSettings(requireNonNullArgument(userId, "userId"), null,
				false);
		return jdbcOps.query(sql, CapacityGroupSettingsRowMapper.INSTANCE);
	}

	@Override
	public Class<? extends CapacityGroupSettings> getObjectType() {
		return CapacityGroupSettings.class;
	}

	@Override
	public UserLongCompositePK save(CapacityGroupSettings entity) {
		final var sql = new InsertCapacityGroupSettings(entity);
		jdbcOps.update(sql);
		return entity.getId();
	}

	@Override
	public CapacityGroupSettings get(UserLongCompositePK id) {
		final var sql = new SelectCapacityGroupSettings(requireNonNullArgument(id, "id").getUserId(),
				id.getEntityId(), false);
		Collection<CapacityGroupSettings> results = jdbcOps.query(sql,
				CapacityGroupSettingsRowMapper.INSTANCE);
		return results.stream().findFirst().orElse(null);
	}

	@Override
	public Collection<CapacityGroupSettings> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(CapacityGroupSettings entity) {
		final Long userId = requireNonNullArgument(requireNonNullArgument(entity, "entity").getUserId(),
				"entity.userId");
		final Long groupId = entity.getGroupId();
		jdbcOps.update(new DeleteCapacityGroupSettings(userId, groupId));

	}

	@Override
	public DatumPublishSettings resolveDatumPublishSettings(Long userId, Long groupId) {
		final var sql = new SelectCapacityGroupSettings(userId, groupId, true);
		Collection<CapacityGroupSettings> results = jdbcOps.query(sql,
				CapacityGroupSettingsRowMapper.INSTANCE);
		return results.stream().findFirst().orElse(null);
	}

}
