/* ==================================================================
 * SelectExternalSystemForHeartbeat.java - 21/08/2022 5:23:34 pm
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

package net.solarnetwork.central.oscp.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.oscp.dao.LockingFilter;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * Select for expired system heartbeat rows.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectExternalSystemForHeartbeat implements PreparedStatementCreator, SqlProvider {

	private static final String[] LOCK_TABLE_NAMES = new String[] { "h" };

	private final OscpRole type;
	private final LockingFilter filter;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the type of token
	 * @param filter
	 *        the pagination and/or locking criteria to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SelectExternalSystemForHeartbeat(OscpRole type, LockingFilter filter) {
		super();
		this.type = requireNonNullArgument(type, "type");
		this.filter = requireNonNullArgument(filter, "filter");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder("""
				SELECT c.id, c.created, c.modified, c.user_id, c.enabled
					, c.fp_id, c.reg_status, c.cname, c.url, c.oscp_ver
					, c.heartbeat_secs, c.meas_styles, h.heartbeat_at, c.offline_at
					, c.sprops
				""");
		if ( type == OscpRole.CapacityOptimizer ) {
			buf.append("	, c.pub_in, c.pub_flux, c.source_id_tmpl\n");
		}
		buf.append("FROM solaroscp.oscp_");
		buf.append(type.getAlias()).append("_conf c\n");
		buf.append("INNER JOIN solaroscp.oscp_").append(type.getAlias()).append("_heartbeat h\n");
		buf.append("\tON c.user_id = h.user_id AND c.id = h.id\n");
		buf.append("""
				WHERE c.reg_status = ascii('r')
					AND c.heartbeat_secs IS NOT NULL
					AND (h.heartbeat_at IS NULL OR heartbeat_at
						+ (c.heartbeat_secs * INTERVAL '1 second') < CURRENT_TIMESTAMP)
					AND c.enabled = TRUE""");
		CommonSqlUtils.limitOffset(filter, buf);
		if ( filter.isLockResults() ) {
			CommonSqlUtils.forUpdate(filter.isSkipLockedResults(), LOCK_TABLE_NAMES, buf);
		}
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		CommonSqlUtils.prepareLimitOffset(filter, con, stmt, 0);
		return stmt;
	}

}
