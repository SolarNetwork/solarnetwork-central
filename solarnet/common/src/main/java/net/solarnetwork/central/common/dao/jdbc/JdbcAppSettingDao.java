/* ==================================================================
 * JdbcAppSettingDao.java - 10/11/2021 9:18:37 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteAppSetting;
import net.solarnetwork.central.common.dao.jdbc.sql.InsertAppSetting;
import net.solarnetwork.central.common.dao.jdbc.sql.SelectAppSetting;
import net.solarnetwork.central.dao.AppSettingDao;
import net.solarnetwork.central.domain.AppSetting;
import net.solarnetwork.central.domain.KeyTypePK;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link AppSettingDao}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class JdbcAppSettingDao implements AppSettingDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcAppSettingDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends AppSetting> getObjectType() {
		return AppSetting.class;
	}

	@Override
	public KeyTypePK save(AppSetting entity) {
		requireNonNullArgument(entity, "entity");
		jdbcOps.update(new InsertAppSetting(entity, true));
		return entity.getId();
	}

	@Override
	public AppSetting get(KeyTypePK id) {
		requireNonNullArgument(id, "id");
		List<AppSetting> result = jdbcOps.query(
				SelectAppSetting.selectForKeyType(id.getKey(), id.getType()),
				AppSettingRowMapper.INSTANCE);
		return (!result.isEmpty() ? result.get(0) : null);
	}

	@Override
	public Collection<AppSetting> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(AppSetting entity) {
		requireNonNullArgument(entity, "entity");
		jdbcOps.update(DeleteAppSetting.deleteForKeyType(entity.getKey(), entity.getType()));
	}

	@Override
	public int deleteAll(String key) {
		requireNonNullArgument(key, "key");
		return jdbcOps.update(DeleteAppSetting.deleteForKey(key));
	}

	@Override
	public AppSetting lockForUpdate(String key, String type) {
		List<AppSetting> result = jdbcOps.query(SelectAppSetting.selectForKeyType(key, type, true),
				AppSettingRowMapper.INSTANCE);
		return (!result.isEmpty() ? result.get(0) : null);
	}

	@Override
	public Collection<AppSetting> lockForUpdate(String key) {
		return jdbcOps.query(SelectAppSetting.selectForKey(key, true), AppSettingRowMapper.INSTANCE);
	}

}
