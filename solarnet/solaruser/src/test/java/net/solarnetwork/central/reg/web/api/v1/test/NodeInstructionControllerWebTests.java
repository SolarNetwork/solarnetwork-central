/* ==================================================================
 * NodeInstructionControllerTests.java - 31/05/2025 2:23:16 pm
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

package net.solarnetwork.central.reg.web.api.v1.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.reg.test.WithMockSecurityUser;
import net.solarnetwork.central.reg.web.api.v1.NodeInstructionController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;

/**
 * Web API level integration tests for the {@link NodeInstructionController}
 * class.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
public class NodeInstructionControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	private static final Long TEST_USER_ID = 1L;
	private static final String TEST_EMAIL = "test1@localhost";
	private static final Clock clock = Clock.tickMillis(ZoneOffset.UTC);

	@Autowired
	private NodeInstructionDao nodeInstructionDao;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MockMvc mvc;

	private Long nodeId;

	@BeforeEach
	public void setup() {
		setupTestUser(TEST_USER_ID, TEST_EMAIL);
		nodeId = randomLong();
		setupTestLocation();
		setupTestNode(nodeId);
		setupTestUserNode(TEST_USER_ID, nodeId);
	}

	@Test
	@WithMockSecurityUser
	public void viewInstruction() throws Exception {
		// GIVEN
		NodeInstruction ni = new NodeInstruction(randomString(), clock.instant(), nodeId);
		ni.setCreated(ni.getInstruction().getInstructionDate());
		ni.getInstruction().setParams(Map.of("a", "one"));
		ni = nodeInstructionDao.get(nodeInstructionDao.save(ni));

		// WHEN
		// @formatter:off
		mvc.perform(get("/api/v1/sec/instr/view")
				.param("id", ni.getId().toString())
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data":%s}
					""".formatted(objectMapper.writeValueAsString(ni)), JsonCompareMode.STRICT))
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void addInstruction() throws Exception {
		// GIVEN
		NodeInstruction ni = new NodeInstruction(randomString(), clock.instant(), nodeId);
		ni.setCreated(ni.getInstruction().getInstructionDate());
		ni.getInstruction().setParams(Map.of("a", "one"));
		ni = nodeInstructionDao.get(nodeInstructionDao.save(ni));

		// WHEN
		// @formatter:off
		mvc.perform(post("/api/v1/sec/instr/add/Test")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nodeId":%d,"params":{"a":"one"}}
						""".formatted(nodeId))
				.accept(MediaType.APPLICATION_JSON)
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success", is(true)))
			.andExpect(jsonPath("$.data.id", is(notNullValue())))
			.andExpect(jsonPath("$.data.nodeId", is(nodeId)))
			.andExpect(jsonPath("$.data.topic", is("Test")))
			.andExpect(jsonPath("$.data.parameters.length()", is(1)))
			.andExpect(jsonPath("$.data.parameters[0].name", is("a")))
			.andExpect(jsonPath("$.data.parameters[0].value", is("one")))
			;
		// @formatter:on
	}

}
