/* ==================================================================
 * JdbcUserSettingsEntityDao.java - 28/10/2024 7:34:16 am
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

package net.solarnetwork.central.c2c.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.c2c.dao.UserSettingsEntityDao;
import net.solarnetwork.central.c2c.dao.jdbc.sql.DeleteUserSettingsEntity;
import net.solarnetwork.central.c2c.dao.jdbc.sql.SelectUserSettingsEntity;
import net.solarnetwork.central.c2c.dao.jdbc.sql.UpsertUserSettingsEntity;
import net.solarnetwork.central.c2c.domain.UserSettingsEntity;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link UserSettingsEntityDao}.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcUserSettingsEntityDao implements UserSettingsEntityDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcUserSettingsEntityDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends UserSettingsEntity> getObjectType() {
		return UserSettingsEntity.class;
	}

	@Override
	public Long save(UserSettingsEntity entity) {
		Long userId = requireNonNullArgument(requireNonNullArgument(entity, "entity").getUserId(),
				"entity.userId");
		final var sql = new UpsertUserSettingsEntity(userId, entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? userId : null);
	}

	@Override
	public UserSettingsEntity get(Long id) {
		var sql = new SelectUserSettingsEntity(id);
		List<UserSettingsEntity> results = jdbcOps.query(sql, UserSettingsEntityRowMapper.INSTANCE);
		return (!results.isEmpty() ? results.getFirst() : null);
	}

	@Override
	public List<UserSettingsEntity> getAll(List<SortDescriptor> sortDescriptors) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(UserSettingsEntity entity) {
		var sql = new DeleteUserSettingsEntity(requireNonNullArgument(
				requireNonNullArgument(entity, "entity").getUserId(), "entity.userId"));
		jdbcOps.update(sql);
	}

}
