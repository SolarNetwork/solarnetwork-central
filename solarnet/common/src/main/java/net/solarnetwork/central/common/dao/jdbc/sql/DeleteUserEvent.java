/* ==================================================================
 * DeleteUserEvent.java - 3/08/2022 2:37:27 pm
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

package net.solarnetwork.central.common.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.biz.UuidTimestampDecoder;
import net.solarnetwork.central.common.dao.BasicUserEventFilter;
import net.solarnetwork.central.common.dao.UserEventMaintenanceDao.UserEventPurgeFilter;
import net.solarnetwork.central.support.TimeBasedV7UuidGenerator;

/**
 * Delete {@link net.solarnetwork.central.domain.UserEvent} entities matching a
 * filter.
 * 
 * @author matt
 * @version 1.0
 */
public class DeleteUserEvent implements PreparedStatementCreator, SqlProvider {

	private final UuidTimestampDecoder uuidTimestampDecoder;
	private final UserEventPurgeFilter filter;

	/**
	 * Create an instance for deleting events for a specific user, older than a
	 * given date.
	 * 
	 * @param userId
	 *        the ID of the user to delete events for
	 * @param date
	 *        the date to delete all events before
	 * @return the new instance
	 */
	public static DeleteUserEvent deleteForUserOlderThanDate(Long userId, Instant date) {
		BasicUserEventFilter f = new BasicUserEventFilter();
		f.setUserId(userId);
		f.setEndDate(date);
		return new DeleteUserEvent(f);
	}

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter criteria
	 */
	public DeleteUserEvent(UserEventPurgeFilter filter) {
		this(TimeBasedV7UuidGenerator.INSTANCE_MICROS, filter);
	}

	public DeleteUserEvent(UuidTimestampDecoder uuidTimestampDecoder, UserEventPurgeFilter filter) {
		super();
		this.uuidTimestampDecoder = requireNonNullArgument(uuidTimestampDecoder, "uuidTimestampDecoder");
		this.filter = requireNonNullArgument(filter, "filter");
		requireNonEmptyArgument(filter.getUserIds(), "filter.userIds");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append("DELETE FROM solaruser.user_event_log\n");
		sqlWhere(buf);
		return buf.toString();
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		CommonSqlUtils.whereOptimizedArrayContains(filter.getUserIds(), "user_id", where);
		CommonSqlUtils.whereDateRange(filter, "event_id", where);
		buf.append("WHERE").append(where.substring(4));
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		p = CommonSqlUtils.prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		if ( filter.getStartDate() != null ) {
			stmt.setObject(++p, uuidTimestampDecoder.createTimestampBoundary(filter.getStartDate()));
		}
		if ( filter.getEndDate() != null ) {
			stmt.setObject(++p, uuidTimestampDecoder.createTimestampBoundary(filter.getEndDate()));
		}
		return stmt;
	}

	/**
	 * Get the configured filter.
	 * 
	 * @return the filter
	 */
	public UserEventPurgeFilter getFilter() {
		return filter;
	}

}
