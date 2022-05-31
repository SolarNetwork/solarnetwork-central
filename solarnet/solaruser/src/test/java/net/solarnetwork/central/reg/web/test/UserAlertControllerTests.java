/* ==================================================================
 * UserAlertControllerTests.java - 30/05/2022 8:49:56 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import net.solarnetwork.central.reg.test.WithMockSecurityUser;
import net.solarnetwork.central.reg.web.UserAlertController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.domain.UserAlertType;

/**
 * Test cases for the {@link UserAlertController} class.
 * 
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
public class UserAlertControllerTests extends AbstractJUnit5CentralTransactionalTest {

	private static final Long TEST_USER_ID = 1L;

	@Autowired
	private MockMvc mvc;

	private Long nodeId;

	@BeforeEach
	public void setup() {
		setupTestUser(TEST_USER_ID);
		nodeId = Math.abs(UUID.randomUUID().getMostSignificantBits());
		setupTestLocation();
		setupTestNode(nodeId);
		setupTestUserNode(TEST_USER_ID, nodeId);
	}

	@Test
	@WithMockSecurityUser
	public void saveAlert_withEmails() throws Exception {
		// GIVEN
		final String email = "foo@localhost";
		String postBody = String.format(
				"{\"nodeId\":%d,\"type\":\"%s\",\"options\":{\"emails\":\"%s\"}}", nodeId,
				UserAlertType.NodeStaleData.name(), email);

		// @formatter:off
		mvc.perform(post("/u/sec/alerts/save").with(csrf())
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postBody)
				)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success", is(true)))
			.andExpect(jsonPath("$.data.id", is(notNullValue())))
			.andExpect(jsonPath("$.data.options.emails.length()", is(1)))
			.andExpect(jsonPath("$.data.options.emails[0]", is(email)))
			;
		
		// @formatter:on
	}

}
