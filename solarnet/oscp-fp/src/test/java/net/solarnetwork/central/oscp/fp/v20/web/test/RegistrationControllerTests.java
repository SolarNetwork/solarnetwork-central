/* ==================================================================
 * RegistrationControllerTests.java - 17/08/2022 2:00:17 pm
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

package net.solarnetwork.central.oscp.fp.v20.web.test;

import static java.time.Instant.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.oscp.fp.test.FlexibilityProviderTestUtils.allCapacityOptimizerConfigurationData;
import static net.solarnetwork.central.oscp.fp.test.FlexibilityProviderTestUtils.allCapacityProviderConfigurationData;
import static net.solarnetwork.central.oscp.fp.test.FlexibilityProviderTestUtils.newCapacityOptimizer;
import static net.solarnetwork.central.oscp.fp.test.FlexibilityProviderTestUtils.newCapacityProvider;
import static net.solarnetwork.central.oscp.fp.test.FlexibilityProviderTestUtils.saveUserAndFlexibilityProviderAuthIdForCurrentActor;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.REGISTER_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.fpUrlPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.fp.test.WithMockAuthenticatedToken;
import net.solarnetwork.central.oscp.fp.v20.web.RegistrationController;
import net.solarnetwork.central.oscp.security.OscpSecurityUtils;
import oscp.v20.Register;
import oscp.v20.VersionUrl;

/**
 * Test cases for the {@link RegistrationController} class.
 * 
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RegistrationControllerTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Autowired
	private CapacityOptimizerConfigurationDao capacityOptimizerDao;

	@Test
	public void register_noAuthorization() throws Exception {
		VersionUrl cpUrl = new VersionUrl("2.0", "/oscp/cp/2.0");
		final Register input = new Register(randomUUID().toString(), singletonList(cpUrl));
		final String inputJson = objectMapper.writeValueAsString(input);

		// @formatter:off
		mvc.perform(post(fpUrlPath(REGISTER_URL_PATH))
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(inputJson)
				)
			.andExpect(status().isForbidden())
			;		
		// @formatter:on
	}

	@Test
	@WithMockAuthenticatedToken(role = "cp")
	public void register_unsupportedVersionUrl() throws Exception {
		VersionUrl cpUrl = new VersionUrl("1.0", "/oscp/cp/1.0");
		final Register input = new Register(randomUUID().toString(), singletonList(cpUrl));
		final String inputJson = objectMapper.writeValueAsString(input);

		// @formatter:off
		mvc.perform(post(fpUrlPath(REGISTER_URL_PATH))
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(inputJson)
				)
			.andExpect(status().isNotImplemented())
			;		
		// @formatter:on
	}

	@Test
	@WithMockAuthenticatedToken(role = "cp")
	public void register_cp_ok() throws Exception {
		// GIVEN
		AuthRoleInfo authInfo = OscpSecurityUtils.authRoleInfo();
		UserLongCompositePK fpId = saveUserAndFlexibilityProviderAuthIdForCurrentActor(jdbcTemplate);
		capacityProviderDao.create(fpId.getUserId(), newCapacityProvider(fpId.getUserId(),
				authInfo.id().getEntityId(), fpId.getEntityId(), now()));

		allCapacityProviderConfigurationData(jdbcTemplate);

		// WHEN
		VersionUrl cpUrl = new VersionUrl("2.0", "/oscp/cp/2.0");
		final Register input = new Register(randomUUID().toString(), singletonList(cpUrl));
		final String inputJson = objectMapper.writeValueAsString(input);

		// THEN
		// @formatter:off
		mvc.perform(post(fpUrlPath(REGISTER_URL_PATH))
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(inputJson)
				)
			.andExpect(status().isNoContent())
			;		
		// @formatter:on
	}

	@Test
	@WithMockAuthenticatedToken(role = "co")
	public void register_co_ok() throws Exception {
		// GIVEN
		AuthRoleInfo authInfo = OscpSecurityUtils.authRoleInfo();
		UserLongCompositePK fpId = saveUserAndFlexibilityProviderAuthIdForCurrentActor(jdbcTemplate);
		capacityOptimizerDao.create(fpId.getUserId(), newCapacityOptimizer(fpId.getUserId(),
				authInfo.id().getEntityId(), fpId.getEntityId(), now()));

		allCapacityOptimizerConfigurationData(jdbcTemplate);

		// WHEN
		VersionUrl cpUrl = new VersionUrl("2.0", "/oscp/co/2.0");
		final Register input = new Register(randomUUID().toString(), singletonList(cpUrl));
		final String inputJson = objectMapper.writeValueAsString(input);

		// @formatter:off
		mvc.perform(post(fpUrlPath(REGISTER_URL_PATH))
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(inputJson)
				)
			.andExpect(status().isNoContent())
			;		
		// @formatter:on
	}

}
