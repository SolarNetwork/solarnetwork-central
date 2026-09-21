/* ==================================================================
 * UserMetadataControllerWebTests.java - 22/09/2026 7:30:52 am
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

package net.solarnetwork.central.reg.web.api.v1.test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.security.SecurityTokenStatus.Active;
import static net.solarnetwork.central.security.SecurityTokenType.User;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityToken;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.security.AuthorizationUtils.AUTHORIZATION_DATE_HEADER_FORMATTER;
import static net.solarnetwork.security.AuthorizationUtils.SN_DATE_HEADER;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import net.solarnetwork.central.reg.web.api.v1.UserMetadataController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Web API level integration tests for the {@link UserMetadataController}
 * class.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
public class UserMetadataControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	private static final String META_PATH = "/api/v1/sec/user/meta/";

	private static final String OTHER_USER_META_JSON = """
			{"m":{"secret":"value"}}
			""";

	private static final String REQUEST_META_JSON = """
			{"m":{"foo":"bar"}}
			""";

	@Autowired
	private MockMvc mvc;

	private Long userId;
	private String tokenId;
	private String tokenSecret;
	private Long otherUserId;

	@BeforeEach
	public void setup() {
		userId = randomLong();
		setupTestUser(userId, randomString() + "@localhost");
		tokenId = randomString(20);
		tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		otherUserId = randomLong();
		setupTestUser(otherUserId, randomString() + "@localhost");
		insertUserMetadata(otherUserId, OTHER_USER_META_JSON);
	}

	private void insertUserMetadata(Long userId, String json) {
		jdbcTemplate.update("""
				INSERT INTO solaruser.user_meta (user_id, created, updated, jdata)
				VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?::jsonb)
				""", userId, json);
	}

	private List<String> userMetadata(Long userId) {
		return jdbcTemplate.queryForList("SELECT jdata::text FROM solaruser.user_meta WHERE user_id = ?",
				String.class, userId);
	}

	private ResultActions performSigned(HttpMethod method, String path, @Nullable String body)
			throws Exception {
		final Instant now = Instant.now();
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(method.name()).host("localhost").path(path).useSnDate(true).date(now);
		final MockHttpServletRequestBuilder req = request(method, path)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON);
		if ( body != null ) {
			auth.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
					.contentSha256(DigestUtils.sha256(body));
			req.content(body).contentType(MediaType.APPLICATION_JSON);
		}
		auth.saveSigningKey(tokenSecret);
		return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, auth.build()));
	}

	@Test
	public void addMetadata() throws Exception {
		// WHEN
		performSigned(HttpMethod.POST, META_PATH + userId, REQUEST_META_JSON)
				.andExpect(status().isOk());

		// THEN
		// @formatter:off
		then(userMetadata(userId))
			.as("Metadata added for token user")
			.singleElement()
			.asInstanceOf(JSON)
			.isEqualTo(REQUEST_META_JSON)
			;
		// @formatter:on
	}

	@Test
	public void addMetadata_otherUser() throws Exception {
		// WHEN
		performSigned(HttpMethod.POST, META_PATH + otherUserId, REQUEST_META_JSON)
				.andExpect(status().isForbidden());

		// THEN
		// @formatter:off
		then(userMetadata(otherUserId))
			.as("Other user metadata unchanged")
			.singleElement()
			.asInstanceOf(JSON)
			.isEqualTo(OTHER_USER_META_JSON)
			;
		// @formatter:on
	}

	@Test
	public void replaceMetadata() throws Exception {
		// GIVEN
		insertUserMetadata(userId, OTHER_USER_META_JSON);

		// WHEN
		performSigned(HttpMethod.PUT, META_PATH + userId, REQUEST_META_JSON)
				.andExpect(status().isOk());

		// THEN
		// @formatter:off
		then(userMetadata(userId))
			.as("Metadata replaced for token user")
			.singleElement()
			.asInstanceOf(JSON)
			.isEqualTo(REQUEST_META_JSON)
			;
		// @formatter:on
	}

	@Test
	public void replaceMetadata_otherUser() throws Exception {
		// WHEN
		performSigned(HttpMethod.PUT, META_PATH + otherUserId, REQUEST_META_JSON)
				.andExpect(status().isForbidden());

		// THEN
		// @formatter:off
		then(userMetadata(otherUserId))
			.as("Other user metadata unchanged")
			.singleElement()
			.asInstanceOf(JSON)
			.isEqualTo(OTHER_USER_META_JSON)
			;
		// @formatter:on
	}

	@Test
	public void deleteMetadata() throws Exception {
		// GIVEN
		insertUserMetadata(userId, OTHER_USER_META_JSON);

		// WHEN
		performSigned(HttpMethod.DELETE, META_PATH + userId, null).andExpect(status().isOk());

		// THEN
		// @formatter:off
		then(userMetadata(userId))
			.as("Metadata deleted for token user")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void deleteMetadata_otherUser() throws Exception {
		// WHEN
		performSigned(HttpMethod.DELETE, META_PATH + otherUserId, null)
				.andExpect(status().isForbidden());

		// THEN
		// @formatter:off
		then(userMetadata(otherUserId))
			.as("Other user metadata not deleted")
			.singleElement()
			.asInstanceOf(JSON)
			.isEqualTo(OTHER_USER_META_JSON)
			;
		// @formatter:on
	}

}
