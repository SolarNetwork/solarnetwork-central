/* ==================================================================
 * DbPopulateAuditDatumDailyMissingTests.java - 12/12/2020 9:27:30 am
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
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumResource;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the {@code solardatm.populate_audit_datm_daily_missing}
 * database procedure.
 * 
 * @author matt
 * @version 1.0
 */
public class DbPopulateAuditDatumDailyMissingTests extends BaseDatumJdbcTestSupport {

	private Long populateAuditDatumDailyMissing(LocalDate date) {
		return jdbcTemplate.execute(new ConnectionCallback<Long>() {

			@Override
			public Long doInConnection(Connection con) throws SQLException, DataAccessException {
				log.debug("Populating audit datum daily missing for {}", date);
				try (CallableStatement stmt = con
						.prepareCall("{? = call solardatm.populate_audit_datm_daily_missing(?)}")) {
					stmt.registerOutParameter(1, Types.BIGINT);
					stmt.setObject(2, date, Types.DATE);
					stmt.execute();
					long result = stmt.getLong(1);
					log.debug("Populated {} audit datum:\n{}", result);
					return result;
				}
			}
		});
	}

	@Test
	public void populateMissing() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = metas.values().iterator().next().getStreamId();

		log.debug("Audit data before query:\n{}", listAuditDatum(jdbcTemplate, Aggregation.RunningTotal)
				.stream().map(Object::toString).collect(joining("\n")));

		// WHEN
		LocalDate date = LocalDate.now(ZoneOffset.UTC);
		Long result = populateAuditDatumDailyMissing(date);

		// THEN
		assertThat("Count returned", result, equalTo(1L));
		List<StaleAuditDatum> stale = DatumDbUtils.listStaleAuditDatum(jdbcTemplate, Aggregation.Month);
		assertThat("One stale audit created", stale, hasSize(1));
		assertThat("Stream ID matches", stale.get(0).getStreamId(), equalTo(streamId));
		assertThat("Timestamp at start of month", stale.get(0).getTimestamp(),
				equalTo(date.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
		assertThat("Audit kind", stale.get(0).getKind(), equalTo(Aggregation.Month));
	}

	@Test
	public void populateMissing_none() throws IOException {
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
		Long result = populateAuditDatumDailyMissing(date);

		// THEN
		assertThat("Count returned", result, equalTo(0L));
		List<StaleAuditDatum> stale = DatumDbUtils.listStaleAuditDatum(jdbcTemplate, Aggregation.Month);
		assertThat("No stale audit created", stale, hasSize(0));
	}

}
