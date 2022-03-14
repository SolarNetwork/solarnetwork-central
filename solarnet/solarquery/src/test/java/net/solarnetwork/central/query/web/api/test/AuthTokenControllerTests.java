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

import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityToken;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUser;
import static net.solarnetwork.security.AuthorizationUtils.AUTHORIZATION_DATE_HEADER_FORMATTER;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import net.solarnetwork.central.query.web.api.AuthTokenController;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.security.AuthorizationUtils;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Test cases for the {@link AuthTokenController}.
 * 
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AuthTokenControllerTests extends AbstractJUnit5CentralTransactionalTest {

	private static final String TEST_TOKEN_SECRET = "secret";

	private Long testUserId;
	private String testTokenId;

	@Autowired
	private MockMvc mvc;

	@BeforeEach
	public void setup() {
		testUserId = insertUser(jdbcTemplate);
		testTokenId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20);
		insertSecurityToken(jdbcTemplate, testTokenId, TEST_TOKEN_SECRET, testUserId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), null);
	}

	@Test
	public void refreshOk() throws Exception {
		// GIVEN
		Instant now = Instant.now();
		LocalDate reqDate = LocalDate.of(2021, 10, 9);
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		queryParams.add("date", reqDate.toString());

		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(testTokenId)
				.host("localhost")
				.path("/api/v1/sec/auth-tokens/refresh/v2")
				.useSnDate(true).date(now)
				.queryParams(queryParams.toSingleValueMap())
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();
		String expectedKey = Hex.encodeHexString(auth.computeSigningKey(
				reqDate.atStartOfDay(ZoneOffset.UTC).toInstant(), TEST_TOKEN_SECRET));
		
		// WHEN
		mvc.perform(get("/api/v1/sec/auth-tokens/refresh/v2")
				.queryParam("date", reqDate.toString())
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(AuthorizationUtils.SN_DATE_HEADER,
						AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success", is(true)))
			.andExpect(jsonPath("$.data.key", is(expectedKey)));
		// @formatter:on
	}

	@Test
	public void badTokenCredentials() throws Exception {
		// GIVEN
		Instant now = Instant.now();
		LocalDate reqDate = LocalDate.of(2021, 10, 9);
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		queryParams.add("date", reqDate.toString());

		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(testTokenId)
				.host("localhost")
				.path("/api/v1/sec/auth-tokens/refresh/v2")
				.useSnDate(true).date(now)
				.queryParams(queryParams.toSingleValueMap())
				.saveSigningKey("wrong.secret");
		String authHeader = auth.build();

		// WHEN
		mvc.perform(get("/api/v1/sec/auth-tokens/refresh/v2")
				.queryParam("date", reqDate.toString())
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(AuthorizationUtils.SN_DATE_HEADER,
						AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
		// @formatter:on
	}

	@Test
	public void wrongSchemeCredentials() throws Exception {
		// GIVEN
		LocalDate reqDate = LocalDate.of(2021, 10, 9);
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		queryParams.add("date", reqDate.toString());

		// @formatter:off
		// WHEN
		mvc.perform(get("/api/v1/sec/auth-tokens/refresh/v2")
				.queryParam("date", reqDate.toString())
				.header(HttpHeaders.AUTHORIZATION, 
						"BASIC " +Base64.getEncoder().encodeToString("foo:bar".getBytes()))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
		// @formatter:on
	}

	@Test
	public void noCredentials() throws Exception {
		// GIVEN
		LocalDate reqDate = LocalDate.of(2021, 10, 9);
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		queryParams.add("date", reqDate.toString());

		// @formatter:off
		// WHEN
		mvc.perform(get("/api/v1/sec/auth-tokens/refresh/v2")
				.queryParam("date", reqDate.toString())
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
		// @formatter:on
	}

}
