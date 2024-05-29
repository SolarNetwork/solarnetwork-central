/* ==================================================================
 * StaleAuditUserServiceValueRowMapper.java - 29/05/2024 3:59:24 pm
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

package net.solarnetwork.central.common.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.dao.StaleAuditUserServiceEntity;
import net.solarnetwork.central.domain.AggregateDatumId;
import net.solarnetwork.central.domain.StaleAuditUserServiceValue;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Map datum rows into {@link StaleAuditUserServiceValue} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>ts_start</li>
 * <li>user_id</li>
 * <li>service</li>
 * <li>agg_kind (key)</li>
 * <li>created</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class StaleAuditUserServiceValueRowMapper implements RowMapper<StaleAuditUserServiceValue> {

	/** A default mapper instance. */
	public static final RowMapper<StaleAuditUserServiceValue> INSTANCE = new StaleAuditUserServiceValueRowMapper();

	@Override
	public StaleAuditUserServiceValue mapRow(ResultSet rs, int rowNum) throws SQLException {
		Timestamp timestamp = rs.getTimestamp(1);
		Number userId = (Number) rs.getObject(2);
		String service = rs.getString(3);
		Aggregation aggregation = Aggregation.forKey(rs.getString(4));
		Timestamp created = rs.getTimestamp(5);
		return new StaleAuditUserServiceEntity(AggregateDatumId.nodeId(
				userId instanceof Long l ? l : userId != null ? userId.longValue() : null, service,
				timestamp.toInstant(), aggregation), created.toInstant());
	}

}
