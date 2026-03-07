/* ==================================================================
 * DatumDateIntervalRowMapper.java - 30/11/2020 7:48:07 am
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
import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.domain.DatumDateInterval;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamIdentity;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Map datum rows into {@link DatumDateInterval} instances.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>stream_id</li>
 * <li>ts_start</li>
 * <li>ts_end</li>
 * <li>obj_id</li>
 * <li>source_id</li>
 * <li>time_zone</li>
 * <li>kind</li>
 * </ol>
 *
 * @author matt
 * @version 1.1
 * @since 3.8
 */
public class DatumDateIntervalRowMapper implements RowMapper<DatumDateInterval> {

	/** A default mapper instance. */
	public static final RowMapper<DatumDateInterval> INSTANCE = new DatumDateIntervalRowMapper();

	@Override
	public DatumDateInterval mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = nonnull(getUuid(rs, 1), "Stream ID");
		Timestamp start = nonnull(rs.getTimestamp(2), "Start");
		Timestamp end = nonnull(rs.getTimestamp(3), "End");
		Object objId = rs.getObject(4);
		String sourceId = nonnull(rs.getString(5), "Source ID");
		String timeZoneId = nonnull(rs.getString(6), "Time Zone ID");

		ObjectDatumKind kind = ObjectDatumKind.forKey(rs.getString(7));

		Long objectId = nonnull(objId instanceof Number n ? n.longValue() : null, "Object ID");

		return new DatumDateInterval(start.toInstant(), end.toInstant(), timeZoneId,
				new BasicObjectDatumStreamIdentity(streamId, kind, objectId, sourceId));
	}

}
