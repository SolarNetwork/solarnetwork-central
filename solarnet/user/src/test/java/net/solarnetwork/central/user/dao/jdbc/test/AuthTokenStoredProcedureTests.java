/* ==================================================================
 * AuthTokenStoredProcedureTests.java - 26/06/2018 11:19:32 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao.jdbc.test;

import static java.util.Arrays.asList;
import static net.solarnetwork.security.AuthorizationUtils.AUTHORIZATION_TIMESTAMP_FORMATTER;
import static net.solarnetwork.security.AuthorizationUtils.computeHmacSha256;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.test.AbstractJdbcDaoTestSupport;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Test cases for authentication related database stored procedures.
 * 
 * @author matt
 * @version 2.0
 */
public class AuthTokenStoredProcedureTests extends AbstractJdbcDaoTestSupport {

	private static final String SQL_SNWS2_CANON_REQ_DATA = "{? = call solaruser.snws2_canon_request_data(?, ?, ?)}";
	private static final String SQL_SNWS2_SIGNATURE_DATA = "{? = call solaruser.snws2_signature_data(?, ?)}";
	private static final String SQL_SNWS2_SIGN_KEY = "{? = call solaruser.snws2_signing_key(?, ?)}";
	private static final String SQL_SNWS2_SIGNATURE = "{? = call solaruser.snws2_signature(?, ?)}";
	private static final String SQL_SNWS2_VALID_REQ_DATE = "{? = call solaruser.snws2_validated_request_date(?)}";
	private static final String SQL_SNWS2_FIND_VERIFIED_TOKEN = "{call solaruser.snws2_find_verified_token_details(?,?,?,?,?)}";

	@Test
	public void snwsCanonicalRequestDataSimple() {
		// GIVEN
		final Instant reqDate = LocalDateTime.of(2018, 6, 25, 13, 50, 25).atZone(ZoneOffset.UTC)
				.toInstant();

		// WHEN
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_CANON_REQ_DATA);
				stmt.registerOutParameter(1, Types.VARCHAR);
				stmt.setTimestamp(2, Timestamp.from(reqDate));
				stmt.setString(3, "localhost");
				stmt.setString(4, "/path");
				return stmt;
			}
		}, asList((SqlParameter) new SqlOutParameter("data", Types.VARCHAR)));

		// THEN
		assertThat("Canonical request data", result,
				hasEntry("data", (Object) new Snws2AuthorizationBuilder("test").host("localhost")
						.path("/path").useSnDate(true).date(reqDate).computeCanonicalRequestMessage()));
	}

	@Test
	public void snwsCanonicalRequestDataSimpleHostWithPort() {
		// GIVEN
		final Instant reqDate = LocalDateTime.of(2018, 6, 25, 13, 50, 25).atZone(ZoneOffset.UTC)
				.toInstant();

		// WHEN
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_CANON_REQ_DATA);
				stmt.registerOutParameter(1, Types.VARCHAR);
				stmt.setTimestamp(2, Timestamp.from(reqDate));
				stmt.setString(3, "data.solarnetwork.net:443");
				stmt.setString(4, "/path");
				return stmt;
			}
		}, asList((SqlParameter) new SqlOutParameter("data", Types.VARCHAR)));

		// THEN
		assertThat("Canonical request data", result,
				hasEntry("data",
						(Object) new Snws2AuthorizationBuilder("test").host("data.solarnetwork.net:443")
								.path("/path").useSnDate(true).date(reqDate)
								.computeCanonicalRequestMessage()));
	}

	private static String signatureData(Instant date, String canonicalRequestData) {
		return "SNWS2-HMAC-SHA256\n" + AUTHORIZATION_TIMESTAMP_FORMATTER.format(date) + "\n"
				+ Hex.encodeHexString(DigestUtils.sha256(canonicalRequestData));

	}

	@Test
	public void snwsSigningData() {
		// GIVEN
		final Instant reqDate = LocalDateTime.of(2018, 6, 25, 14, 30, 15).atZone(ZoneOffset.UTC)
				.toInstant();

		// when
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_SIGNATURE_DATA);
				stmt.registerOutParameter(1, Types.VARCHAR);
				stmt.setTimestamp(2, Timestamp.from(reqDate));
				stmt.setString(3, "foobar");
				return stmt;
			}
		}, asList((SqlParameter) new SqlOutParameter("data", Types.VARCHAR)));

		// THEN
		assertThat("Signature data", result,
				hasEntry("data", (Object) signatureData(reqDate, "foobar")));
	}

	@Test
	public void snwsSigningKey() {
		// GIVEN
		final Instant reqDate = LocalDateTime.of(2018, 6, 25, 14, 30, 15).atZone(ZoneOffset.UTC)
				.toInstant();

		// WHEN
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_SIGN_KEY);
				stmt.registerOutParameter(1, Types.BINARY);
				LocalDate date = reqDate.atOffset(ZoneOffset.UTC).toLocalDate();
				stmt.setDate(2, java.sql.Date.valueOf(date));
				stmt.setString(3, "foobar");
				return stmt;
			}
		}, asList((SqlParameter) new SqlOutParameter("data", Types.BINARY)));

		// THEN
		assertThat("Signature data", result, hasKey("data"));
		byte[] key = (byte[]) result.get("data");
		Snws2AuthorizationBuilder b = new Snws2AuthorizationBuilder("token").date(reqDate);
		byte[] expected = b.computeSigningKey(reqDate, "foobar");
		assertThat("Sign key", Arrays.equals(key, expected), is(true));
	}

	@Test
	public void snwsSignature() {
		// GIVEN
		final byte[] key = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };

		// WHEN
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_SIGNATURE);
				stmt.registerOutParameter(1, Types.VARCHAR);
				stmt.setString(2, "foobar");
				stmt.setBytes(3, key);
				return stmt;
			}
		}, asList((SqlParameter) new SqlOutParameter("data", Types.VARCHAR)));

		// then
		assertThat("Signature", result,
				hasEntry("data", (Object) encodeHexString(computeHmacSha256(key, "foobar"))));
	}

	@Test
	public void snwsValidateRequestDateSuccess() {
		// given
		final Date reqDate = new Date();

		// when
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_VALID_REQ_DATE);
				stmt.registerOutParameter(1, Types.BOOLEAN);
				stmt.setTimestamp(2, new Timestamp(reqDate.getTime()));
				return stmt;
			}
		}, asList((SqlParameter) new SqlOutParameter("data", Types.BOOLEAN)));

		// then
		assertThat("Success", result, hasEntry("data", (Object) Boolean.TRUE));
	}

	@Test
	public void snwsValidateRequestDateSuccessSlightlyOld() {
		// given
		final Date reqDate = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1));

		// when
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_VALID_REQ_DATE);
				stmt.registerOutParameter(1, Types.BOOLEAN);
				stmt.setTimestamp(2, new Timestamp(reqDate.getTime()));
				return stmt;
			}
		}, asList((SqlParameter) new SqlOutParameter("data", Types.BOOLEAN)));

		// then
		assertThat("Success", result, hasEntry("data", (Object) Boolean.TRUE));
	}

	@Test
	public void snwsValidateRequestDateSuccessSlightlyFuturistic() {
		// given
		final Date reqDate = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));

		// when
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_VALID_REQ_DATE);
				stmt.registerOutParameter(1, Types.BOOLEAN);
				stmt.setTimestamp(2, new Timestamp(reqDate.getTime()));
				return stmt;
			}
		}, asList((SqlParameter) new SqlOutParameter("data", Types.BOOLEAN)));

		// then
		assertThat("Success", result, hasEntry("data", (Object) Boolean.TRUE));
	}

	@Test
	public void snwsValidateRequestDateTooOld() {
		// given
		final Date reqDate = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10));

		// when
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_VALID_REQ_DATE);
				stmt.registerOutParameter(1, Types.BOOLEAN);
				stmt.setTimestamp(2, new Timestamp(reqDate.getTime()));
				return stmt;
			}
		}, asList((SqlParameter) new SqlOutParameter("data", Types.BOOLEAN)));

		// then
		assertThat("Success", result, hasEntry("data", (Object) Boolean.FALSE));
	}

	@Test
	public void snwsValidateRequestDateTooFuturistic() {
		// given
		final Date reqDate = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));

		// when
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_VALID_REQ_DATE);
				stmt.registerOutParameter(1, Types.BOOLEAN);
				stmt.setTimestamp(2, new Timestamp(reqDate.getTime()));
				return stmt;
			}
		}, asList((SqlParameter) new SqlOutParameter("data", Types.BOOLEAN)));

		// then
		assertThat("Success", result, hasEntry("data", (Object) Boolean.FALSE));
	}

	private void createUser(Long userId, String email) {
		jdbcTemplate.update(
				"INSERT INTO solaruser.user_user(id,email,disp_name,password) VALUES (?,?,?,?)", userId,
				email, "Test User", "test.password");
	}

	private void createToken(String tokenId, String tokenSecret, Long userId, SecurityTokenStatus status,
			SecurityTokenType type, String policy) {
		jdbcTemplate.update(
				"INSERT INTO solaruser.user_auth_token(auth_token,auth_secret,user_id,status,token_type,jpolicy)"
						+ " VALUES (?,?,?,?::solaruser.user_auth_token_status,?::solaruser.user_auth_token_type,?::json)",
				tokenId, tokenSecret, userId, status.name(), type.name(), policy);
	}

	@Test
	public void snwsFindVerifiedTokenDetailsNoToken() {
		// GIVEN
		final Instant reqDate = LocalDateTime.of(2017, 4, 25, 14, 30).atZone(ZoneOffset.UTC).toInstant();
		final String tokenId = "123456789abcdefghijk";

		// WHEN
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_FIND_VERIFIED_TOKEN);
				stmt.setString(1, tokenId);
				stmt.setTimestamp(2, Timestamp.from(reqDate));
				stmt.setString(3, "localhost");
				stmt.setString(4, "/foobar");
				stmt.setString(5, "f366ddc9e6299794928cc956e9fa409333078df6e6b9d94d4c1e64dbecf499db");
				return stmt;
			}
		}, asList((SqlParameter) new SqlReturnResultSet("data", new ColumnMapRowMapper())));

		// THEN
		assertThat("Result available", result, hasKey("data"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
		assertThat("Result length", data, hasSize(0));
	}

	@Test
	public void snwsFindVerifiedTokenDetails() {
		// GIVEN
		final Instant reqDate = LocalDateTime.of(2017, 4, 25, 14, 30).atZone(ZoneOffset.UTC).toInstant();
		final String tokenId = "123456789abcdefghijk";
		final String tokenSecret = "password";
		final Long userId = -1L;
		createUser(userId, "test@localhost");
		createToken(tokenId, tokenSecret, userId, SecurityTokenStatus.Active,
				SecurityTokenType.ReadNodeData, null);

		// when
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_FIND_VERIFIED_TOKEN);
				stmt.setString(1, tokenId);
				stmt.setTimestamp(2, Timestamp.from(reqDate));
				stmt.setString(3, "localhost");
				stmt.setString(4, "/foobar");
				stmt.setString(5, "f366ddc9e6299794928cc956e9fa409333078df6e6b9d94d4c1e64dbecf499db");
				return stmt;
			}
		}, asList((SqlParameter) new SqlReturnResultSet("data", new ColumnMapRowMapper())));

		// then
		assertThat("Result available", result, hasKey("data"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
		assertThat("Result length", data, hasSize(1));
		assertThat("Result user ID", data.get(0), hasEntry("user_id", (Object) userId));
		assertThat("Result token type", data.get(0),
				hasEntry(Matchers.equalTo("token_type"), new CustomMatcher<Object>("Enum value") {

					@Override
					public boolean matches(Object val) {
						return (val != null
								&& val.toString().equals(SecurityTokenType.ReadNodeData.name()));
					}

				}));
	}

	@Test
	public void snwsFindVerifiedTokenDetailsDisabled() {
		// GIVEN
		final Instant reqDate = LocalDateTime.of(2017, 4, 25, 14, 30).atZone(ZoneOffset.UTC).toInstant();
		final String tokenId = "123456789abcdefghijk";
		final String tokenSecret = "password";
		final Long userId = -1L;
		createUser(userId, "test@localhost");
		createToken(tokenId, tokenSecret, userId, SecurityTokenStatus.Disabled,
				SecurityTokenType.ReadNodeData, null);

		// WHEN
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_FIND_VERIFIED_TOKEN);
				stmt.setString(1, tokenId);
				stmt.setTimestamp(2, Timestamp.from(reqDate));
				stmt.setString(3, "localhost");
				stmt.setString(4, "/foobar");
				stmt.setString(5, "f366ddc9e6299794928cc956e9fa409333078df6e6b9d94d4c1e64dbecf499db");
				return stmt;
			}
		}, asList((SqlParameter) new SqlReturnResultSet("data", new ColumnMapRowMapper())));

		// THEN
		assertThat("Result available", result, hasKey("data"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
		assertThat("Result length", data, hasSize(0));
	}

	@Test
	public void snwsFindVerifiedTokenDetailsSignatureMismatch() {
		// GIVEN
		final Instant reqDate = LocalDateTime.of(2017, 4, 25, 14, 30).atZone(ZoneOffset.UTC).toInstant();
		final String tokenId = "123456789abcdefghijk";
		final String tokenSecret = "password";
		final Long userId = -1L;
		createUser(userId, "test@localhost");
		createToken(tokenId, tokenSecret, userId, SecurityTokenStatus.Active,
				SecurityTokenType.ReadNodeData, null);

		// when
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_FIND_VERIFIED_TOKEN);
				stmt.setString(1, tokenId);
				stmt.setTimestamp(2, Timestamp.from(reqDate));
				stmt.setString(3, "localhost");
				stmt.setString(4, "/foobar");
				stmt.setString(5, "f3");
				return stmt;
			}
		}, asList((SqlParameter) new SqlReturnResultSet("data", new ColumnMapRowMapper())));

		// then
		assertThat("Result available", result, hasKey("data"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
		assertThat("Result length", data, hasSize(0));
	}

	@Test
	public void snwsFindVerifiedTokenDetailsPolicyExpired() {
		// GIVEN
		final Instant reqDate = LocalDateTime.of(2017, 4, 25, 14, 30).atZone(ZoneOffset.UTC).toInstant();
		final String tokenId = "123456789abcdefghijk";
		final String tokenSecret = "password";
		final Long userId = -1L;
		final BasicSecurityPolicy policy = new BasicSecurityPolicy(null, null, null, null, null, null,
				null, null, reqDate.minus(1, ChronoUnit.MINUTES), true);
		createUser(userId, "test@localhost");
		createToken(tokenId, tokenSecret, userId, SecurityTokenStatus.Active,
				SecurityTokenType.ReadNodeData, JsonUtils.getJSONString(policy, null));

		// WHEN
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_FIND_VERIFIED_TOKEN);
				stmt.setString(1, tokenId);
				stmt.setTimestamp(2, Timestamp.from(reqDate));
				stmt.setString(3, "localhost");
				stmt.setString(4, "/foobar");
				stmt.setString(5, "f366ddc9e6299794928cc956e9fa409333078df6e6b9d94d4c1e64dbecf499db");
				return stmt;
			}
		}, asList((SqlParameter) new SqlReturnResultSet("data", new ColumnMapRowMapper())));

		// THEN
		assertThat("Result available", result, hasKey("data"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
		assertThat("Result length", data, hasSize(0));
	}

	@Test
	public void snwsFindVerifiedTokenPolicyNotExpired() {
		// GIVEN
		final Instant reqDate = LocalDateTime.of(2017, 4, 25, 14, 30).atZone(ZoneOffset.UTC).toInstant();
		final String tokenId = "123456789abcdefghijk";
		final String tokenSecret = "password";
		final Long userId = -1L;
		final BasicSecurityPolicy policy = new BasicSecurityPolicy(null, null, null, null, null, null,
				null, null, reqDate.plus(10, ChronoUnit.MINUTES), true);
		createUser(userId, "test@localhost");
		createToken(tokenId, tokenSecret, userId, SecurityTokenStatus.Active,
				SecurityTokenType.ReadNodeData, JsonUtils.getJSONString(policy, null));

		// when
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_SNWS2_FIND_VERIFIED_TOKEN);
				stmt.setString(1, tokenId);
				stmt.setTimestamp(2, Timestamp.from(reqDate));
				stmt.setString(3, "localhost");
				stmt.setString(4, "/foobar");
				stmt.setString(5, "f366ddc9e6299794928cc956e9fa409333078df6e6b9d94d4c1e64dbecf499db");
				return stmt;
			}
		}, asList((SqlParameter) new SqlReturnResultSet("data", new ColumnMapRowMapper())));

		// then
		assertThat("Result available", result, hasKey("data"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
		assertThat("Result length", data, hasSize(1));
		assertThat("Result user ID", data.get(0), hasEntry("user_id", (Object) userId));
		assertThat("Result token type", data.get(0),
				hasEntry(Matchers.equalTo("token_type"), new CustomMatcher<Object>("Enum value") {

					@Override
					public boolean matches(Object val) {
						return (val != null
								&& val.toString().equals(SecurityTokenType.ReadNodeData.name()));
					}

				}));
	}

}
