/* ==================================================================
 * DbAuditIncrementMqttPublishByteCountTests.java - 14/06/2021 2:32:14 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.StaleAuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the {@code solardatm.audit_increment_mqtt_publish_byte_count}
 * procedure.
 * 
 * @author matt
 * @version 1.0
 */
public class DbAuditIncrementMqttPublishByteCountTests extends BaseDatumJdbcTestSupport {

	private ObjectDatumStreamMetadata lastMeta;

	private ObjectDatumStreamMetadata testStreamMetadata() {
		return testStreamMetadata(TEST_NODE_ID, "a", TEST_TZ);
	}

	private ObjectDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId,
			String timeZoneId) {
		BasicObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				timeZoneId, ObjectDatumKind.Node, nodeId, sourceId, new String[] { "x" },
				new String[] { "w" }, new String[] { "st" });
		lastMeta = meta;
		return meta;
	}

	@Test
	public void insert() {
		// GIVEN
		setupTestNode(); // for TZ
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		Instant now = Instant.now();
		jdbcTemplate.execute(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(
						"{call solardatm.audit_increment_mqtt_publish_byte_count(?,?,?,?,?)}");
				stmt.setString(1, "solarflux");
				stmt.setObject(2, meta.getObjectId());
				stmt.setString(3, meta.getSourceId());
				stmt.setTimestamp(4,
						new java.sql.Timestamp(now.truncatedTo(ChronoUnit.HOURS).toEpochMilli()));
				stmt.setInt(5, 123);
				return stmt;
			}
		}, new CallableStatementCallback<Void>() {

			@Override
			public Void doInCallableStatement(CallableStatement cs)
					throws SQLException, DataAccessException {
				cs.execute();
				return null;
			}
		});

		// THEN
		List<Map<String, Object>> auditRows = jdbcTemplate
				.queryForList("select * from solardatm.aud_datm_io");
		assertThat("One audit row created", auditRows, hasSize(1));
		assertThat("Audit byte count", auditRows.get(0), hasEntry("flux_byte_count", 123));

		// verify stale record added for Day
		List<StaleAuditDatum> stale = DatumDbUtils.listStaleAuditDatum(jdbcTemplate);
		assertThat("One stale audit row created", stale, hasSize(1));
		DatumTestUtils.assertStaleAuditDatum("Stale", stale.get(0),
				new StaleAuditDatumEntity(meta.getStreamId(),
						now.atZone(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS).toInstant(),
						Aggregation.Day, null));
	}

	@Test
	public void insert_withLeadingSlash() {
		// GIVEN
		setupTestNode(); // for TZ
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		Instant now = Instant.now();
		jdbcTemplate.execute(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(
						"{call solardatm.audit_increment_mqtt_publish_byte_count(?,?,?,?,?)}");
				stmt.setString(1, "solarflux");
				stmt.setObject(2, meta.getObjectId());
				stmt.setString(3, "/" + meta.getSourceId());
				stmt.setTimestamp(4,
						new java.sql.Timestamp(now.truncatedTo(ChronoUnit.HOURS).toEpochMilli()));
				stmt.setInt(5, 123);
				return stmt;
			}
		}, new CallableStatementCallback<Void>() {

			@Override
			public Void doInCallableStatement(CallableStatement cs)
					throws SQLException, DataAccessException {
				cs.execute();
				return null;
			}
		});

		// THEN
		List<Map<String, Object>> auditRows = jdbcTemplate
				.queryForList("select * from solardatm.aud_datm_io");
		assertThat("One audit row created", auditRows, hasSize(1));
		assertThat("Audit byte count", auditRows.get(0), hasEntry("flux_byte_count", 123));

		// verify stale record added for Day
		List<StaleAuditDatum> stale = DatumDbUtils.listStaleAuditDatum(jdbcTemplate);
		assertThat("One stale audit row created", stale, hasSize(1));
		DatumTestUtils.assertStaleAuditDatum("Stale", stale.get(0),
				new StaleAuditDatumEntity(meta.getStreamId(),
						now.atZone(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS).toInstant(),
						Aggregation.Day, null));
	}

	@Test
	public void update() {
		// GIVEN
		insert();
		ObjectDatumStreamMetadata meta = lastMeta;

		// WHEN
		Instant now = Instant.now();
		jdbcTemplate.execute(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(
						"{call solardatm.audit_increment_mqtt_publish_byte_count(?,?,?,?,?)}");
				stmt.setString(1, "solarflux");
				stmt.setObject(2, meta.getObjectId());
				stmt.setString(3, meta.getSourceId());
				stmt.setTimestamp(4,
						new java.sql.Timestamp(now.truncatedTo(ChronoUnit.HOURS).toEpochMilli()));
				stmt.setInt(5, 123);
				return stmt;
			}
		}, new CallableStatementCallback<Void>() {

			@Override
			public Void doInCallableStatement(CallableStatement cs)
					throws SQLException, DataAccessException {
				cs.execute();
				return null;
			}
		});

		// THEN
		List<Map<String, Object>> auditRows = jdbcTemplate
				.queryForList("select * from solardatm.aud_datm_io");
		assertThat("One audit row available", auditRows, hasSize(1));
		assertThat("Audit byte count updated", auditRows.get(0), hasEntry("flux_byte_count", 246));

		// verify still only 1 stale record for Day
		List<StaleAuditDatum> stale = DatumDbUtils.listStaleAuditDatum(jdbcTemplate);
		assertThat("One stale audit row created", stale, hasSize(1));
		DatumTestUtils.assertStaleAuditDatum("Stale", stale.get(0),
				new StaleAuditDatumEntity(meta.getStreamId(),
						now.atZone(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS).toInstant(),
						Aggregation.Day, null));
	}

}
