/* ==================================================================
 * JdbcUserEventDao.java - 1/08/2022 2:42:50 pm
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

package net.solarnetwork.central.common.dao.jdbc;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.UserEventDao;
import net.solarnetwork.central.common.dao.UserEventFilter;
import net.solarnetwork.central.common.dao.UserEventMaintenanceDao;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteUserEvent;
import net.solarnetwork.central.common.dao.jdbc.sql.InsertUserEvent;
import net.solarnetwork.central.common.dao.jdbc.sql.SelectUserEvent;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link UserEventDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcUserEventDao implements UserEventDao, UserEventMaintenanceDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcUserEventDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public void add(UserEvent event) {
		final InsertUserEvent sql = new InsertUserEvent(event);
		jdbcOps.update(sql);
	}

	@Override
	public FilterResults<UserEvent, UserUuidPK> findFiltered(UserEventFilter filter,
			List<SortDescriptor> sorts, Integer offset, Integer max) {
		SelectUserEvent sql = new SelectUserEvent(filter);
		return executeFilterQuery(jdbcOps, filter, sql, UserEventRowMapper.INSTANCE);
	}

	@Override
	public long purgeEvents(UserEventPurgeFilter filter) {
		DeleteUserEvent sql = new DeleteUserEvent(filter);
		return jdbcOps.update(sql);
	}

}
