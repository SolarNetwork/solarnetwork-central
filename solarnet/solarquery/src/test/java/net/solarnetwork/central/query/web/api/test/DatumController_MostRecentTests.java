/* ==================================================================
 * DatumController_MostRecentTests.java - 10/10/2021 6:48:33 PM
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

package net.solarnetwork.central.query.web.api.test;

import static net.solarnetwork.central.test.CommonDbTestUtils.insertLocation;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUser;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import net.solarnetwork.central.query.web.api.DatumController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link DatumController} {@literal /mostRecent} endpoint.
 * 
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
public class DatumController_MostRecentTests extends AbstractJUnit5CentralTransactionalTest {

	private Long userId;
	private Long locId;
	private Long nodeId;

	@Autowired
	private MockMvc mvc;

	@BeforeEach
	public void setup() {
		userId = insertUser(jdbcTemplate);
		locId = insertLocation(jdbcTemplate, TEST_LOC_COUNTRY, TEST_TZ);
		nodeId = CommonDbTestUtils.insertNode(jdbcTemplate, locId);
		insertUserNode(jdbcTemplate, userId, nodeId);
	}

	@Test
	public void mostRecent_noData() throws Exception {
		// GIVEN
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		queryParams.add("nodeId", nodeId.toString());

		// @formatter:off
		mvc.perform(get("/api/v1/pub/datum/mostRecent")
				.queryParams(queryParams)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success", is(true)));
		// @formatter:on

	}

}
