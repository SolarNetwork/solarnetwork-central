/* ==================================================================
 * SelectChargePointActionStatus.java - 18/11/2022 6:45:25 am
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

package net.solarnetwork.central.ocpp.dao.jdbc.sql;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.WHERE_COMPONENT_PREFIX_LENGTH;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.limitOffset;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.orderBySorts;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareDateRange;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareLimitOffset;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereDateRange;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.ocpp.dao.ChargePointActionStatusFilter;

/**
 * Generate dynamic SQL for a "find charge point action status" query.
 * 
 * @author matt
 * @version 1.1
 */
public class SelectChargePointActionStatus implements PreparedStatementCreator, SqlProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	/**
	 * A standard mapping of sort keys to SQL column names suitable for ordering
	 * by charge point status entities.
	 * 
	 * <p>
	 * This map contains the following entries:
	 * </p>
	 * 
	 * <ol>
	 * <li>action -&gt; action</li>
	 * <li>connector -&gt; conn_id</li>
	 * <li>charger -&gt; cp_id</li>
	 * <li>created -&gt; created</li>
	 * <li>evse -&gt; evse_id</li>
	 * <li>time -&gt; ts</li>
	 * <li>user -&gt; user_id</li>
	 * </ol>
	 * 
	 * @see #orderBySorts(Iterable, Map, StringBuilder)
	 */
	public static final Map<String, String> SORT_KEY_MAPPING;
	static {
		Map<String, String> map = new LinkedHashMap<>(4);
		map.put("action", "action");
		map.put("connector", "conn_id");
		map.put("charger", "cp_id");
		map.put("created", "created");
		map.put("evse", "evse_id");
		map.put("time", "ts");
		map.put("user", "user_id");
		SORT_KEY_MAPPING = Collections.unmodifiableMap(map);
	}

	private final ChargePointActionStatusFilter filter;
	private final int fetchSize;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null} or invalid
	 */
	public SelectChargePointActionStatus(ChargePointActionStatusFilter filter) {
		this(filter, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 * @param fetchSize
	 *        the row fetch size
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null} or invalid
	 */
	public SelectChargePointActionStatus(ChargePointActionStatusFilter filter, int fetchSize) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		this.fetchSize = fetchSize;
	}

	private void sqlCore(StringBuilder buf, boolean ordered) {
		buf.append("""
				SELECT created, user_id, cp_id, evse_id, conn_id, action, msg_id, ts
				FROM solarev.ocpp_charge_point_action_status
				""");
		sqlWhere(buf);
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		idx += whereOptimizedArrayContains(filter.getUserIds(), "user_id", where);
		idx += whereOptimizedArrayContains(filter.getChargePointIds(), "cp_id", where);
		idx += whereOptimizedArrayContains(filter.getEvseIds(), "evse_id", where);
		idx += whereOptimizedArrayContains(filter.getConnectorIds(), "conn_id", where);
		idx += whereOptimizedArrayContains(filter.getActions(), "action", where);
		idx += whereDateRange(filter, "ts", where);
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(WHERE_COMPONENT_PREFIX_LENGTH));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		StringBuilder order = new StringBuilder();
		int idx = 2;
		if ( filter.hasSorts() ) {
			idx = orderBySorts(filter.getSorts(), SORT_KEY_MAPPING, order);
		} else {
			order.append(", user_id, cp_id, evse_id, conn_id, action");
		}
		if ( order.length() > 0 ) {
			buf.append("ORDER BY ").append(order.substring(idx));
		}
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf, true);
		sqlOrderBy(buf);
		limitOffset(filter, buf);
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		int p = prepareCore(con, stmt, 0);
		prepareLimitOffset(filter, con, stmt, p);
		if ( fetchSize > 0 ) {
			stmt.setFetchSize(fetchSize);
		}
		return stmt;
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getChargePointIds());
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getEvseIds());
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getConnectorIds());
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getActions());
		p = prepareDateRange(filter, stmt, p);
		return p;
	}

}
