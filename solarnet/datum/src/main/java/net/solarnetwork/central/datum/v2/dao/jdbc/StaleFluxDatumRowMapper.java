/* ==================================================================
 * StaleFluxDatumRowMapper.java - 9/11/2020 7:21:26 pm
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getUuid;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.domain.BasicStaleFluxDatum;
import net.solarnetwork.central.datum.v2.domain.StaleFluxDatum;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Map stale audit rows into {@link StaleFluxDatum} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>stream_id</li>
 * <li>kind (character)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.1
 * @since 3.8
 */
public class StaleFluxDatumRowMapper implements RowMapper<StaleFluxDatum> {

	/** A default mapper instance. */
	public static final RowMapper<StaleFluxDatum> INSTANCE = new StaleFluxDatumRowMapper();

	@Override
	public StaleFluxDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = getUuid(rs, 1);
		String aggKind = rs.getString(2);
		return new BasicStaleFluxDatum(streamId, Aggregation.forKey(aggKind));
	}

}
