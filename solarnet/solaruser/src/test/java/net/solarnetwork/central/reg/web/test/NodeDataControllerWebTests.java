/* ==================================================================
 * NodeDataControllerWebTests.java - 22/09/2026 9:02:36 am
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

package net.solarnetwork.central.reg.web.test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.common.dao.jdbc.CommonDbUtils.insertObjectDatumStreamMetadata;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertLocation;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertNode;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUser;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static net.solarnetwork.central.test.CommonTestUtils.randomEmail;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_NAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USERNAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USER_ID;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.reg.web.NodeDataController;
import net.solarnetwork.central.test.security.WithMockSecurityUser;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Web integration tests for the {@link NodeDataController} class.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class NodeDataControllerWebTests {

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private MockMvc mvc;

	private Long nodeId;
	private String sourceId;
	private Long otherNodeId;

	@BeforeEach
	public void setup() {
		final Long locId = insertLocation(jdbcOperations, "NZ", "Pacific/Auckland");

		insertUser(jdbcOperations, DEFAULT_USER_ID, DEFAULT_USERNAME, randomString(), DEFAULT_NAME);
		nodeId = insertNode(jdbcOperations, locId);
		insertUserNode(jdbcOperations, DEFAULT_USER_ID, nodeId, true);
		sourceId = randomSourceId();

		// another user's private node
		final Long otherUserId = randomLong();
		insertUser(jdbcOperations, otherUserId, randomEmail(), randomString(), randomString());
		otherNodeId = insertNode(jdbcOperations, locId);
		insertUserNode(jdbcOperations, otherUserId, otherNodeId, true);

		insertObjectDatumStreamMetadata(null, jdbcOperations,
				List.of(nodeStream(nodeId, sourceId), nodeStream(otherNodeId, randomSourceId())));
	}

	private static ObjectDatumStreamMetadata nodeStream(Long nodeId, String sourceId) {
		return new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC", ObjectDatumKind.Node, nodeId,
				sourceId, new String[] { "watts" }, null, null);
	}

	@WithMockSecurityUser
	@Test
	public void nodeSources() throws Exception {
		// WHEN
		// @formatter:off
		final String result = mvc.perform(get("/u/sec/node-data/{nodeId}/sources", nodeId)
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.node("data")
			.as("Own node source IDs returned")
			.isArray()
			.containsExactly(sourceId)
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void nodeSources_otherUserPrivateNode() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get("/u/sec/node-data/{nodeId}/sources", otherNodeId)
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isForbidden())
			;
		// @formatter:on
	}

}
