/* ==================================================================
 * SelectExternalSystemForMeasurement.java - 21/08/2022 5:23:34 pm
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
 * Select for expired system measurement rows.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectExternalSystemForMeasurement implements PreparedStatementCreator, SqlProvider {

	private static final String[] LOCK_TABLE_NAMES = new String[] { "m" };

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
	public SelectExternalSystemForMeasurement(OscpRole type, LockingFilter filter) {
		super();
		this.type = requireNonNullArgument(type, "type");
		this.filter = requireNonNullArgument(filter, "filter");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder("""
				SELECT c.id, c.created, c.modified, c.user_id, c.enabled
					, c.fp_id, c.reg_status, c.cname, c.url, c.oscp_ver
					, c.heartbeat_secs, c.meas_styles, NULL::timestamptz AS heartbeat_at, c.offline_at
					, c.sprops
				""");
		if ( type == OscpRole.CapacityOptimizer ) {
			buf.append("	, c.pub_in, c.pub_flux, c.source_id_tmpl\n");
		}
		buf.append("""
					, g.id, g.created, g.modified, g.user_id, g.enabled, g.cname
					, g.ident, g.cp_meas_secs, g.co_meas_secs, g.cp_id, g.co_id, g.sprops
				""");
		if ( type == OscpRole.CapacityProvider ) {
			buf.append("\t, m.meas_at AS cp_meas_at, NULL::timestamptz AS co_meas_at\n");
		} else {
			buf.append("\t, NULL::timestamptz AS cp_meas_at, m.meas_at AS co_meas_at\n");
		}
		buf.append("FROM solaroscp.oscp_cg_conf g\n");
		buf.append("INNER JOIN solaroscp.oscp_").append(type.getAlias()).append("_conf c\n");
		buf.append("\tON c.user_id = g.user_id AND c.id = g.").append(type.getAlias()).append("_id\n");

		// also INNER JOIN to assets to weed out configurations that don't have any assets
		buf.append("INNER JOIN solaroscp.oscp_asset_conf a\n");
		buf.append("\tON a.user_id = g.user_id AND a.cg_id = g.id AND a.audience = ")
				.append(type.getCode()).append("\n");

		buf.append("INNER JOIN solaroscp.oscp_cg_").append(type.getAlias()).append("_meas m\n");
		buf.append("\tON m.user_id = g.user_id AND m.cg_id = g.id\n");
		buf.append("""
				WHERE c.reg_status = ascii('r')
					AND c.enabled = TRUE
					AND (m.meas_at IS NULL OR m.meas_at + (g.""");
		buf.append(type.getAlias()).append("_meas_secs * INTERVAL '1 second') < CURRENT_TIMESTAMP)");
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
