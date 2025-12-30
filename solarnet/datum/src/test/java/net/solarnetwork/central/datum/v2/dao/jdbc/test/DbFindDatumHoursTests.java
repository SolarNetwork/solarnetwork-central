/* ==================================================================
 * DbFindDatumHoursTests.java - 11/08/2025 8:35:01 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static java.util.Collections.singleton;
import static org.assertj.core.api.BDDAssertions.then;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StreamPK;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@code solardatm.find_datm_hours()} stored procedure.
 *
 * @author matt
 * @version 1.0
 */
public class DbFindDatumHoursTests extends BaseDatumJdbcTestSupport {

	private List<StreamPK> listDatumHours(UUID streamId, Instant start, Instant end) {
		return jdbcTemplate.query((con) -> {
			CallableStatement stmt = con.prepareCall("{call solardatm.find_datm_hours(?::uuid,?,?)}");
			stmt.setString(1, streamId.toString());
			stmt.setTimestamp(2, Timestamp.from(start));
			stmt.setTimestamp(3, Timestamp.from(end));
			return stmt;
		}, (ResultSet rs, int _) -> {
			Timestamp ts = rs.getTimestamp(2);
			return new StreamPK(streamId, ts.toInstant());
		});
	}

	@Test
	public void findForOnlyResetRecord() {
		// GIVEN
		// setup a stream
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "a" }, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// create an auxiliary reset
		Instant currHour = Instant.now().truncatedTo(ChronoUnit.HOURS);
		DatumAuxiliaryEntity reset = new DatumAuxiliaryEntity(meta.getStreamId(),
				currHour.plusSeconds(1), DatumAuxiliaryType.Reset, Instant.now(),
				new DatumSamples(null, Map.of("a", 100), null),
				new DatumSamples(null, Map.of("a", 1), null), null, null);
		DatumDbUtils.insertDatumAuxiliary(log, jdbcTemplate, singleton(reset));

		// WHEN
		List<StreamPK> result = listDatumHours(meta.getStreamId(), currHour,
				currHour.plus(1L, ChronoUnit.HOURS));

		// THEN
		// @formatter:off
		then(result)
			.as("Result has one record for reset")
			.containsExactly(new StreamPK(meta.getStreamId(), currHour))
			;
		// @formatter:on
	}

}
