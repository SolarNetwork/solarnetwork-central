/* ==================================================================
 * InsertUserEvent.java - 1/08/2022 2:43:51 pm
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.UserEvent;

/**
 * Insert {@link UserEvent} entities.
 * 
 * @author matt
 * @version 1.0
 */
public class InsertUserEvent implements PreparedStatementCreator, SqlProvider {

	private static final String SQL;
	static {
		// @formatter:off
		SQL =     "INSERT INTO solaruser.user_event_log (user_id,event_id,tags,message,jdata)\n"
				+ "VALUES (?,?,?,?,?::jsonb)\n"
				+ "ON CONFLICT (user_id,event_id) DO NOTHING";
		// @formatter:on
	}

	private final UserEvent event;

	/**
	 * Constructor.
	 * 
	 * @param event
	 *        the event to insert
	 */
	public InsertUserEvent(UserEvent event) {
		super();
		this.event = requireNonNullArgument(event, "event");
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setObject(1, event.getUserId());
		stmt.setObject(2, event.getEventId());

		CommonSqlUtils.prepareArrayParameter(con, stmt, 2, event.getTags());

		if ( event.getMessage() != null ) {
			stmt.setString(4, event.getMessage());
		} else {
			stmt.setNull(4, Types.VARCHAR);
		}
		if ( event.getData() != null ) {
			stmt.setString(5, event.getData());
		} else {
			stmt.setNull(5, Types.VARCHAR);
		}
		return stmt;
	}

}
