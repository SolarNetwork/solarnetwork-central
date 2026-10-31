/* ==================================================================
 * DatumController_StreamAliasWebTests.java - 31/10/2026 8:36:20 pm
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

import static net.solarnetwork.central.test.CommonDbTestUtils.insertLocation;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUser;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcObjectDatumStreamAliasGrantEntityDao;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.datum.v2.support.DatumCsvUtils;
import net.solarnetwork.central.query.SolarQueryApp;
import net.solarnetwork.central.query.web.api.DatumController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link DatumController} anonymous query of a datum stream
 * from another account through an alias.
 *
 * <p>
 * The original node is always private to the stream owner, so these requests
 * can only get datum by way of an alias.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
public class DatumController_StreamAliasWebTests extends AbstractJUnit5CentralTransactionalTest {

	private static final Long CSV_NODE_ID = 1L;
	private static final String ORIG_SOURCE_ID = "A";
	private static final String ALIAS_SOURCE_ID = "alias/A";

	private Long ownerId;
	private Long hostUserId;
	private Long origNodeId;
	private Long hostNodeId;
	private Long locId;

	@Autowired
	private MockMvc mvc;

	@BeforeEach
	public void setup() {
		ownerId = insertUser(jdbcTemplate);
		hostUserId = insertUser(jdbcTemplate);
		locId = insertLocation(jdbcTemplate, TEST_LOC_COUNTRY, TEST_TZ);
		origNodeId = randomLong();
		CommonDbTestUtils.insertNode(jdbcTemplate, origNodeId, locId);
		hostNodeId = randomLong();
		CommonDbTestUtils.insertNode(jdbcTemplate, hostNodeId, locId);
		insertUserNode(jdbcTemplate, ownerId, origNodeId, true);

		// node ownership is cached across tests by node ID, so the CSV node ID cannot be used
		final UUID streamId = UUID.randomUUID();
		final var csvMeta = new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Node,
				CSV_NODE_ID, ORIG_SOURCE_ID, new String[] { "watts" }, new String[] { "wattHours" },
				null);
		final var meta = new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Node,
				origNodeId, ORIG_SOURCE_ID, new String[] { "watts" }, new String[] { "wattHours" },
				null);
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ssX");
		final List<Datum> datum = DatumCsvUtils.datumResourceList(SolarQueryApp.class,
				"testdata/datum-raw-data-01.csv", staticProvider(List.of(csvMeta)), formatter);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		final Instant now = Instant.now();
		jdbcTemplate.update("""
				INSERT INTO solardatm.da_datm_alias (
					stream_id,created,modified,node_id,source_id,alias_node_id,alias_source_id
				) VALUES (?,?::timestamptz,?::timestamptz,?,?,?,?)
				""", UUID.randomUUID(), now.toString(), now.toString(), origNodeId, ORIG_SOURCE_ID,
				hostNodeId, ALIAS_SOURCE_ID);
	}

	private void host(boolean requiresAuth) {
		insertUserNode(jdbcTemplate, hostUserId, hostNodeId, requiresAuth);
	}

	private void grant(boolean allowPublic, Long granteeId, String... sourceIds) {
		final var grant = new ObjectDatumStreamAliasGrantEntity(ownerId, 0L, Instant.now(), granteeId);
		grant.setEnabled(true);
		grant.setAllowPublic(allowPublic);
		grant.setSourceIds(sourceIds);
		new JdbcObjectDatumStreamAliasGrantEntityDao(jdbcTemplate).create(ownerId, grant);
	}

	private ResultActions anonymousMostRecent(Long nodeId, String sourceId) throws Exception {
		// @formatter:off
		return mvc.perform(get("/api/v1/pub/datum/mostRecent")
				.param("nodeId", nodeId.toString())
				.param("sourceId", sourceId)
				.param("startDate", "2001-01-01")
				.param("includeStreamAliases", "true")
				.accept(MediaType.APPLICATION_JSON));
		// @formatter:on
	}

	private ResultActions anonymousList() throws Exception {
		// @formatter:off
		return mvc.perform(get("/api/v1/pub/datum/list")
				.param("nodeId", hostNodeId.toString())
				.param("sourceId", ALIAS_SOURCE_ID)
				.param("startDate", "2001-01-01")
				.param("endDate", "2030-01-01")
				.param("max", "1")
				.param("includeStreamAliases", "true")
				.accept(MediaType.APPLICATION_JSON));
		// @formatter:on
	}

	@Test
	public void publicHost_allowPublic() throws Exception {
		// GIVEN
		host(false);
		grant(true, hostUserId, ORIG_SOURCE_ID);

		// WHEN
		// @formatter:off
		anonymousMostRecent(hostNodeId, ALIAS_SOURCE_ID)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results.length()", is(1)))
			.andExpect(jsonPath("$.data.results[0].nodeId", is(hostNodeId), Long.class))
			.andExpect(jsonPath("$.data.results[0].sourceId", is(ALIAS_SOURCE_ID)))
			;
		anonymousList()
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results.length()", is(1)))
			.andExpect(jsonPath("$.data.results[0].nodeId", is(hostNodeId), Long.class))
			.andExpect(jsonPath("$.data.results[0].sourceId", is(ALIAS_SOURCE_ID)))
			;
		// @formatter:on
	}

	@Test
	public void publicHost_notAllowPublic() throws Exception {
		// GIVEN
		host(false);
		grant(false, hostUserId, ORIG_SOURCE_ID);

		// WHEN
		// @formatter:off
		anonymousMostRecent(hostNodeId, ALIAS_SOURCE_ID)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results").isEmpty())
			;
		anonymousList()
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results").isEmpty())
			;
		// @formatter:on
	}

	@Test
	public void publicHost_allowPublicOnOtherSource() throws Exception {
		// GIVEN
		host(false);
		grant(false, hostUserId, ORIG_SOURCE_ID);
		grant(true, hostUserId, "other/*");

		// WHEN
		// @formatter:off
		anonymousMostRecent(hostNodeId, ALIAS_SOURCE_ID)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results").isEmpty())
			;
		// @formatter:on
	}

	@Test
	public void publicHost_allowPublicForOtherUser() throws Exception {
		// GIVEN
		host(false);
		grant(true, insertUser(jdbcTemplate), ORIG_SOURCE_ID);

		// WHEN
		// @formatter:off
		anonymousMostRecent(hostNodeId, ALIAS_SOURCE_ID)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results").isEmpty())
			;
		// @formatter:on
	}

	@Test
	public void privateHost_allowPublic() throws Exception {
		// GIVEN
		host(true);
		grant(true, hostUserId, ORIG_SOURCE_ID);

		// WHEN
		// @formatter:off
		anonymousMostRecent(hostNodeId, ALIAS_SOURCE_ID)
			.andExpect(status().isForbidden())
			;
		// @formatter:on
	}

	@Test
	public void originalNode_stillPrivate() throws Exception {
		// GIVEN
		host(false);
		grant(true, hostUserId, ORIG_SOURCE_ID);

		// WHEN
		// @formatter:off
		anonymousMostRecent(origNodeId, ORIG_SOURCE_ID)
			.andExpect(status().isForbidden())
			;
		// @formatter:on
	}

}
