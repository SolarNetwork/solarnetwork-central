/* ==================================================================
 * DatumController_StreamAliasAuditWebTests.java - 1/11/2026 8:12:44 am
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
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertLocation;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertNode;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUser;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcObjectDatumStreamAliasGrantEntityDao;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.datum.v2.support.DatumCsvUtils;
import net.solarnetwork.central.query.SolarQueryApp;
import net.solarnetwork.central.query.web.api.DatumController;
import net.solarnetwork.central.query.web.api.DatumStreamController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test the query auditing of anonymous queries of a datum stream from another
 * account through an alias, using {@link DatumController} and
 * {@link DatumStreamController}.
 *
 * <p>
 * The query usage of an alias must be recorded on the alias stream, because
 * that is what bills the owner of the alias node. Turning a recorded alias
 * stream into billing usage is covered by {@code BillingUsageTests}.
 * </p>
 *
 * <p>
 * The auditor writes on its own connection in its own thread, so it can only
 * see committed data. These tests are therefore not transactional, and remove
 * the data they commit when finished.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest(properties = "app.datum.query-auditor.flush-delay=50")
@ActiveProfiles("query-auditor")
@AutoConfigureMockMvc
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class DatumController_StreamAliasAuditWebTests extends AbstractJUnit5CentralTransactionalTest {

	private static final long AUDIT_TIMEOUT_NS = SECONDS.toNanos(10);
	private static final Long CSV_NODE_ID = 1L;
	private static final String CSV_SOURCE_ID = "A";
	private static final String ORIG_SOURCE_ID = "orig/A";
	private static final String ALIAS_SOURCE_ID = "alias/A";
	private static final int MAX = 3;

	private final List<UUID> streamIds = new ArrayList<>();
	private final List<Long> nodeIds = new ArrayList<>();
	private final List<Long> userIds = new ArrayList<>();

	private Long ownerId;
	private Long hostUserId;
	private Long locId;
	private Long origNodeId;
	private Long hostNodeId;
	private UUID origStreamId;
	private UUID aliasStreamId;

	@Autowired
	private MockMvc mvc;

	private Long node(Long userId, boolean requiresAuth) {
		final Long nodeId = insertNode(jdbcTemplate, locId);
		nodeIds.add(nodeId);
		insertUserNode(jdbcTemplate, userId, nodeId, requiresAuth);
		return nodeId;
	}

	private Long user() {
		final Long userId = insertUser(jdbcTemplate);
		userIds.add(userId);
		return userId;
	}

	private UUID stream(Long nodeId, String sourceId) {
		final UUID streamId = UUID.randomUUID();
		streamIds.add(streamId);

		// the CSV node and source are mapped to a stream ID, which can then be for any node and source
		final String[] names = new String[] { "watts" };
		final String[] accumulating = new String[] { "wattHours" };
		final var csvMeta = new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Node,
				CSV_NODE_ID, CSV_SOURCE_ID, names, accumulating, null);
		final var meta = new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Node,
				nodeId, sourceId, names, accumulating, null);
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ssX");
		final List<Datum> datum = DatumCsvUtils.datumResourceList(SolarQueryApp.class,
				"testdata/datum-raw-data-01.csv", staticProvider(List.of(csvMeta)), formatter);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);
		return streamId;
	}

	private void grant(boolean allowPublic) {
		final var grant = new ObjectDatumStreamAliasGrantEntity(ownerId, 0L, Instant.now(), hostUserId);
		grant.setEnabled(true);
		grant.setAllowPublic(allowPublic);
		grant.setNodeIds(new Long[] { origNodeId });
		new JdbcObjectDatumStreamAliasGrantEntityDao(jdbcTemplate).create(ownerId, grant);
	}

	@BeforeEach
	public void setup() {
		locId = insertLocation(jdbcTemplate, TEST_LOC_COUNTRY, TEST_TZ);
		ownerId = user();
		hostUserId = user();
		origNodeId = node(ownerId, true);
		hostNodeId = node(hostUserId, false);
		origStreamId = stream(origNodeId, ORIG_SOURCE_ID);

		aliasStreamId = UUID.randomUUID();
		streamIds.add(aliasStreamId);
		final Instant now = Instant.now();
		jdbcTemplate.update("""
				INSERT INTO solardatm.da_datm_alias (
					stream_id,created,modified,node_id,source_id,alias_node_id,alias_source_id
				) VALUES (?,?::timestamptz,?::timestamptz,?,?,?,?)
				""", aliasStreamId, now.toString(), now.toString(), origNodeId, ORIG_SOURCE_ID,
				hostNodeId, ALIAS_SOURCE_ID);
	}

	@AfterEach
	public void cleanup() {
		for ( UUID streamId : streamIds ) {
			jdbcTemplate.update("DELETE FROM solardatm.aud_datm_io WHERE stream_id = ?", streamId);
			jdbcTemplate.update("DELETE FROM solardatm.aud_stale_datm WHERE stream_id = ?", streamId);
			jdbcTemplate.update("DELETE FROM solardatm.da_datm_alias WHERE stream_id = ?", streamId);
			jdbcTemplate.update("DELETE FROM solardatm.da_datm_alias_del WHERE stream_id = ?", streamId);
			jdbcTemplate.update("DELETE FROM solardatm.da_datm WHERE stream_id = ?", streamId);
			jdbcTemplate.update("DELETE FROM solardatm.da_datm_meta WHERE stream_id = ?", streamId);
		}
		for ( Long userId : userIds ) {
			jdbcTemplate.update("DELETE FROM solaruser.user_node WHERE user_id = ?", userId);
			jdbcTemplate.update("DELETE FROM solaruser.user_user WHERE id = ?", userId);
		}
		for ( Long nodeId : nodeIds ) {
			jdbcTemplate.update("DELETE FROM solarnet.sn_node WHERE node_id = ?", nodeId);
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

	/**
	 * Wait for the auditor to record at least a given count.
	 *
	 * @param streamId
	 *        the stream to wait on
	 * @param count
	 *        the count to wait for
	 * @return the recorded count
	 */
	private long awaitAuditedCount(UUID streamId, long count) throws InterruptedException {
		final long start = nanoTime();
		long result = auditedCount(streamId);
		while ( result < count && (nanoTime() - start) < AUDIT_TIMEOUT_NS ) {
			Thread.sleep(20);
			result = auditedCount(streamId);
		}
		return result;
	}

	private static MockHttpServletRequestBuilder anonymous(String path) {
		// @formatter:off
		return get(path)
				.param("startDate", "2001-01-01")
				.param("endDate", "2030-01-01")
				.param("includeStreamAliases", "true")
				.accept(MediaType.APPLICATION_JSON);
		// @formatter:on
	}

	private void thenAliasStreamAudited(long count) throws InterruptedException {
		// @formatter:off
		then(awaitAuditedCount(aliasStreamId, count))
			.as("Query usage recorded on alias stream, so owner of alias node pays")
			.isEqualTo(count)
			;
		then(auditedCount(origStreamId))
			.as("Query usage not recorded on original stream, so stream owner does not pay")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void list_sourceId() throws Exception {
		// GIVEN
		grant(true);

		// WHEN
		// @formatter:off
		mvc.perform(anonymous("/api/v1/pub/datum/list")
				.param("nodeId", hostNodeId.toString())
				.param("sourceId", ALIAS_SOURCE_ID)
				.param("max", String.valueOf(MAX)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results.length()", is(MAX)))
			;
		// @formatter:on

		// THEN
		thenAliasStreamAudited(MAX);
	}

	@Test
	public void list_sourceIds() throws Exception {
		// GIVEN
		grant(true);

		// WHEN
		// @formatter:off
		mvc.perform(anonymous("/api/v1/pub/datum/list")
				.param("nodeId", hostNodeId.toString())
				.param("sourceIds", ALIAS_SOURCE_ID, "does/not/exist")
				.param("max", String.valueOf(MAX)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results.length()", is(MAX)))
			;
		// @formatter:on

		// THEN
		thenAliasStreamAudited(MAX);
	}

	@Disabled("JdbcQueryAuditor records the source pattern from the filter, not the matched sources")
	@Test
	public void list_sourcePattern() throws Exception {
		// GIVEN
		grant(true);

		// WHEN
		// @formatter:off
		mvc.perform(anonymous("/api/v1/pub/datum/list")
				.param("nodeId", hostNodeId.toString())
				.param("sourceId", "alias/*")
				.param("max", String.valueOf(MAX)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results.length()", is(MAX)))
			;
		// @formatter:on

		// THEN
		thenAliasStreamAudited(MAX);
	}

	@Test
	public void mostRecent() throws Exception {
		// GIVEN
		grant(true);

		// WHEN
		// @formatter:off
		mvc.perform(anonymous("/api/v1/pub/datum/mostRecent")
				.param("nodeId", hostNodeId.toString())
				.param("sourceId", ALIAS_SOURCE_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results.length()", is(1)))
			;
		// @formatter:on

		// THEN
		thenAliasStreamAudited(1);
	}

	@Test
	public void streamDatum() throws Exception {
		// GIVEN
		grant(true);

		// WHEN
		// @formatter:off
		mvc.perform(anonymous("/api/v1/pub/datum/stream/datum")
				.param("nodeId", hostNodeId.toString())
				.param("sourceId", ALIAS_SOURCE_ID)
				.param("max", String.valueOf(MAX)))
			.andExpect(status().isOk())
			;
		// @formatter:on

		// THEN
		thenAliasStreamAudited(MAX);
	}

	@Test
	public void notAllowPublic() throws Exception {
		// GIVEN
		grant(false);
		final UUID ownStreamId = stream(hostNodeId, "own/A");

		// WHEN
		// @formatter:off
		mvc.perform(anonymous("/api/v1/pub/datum/mostRecent")
				.param("nodeId", hostNodeId.toString())
				.param("sourceId", ALIAS_SOURCE_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results").isEmpty())
			;

		// query another stream afterwards, to know the auditor has processed the first request
		mvc.perform(anonymous("/api/v1/pub/datum/mostRecent")
				.param("nodeId", hostNodeId.toString())
				.param("sourceId", "own/A"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results.length()", is(1)))
			;

		// THEN
		then(awaitAuditedCount(ownStreamId, 1))
			.as("Query usage recorded on host's own stream")
			.isEqualTo(1)
			;
		then(auditedCount(aliasStreamId))
			.as("No usage recorded on alias when nothing returned")
			.isZero()
			;
		then(auditedCount(origStreamId))
			.as("No usage recorded on original stream")
			.isZero()
			;
		// @formatter:on
	}

}
