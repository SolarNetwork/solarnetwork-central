/* ==================================================================
 * UserNodeInstructionTaskEntityTests.java - 3/12/2025 10:28:10 am
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

package net.solarnetwork.central.user.domain.test;

import static net.solarnetwork.central.domain.UserIdentifiableSystem.userIdSystemIdentifier;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity.EXPRESSION_SETTINGS_PROP;
import static net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity.OAUTH_SYSTEM_NAME;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.Map;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.common.http.HttpConstants;
import net.solarnetwork.central.common.http.OAuth2ClientIdentity;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;

/**
 * Test cases for the {@link UserNodeInstructionTaskEntity}.
 * 
 * @author matt
 * @version 1.0
 */
public class UserNodeInstructionTaskEntityTests {

	@Test
	public void oauthClientIdentity_missingTokenUrl() {
		// GIVEN
		UserNodeInstructionTaskEntity task = new UserNodeInstructionTaskEntity(randomLong(),
				randomLong());

		final String clientId = randomString();
		// @formatter:off
		task.setServiceProps(Map.of(EXPRESSION_SETTINGS_PROP, Map.of(
				HttpConstants.OAUTH_CLIENT_ID_SETTING, clientId
				)));
		// @formatter:on

		// WHEN
		OAuth2ClientIdentity result = task.oauthClientIdentity();

		// THEN
		// @formatter:off
		then(result)
			.as("No result provided because token URL missing")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void oauthClientIdentity_username() {
		// GIVEN
		UserNodeInstructionTaskEntity task = new UserNodeInstructionTaskEntity(randomLong(),
				randomLong());

		final String username = randomString();
		// @formatter:off
		task.setServiceProps(Map.of(EXPRESSION_SETTINGS_PROP, Map.of(
				HttpConstants.OAUTH_TOKEN_URL_SETTING, randomString(),
				HttpConstants.USERNAME_SETTING, username
				)));
		// @formatter:on

		// WHEN
		OAuth2ClientIdentity result = task.oauthClientIdentity();

		// THEN
		// @formatter:off
		then(result)
			.as("Result provided")
			.isNotNull()
			.as("Task ID is configuration ID")
			.returns(task.getId(), from(OAuth2ClientIdentity::configId))
			.as("Registration ID composed from configuration ID")
			.returns(userIdSystemIdentifier(task.getUserId(), OAUTH_SYSTEM_NAME, task.getConfigId()),
					from(OAuth2ClientIdentity::registrationId))
			.as("Username returned as principal")
			.returns(username, from(OAuth2ClientIdentity::principal))
			;
		// @formatter:on
	}

	@Test
	public void oauthClientIdentity_clientId() {
		// GIVEN
		UserNodeInstructionTaskEntity task = new UserNodeInstructionTaskEntity(randomLong(),
				randomLong());

		final String clientId = randomString();
		// @formatter:off
		task.setServiceProps(Map.of(EXPRESSION_SETTINGS_PROP, Map.of(
				HttpConstants.OAUTH_TOKEN_URL_SETTING, randomString(),
				HttpConstants.OAUTH_CLIENT_ID_SETTING, clientId
				)));
		// @formatter:on

		// WHEN
		OAuth2ClientIdentity result = task.oauthClientIdentity();

		// THEN
		// @formatter:off
		then(result)
			.as("Result provided")
			.isNotNull()
			.as("Task ID is configuration ID")
			.returns(task.getId(), from(OAuth2ClientIdentity::configId))
			.as("Registration ID composed from configuration ID")
			.returns(userIdSystemIdentifier(task.getUserId(), OAUTH_SYSTEM_NAME, task.getConfigId()),
					from(OAuth2ClientIdentity::registrationId))
			.as("Client ID returned as principal")
			.returns(clientId, from(OAuth2ClientIdentity::principal))
			;
		// @formatter:on
	}

	@Test
	public void oauthClientIdentity_synthetic() {
		// GIVEN
		UserNodeInstructionTaskEntity task = new UserNodeInstructionTaskEntity(randomLong(),
				randomLong());
		task.setName(randomString());

		// @formatter:off
		task.setServiceProps(Map.of(EXPRESSION_SETTINGS_PROP, Map.of(
				HttpConstants.OAUTH_TOKEN_URL_SETTING, randomString()
				)));
		// @formatter:on

		// WHEN
		OAuth2ClientIdentity result = task.oauthClientIdentity();

		// THEN
		// @formatter:off
		then(result)
			.as("Result provided")
			.isNotNull()
			.as("Task ID is configuration ID")
			.returns(task.getId(), from(OAuth2ClientIdentity::configId))
			.as("Registration ID composed from configuration ID")
			.returns(userIdSystemIdentifier(task.getUserId(), OAUTH_SYSTEM_NAME, task.getConfigId()),
					from(OAuth2ClientIdentity::registrationId))
			.as("Client ID returned as principal")
			.returns("%s %s".formatted(task.getId().ident(), task.getName()), from(OAuth2ClientIdentity::principal))
			;
		// @formatter:on
	}

}
