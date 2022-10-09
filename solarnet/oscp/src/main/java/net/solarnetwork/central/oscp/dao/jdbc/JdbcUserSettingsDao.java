/* ==================================================================
 * JdbcUserSettingsDao.java - 10/10/2022 9:00:00 am
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
import net.solarnetwork.central.oscp.dao.UserSettingsDao;
import net.solarnetwork.central.oscp.dao.jdbc.sql.DeleteUserSettings;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertUserSettings;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectUserSettings;
import net.solarnetwork.central.oscp.domain.UserSettings;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link UserSettingsDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcUserSettingsDao implements UserSettingsDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcUserSettingsDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends UserSettings> getObjectType() {
		return UserSettings.class;
	}

	@Override
	public Long save(UserSettings entity) {
		final var sql = new InsertUserSettings(entity);
		jdbcOps.update(sql);
		return entity.getId();
	}

	@Override
	public UserSettings get(Long id) {
		final var sql = new SelectUserSettings(id);
		Collection<UserSettings> results = jdbcOps.query(sql, UserSettingsRowMapper.INSTANCE);
		return results.stream().findFirst().orElse(null);
	}

	@Override
	public Collection<UserSettings> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(UserSettings entity) {
		final Long userId = requireNonNullArgument(requireNonNullArgument(entity, "entity").getId(),
				"entity.id");
		jdbcOps.update(new DeleteUserSettings(userId));
	}

}
