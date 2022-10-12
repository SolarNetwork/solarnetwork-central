/* ==================================================================
 * HandshakeControllerTests.java - 19/08/2022 2:53:17 pm
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
import static net.solarnetwork.central.oscp.fp.test.FlexibilityProviderTestUtils.newCapacityProvider;
import static net.solarnetwork.central.oscp.fp.test.FlexibilityProviderTestUtils.saveUserAndFlexibilityProviderAuthIdForCurrentActor;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.HANDSHAKE_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.fpUrlPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.EnumSet;
import java.util.UUID;
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
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.fp.test.WithMockAuthenticatedToken;
import net.solarnetwork.central.oscp.fp.v20.web.RegistrationController;
import net.solarnetwork.central.oscp.security.OscpSecurityUtils;
import net.solarnetwork.central.oscp.web.OscpWebUtils;
import oscp.v20.Handshake;
import oscp.v20.MeasurementConfiguration;
import oscp.v20.RequiredBehaviour;

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
public class HandshakeControllerTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Test
	public void handshake_noAuthorization() throws Exception {
		final RequiredBehaviour b = new RequiredBehaviour(
				EnumSet.of(MeasurementConfiguration.CONTINUOUS));
		b.setHeartbeatInterval(60.0);
		final Handshake input = new Handshake();
		input.setRequiredBehaviour(b);
		final String inputJson = objectMapper.writeValueAsString(input);

		// @formatter:off
		mvc.perform(post(fpUrlPath(HANDSHAKE_URL_PATH))
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
	public void handshake_cp_ok() throws Exception {
		// GIVEN
		AuthRoleInfo authInfo = OscpSecurityUtils.authRoleInfo();
		UserLongCompositePK fpId = saveUserAndFlexibilityProviderAuthIdForCurrentActor(jdbcTemplate);
		capacityProviderDao.create(fpId.getUserId(), newCapacityProvider(fpId.getUserId(),
				authInfo.id().getEntityId(), fpId.getEntityId(), now()));

		// WHEN
		final RequiredBehaviour b = new RequiredBehaviour(
				EnumSet.of(MeasurementConfiguration.CONTINUOUS));
		b.setHeartbeatInterval(60.0);
		final Handshake input = new Handshake();
		input.setRequiredBehaviour(b);
		final String inputJson = objectMapper.writeValueAsString(input);

		// THEN
		final String requestId = UUID.randomUUID().toString();
		// @formatter:off
		mvc.perform(post(fpUrlPath(HANDSHAKE_URL_PATH))
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.header(OscpWebUtils.REQUEST_ID_HEADER, requestId)
				.content(inputJson)
				)
			.andExpect(status().isNoContent())
			;		
		// @formatter:on
	}

}
