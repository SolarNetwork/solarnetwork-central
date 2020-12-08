/* ==================================================================
 * StaleAggregateDatumEntityRowMapper.java - 3/11/2020 12:54:55 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.dao.StaleAuditDatumEntity;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Map stale audit rows into {@link StaleAuditDatumEntity} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>stream_id</li>
 * <li>ts_start</li>
 * <li>kind (character)</li>
 * <li>created timestamp</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class StaleAuditDatumEntityRowMapper implements RowMapper<StaleAuditDatum> {

	/** A default mapper instance. */
	public static final RowMapper<StaleAuditDatum> INSTANCE = new StaleAuditDatumEntityRowMapper();

	@Override
	public StaleAuditDatumEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = DatumJdbcUtils.getUuid(rs, 1);
		Instant ts = rs.getTimestamp(2).toInstant();
		String aggKind = rs.getString(3);
		Instant created = rs.getTimestamp(4).toInstant();
		return new StaleAuditDatumEntity(streamId, ts, Aggregation.forKey(aggKind), created);
	}

}
