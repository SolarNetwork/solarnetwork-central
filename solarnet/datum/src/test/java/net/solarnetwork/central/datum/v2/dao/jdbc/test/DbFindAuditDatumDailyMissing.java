/* ==================================================================
 * DbFindAuditDatumDailyMissing.java - 25/11/2020 9:19:46 am
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumResource;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumJdbcUtils;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the "find missing audit datum" database stored procedure.
 * 
 * @author matt
 * @version 1.0
 */
public class DbFindAuditDatumDailyMissing extends BaseDatumJdbcTestSupport {

	private static class MissingDatum {

		private final UUID streamId;
		private final Instant timestamp;
		private final String timeZoneId;

		private MissingDatum(UUID streamId, Instant timestamp, String timeZoneId) {
			super();
			this.streamId = streamId;
			this.timestamp = timestamp;
			this.timeZoneId = timeZoneId;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("MissingDatum{");
			if ( streamId != null ) {
				builder.append("streamId=");
				builder.append(streamId);
				builder.append(", ");
			}
			if ( timestamp != null ) {
				builder.append("timestamp=");
				builder.append(timestamp);
				builder.append(", ");
			}
			if ( timeZoneId != null ) {
				builder.append("timeZoneId=");
				builder.append(timeZoneId);
			}
			builder.append("}");
			return builder.toString();
		}

	}

	private static class MissingDatumRowMapper implements RowMapper<MissingDatum> {

		private static final RowMapper<MissingDatum> INSTANCE = new MissingDatumRowMapper();

		@Override
		public MissingDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
			UUID streamId = DatumJdbcUtils.getUuid(rs, 1);
			Instant ts = rs.getTimestamp(2).toInstant();
			return new MissingDatum(streamId, ts, rs.getString(3));
		}

	}

	private List<MissingDatum> findAuditDatumDailyMissing(LocalDate date) {
		return jdbcTemplate.execute(new ConnectionCallback<List<MissingDatum>>() {

			@Override
			public List<MissingDatum> doInConnection(Connection con)
					throws SQLException, DataAccessException {
				log.debug("Finding audit datum daily missing {}", date);
				List<MissingDatum> result = new ArrayList<>();
				try (CallableStatement stmt = con
						.prepareCall("{call solardatm.find_audit_datm_daily_missing(?)}")) {
					stmt.setObject(1, date);
					if ( stmt.execute() ) {
						try (ResultSet rs = stmt.getResultSet()) {
							int i = 0;
							while ( rs.next() ) {
								MissingDatum d = MissingDatumRowMapper.INSTANCE.mapRow(rs, ++i);
								result.add(d);
							}
						}
					}
				}
				log.debug("Found audit datum daily missing:\n{}",
						result.stream().map(Object::toString).collect(joining("\n")));
				return result;
			}
		});
	}

	@Test
	public void findMissing() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = metas.values().iterator().next().getStreamId();

		log.debug("Audit data before query:\n{}", listAuditDatum(jdbcTemplate, Aggregation.RunningTotal)
				.stream().map(Object::toString).collect(joining("\n")));

		// WHEN
		LocalDate date = LocalDate.now(ZoneOffset.UTC);
		List<MissingDatum> result = findAuditDatumDailyMissing(date);

		// THEN
		assertThat("Row returned for missing day", result, hasSize(1));
		assertThat("Stream ID matches", result.get(0).streamId, equalTo(streamId));
		assertThat("Timestamp matches", result.get(0).timestamp,
				equalTo(date.atStartOfDay(ZoneOffset.UTC).toInstant()));
		assertThat("Time zone matches", result.get(0).timeZoneId, equalTo("UTC"));
	}

	@Test
	public void findMissing_none() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = metas.values().iterator().next().getStreamId();

		LocalDate date = LocalDate.now(ZoneOffset.UTC);
		AuditDatumEntity acc = AuditDatumEntity.accumulativeAuditDatum(streamId,
				date.atStartOfDay(ZoneOffset.UTC).toInstant(), 7L, 0L, 0, 0);
		insertAuditDatum(log, jdbcTemplate, singleton(acc));

		log.debug("Audit data before query:\n{}", listAuditDatum(jdbcTemplate, Aggregation.RunningTotal)
				.stream().map(Object::toString).collect(joining("\n")));

		// WHEN
		List<MissingDatum> result = findAuditDatumDailyMissing(date);

		// THEN
		assertThat("No row returned for missing day", result, hasSize(0));
	}

	@Test
	public void findMissing_oneMissingOneNot() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		List<GeneralNodeDatum> datums2 = DatumDbUtils
				.elementsOf(DatumDbUtils.loadJsonDatumAndAuxiliaryResource("test-datum-01.txt",
						getClass(), new Consumer<GeneralNodeDatum>() {

							@Override
							public void accept(GeneralNodeDatum d) {
								d.setNodeId(2L);
							}
						}, null), GeneralNodeDatum.class);
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate,
				concat(datums.stream(), datums2.stream()).collect(toList()), "UTC");
		UUID streamId_1 = metas.get(new NodeSourcePK(1L, "a")).getStreamId();
		UUID streamId_2 = metas.get(new NodeSourcePK(2L, "a")).getStreamId();

		// add audit for one stream
		LocalDate date = LocalDate.now(ZoneOffset.UTC);
		AuditDatumEntity acc = AuditDatumEntity.accumulativeAuditDatum(streamId_1,
				date.atStartOfDay(ZoneOffset.UTC).toInstant(), 7L, 0L, 0, 0);
		insertAuditDatum(log, jdbcTemplate, singleton(acc));

		log.debug("Audit data before query:\n{}", listAuditDatum(jdbcTemplate, Aggregation.RunningTotal)
				.stream().map(Object::toString).collect(joining("\n")));

		// WHEN
		List<MissingDatum> result = findAuditDatumDailyMissing(date);

		// THEN
		assertThat("Row returned for missing day", result, hasSize(1));
		assertThat("Stream ID matches", result.get(0).streamId, equalTo(streamId_2));
		assertThat("Timestamp matches", result.get(0).timestamp,
				equalTo(date.atStartOfDay(ZoneOffset.UTC).toInstant()));
		assertThat("Time zone matches", result.get(0).timeZoneId, equalTo("UTC"));
	}
}
