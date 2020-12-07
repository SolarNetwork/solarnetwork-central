/* ==================================================================
 * AuditDatumIoEntityRowMapper.java - 3/11/2020 1:36:39 pm
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
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;

/**
 * Map daily datum audit rows into {@link AuditDatumEntity} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>stream_id</li>
 * <li>ts_start</li>
 * <li>prop_count</li>
 * <li>datum_q_count</li>
 * <li>datum_count</li>
 * <li>datum_hourly_count</li>
 * <li>datum_daily_pres</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class AuditDatumDailyEntityRowMapper implements RowMapper<AuditDatum> {

	/** A default mapper instance. */
	public static final RowMapper<AuditDatum> INSTANCE = new AuditDatumDailyEntityRowMapper();

	@Override
	public AuditDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = DatumSqlUtils.getUuid(rs, 1);
		Instant ts = rs.getTimestamp(2).toInstant();
		boolean dayPresent = rs.getBoolean(7);
		return AuditDatumEntity.dailyAuditDatum(streamId, ts, rs.getLong(5), rs.getLong(6),
				dayPresent ? 1 : 0, rs.getLong(3), rs.getLong(4));
	}

}
