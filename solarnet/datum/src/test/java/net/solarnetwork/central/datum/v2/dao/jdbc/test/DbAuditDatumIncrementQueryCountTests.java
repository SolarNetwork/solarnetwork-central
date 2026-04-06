/* ==================================================================
 * DbAuditDatumIncrementQueryCountTests.java - 14/12/2020 9:18:06 am
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

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntity.ioAuditDatum;
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.domain.datum.Aggregation.Day;
import static net.solarnetwork.domain.datum.Aggregation.Hour;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static org.assertj.core.api.BDDAssertions.then;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.StaleAuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@literal solardatm.aud_datm_io_inc_datum_q_count}
 * database procedure.
 *
 * @author matt
 * @version 1.4
 */
public class DbAuditDatumIncrementQueryCountTests extends BaseDatumJdbcTestSupport {

	private AuditDatum incrementAndGet(ObjectDatumStreamMetadata meta, Instant ts, int datumCount) {
		return incrementAndGet(meta, ts, datumCount, meta.getStreamId());
	}

	private AuditDatum incrementAndGet(ObjectDatumStreamMetadata meta, Instant ts, int datumCount,
			UUID expectedStreamId) {
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				log.debug("Incrementing audit query count for stream {} @ {}: +{}", meta.getStreamId(),
						ts, datumCount);
				try (CallableStatement stmt = con
						.prepareCall("{call solardatm.audit_increment_datum_q_count(?,?,?,?)}")) {
					stmt.setObject(1, meta.getObjectId());
					stmt.setString(2, meta.getSourceId());
					stmt.setTimestamp(3, Timestamp.from(ts));
					stmt.setInt(4, datumCount);
					stmt.execute();
				}
				return null;
			}
		});
		Instant tsHour = ts.truncatedTo(HOURS);
		return DatumDbUtils.listAuditDatum(jdbcTemplate, Hour).stream().filter(e -> {
			return (e.getStreamId().equals(expectedStreamId) && e.getTimestamp().equals(tsHour));
		}).findAny().orElseThrow(IllegalStateException::new);
	}

	@Test
	public void insert() {
		// GIVEN
		setupTestNode(); // for TZ
		final UUID streamId = UUID.randomUUID();
		final Instant now = Instant.now();
		final ObjectDatumStreamMetadata meta = emptyMeta(streamId, TEST_TZ, Node, TEST_NODE_ID, "a");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		// WHEN
		final AuditDatum d = incrementAndGet(meta, now, 123);

		// THEN
		DatumTestUtils.assertAuditDatum("Inserted query row", d,
				AuditDatumEntity.ioAuditDatum(streamId, now.truncatedTo(HOURS), 0L, 0L, 123L, 0L, 0L));

		// verify stale record added for Day
		List<StaleAuditDatum> stale = DatumDbUtils.listStaleAuditDatum(jdbcTemplate);
		then(stale).as("One stale audit row created").hasSize(1);
		DatumTestUtils.assertStaleAuditDatum("Stale", stale.get(0),
				new StaleAuditDatumEntity(meta.getStreamId(),
						now.atZone(ZoneId.of(TEST_TZ)).truncatedTo(DAYS).toInstant(), Day, now()));
	}

	@Test
	public void update() {
		// GIVEN
		setupTestNode(); // for TZ
		final UUID streamId = UUID.randomUUID();
		final Instant now = Instant.now();
		final ObjectDatumStreamMetadata meta = emptyMeta(streamId, TEST_TZ, Node, TEST_NODE_ID, "a");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));
		DatumDbUtils.insertAuditDatum(log, jdbcTemplate,
				Set.of(ioAuditDatum(streamId, now.truncatedTo(HOURS), 0L, 0L, 123L, 0L, 0L)));

		// WHEN
		final AuditDatum d = incrementAndGet(meta, now, 321);

		// THEN
		DatumTestUtils.assertAuditDatum("Updated query row", d,
				ioAuditDatum(streamId, now.truncatedTo(HOURS), 0L, 0L, 444L, 0L, 0L));

		// verify stale record added for Day
		List<StaleAuditDatum> stale = DatumDbUtils.listStaleAuditDatum(jdbcTemplate);
		then(stale).as("One stale audit row created").hasSize(1);
		DatumTestUtils.assertStaleAuditDatum("Stale", stale.get(0), new StaleAuditDatumEntity(streamId,
				now.atZone(ZoneId.of(TEST_TZ)).truncatedTo(DAYS).toInstant(), Day, now()));
	}

	@Test
	public void insert_alias() {
		// GIVEN
		setupTestNode(); // for TZ

		final UUID streamId = UUID.randomUUID();
		final Instant now = Instant.now();
		final ObjectDatumStreamMetadata meta = emptyMeta(streamId, TEST_TZ, Node, TEST_NODE_ID, "a");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		final Long aliasNodeId = randomLong();
		setupTestNode(aliasNodeId);

		final UUID aliasStreamId = UUID.randomUUID();
		final ObjectDatumStreamMetadata aliasMeta = BasicObjectDatumStreamMetadata
				.emptyMeta(aliasStreamId, TEST_TZ, Node, aliasNodeId, "b");

		final var alias = new ObjectDatumStreamAliasEntity(aliasStreamId, now, now, Node,
				aliasMeta.getObjectId(), aliasMeta.getSourceId(), meta.getObjectId(),
				meta.getSourceId());
		DatumDbUtils.insertObjectDatumStreamAliases(log, jdbcTemplate, List.of(alias));

		// WHEN
		final int count = randomInt();
		final AuditDatum d = incrementAndGet(aliasMeta, now, count, streamId);

		// THEN
		DatumTestUtils.assertAuditDatum("Inserted query row for ORIGINAL stream", d, AuditDatumEntity
				.ioAuditDatum(streamId, now.truncatedTo(HOURS), 0L, 0L, (long) count, 0L, 0L));

		// verify stale record added for Day
		List<StaleAuditDatum> stale = DatumDbUtils.listStaleAuditDatum(jdbcTemplate);
		then(stale).as("One stale audit row created").hasSize(1);
		DatumTestUtils.assertStaleAuditDatum("Stale for ORIGINAL stream", stale.get(0),
				new StaleAuditDatumEntity(streamId,
						now.atZone(ZoneId.of(TEST_TZ)).truncatedTo(DAYS).toInstant(), Day, now()));
	}

}
