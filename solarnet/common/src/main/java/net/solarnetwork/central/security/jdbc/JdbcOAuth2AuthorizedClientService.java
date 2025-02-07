/* ==================================================================
 * JdbcOAuth2AuthorizedClientService.java - 20/09/2024 1:10:34 pm
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

package net.solarnetwork.central.security.jdbc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getTimestampInstant;
import static net.solarnetwork.central.domain.UserIdentifiableSystem.userIdFromSystemIdentifier;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.util.StringUtils;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.security.ClientAccessTokenEntity;
import net.solarnetwork.domain.SortDescriptor;

/**
 * A JDBC implementation of an {@link OAuth2AuthorizedClientService} that uses a
 * {@link JdbcOperations} for {@link OAuth2AuthorizedClient} persistence.
 *
 * <p>
 * Based on
 * {@link org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService}.
 * The SQL table is expected to have the following structure:
 * </p>
 *
 * <pre>{@code TABLE oauth2_authorized_client (
 *   user_id BIGINT NOT NULL,
 *   client_registration_id varchar(100) NOT NULL,
 *   principal_name varchar(200) NOT NULL,
 *   access_token_type varchar(100) NOT NULL,
 *   access_token_value bytea NOT NULL,
 *   access_token_issued_at timestamp NOT NULL,
 *   access_token_expires_at timestamp NOT NULL,
 *   access_token_scopes varchar(1000) DEFAULT NULL,
 *   refresh_token_value bytea DEFAULT NULL,
 *   refresh_token_issued_at timestamp DEFAULT NULL,
 *   created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
 *   PRIMARY KEY (client_registration_id, principal_name)
 * )}</pre>
 *
 * @author matt
 * @version 1.2
 */
public class JdbcOAuth2AuthorizedClientService
		implements OAuth2AuthorizedClientService, ClientAccessTokenDao {

	/** The default SQL table name to use. */
	public static final String DEFAULT_TABLE_NAME = "solarnet.oauth2_authorized_client";

	private static final String LOAD_AUTHORIZED_CLIENT_SQL_TMPL = """
			SELECT user_id
				, client_registration_id
				, principal_name
				, access_token_type
				, access_token_value
				, access_token_issued_at
				, access_token_expires_at
				, access_token_scopes
				, refresh_token_value
				, refresh_token_issued_at
				, created_at
			FROM %s
			WHERE user_id = ? AND client_registration_id = ? AND principal_name = ?
			""";

	private static final String SAVE_AUTHORIZED_CLIENT_SQL_TMPL = """
			INSERT INTO %s (
				  user_id
				, client_registration_id
				, principal_name
				, access_token_type
				, access_token_value
				, access_token_issued_at
				, access_token_expires_at
				, access_token_scopes
				, refresh_token_value
				, refresh_token_issued_at
				, created_at
			)
			VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			ON CONFLICT (user_id, client_registration_id , principal_name) DO UPDATE
			SET access_token_type = EXCLUDED.access_token_type
				, access_token_value = EXCLUDED.access_token_value
				, access_token_issued_at = EXCLUDED.access_token_issued_at
				, access_token_expires_at = EXCLUDED.access_token_expires_at
				, access_token_scopes = EXCLUDED.access_token_scopes
				, refresh_token_value = EXCLUDED.refresh_token_value
				, refresh_token_issued_at = EXCLUDED.refresh_token_issued_at
			""";

	private static final String REMOVE_AUTHORIZED_CLIENT_SQL_TMPL = """
			DELETE FROM %s WHERE user_id = ? AND client_registration_id = ? AND principal_name = ?
			""";

	private final BytesEncryptor encryptor;
	private final JdbcOperations jdbcOperations;
	private final RowMapper<OAuth2AuthorizedClient> authorizedClientRowMapper;
	private final RowMapper<ClientAccessTokenEntity> clientAccessTokenRowMapper;
	private final String sqlSelect;
	private final String sqlInsert;
	private final String sqlDelete;

	//protected final LobHandler lobHandler;

	/**
	 * Constructs a {@code JdbcOAuth2AuthorizedClientService} using the provided
	 * parameters.
	 *
	 * @param encryptor
	 *        the encryptor
	 * @param jdbcOperations
	 *        the JDBC operations
	 * @param clientRegistrationRepository
	 *        the repository of client registrations
	 */
	public JdbcOAuth2AuthorizedClientService(BytesEncryptor encryptor, JdbcOperations jdbcOperations,
			ClientRegistrationRepository clientRegistrationRepository) {
		this(encryptor, jdbcOperations, clientRegistrationRepository, DEFAULT_TABLE_NAME);
	}

	/**
	 * Constructs a {@code JdbcOAuth2AuthorizedClientService} using the provided
	 * parameters.
	 *
	 * @param encryptor
	 *        the encryptor
	 * @param jdbcOperations
	 *        the JDBC operations
	 * @param clientRegistrationRepository
	 *        the repository of client registrations
	 * @param tableName
	 *        the SQL table name to use
	 */
	public JdbcOAuth2AuthorizedClientService(BytesEncryptor encryptor, JdbcOperations jdbcOperations,
			ClientRegistrationRepository clientRegistrationRepository, String tableName) {
		this.encryptor = requireNonNullArgument(encryptor, "encryptor");
		this.jdbcOperations = requireNonNullArgument(jdbcOperations, "jdbcOperations");
		requireNonNullArgument(clientRegistrationRepository, "clientRegistrationRepository");
		requireNonNullArgument(tableName, "tableName");

		this.authorizedClientRowMapper = new OAuth2AuthorizedClientRowMapper(
				clientRegistrationRepository);
		this.clientAccessTokenRowMapper = new ClientAccessTokenEntityRowMapper();

		this.sqlSelect = LOAD_AUTHORIZED_CLIENT_SQL_TMPL.formatted(tableName);
		this.sqlInsert = SAVE_AUTHORIZED_CLIENT_SQL_TMPL.formatted(tableName);
		this.sqlDelete = REMOVE_AUTHORIZED_CLIENT_SQL_TMPL.formatted(tableName);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId,
			String principalName) {
		final Long userId = userIdFromSystemIdentifier(clientRegistrationId);
		if ( userId == null ) {
			return null;
		}
		final var sql = new SelectAuthorizedClient(userId, clientRegistrationId, principalName);
		List<OAuth2AuthorizedClient> result = this.jdbcOperations.query(sql,
				this.authorizedClientRowMapper);
		return !result.isEmpty() ? (T) result.getFirst() : null;
	}

	@Override
	public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
		final var sql = upsertForClient(authorizedClient, principal);
		jdbcOperations.update(sql);
	}

	@Override
	public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
		final Long userId = userIdFromSystemIdentifier(clientRegistrationId);
		if ( userId == null ) {
			return;
		}
		final var sql = new DeleteAuthorizedClient(userId, clientRegistrationId, principalName);
		jdbcOperations.update(sql);
	}

	@Override
	public Class<? extends ClientAccessTokenEntity> getObjectType() {
		return ClientAccessTokenEntity.class;
	}

	@Override
	public UserStringStringCompositePK save(ClientAccessTokenEntity entity) {
		byte[] refreshTokenValue;
		if ( entity.getRefreshToken() != null ) {
			refreshTokenValue = encryptor.encrypt(entity.getRefreshToken());
			entity = entity.clone();
			entity.setRefreshToken(refreshTokenValue);
		}
		final var sql = new UpsertAuthorizedClient(entity);
		jdbcOperations.update(sql);
		return entity.getId();
	}

	@Override
	public ClientAccessTokenEntity get(UserStringStringCompositePK id) {
		final var sql = new SelectAuthorizedClient(id.getUserId(), id.getGroupId(), id.getEntityId());
		List<ClientAccessTokenEntity> result = this.jdbcOperations.query(sql,
				this.clientAccessTokenRowMapper);
		return !result.isEmpty() ? result.getFirst() : null;
	}

	@Override
	public List<ClientAccessTokenEntity> getAll(List<SortDescriptor> sortDescriptors) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(ClientAccessTokenEntity entity) {
		final var sql = new DeleteAuthorizedClient(entity.getUserId(), entity.getRegistrationId(),
				entity.getPrincipalName());
		jdbcOperations.update(sql);
	}

	private final class SelectAuthorizedClient implements PreparedStatementCreator, SqlProvider {

		private final Long userId;
		private final String clientRegistrationId;
		private final String principalName;

		public SelectAuthorizedClient(Long userId, String clientRegistrationId, String principalName) {
			super();
			this.userId = requireNonNullArgument(userId, "userId");
			this.clientRegistrationId = requireNonNullArgument(clientRegistrationId,
					"clientRegistrationId");
			this.principalName = requireNonNullArgument(principalName, "principalName");
		}

		@Override
		public String getSql() {
			return sqlSelect;
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			final PreparedStatement stmt = con.prepareStatement(getSql());
			stmt.setObject(1, userId);
			stmt.setString(2, clientRegistrationId);
			stmt.setString(3, principalName);
			return stmt;
		}

	}

	private UpsertAuthorizedClient upsertForClient(OAuth2AuthorizedClient authorizedClient,
			Authentication principal) {
		requireNonNullArgument(authorizedClient, "authorizedClient");
		requireNonNullArgument(principal, "principal");
		ClientRegistration clientRegistration = authorizedClient.getClientRegistration();
		OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
		OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
		byte[] refreshTokenValue = null;
		if ( refreshToken != null ) {
			refreshTokenValue = encryptor.encrypt(refreshToken.getTokenValue().getBytes(UTF_8));
		}
		var entity = new ClientAccessTokenEntity(
				userIdFromSystemIdentifier(clientRegistration.getRegistrationId()),
				clientRegistration.getRegistrationId(), principal.getName(), Instant.now());
		entity.setAccessTokenType(accessToken.getTokenType().getValue());
		entity.setAccessToken(accessToken.getTokenValue().getBytes(UTF_8));
		entity.setAccessTokenIssuedAt(accessToken.getIssuedAt());
		entity.setAccessTokenExpiresAt(accessToken.getExpiresAt());
		entity.setAccessTokenScopes(accessToken.getScopes());
		entity.setRefreshToken(refreshTokenValue);
		if ( refreshToken != null ) {
			entity.setRefreshTokenIssuedAt(refreshToken.getIssuedAt());
		}
		return new UpsertAuthorizedClient(entity);
	}

	private final class UpsertAuthorizedClient implements PreparedStatementCreator, SqlProvider {

		private final ClientAccessTokenEntity entity;

		private UpsertAuthorizedClient(ClientAccessTokenEntity entity) {
			super();
			this.entity = requireNonNullArgument(entity, "entity");
		}

		@Override
		public String getSql() {
			return sqlInsert;
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			final PreparedStatement stmt = con.prepareStatement(getSql());
			stmt.setObject(1, entity.getUserId());
			stmt.setString(2, entity.getRegistrationId());
			stmt.setString(3, entity.getPrincipalName());
			stmt.setString(4, entity.getAccessTokenType());
			stmt.setBytes(5, entity.getAccessToken());
			stmt.setTimestamp(6, Timestamp.from(entity.getAccessTokenIssuedAt()));
			stmt.setTimestamp(7, Timestamp.from(entity.getAccessTokenExpiresAt()));
			stmt.setString(8, commaDelimitedStringFromCollection(entity.getAccessTokenScopes()));
			Timestamp refreshTokenIssuedAtTs = null;
			if ( entity.getRefreshTokenIssuedAt() != null ) {
				refreshTokenIssuedAtTs = Timestamp.from(entity.getRefreshTokenIssuedAt());
			}
			stmt.setBytes(9, entity.getRefreshToken());
			stmt.setTimestamp(10, refreshTokenIssuedAtTs);
			stmt.setTimestamp(11, Timestamp.from(entity.getCreated()));
			return stmt;
		}

	}

	private final class DeleteAuthorizedClient implements PreparedStatementCreator, SqlProvider {

		private final Long userId;
		private final String clientRegistrationId;
		private final String principalName;

		private DeleteAuthorizedClient(Long userId, String clientRegistrationId, String principalName) {
			super();
			this.userId = requireNonNullArgument(userId, "userId");
			this.clientRegistrationId = requireNonNullArgument(clientRegistrationId,
					"clientRegistrationId");
			this.principalName = requireNonNullArgument(principalName, "principalName");
		}

		@Override
		public String getSql() {
			return sqlDelete;
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			final PreparedStatement stmt = con.prepareStatement(getSql());
			stmt.setObject(1, userId);
			stmt.setString(2, clientRegistrationId);
			stmt.setString(3, principalName);
			return stmt;
		}

	}

	/**
	 * The default {@link RowMapper} that maps the current row in
	 * {@code java.sql.ResultSet} to {@link OAuth2AuthorizedClient}.
	 */
	private final class OAuth2AuthorizedClientRowMapper implements RowMapper<OAuth2AuthorizedClient> {

		private final ClientRegistrationRepository clientRegistrationRepository;

		public OAuth2AuthorizedClientRowMapper(
				ClientRegistrationRepository clientRegistrationRepository) {
			requireNonNullArgument(clientRegistrationRepository, "clientRegistrationRepository");
			this.clientRegistrationRepository = clientRegistrationRepository;
		}

		@Override
		public OAuth2AuthorizedClient mapRow(ResultSet rs, int rowNum) throws SQLException {
			String clientRegistrationId = rs.getString(2);
			ClientRegistration clientRegistration = this.clientRegistrationRepository
					.findByRegistrationId(clientRegistrationId);
			if ( clientRegistration == null ) {
				throw new DataRetrievalFailureException("The ClientRegistration with id '"
						+ clientRegistrationId + "' exists in the data source, "
						+ "however, it was not found in the ClientRegistrationRepository.");
			}
			String principalName = rs.getString(3);
			String tokenTypeVal = rs.getString(4);
			OAuth2AccessToken.TokenType tokenType;
			if ( OAuth2AccessToken.TokenType.BEARER.getValue().equalsIgnoreCase(tokenTypeVal) ) {
				tokenType = OAuth2AccessToken.TokenType.BEARER;
			} else {
				throw new DataRetrievalFailureException("The ClientRegistration with id '"
						+ clientRegistrationId + "' token type unsupported: [" + tokenTypeVal + "].");
			}
			String tokenValue = new String(rs.getBytes(5), UTF_8);
			Instant issuedAt = getTimestampInstant(rs, 6);
			Instant expiresAt = getTimestampInstant(rs, 7);
			Set<String> scopes = StringUtils.commaDelimitedListToSet(rs.getString(8));
			OAuth2AccessToken accessToken = new OAuth2AccessToken(tokenType, tokenValue, issuedAt,
					expiresAt, scopes);
			OAuth2RefreshToken refreshToken = null;
			byte[] refreshTokenValue = rs.getBytes(9);
			if ( refreshTokenValue != null ) {
				tokenValue = new String(encryptor.decrypt(refreshTokenValue), UTF_8);
				issuedAt = getTimestampInstant(rs, 10);
				refreshToken = new OAuth2RefreshToken(tokenValue, issuedAt);
			}
			return new OAuth2AuthorizedClient(clientRegistration, principalName, accessToken,
					refreshToken);
		}

	}

	private final class ClientAccessTokenEntityRowMapper implements RowMapper<ClientAccessTokenEntity> {

		@Override
		public ClientAccessTokenEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
			Long userId = rs.getObject(1, Long.class);
			String clientRegistrationId = rs.getString(2);
			String principalName = rs.getString(3);
			Instant created = getTimestampInstant(rs, 11);
			var result = new ClientAccessTokenEntity(userId, clientRegistrationId, principalName,
					created);

			result.setAccessTokenType(rs.getString(4));
			result.setAccessToken(rs.getBytes(5));
			result.setAccessTokenIssuedAt(getTimestampInstant(rs, 6));
			result.setAccessTokenExpiresAt(getTimestampInstant(rs, 7));
			result.setAccessTokenScopes(StringUtils.commaDelimitedListToSet(rs.getString(8)));
			byte[] refreshTokenValue = rs.getBytes(9);
			if ( refreshTokenValue != null ) {
				result.setRefreshToken(encryptor.decrypt(refreshTokenValue));
				result.setRefreshTokenIssuedAt(getTimestampInstant(rs, 10));
			}

			return result;
		}

	}

}
