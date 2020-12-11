/* ==================================================================
 * ZonedStreamsTimeRangeRowMapper.java - 14/11/2020 8:23:40 am
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

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.domain.ZonedStreamsTimeRange;

/**
 * Map rows into {@link ZonedStreamsTimeRange} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>ts_start</li>
 * <li>ts_end</li>
 * <li>time_zone</li>
 * <li>stream_ids</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class ZonedStreamsTimeRangeRowMapper implements RowMapper<ZonedStreamsTimeRange> {

	/** A default mapper instance. */
	public static final RowMapper<ZonedStreamsTimeRange> INSTANCE = new ZonedStreamsTimeRangeRowMapper();

	@Override
	public ZonedStreamsTimeRange mapRow(ResultSet rs, int rowNum) throws SQLException {
		Instant startDate = rs.getTimestamp(1).toInstant();
		Instant endDate = rs.getTimestamp(2).toInstant();
		String timeZoneId = rs.getString(3);
		Array array = rs.getArray(4);
		UUID[] streamIds = (array != null ? (UUID[]) array.getArray() : null);
		array.free();
		return new ZonedStreamsTimeRange(startDate, endDate, timeZoneId, streamIds);
	}

}
