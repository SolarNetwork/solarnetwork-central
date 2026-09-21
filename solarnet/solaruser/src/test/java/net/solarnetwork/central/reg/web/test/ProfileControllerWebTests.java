/* ==================================================================
 * ProfileControllerWebTests.java - 22/09/2026 7:52:08 am
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

import static java.util.Map.entry;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUser;
import static net.solarnetwork.central.test.CommonTestUtils.randomEmail;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_NAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USERNAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USER_ID;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.reg.web.ProfileController;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.test.security.WithMockSecurityUser;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.domain.User;

/**
 * Web integration tests for the {@link ProfileController} class.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ProfileControllerWebTests {

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private RegistrationBiz registrationBiz;

	private Long otherUserId;
	private String otherUserEmail;
	private String otherUserName;

	@BeforeEach
	public void setup() {
		insertUser(jdbcOperations, DEFAULT_USER_ID, DEFAULT_USERNAME, randomString(), DEFAULT_NAME);

		otherUserId = randomLong();
		otherUserEmail = randomEmail();
		otherUserName = randomString();
		insertUser(jdbcOperations, otherUserId, otherUserEmail, randomString(), otherUserName);
	}

	private Map<String, Object> userRow(Long userId) {
		return jdbcOperations.queryForMap(
				"SELECT email::text AS email, disp_name AS name FROM solaruser.user_user WHERE id = ?",
				userId);
	}

	@WithMockSecurityUser
	@Test
	public void saveProfile() throws Exception {
		// GIVEN
		final String name = randomString();

		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/profile/save")
				.param("id", DEFAULT_USER_ID.toString())
				.param("email", DEFAULT_USERNAME)
				.param("name", name)
				.with(csrf())
			)
			.andExpect(status().isOk())
			;

		// THEN
		then(userRow(DEFAULT_USER_ID))
			.as("Active user profile updated")
			.containsOnly(entry("email", DEFAULT_USERNAME), entry("name", name))
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void saveProfile_otherUserId() throws Exception {
		// GIVEN
		final String email = randomEmail();
		final String name = randomString();

		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/profile/save")
				.param("id", otherUserId.toString())
				.param("email", email)
				.param("name", name)
				.with(csrf())
			)
			.andExpect(status().isOk())
			;

		// THEN
		then(userRow(otherUserId))
			.as("Other user profile unchanged")
			.containsOnly(entry("email", otherUserEmail), entry("name", otherUserName))
			;
		then(userRow(DEFAULT_USER_ID))
			.as("Active user profile updated instead of the given user ID")
			.containsOnly(entry("email", email), entry("name", name))
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void updateUser_otherUser() {
		// GIVEN
		final User userEntry = new User(otherUserId, randomEmail());
		userEntry.setName(randomString());

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Updating another user denied")
			.isThrownBy(() -> registrationBiz.updateUser(userEntry))
			;

		// THEN
		then(userRow(otherUserId))
			.as("Other user profile unchanged")
			.containsOnly(entry("email", otherUserEmail), entry("name", otherUserName))
			;
		// @formatter:on
	}

}
