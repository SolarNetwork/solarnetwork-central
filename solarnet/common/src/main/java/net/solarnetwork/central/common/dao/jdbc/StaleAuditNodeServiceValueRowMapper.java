/* ==================================================================
 * StaleAuditNodeServiceValueRowMapper.java - 22/01/2023 3:17:18 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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
import net.solarnetwork.central.dao.StaleAuditNodeServiceEntity;
import net.solarnetwork.central.domain.AggregateDatumId;
import net.solarnetwork.central.domain.StaleAuditNodeServiceValue;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Map datum rows into {@link StaleAuditNodeServiceValue} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>ts_start</li>
 * <li>node_id</li>
 * <li>service</li>
 * <li>agg_kind (key)</li>
 * <li>created</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class StaleAuditNodeServiceValueRowMapper implements RowMapper<StaleAuditNodeServiceValue> {

	/** A default mapper instance. */
	public static final RowMapper<StaleAuditNodeServiceValue> INSTANCE = new StaleAuditNodeServiceValueRowMapper();

	@Override
	public StaleAuditNodeServiceValue mapRow(ResultSet rs, int rowNum) throws SQLException {
		Timestamp timestamp = rs.getTimestamp(1);
		Number nodeId = (Number) rs.getObject(2);
		String service = rs.getString(3);
		Aggregation aggregation = Aggregation.forKey(rs.getString(4));
		Timestamp created = rs.getTimestamp(5);
		return new StaleAuditNodeServiceEntity(AggregateDatumId.nodeId(
				nodeId instanceof Long l ? l : nodeId != null ? nodeId.longValue() : null, service,
				timestamp.toInstant(), aggregation), created.toInstant());
	}

}
