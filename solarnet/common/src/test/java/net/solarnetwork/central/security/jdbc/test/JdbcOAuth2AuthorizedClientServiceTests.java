/* ==================================================================
 * JdbcOAuth2AuthorizedClientServiceTests.java - 21/09/2024 2:26:04 pm
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
import static net.solarnetwork.central.domain.UserIdentifiableSystem.userIdSystemIdentifier;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import net.solarnetwork.central.security.ClientAccessTokenEntity;
import net.solarnetwork.central.security.PrefixedTextEncryptor;
import net.solarnetwork.central.security.jdbc.JdbcOAuth2AuthorizedClientService;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.central.test.CommonTestUtils;

/**
 * Test cases for the {@link JdbcOAuth2AuthorizedClientService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcOAuth2AuthorizedClientServiceTests extends AbstractJUnit5JdbcDaoTestSupport {

	private PrefixedTextEncryptor encryptor;
	private Map<String, ClientRegistration> clientRegistrations;
	private JdbcOAuth2AuthorizedClientService service;

	private Long userId;

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
				"client_registration_id, principal_name");
	}

	@Test
	public void insert() {
		// GIVEN
		final Instant tokenIssueDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final OAuth2AccessToken accessToken = new OAuth2AccessToken(TokenType.BEARER, "TEST",
				tokenIssueDate, tokenIssueDate.plusSeconds(3600L),
				new LinkedHashSet<>(Arrays.asList("a", "b")));

		final OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("REFRESH", tokenIssueDate,
				tokenIssueDate.plusSeconds(3600L));

		final String registrationId = userIdSystemIdentifier(userId, "sntest", 1, 2);

		// @formatter:off
		final ClientRegistration clientReg = ClientRegistration.withRegistrationId(registrationId)
				.tokenUri("http://example.com/token")
				.clientId("client.id")
				.clientSecret("client.secret")
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.build();
		// @formatter:on

		final String principalName = "SN Test User 1 Config 1";

		final OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(clientReg, principalName,
				accessToken, refreshToken);

		final String username = "test.user";

		final Authentication auth = new UsernamePasswordAuthenticationToken(username, null);

		// WHEN
		service.saveAuthorizedClient(client, auth);

		// THEN
		List<Map<String, Object>> rows = allAuthClientServiceRows();

		// @formatter:off
		then(rows)
			.as("Authorized client saved to database row")
			.hasSize(1)
			.first(InstanceOfAssertFactories.map(String.class, Object.class))
			.as("User ID persisted")
			.containsEntry("user_id", userId)
			.as("Registration ID persisted")
			.containsEntry("client_registration_id", registrationId)
			.as("Principal name from Authentication persisted")
			.containsEntry("principal_name", username)
			.as("Access token type persisted")
			.containsEntry("access_token_type", accessToken.getTokenType().getValue())
			.as("Access token value persisted as UTF-8 bytes")
			.hasEntrySatisfying("access_token_value", value -> {
				then(value)
					.asInstanceOf(InstanceOfAssertFactories.BYTE_ARRAY)
					.containsExactly(accessToken.getTokenValue().getBytes(UTF_8))
					;
			})
			.as("Access token issue date persisted")
			.containsEntry("access_token_issued_at", Timestamp.from(accessToken.getIssuedAt()))
			.as("Access token expires date persisted")
			.containsEntry("access_token_expires_at", Timestamp.from(accessToken.getExpiresAt()))
			.as("Access token scopes persisted as comma-delimited string")
			.containsEntry("access_token_scopes", commaDelimitedStringFromCollection(accessToken.getScopes()))
			.as("Refresh token value persisted as UTF-8 bytes")
			.hasEntrySatisfying("refresh_token_value", value -> {
				then(value)
					.asInstanceOf(InstanceOfAssertFactories.BYTE_ARRAY)
					.as("Can decrypt refresh token value back to original plain text")
					.satisfies(cipherText -> {
						then(encryptor.decrypt(cipherText))
							.as("Decrypted value same as original plain text")
							.isEqualTo(refreshToken.getTokenValue().getBytes(UTF_8))
							;
					})
					;
			})
			.as("Refresh token issue date persisted")
			.containsEntry("access_token_issued_at", Timestamp.from(refreshToken.getIssuedAt()))
			.as("Refresh token expires date persisted")
			.containsEntry("access_token_expires_at", Timestamp.from(refreshToken.getExpiresAt()))
			;
		// @formatter:on
	}

	@Test
	public void select() {
		// GIVEN
		final Instant tokenIssueDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final OAuth2AccessToken accessToken = new OAuth2AccessToken(TokenType.BEARER, "TEST",
				tokenIssueDate, tokenIssueDate.plusSeconds(3600L),
				new LinkedHashSet<>(Arrays.asList("a", "b")));

		final OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("REFRESH", tokenIssueDate,
				tokenIssueDate.plusSeconds(3600L));

		final String registrationId = userIdSystemIdentifier(userId, "sntest", 1, 2);

		// @formatter:off
		final ClientRegistration clientReg = ClientRegistration.withRegistrationId(registrationId)
				.tokenUri("http://example.com/token")
				.clientId("client.id")
				.clientSecret("client.secret")
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.build();
		// @formatter:on

		clientRegistrations.put(registrationId, clientReg);

		final String principalName = "SN Test User 1 Config 1";

		final OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(clientReg, principalName,
				accessToken, refreshToken);

		final String username = "test.user";

		final Authentication auth = new UsernamePasswordAuthenticationToken(username, null);

		// WHEN
		service.saveAuthorizedClient(client, auth);

		OAuth2AuthorizedClient result = service.loadAuthorizedClient(registrationId, username);

		// THEN
		// @formatter:off
		then(result)
			.as("Authorized client returned from database")
			.isNotNull()
			.satisfies(c -> {
				then(c.getClientRegistration())
					.as("Registration returned from ClientRegistrationRepository")
					.isSameAs(clientReg)
					;
			})
			.as("Principal name populated from database")
			.returns(username, from(OAuth2AuthorizedClient::getPrincipalName))
			.satisfies(c -> {
				then(c.getAccessToken())
					.as("Access token type populated from database")
					.returns(accessToken.getTokenType(), from(OAuth2AccessToken::getTokenType))
					.as("Access token value populated from database")
					.returns(accessToken.getTokenValue(), from(OAuth2AccessToken::getTokenValue))
					.as("Access token issue date populated from database")
					.returns(accessToken.getIssuedAt(), from(OAuth2AccessToken::getIssuedAt))
					.as("Access token expire date populated from database")
					.returns(accessToken.getExpiresAt(), from(OAuth2AccessToken::getExpiresAt))
					.as("Access token scopes populated from database")
					.returns(accessToken.getScopes(), from(OAuth2AccessToken::getScopes))
					;
			})
			.satisfies(c -> {
				then(c.getRefreshToken())
					.as("Refresh token value populated from database")
					.returns(refreshToken.getTokenValue(), from(OAuth2RefreshToken::getTokenValue))
					.as("Refresh token issue date populated from database")
					.returns(refreshToken.getIssuedAt(), from(OAuth2RefreshToken::getIssuedAt))
					.as("Refresh token expiration not available from database")
					.returns(null, from(OAuth2RefreshToken::getExpiresAt))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void select_noMatch() {
		// GIVEN
		insert(); // populate a row

		// WHEN
		OAuth2AuthorizedClient result = service.loadAuthorizedClient(randomString(), randomString());

		// THEN
		// @formatter:off
		then(result)
			.as("Authorized client not found in database")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void select_unencryptedRefreshToken() {
		// GIVEN
		final ClientAccessTokenEntity entity = new ClientAccessTokenEntity(userId,
				"%d:%s".formatted(userId, randomString()), randomString(),
				now().truncatedTo(ChronoUnit.MILLIS));
		entity.setAccessTokenType(TokenType.BEARER.getValue());
		entity.setAccessToken(randomString().getBytes(UTF_8));
		entity.setAccessTokenIssuedAt(now().truncatedTo(ChronoUnit.MILLIS));
		entity.setAccessTokenExpiresAt(entity.getAccessTokenIssuedAt().plusSeconds(3600L));
		entity.setAccessTokenScopes(Set.of());
		entity.setRefreshToken(randomString().getBytes(UTF_8));
		entity.setRefreshTokenIssuedAt(entity.getAccessTokenIssuedAt().plusSeconds(1L));

		JdbcOAuth2AuthorizedClientService_ClientAccessTokenDaoTests.save(jdbcTemplate, entity);

		// @formatter:off
		final ClientRegistration clientReg = ClientRegistration.withRegistrationId(entity.getRegistrationId())
				.tokenUri("http://example.com/token")
				.clientId("client.id")
				.clientSecret("client.secret")
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.build();
		// @formatter:on

		clientRegistrations.put(entity.getRegistrationId(), clientReg);

		// WHEN
		OAuth2AuthorizedClient result = service.loadAuthorizedClient(entity.getRegistrationId(),
				entity.getPrincipalName());

		// THEN
		// @formatter:off
		then(result)
			.as("Authorized client returned from database")
			.isNotNull()
			.satisfies(c -> {
				then(c.getClientRegistration())
					.as("Registration returned from ClientRegistrationRepository")
					.isSameAs(clientReg)
					;
			})
			.as("Principal name populated from database")
			.returns(entity.getPrincipalName(), from(OAuth2AuthorizedClient::getPrincipalName))
			.satisfies(c -> {
				then(c.getAccessToken())
					.as("Access token type populated from database")
					.returns(TokenType.BEARER, from(OAuth2AccessToken::getTokenType))
					.as("Access token value populated from database")
					.returns(entity.getAccessTokenValue(), from(OAuth2AccessToken::getTokenValue))
					.as("Access token issue date populated from database")
					.returns(entity.getAccessTokenIssuedAt(), from(OAuth2AccessToken::getIssuedAt))
					.as("Access token expire date populated from database")
					.returns(entity.getAccessTokenExpiresAt(), from(OAuth2AccessToken::getExpiresAt))
					.as("Access token scopes populated from database")
					.returns(Set.of(), from(OAuth2AccessToken::getScopes))
					;
			})
			.satisfies(c -> {
				then(c.getRefreshToken())
					.as("Refresh token value populated from database")
					.returns(entity.getRefreshTokenValue(), from(OAuth2RefreshToken::getTokenValue))
					.as("Refresh token issue date populated from database")
					.returns(entity.getRefreshTokenIssuedAt(), from(OAuth2RefreshToken::getIssuedAt))
					.as("Refresh token expiration not available from database")
					.returns(null, from(OAuth2RefreshToken::getExpiresAt))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void update() {
		// GIVEN
		final Instant tokenIssueDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final OAuth2AccessToken accessToken = new OAuth2AccessToken(TokenType.BEARER, "TEST",
				tokenIssueDate, tokenIssueDate.plusSeconds(3600L),
				new LinkedHashSet<>(Arrays.asList("a", "b")));

		final OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("REFRESH", tokenIssueDate,
				tokenIssueDate.plusSeconds(3600L));

		final String registrationId = userIdSystemIdentifier(userId, "sntest", 1, 2);

		// @formatter:off
		final ClientRegistration clientReg = ClientRegistration.withRegistrationId(registrationId)
				.tokenUri("http://example.com/token")
				.clientId("client.id")
				.clientSecret("client.secret")
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.build();
		// @formatter:on

		final String principalName = "SN Test User 1 Config 1";

		final OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(clientReg, principalName,
				accessToken, refreshToken);

		final String username = "test.user";

		final Authentication auth = new UsernamePasswordAuthenticationToken(username, null);

		// WHEN
		service.saveAuthorizedClient(client, auth);

		final List<Map<String, Object>> rows1 = allAuthClientServiceRows();

		final Instant tokenIssueDate2 = tokenIssueDate.plusSeconds(3600L);
		final OAuth2AccessToken accessToken2 = new OAuth2AccessToken(TokenType.BEARER, "TEST2",
				tokenIssueDate2, tokenIssueDate2.plusSeconds(3600L),
				new LinkedHashSet<>(Arrays.asList("a", "b", "c")));

		final OAuth2RefreshToken refreshToken2 = new OAuth2RefreshToken("REFRESH2", tokenIssueDate2,
				tokenIssueDate2.plusSeconds(3600L));

		final OAuth2AuthorizedClient client2 = new OAuth2AuthorizedClient(clientReg, principalName,
				accessToken2, refreshToken2);

		service.saveAuthorizedClient(client2, auth);

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
			.containsEntry("client_registration_id", registrationId)
			.as("Principal name from Authentication persisted")
			.containsEntry("principal_name", username)
			.as("Access token type persisted")
			.containsEntry("access_token_type", accessToken2.getTokenType().getValue())
			.as("Access token value persisted as UTF-8 bytes")
			.hasEntrySatisfying("access_token_value", value -> {
				then(value)
					.asInstanceOf(InstanceOfAssertFactories.BYTE_ARRAY)
					.containsExactly(accessToken2.getTokenValue().getBytes(StandardCharsets.UTF_8))
					;
			})
			.as("Access token issue date persisted")
			.containsEntry("access_token_issued_at", Timestamp.from(accessToken2.getIssuedAt()))
			.as("Access token expires date persisted")
			.containsEntry("access_token_expires_at", Timestamp.from(accessToken2.getExpiresAt()))
			.as("Access token scopes persisted as comma-delimited string")
			.containsEntry("access_token_scopes", commaDelimitedStringFromCollection(accessToken2.getScopes()))
			.as("Refresh token value persisted as UTF-8 bytes")
			.hasEntrySatisfying("refresh_token_value", value -> {
				then(value)
					.asInstanceOf(InstanceOfAssertFactories.BYTE_ARRAY)
					.as("Can decrypt refresh token value back to original plain text")
					.satisfies(cipherText -> {
						then(encryptor.decrypt(cipherText))
							.as("Decrypted value same as original plain text")
							.isEqualTo(refreshToken2.getTokenValue().getBytes(UTF_8))
							;
					})
					;
			})
			.as("Refresh token issue date persisted")
			.containsEntry("access_token_issued_at", Timestamp.from(refreshToken2.getIssuedAt()))
			.as("Refresh token expires date persisted")
			.containsEntry("access_token_expires_at", Timestamp.from(refreshToken2.getExpiresAt()))
			;
		// @formatter:on
	}

	@Test
	public void remove() {
		// GIVEN
		final Instant tokenIssueDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final OAuth2AccessToken accessToken = new OAuth2AccessToken(TokenType.BEARER, "TEST",
				tokenIssueDate, tokenIssueDate.plusSeconds(3600L),
				new LinkedHashSet<>(Arrays.asList("a", "b")));

		final OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("REFRESH", tokenIssueDate,
				tokenIssueDate.plusSeconds(3600L));

		final String registrationId = userIdSystemIdentifier(userId, "sntest", 1, 2);

		// @formatter:off
		final ClientRegistration clientReg = ClientRegistration.withRegistrationId(registrationId)
				.tokenUri("http://example.com/token")
				.clientId("client.id")
				.clientSecret("client.secret")
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.build();
		// @formatter:on

		final String principalName = "SN Test User 1 Config 1";

		final OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(clientReg, principalName,
				accessToken, refreshToken);

		final String username = "test.user";

		final Authentication auth = new UsernamePasswordAuthenticationToken(username, null);

		// WHEN
		service.saveAuthorizedClient(client, auth);

		List<Map<String, Object>> rows1 = allAuthClientServiceRows();

		service.removeAuthorizedClient(registrationId, username);

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
