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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getTimestampInstant;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.nio.charset.StandardCharsets;
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
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.util.StringUtils;

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
 * @version 1.0
 */
public class JdbcOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

	/** The default SQL table name to use. */
	public static final String DEFAULT_TABLE_NAME = "solarnet.oauth2_authorized_client";

	private static final String LOAD_AUTHORIZED_CLIENT_SQL_TMPL = """
			SELECT client_registration_id
				, principal_name
				, access_token_type
				, access_token_value
				, access_token_issued_at
				, access_token_expires_at
				, access_token_scopes
				, refresh_token_value
				, refresh_token_issued_at
			FROM %s
			WHERE client_registration_id = ? AND principal_name = ?
			""";

	private static final String SAVE_AUTHORIZED_CLIENT_SQL_TMPL = """
			INSERT INTO %s (
				  client_registration_id
				, principal_name
				, access_token_type
				, access_token_value
				, access_token_issued_at
				, access_token_expires_at
				, access_token_scopes
				, refresh_token_value
				, refresh_token_issued_at
			)
			VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
			ON CONFLICT (client_registration_id , principal_name) DO UPDATE
			SET access_token_type = EXCLUDED.access_token_type
				, access_token_value = EXCLUDED.access_token_value
				, access_token_issued_at = EXCLUDED.access_token_issued_at
				, access_token_expires_at = EXCLUDED.access_token_expires_at
				, access_token_scopes = EXCLUDED.access_token_scopes
				, refresh_token_value = EXCLUDED.refresh_token_value
				, refresh_token_issued_at = EXCLUDED.refresh_token_issued_at
			""";

	private static final String REMOVE_AUTHORIZED_CLIENT_SQL_TMPL = """
			DELETE FROM %s WHERE client_registration_id = ? AND principal_name = ?
			""";

	private final JdbcOperations jdbcOperations;
	private final RowMapper<OAuth2AuthorizedClient> authorizedClientRowMapper;
	private final String sqlSelect;
	private final String sqlInsert;
	private final String sqlDelete;

	//protected final LobHandler lobHandler;

	/**
	 * Constructs a {@code JdbcOAuth2AuthorizedClientService} using the provided
	 * parameters.
	 * 
	 * @param jdbcOperations
	 *        the JDBC operations
	 * @param clientRegistrationRepository
	 *        the repository of client registrations
	 */
	public JdbcOAuth2AuthorizedClientService(JdbcOperations jdbcOperations,
			ClientRegistrationRepository clientRegistrationRepository) {
		this(jdbcOperations, clientRegistrationRepository, DEFAULT_TABLE_NAME);
	}

	/**
	 * Constructs a {@code JdbcOAuth2AuthorizedClientService} using the provided
	 * parameters.
	 * 
	 * @param jdbcOperations
	 *        the JDBC operations
	 * @param clientRegistrationRepository
	 *        the repository of client registrations
	 * @param tableName
	 *        the SQL table name to use
	 */
	public JdbcOAuth2AuthorizedClientService(JdbcOperations jdbcOperations,
			ClientRegistrationRepository clientRegistrationRepository, String tableName) {
		requireNonNullArgument(jdbcOperations, "jdbcOperations");
		requireNonNullArgument(clientRegistrationRepository, "clientRegistrationRepository");
		requireNonNullArgument(tableName, "tableName");
		this.jdbcOperations = jdbcOperations;

		OAuth2AuthorizedClientRowMapper authorizedClientRowMapper = new OAuth2AuthorizedClientRowMapper(
				clientRegistrationRepository);
		this.authorizedClientRowMapper = authorizedClientRowMapper;

		this.sqlSelect = LOAD_AUTHORIZED_CLIENT_SQL_TMPL.formatted(tableName);
		this.sqlInsert = SAVE_AUTHORIZED_CLIENT_SQL_TMPL.formatted(tableName);
		this.sqlDelete = REMOVE_AUTHORIZED_CLIENT_SQL_TMPL.formatted(tableName);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId,
			String principalName) {
		final var sql = new SelectAuthorizedClient(clientRegistrationId, principalName);
		List<OAuth2AuthorizedClient> result = this.jdbcOperations.query(sql,
				this.authorizedClientRowMapper);
		return !result.isEmpty() ? (T) result.get(0) : null;
	}

	@Override
	public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
		final var sql = new UpsertAuthorizedClient(authorizedClient, principal);
		jdbcOperations.update(sql);
	}

	@Override
	public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
		final var sql = new DeleteAuthorizedClient(clientRegistrationId, principalName);
		this.jdbcOperations.update(sql);
	}

	private final class SelectAuthorizedClient implements PreparedStatementCreator, SqlProvider {

		private final String clientRegistrationId;
		private final String principalName;

		public SelectAuthorizedClient(String clientRegistrationId, String principalName) {
			super();
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
			stmt.setString(1, clientRegistrationId);
			stmt.setString(2, principalName);
			return stmt;
		}

	}

	private final class UpsertAuthorizedClient implements PreparedStatementCreator, SqlProvider {

		private final OAuth2AuthorizedClient authorizedClient;
		private final Authentication principal;

		public UpsertAuthorizedClient(OAuth2AuthorizedClient authorizedClient,
				Authentication principal) {
			super();
			this.authorizedClient = requireNonNullArgument(authorizedClient, "authorizedClient");
			this.principal = requireNonNullArgument(principal, "principal");
		}

		@Override
		public String getSql() {
			return sqlInsert;
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			final PreparedStatement stmt = con.prepareStatement(getSql());
			final ClientRegistration clientRegistration = authorizedClient.getClientRegistration();
			final OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
			final OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
			int p = 0;
			stmt.setString(++p, clientRegistration.getRegistrationId());
			stmt.setString(++p, principal.getName());
			stmt.setString(++p, accessToken.getTokenType().getValue());
			stmt.setBytes(++p, accessToken.getTokenValue().getBytes(StandardCharsets.UTF_8));
			stmt.setTimestamp(++p, Timestamp.from(accessToken.getIssuedAt()));
			stmt.setTimestamp(++p, Timestamp.from(accessToken.getExpiresAt()));
			stmt.setString(++p, commaDelimitedStringFromCollection(accessToken.getScopes()));
			byte[] refreshTokenValue = null;
			Timestamp refreshTokenIssuedAt = null;
			if ( refreshToken != null ) {
				refreshTokenValue = refreshToken.getTokenValue().getBytes(StandardCharsets.UTF_8);
				if ( refreshToken.getIssuedAt() != null ) {
					refreshTokenIssuedAt = Timestamp.from(refreshToken.getIssuedAt());
				}
			}
			stmt.setBytes(++p, refreshTokenValue);
			stmt.setTimestamp(++p, refreshTokenIssuedAt);
			return stmt;
		}

	}

	private final class DeleteAuthorizedClient implements PreparedStatementCreator, SqlProvider {

		private final String clientRegistrationId;
		private final String principalName;

		public DeleteAuthorizedClient(String clientRegistrationId, String principalName) {
			super();
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
			stmt.setString(1, clientRegistrationId);
			stmt.setString(2, principalName);
			return stmt;
		}

	}

	/**
	 * The default {@link RowMapper} that maps the current row in
	 * {@code java.sql.ResultSet} to {@link OAuth2AuthorizedClient}.
	 */
	public static class OAuth2AuthorizedClientRowMapper implements RowMapper<OAuth2AuthorizedClient> {

		private final ClientRegistrationRepository clientRegistrationRepository;

		public OAuth2AuthorizedClientRowMapper(
				ClientRegistrationRepository clientRegistrationRepository) {
			requireNonNullArgument(clientRegistrationRepository, "clientRegistrationRepository");
			this.clientRegistrationRepository = clientRegistrationRepository;
		}

		@Override
		public OAuth2AuthorizedClient mapRow(ResultSet rs_, int rowNum) throws SQLException {
			String clientRegistrationId = rs_.getString(1);
			ClientRegistration clientRegistration = this.clientRegistrationRepository
					.findByRegistrationId(clientRegistrationId);
			if ( clientRegistration == null ) {
				throw new DataRetrievalFailureException("The ClientRegistration with id '"
						+ clientRegistrationId + "' exists in the data source, "
						+ "however, it was not found in the ClientRegistrationRepository.");
			}
			String principalName = rs_.getString(2);
			OAuth2AccessToken.TokenType tokenType = null;
			if ( OAuth2AccessToken.TokenType.BEARER.getValue().equalsIgnoreCase(rs_.getString(3)) ) {
				tokenType = OAuth2AccessToken.TokenType.BEARER;
			}
			String tokenValue = new String(rs_.getBytes(4), StandardCharsets.UTF_8);
			Instant issuedAt = getTimestampInstant(rs_, 5);
			Instant expiresAt = getTimestampInstant(rs_, 6);
			Set<String> scopes = StringUtils.commaDelimitedListToSet(rs_.getString(7));
			OAuth2AccessToken accessToken = new OAuth2AccessToken(tokenType, tokenValue, issuedAt,
					expiresAt, scopes);
			OAuth2RefreshToken refreshToken = null;
			byte[] refreshTokenValue = rs_.getBytes(8);
			if ( refreshTokenValue != null ) {
				tokenValue = new String(refreshTokenValue, StandardCharsets.UTF_8);
				issuedAt = getTimestampInstant(rs_, 9);
				refreshToken = new OAuth2RefreshToken(tokenValue, issuedAt);
			}
			return new OAuth2AuthorizedClient(clientRegistration, principalName, accessToken,
					refreshToken);
		}

	}

}
