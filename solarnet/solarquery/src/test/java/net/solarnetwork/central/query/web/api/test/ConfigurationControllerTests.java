/* ==================================================================
 * AuthTokenControllerTests.java - 9/10/2021 5:41:35 PM
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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import net.solarnetwork.central.query.web.api.ConfigurationController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;

/**
 * Test cases for the {@link ConfigurationController}.
 * 
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ConfigurationControllerTests extends AbstractJUnit5CentralTransactionalTest {

	@Autowired
	private MockMvc mvc;

	@Test
	public void refreshOk() throws Exception {
		// @formatter:off
		mvc.perform(get("/api/v1/pub/config")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success", is(true)))
			.andExpect(jsonPath("$.data.serviceUrls.node-dashboard", is("http://localhost/node-dashboard")));
		// @formatter:on
	}

}
