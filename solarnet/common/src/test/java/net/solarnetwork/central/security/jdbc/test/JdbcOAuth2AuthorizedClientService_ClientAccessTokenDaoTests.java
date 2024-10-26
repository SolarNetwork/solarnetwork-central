/* ==================================================================
 * JdbcOAuth2AuthorizedClientService_ClientAccessTokenDaoTests.java - 25/10/2024 10:34:11 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.security.jdbc.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.security.ClientAccessTokenEntity;
import net.solarnetwork.central.security.PrefixedTextEncryptor;
import net.solarnetwork.central.security.jdbc.JdbcOAuth2AuthorizedClientService;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.central.test.CommonTestUtils;

/**
 * Test cases for the {@link JdbcOAuth2AuthorizedClientService} class operating
 * as a {@link ClientAccessTokenDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcOAuth2AuthorizedClientService_ClientAccessTokenDaoTests
		extends AbstractJUnit5JdbcDaoTestSupport {

	private PrefixedTextEncryptor encryptor;
	private Map<String, ClientRegistration> clientRegistrations;
	private ClientAccessTokenDao service;

	private Long userId;

	private ClientAccessTokenEntity last;

	@BeforeEach
	public void setup() {
		encryptor = PrefixedTextEncryptor.aesTextEncryptor(randomString(), randomString());
		clientRegistrations = new HashMap<>();
		service = new JdbcOAuth2AuthorizedClientService(encryptor, jdbcTemplate,
				new InMemoryClientRegistrationRepository(clientRegistrations));

		userId = CommonTestUtils.randomLong();
		setupTestUser(userId);
	}

	private List<Map<String, Object>> allAuthClientServiceRows() {
		return CommonDbTestUtils.allTableData(log, jdbcTemplate,
				JdbcOAuth2AuthorizedClientService.DEFAULT_TABLE_NAME,
				"user_id, client_registration_id, principal_name");
	}

	@Test
	public void insert() {
		// GIVEN

		final ClientAccessTokenEntity entity = new ClientAccessTokenEntity(userId, randomString(),
				randomString(), now());
		entity.setAccessTokenType(randomString());
		entity.setAccessToken(randomString().getBytes(UTF_8));
		entity.setAccessTokenIssuedAt(now().truncatedTo(ChronoUnit.MILLIS));
		entity.setAccessTokenExpiresAt(entity.getAccessTokenIssuedAt().plusSeconds(3600L));
		entity.setAccessTokenScopes(new LinkedHashSet<>(Arrays.asList("a", "b")));
		entity.setRefreshToken(randomString().getBytes(UTF_8));
		entity.setRefreshTokenIssuedAt(entity.getAccessTokenIssuedAt().plusSeconds(1L));

		// WHEN
		var result = service.store(entity);

		// THEN
		// @formatter:off
		then(result)
			.as("Primary key returned")
			.isNotNull()
			.as("Primary key equals that provided")
			.isEqualTo(entity.getId())
			;

		List<Map<String, Object>> rows = allAuthClientServiceRows();
		then(rows)
			.as("Authorized client saved to database row")
			.hasSize(1)
			.first(InstanceOfAssertFactories.map(String.class, Object.class))
			.as("User ID persisted")
			.containsEntry("user_id", userId)
			.as("Registration ID persisted")
			.containsEntry("client_registration_id", entity.getRegistrationId())
			.as("Principal name from Authentication persisted")
			.containsEntry("principal_name", entity.getPrincipalName())
			.as("Access token type persisted")
			.containsEntry("access_token_type", entity.getAccessTokenType())
			.as("Access token value persisted as UTF-8 bytes")
			.hasEntrySatisfying("access_token_value", value -> {
				then(value)
					.asInstanceOf(InstanceOfAssertFactories.BYTE_ARRAY)
					.containsExactly(entity.getAccessToken())
					;
			})
			.as("Access token issue date persisted")
			.containsEntry("access_token_issued_at", Timestamp.from(entity.getAccessTokenIssuedAt()))
			.as("Access token expires date persisted")
			.containsEntry("access_token_expires_at", Timestamp.from(entity.getAccessTokenExpiresAt()))
			.as("Access token scopes persisted as comma-delimited string")
			.containsEntry("access_token_scopes", commaDelimitedStringFromCollection(entity.getAccessTokenScopes()))
			.as("Refresh token value persisted as UTF-8 bytes")
			.hasEntrySatisfying("refresh_token_value", value -> {
				then(value)
					.asInstanceOf(InstanceOfAssertFactories.BYTE_ARRAY)
					.as("Can decrypt refresh token value back to original plain text")
					.satisfies(cipherText -> {
						then(encryptor.decrypt(cipherText))
							.as("Decrypted value same as original plain text")
							.containsExactly(entity.getRefreshToken())
							;
					})
					;
			})
			.as("Refresh token issue date persisted")
			.containsEntry("refresh_token_issued_at", Timestamp.from(entity.getRefreshTokenIssuedAt()))
			.as("Creation date persisted")
			.containsEntry("created_at", Timestamp.from(entity.getCreated()))
			;
		// @formatter:on

		last = entity;
	}

	@Test
	public void select() {
		// GIVEN
		insert();

		// WHEN
		ClientAccessTokenEntity result = service.get(last.getId());

		// THEN
		// @formatter:off
		then(result)
			.as("Authorized client returned from database")
			.isNotNull()
			.as("Registration ID populated from database")
			.returns(last.getRegistrationId(), from(ClientAccessTokenEntity::getRegistrationId))
			.as("Principal name populated from database")
			.returns(last.getPrincipalName(), from(ClientAccessTokenEntity::getPrincipalName))
			.as("Access token type populated from database")
			.returns(last.getAccessTokenType(), from(ClientAccessTokenEntity::getAccessTokenType))
			.as("Access token value populated from database")
			.returns(last.getAccessTokenValue(), from(ClientAccessTokenEntity::getAccessTokenValue))
			.as("Access token issue date populated from database")
			.returns(last.getAccessTokenIssuedAt(), from(ClientAccessTokenEntity::getAccessTokenIssuedAt))
			.as("Access token expire date populated from database")
			.returns(last.getAccessTokenExpiresAt(), from(ClientAccessTokenEntity::getAccessTokenExpiresAt))
			.as("Access token scopes populated from database")
			.returns(last.getAccessTokenScopes(), from(ClientAccessTokenEntity::getAccessTokenScopes))
			.as("Refresh token value populated from database")
			.returns(last.getRefreshTokenValue(), from(ClientAccessTokenEntity::getRefreshTokenValue))
			.as("Refresh token issue date populated from database")
			.returns(last.getRefreshTokenIssuedAt(), from(ClientAccessTokenEntity::getRefreshTokenIssuedAt))
			.as("Creation date populated from database")
			.returns(last.getCreated(), from(ClientAccessTokenEntity::getCreated))
			;
		// @formatter:on
	}

	@Test
	public void select_noMatch() {
		// GIVEN
		insert(); // populate a row

		// WHEN
		ClientAccessTokenEntity result = service
				.get(new UserStringStringCompositePK(randomLong(), randomString(), randomString()));

		// THEN
		// @formatter:off
		then(result)
			.as("Authorized client not found in database")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		final List<Map<String, Object>> rows1 = allAuthClientServiceRows();

		ClientAccessTokenEntity entity = last.clone();
		entity.setAccessToken("TEST2".getBytes(UTF_8));
		entity.setAccessTokenIssuedAt(last.getAccessTokenIssuedAt().plusSeconds(3600L));
		entity.setAccessTokenExpiresAt(entity.getAccessTokenIssuedAt().plusSeconds(3600L));
		entity.setAccessTokenScopes(new LinkedHashSet<>(Arrays.asList("a", "b", "c")));
		entity.setRefreshToken("REFRESH2".getBytes(UTF_8));
		entity.setRefreshTokenIssuedAt(entity.getAccessTokenIssuedAt().plusSeconds(1L));

		service.store(entity);

		// THEN
		List<Map<String, Object>> rows2 = allAuthClientServiceRows();

		// @formatter:off
		then(rows1)
			.as("Authorized client saved as database row")
			.hasSize(1)
			;
		
		then(rows2)
			.as("Refreshed authorized client saved to database row")
			.hasSize(1)
			.first(InstanceOfAssertFactories.map(String.class, Object.class))
			.as("User ID persisted")
			.containsEntry("user_id", userId)
			.as("Registration ID persisted")
			.containsEntry("client_registration_id", entity.getRegistrationId())
			.as("Principal name from Authentication persisted")
			.containsEntry("principal_name", entity.getPrincipalName())
			.as("Access token type persisted")
			.containsEntry("access_token_type", entity.getAccessTokenType())
			.as("Access token value persisted as UTF-8 bytes")
			.hasEntrySatisfying("access_token_value", value -> {
				then(value)
					.asInstanceOf(InstanceOfAssertFactories.BYTE_ARRAY)
					.containsExactly(entity.getAccessToken())
					;
			})
			.as("Access token issue date persisted")
			.containsEntry("access_token_issued_at", Timestamp.from(entity.getAccessTokenIssuedAt()))
			.as("Access token expires date persisted")
			.containsEntry("access_token_expires_at", Timestamp.from(entity.getAccessTokenExpiresAt()))
			.as("Access token scopes persisted as comma-delimited string")
			.containsEntry("access_token_scopes", commaDelimitedStringFromCollection(entity.getAccessTokenScopes()))
			.as("Refresh token value persisted as UTF-8 bytes")
			.hasEntrySatisfying("refresh_token_value", value -> {
				then(value)
					.asInstanceOf(InstanceOfAssertFactories.BYTE_ARRAY)
					.as("Can decrypt refresh token value back to original plain text")
					.satisfies(cipherText -> {
						then(encryptor.decrypt(cipherText))
							.as("Decrypted value same as original plain text")
							.containsExactly(entity.getRefreshToken())
							;
					})
					;
			})
			.as("Refresh token issue date persisted")
			.containsEntry("refresh_token_issued_at", Timestamp.from(entity.getRefreshTokenIssuedAt()))
			.as("Creation date persisted")
			.containsEntry("created_at", Timestamp.from(entity.getCreated()))
			;
		// @formatter:on
	}

	@Test
	public void remove() {
		// GIVEN
		insert();

		// WHEN
		List<Map<String, Object>> rows1 = allAuthClientServiceRows();

		service.delete(last);

		// THEN
		List<Map<String, Object>> rows2 = allAuthClientServiceRows();

		// @formatter:off
		then(rows1)
			.as("Authorized client saved as database row")
			.hasSize(1)
			;
	
		then(rows2)
			.as("Authorized client row removed from database")
			.isEmpty()
			;
	
		// @formatter:on
	}

}
