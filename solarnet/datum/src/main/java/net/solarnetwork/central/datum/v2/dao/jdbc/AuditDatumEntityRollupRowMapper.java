/* ==================================================================
 * AuditDatumEntityRollupRowMapper.java - 20/11/2020 6:27:52 pm
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
import java.sql.Timestamp;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntityRollup;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Map datum audit rollup rows into {@link AuditDatumRollup} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>ts_start</li>
 * <li>node_id</li>
 * <li>source_id</li>
 * <li>agg_kind</li>
 * <li>datum_count</li>
 * <li>prop_count</li>
 * <li>prop_u_count</li>
 * <li>datum_q_count</li>
 * <li>datum_hourly_count</li>
 * <li>datum_daily_count</li>
 * <li>datum_monthly_count</li>
 * </ol>
 * 
 * @author matt
 * @version 1.1
 * @since 3.8
 */
public class AuditDatumEntityRollupRowMapper implements RowMapper<AuditDatumRollup> {

	/** A default mapper instance. */
	public static final RowMapper<AuditDatumRollup> INSTANCE = new AuditDatumEntityRollupRowMapper();

	@Override
	public AuditDatumRollup mapRow(ResultSet rs, int rowNum) throws SQLException {
		Timestamp timestamp = rs.getTimestamp(1);
		Number nodeId = (Number) rs.getObject(2);
		String sourceId = rs.getString(3);
		Aggregation aggregation = Aggregation.forKey(rs.getString(4));
		Number datumCount = (Number) rs.getObject(5);
		Number datumPropertyCount = (Number) rs.getObject(6);
		Number datumPropertyUpdateCount = (Number) rs.getObject(7);
		Number datumQueryCount = (Number) rs.getObject(8);
		Number datumHourlyCount = (Number) rs.getObject(9);
		Number datumDailyCount = (Number) rs.getObject(10);
		Number datumMonthlyCount = (Number) rs.getObject(11);
		return new AuditDatumEntityRollup(nodeId != null ? nodeId.longValue() : null, sourceId,
				timestamp != null ? timestamp.toInstant() : null, aggregation,
				datumCount != null ? datumCount.longValue() : null,
				datumHourlyCount != null ? datumHourlyCount.longValue() : null,
				datumDailyCount != null ? datumDailyCount.intValue() : null,
				datumMonthlyCount != null ? datumMonthlyCount.intValue() : null,
				datumPropertyCount != null ? datumPropertyCount.longValue() : null,
				datumQueryCount != null ? datumQueryCount.longValue() : null,
				datumPropertyUpdateCount != null ? datumPropertyUpdateCount.longValue() : null);
	}

}
