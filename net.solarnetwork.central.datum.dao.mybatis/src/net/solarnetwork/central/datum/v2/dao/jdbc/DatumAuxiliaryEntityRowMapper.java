/* ==================================================================
 * DatumAuxiliaryEntityRowMapper.java - 4/11/2020 2:08:33 pm
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
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.util.JsonUtils;

/**
 * Map hourly datum audit rows into {@link DatumAuxiliaryEntity} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>stream_id</li>
 * <li>ts_start</li>
 * <li>atype</li>
 * <li>updated</li>
 * <li>notes</li>
 * <li>jdata_af</li>
 * <li>jdata_as</li>
 * <li>jmeta</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class DatumAuxiliaryEntityRowMapper implements RowMapper<DatumAuxiliary> {

	/** A default mapper instance. */
	public static final RowMapper<DatumAuxiliary> INSTANCE = new DatumAuxiliaryEntityRowMapper();

	private static GeneralDatumSamples accumulatingSamplesForJson(String json) {
		GeneralDatumSamples s = new GeneralDatumSamples();
		if ( json != null ) {
			Map<String, ?> afMap = JsonUtils.getStringMap(json);
			for ( Map.Entry<String, ?> e : afMap.entrySet() ) {
				if ( e.getValue() instanceof Number ) {
					s.putAccumulatingSampleValue(e.getKey(), (Number) e.getValue());
				}
			}
		}
		return s.isEmpty() ? null : s;
	}

	@Override
	public DatumAuxiliary mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = DatumSqlUtils.getUuid(rs, 1);
		Instant timestamp = rs.getTimestamp(2).toInstant();
		DatumAuxiliaryType kind = DatumAuxiliaryType.valueOf(rs.getString(3));
		Instant updated = rs.getTimestamp(4).toInstant();
		String notes = rs.getString(5);

		GeneralDatumSamples samplesFinal = accumulatingSamplesForJson(rs.getString(6));
		GeneralDatumSamples samplesStart = accumulatingSamplesForJson(rs.getString(7));
		GeneralDatumMetadata metadata = JsonUtils.getObjectFromJSON(rs.getString(8),
				GeneralDatumMetadata.class);

		return new DatumAuxiliaryEntity(streamId, timestamp, kind, updated, samplesFinal, samplesStart,
				notes, metadata);
	}

}
