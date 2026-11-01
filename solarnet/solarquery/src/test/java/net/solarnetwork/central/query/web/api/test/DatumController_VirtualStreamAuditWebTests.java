/* ==================================================================
 * DatumController_VirtualStreamAuditWebTests.java - 1/11/2026 3:12:41 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.web.api.test;

import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertLocation;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertNode;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUser;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.query.web.api.DatumController;
import net.solarnetwork.central.query.web.api.DatumStreamController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test the query auditing of combining queries, using {@link DatumController}
 * and {@link DatumStreamController}.
 *
 * <p>
 * A combining query labels its results with virtual IDs chosen by the caller.
 * Those IDs must never be recorded as query usage, because they can match a
 * stream the query did not read.
 * </p>
 *
 * <p>
 * The auditor writes on its own connection in its own thread, so it can only
 * see committed data. These tests are therefore not transactional, and remove
 * the data they commit when finished.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
@SpringBootTest(properties = "app.datum.query-auditor.flush-delay=50")
@ActiveProfiles("query-auditor")
@AutoConfigureMockMvc
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class DatumController_VirtualStreamAuditWebTests extends AbstractJUnit5CentralTransactionalTest {

	private static final long AUDIT_TIMEOUT_NS = SECONDS.toNanos(10);
	private static final long AUDIT_QUIET_NS = MILLISECONDS.toNanos(500);
	private static final String SOURCE_A = "virt/A";
	private static final String SOURCE_B = "virt/B";
	private static final String SOURCE_C = "virt/C";
	private static final String VIRTUAL_SOURCE_ID = "virt/V";
	private static final int HOURS = 3;

	private final List<UUID> streamIds = new ArrayList<>();
	private final List<Long> nodeIds = new ArrayList<>();
	private final List<Long> userIds = new ArrayList<>();

	private Long locId;
	private Long nodeId;
	private Long otherNodeId;
	private Long victimNodeId;
	private UUID unmappedStreamId;
	private UUID victimStreamId;

	@Autowired
	private MockMvc mvc;

	private Long node(Long userId, boolean requiresAuth) {
		final Long result = insertNode(jdbcTemplate, locId);
		nodeIds.add(result);
		insertUserNode(jdbcTemplate, userId, result, requiresAuth);
		return result;
	}

	private Long user() {
		final Long result = insertUser(jdbcTemplate);
		userIds.add(result);
		return result;
	}

	private UUID stream(Long objectId, String sourceId) {
		final UUID streamId = UUID.randomUUID();
		streamIds.add(streamId);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate,
				List.of(new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Node,
						objectId, sourceId, new String[] { "watts" }, new String[] { "wattHours" },
						null)));
		return streamId;
	}

	private void hourlyDatum(UUID streamId) {
		final Instant start = Instant.parse("2026-06-01T00:00:00Z");
		for ( int i = 0; i < HOURS; i++ ) {
			jdbcTemplate.update("""
					INSERT INTO solardatm.agg_datm_hourly (
						stream_id, ts_start, data_i, data_a, stat_i, read_a
					) VALUES (
						?, ?, '{1.5}'::numeric[], '{3}'::numeric[]
						, '{{60,1,2}}'::numeric[], '{{3,0,3}}'::numeric[]
					)
					""", streamId, Timestamp.from(start.plus(i, ChronoUnit.HOURS)));
		}
	}

	@BeforeEach
	public void setup() {
		locId = insertLocation(jdbcTemplate, TEST_LOC_COUNTRY, TEST_TZ);
		final Long userId = user();
		final Long victimUserId = user();
		nodeId = node(userId, false);
		otherNodeId = node(userId, false);
		victimNodeId = node(victimUserId, true);

		hourlyDatum(stream(nodeId, SOURCE_A));
		hourlyDatum(stream(nodeId, SOURCE_B));
		unmappedStreamId = stream(otherNodeId, SOURCE_C);
		hourlyDatum(unmappedStreamId);

		victimStreamId = stream(victimNodeId, VIRTUAL_SOURCE_ID);
	}

	@AfterEach
	public void cleanup() {
		for ( UUID streamId : streamIds ) {
			jdbcTemplate.update("DELETE FROM solardatm.aud_datm_io WHERE stream_id = ?", streamId);
			jdbcTemplate.update("DELETE FROM solardatm.aud_stale_datm WHERE stream_id = ?", streamId);
			jdbcTemplate.update("DELETE FROM solardatm.agg_datm_hourly WHERE stream_id = ?", streamId);
			jdbcTemplate.update("DELETE FROM solardatm.da_datm_meta WHERE stream_id = ?", streamId);
		}
		for ( Long userId : userIds ) {
			jdbcTemplate.update("DELETE FROM solaruser.user_node WHERE user_id = ?", userId);
			jdbcTemplate.update("DELETE FROM solaruser.user_user WHERE id = ?", userId);
		}
		for ( Long id : nodeIds ) {
			jdbcTemplate.update("DELETE FROM solarnet.sn_node WHERE node_id = ?", id);
		}
		jdbcTemplate.update("DELETE FROM solarnet.sn_loc WHERE id = ?", locId);
	}

	private long auditedCount(UUID streamId) {
		return jdbcTemplate.queryForObject("""
				SELECT COALESCE(SUM(datum_q_count), 0)
				FROM solardatm.aud_datm_io
				WHERE stream_id = ?
				""", Long.class, streamId);
	}

	private long awaitAuditedCount(UUID streamId, long count, long timeoutNs)
			throws InterruptedException {
		final long start = nanoTime();
		long result = auditedCount(streamId);
		while ( result < count && (nanoTime() - start) < timeoutNs ) {
			Thread.sleep(20);
			result = auditedCount(streamId);
		}
		return result;
	}

	private MockHttpServletRequestBuilder combiningQuery(String path, String nodeIds, String sourceIds) {
		return combiningQuery(path, nodeIds, sourceIds, "2026-05-31", "2026-06-03");
	}

	private MockHttpServletRequestBuilder combiningQuery(String path, String nodeIds, String sourceIds,
			String startDate, String endDate) {
		// @formatter:off
		return get(path)
				.param("nodeIds", nodeIds)
				.param("sourceIds", sourceIds)
				.param("nodeIdMaps", victimNodeId + ":" + nodeId)
				.param("sourceIdMaps", VIRTUAL_SOURCE_ID + ":" + SOURCE_A + "," + SOURCE_B)
				.param("aggregation", "Hour")
				.param("startDate", startDate)
				.param("endDate", endDate)
				.accept(MediaType.APPLICATION_JSON);
		// @formatter:on
	}

	private void thenVictimStreamNotAudited() throws InterruptedException {
		// @formatter:off
		then(awaitAuditedCount(victimStreamId, 1, AUDIT_QUIET_NS))
			.as("Stream of another account matching the virtual IDs is not counted")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void list_virtualIdsMatchOtherAccountStream() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(combiningQuery("/api/v1/pub/datum/list", nodeId + "," + otherNodeId,
				SOURCE_A + "," + SOURCE_B + "," + SOURCE_C))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results.length()", is(HOURS * 2)))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		then(awaitAuditedCount(unmappedStreamId, HOURS, AUDIT_TIMEOUT_NS))
			.as("Unmapped stream returned under its own IDs is counted")
			.isEqualTo(HOURS)
			;
		// @formatter:on
		thenVictimStreamNotAudited();
	}

	@Test
	public void list_runningTotal() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get("/api/v1/pub/datum/list")
				.param("nodeIds", nodeId.toString())
				.param("sourceIds", SOURCE_A + "," + SOURCE_B)
				.param("nodeIdMaps", victimNodeId + ":" + nodeId)
				.param("sourceIdMaps", VIRTUAL_SOURCE_ID + ":" + SOURCE_A + "," + SOURCE_B)
				.param("aggregation", "RunningTotal")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results[*].sourceId", containsInAnyOrder(SOURCE_A, SOURCE_B)))
			;
		// @formatter:on
	}

	@Test
	public void stream_virtualIdsMatchOtherAccountStream() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(combiningQuery("/api/v1/pub/datum/stream/datum", nodeId + "," + otherNodeId,
				SOURCE_A + "," + SOURCE_B + "," + SOURCE_C))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meta.length()", is(2)))
			.andExpect(jsonPath("$.data.length()", is(HOURS * 2)))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		then(awaitAuditedCount(unmappedStreamId, HOURS, AUDIT_TIMEOUT_NS))
			.as("Unmapped stream returned under its own IDs is counted")
			.isEqualTo(HOURS)
			;
		// @formatter:on
		thenVictimStreamNotAudited();
	}

	@Test
	public void stream_csv() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(combiningQuery("/api/v1/pub/datum/stream/datum", nodeId.toString(),
				SOURCE_A + "," + SOURCE_B)
				.accept(MediaType.parseMediaType("text/csv")))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/csv"))
			.andExpect(content().string(containsString(VIRTUAL_SOURCE_ID)))
			;
		// @formatter:on

		// THEN
		thenVictimStreamNotAudited();
	}

	@Test
	public void stream_cbor() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(combiningQuery("/api/v1/pub/datum/stream/datum", nodeId.toString(),
				SOURCE_A + "," + SOURCE_B)
				.accept(MediaType.parseMediaType("application/cbor")))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("application/cbor"))
			;
		// @formatter:on

		// THEN
		thenVictimStreamNotAudited();
	}

	@Test
	public void stream_noResults() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(combiningQuery("/api/v1/pub/datum/stream/datum", nodeId.toString(),
				SOURCE_A + "," + SOURCE_B, "2020-01-01", "2020-01-02"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success", is(true)))
			.andExpect(jsonPath("$.meta").doesNotExist())
			.andExpect(jsonPath("$.data").doesNotExist())
			;
		// @formatter:on
	}

	@Test
	public void streamReading_virtualIdsMatchOtherAccountStream() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(combiningQuery("/api/v1/pub/datum/stream/reading", nodeId + "," + otherNodeId,
				SOURCE_A + "," + SOURCE_B + "," + SOURCE_C)
				.param("readingType", "Difference"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()", is(HOURS * 2)))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		then(awaitAuditedCount(unmappedStreamId, HOURS, AUDIT_TIMEOUT_NS))
			.as("Unmapped stream returned under its own IDs is counted")
			.isEqualTo(HOURS)
			;
		// @formatter:on
		thenVictimStreamNotAudited();
	}

	@Test
	public void streamReading_noMappings() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get("/api/v1/pub/datum/stream/reading")
				.param("nodeId", otherNodeId.toString())
				.param("sourceId", SOURCE_C)
				.param("readingType", "Difference")
				.param("aggregation", "Hour")
				.param("startDate", "2026-05-31")
				.param("endDate", "2026-06-03")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()", is(HOURS)))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		then(awaitAuditedCount(unmappedStreamId, HOURS, AUDIT_TIMEOUT_NS))
			.as("Stream readings are counted")
			.isEqualTo(HOURS)
			;
		// @formatter:on
	}

}
