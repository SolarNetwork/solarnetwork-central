/* ==================================================================
 * AuditNodeServiceEntityRowMapper.java - 22/01/2023 12:32:02 pm
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
import net.solarnetwork.central.dao.AuditNodeServiceEntity;
import net.solarnetwork.central.domain.AuditNodeServiceValue;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumId;

/**
 * Map datum rows into {@link AuditNodeServiceValue} instances.
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
 * <li>cnt</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class AuditNodeServiceValueRowMapper implements RowMapper<AuditNodeServiceValue> {

	/** A default mapper instance. */
	public static final RowMapper<AuditNodeServiceValue> INSTANCE = new AuditNodeServiceValueRowMapper();

	@Override
	public AuditNodeServiceValue mapRow(ResultSet rs, int rowNum) throws SQLException {
		Timestamp timestamp = rs.getTimestamp(1);
		Number nodeId = (Number) rs.getObject(2);
		String service = rs.getString(3);
		Aggregation aggregation = Aggregation.forKey(rs.getString(4));
		Number count = (Number) rs.getObject(5);
		return new AuditNodeServiceEntity(
				DatumId.nodeId(nodeId instanceof Long l ? l : nodeId != null ? nodeId.longValue() : null,
						service, timestamp.toInstant()),
				aggregation, count != null ? count.longValue() : 0L);
	}

}
