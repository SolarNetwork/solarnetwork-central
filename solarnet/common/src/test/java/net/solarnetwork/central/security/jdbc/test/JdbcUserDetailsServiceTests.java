/* ==================================================================
 * JdbcUserDetailsServiceTests.java - 9/10/2016 4:18:18 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.security.jdbc.JdbcUserDetailsService;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.codec.BasicSecurityPolicyDeserializer;
import net.solarnetwork.codec.ObjectMapperFactoryBean;
import net.solarnetwork.codec.SecurityPolicySerializer;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.SecurityPolicy;

/**
 * Test cases for the {@link JdbcUserDetailsService} class.
 * 
 * @author matt
 * @version 2.1
 */
public class JdbcUserDetailsServiceTests extends AbstractJUnit5JdbcDaoTestSupport {

	private static final String TEST_PASSWORD = "password";
	private static final String TEST_ROLE = "ROLE_USER";
	private static final String TEST_TOKEN = "01234567890123456789";
	private static final String TEST_TOKEN_PASSWORD = "token-password";

	private PasswordEncoder passwordEncoder;
	private JdbcUserDetailsService service;
	private ObjectMapper objectMapper;

	@BeforeEach
	public void setup() throws Exception {
		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		factory.setSerializers(List.of(new SecurityPolicySerializer()));
		factory.setDeserializers(List.of(new BasicSecurityPolicyDeserializer()));
		objectMapper = factory.getObject();

		passwordEncoder = new BCryptPasswordEncoder(12, new java.security.SecureRandom());

		service = new JdbcUserDetailsService();
		service.setDataSource(jdbcTemplate.getDataSource());
	}

	@Test
	public void noMatchingUser() {
		thenExceptionOfType(UsernameNotFoundException.class)
				.isThrownBy(() -> service.loadUserByUsername("foobar"));
	}

	@Override
	protected String setupTestUser(Long id) {
		String username = setupTestUser(id, passwordEncoder.encode(TEST_PASSWORD));
		jdbcTemplate.update("INSERT INTO solaruser.user_role (user_id,role_name) VALUES (?,?)", id,
				TEST_ROLE);
		return username;
	}

	@Test
	public void matchingUser() {
		final Long userId = 123L;
		final String username = setupTestUser(userId);
		UserDetails details = service.loadUserByUsername(username);

		Set<GrantedAuthority> auths = new HashSet<GrantedAuthority>();
		auths.add(new SimpleGrantedAuthority(TEST_ROLE));

		then(details).isNotNull().returns(username, from(UserDetails::getUsername)).returns(auths,
				from(UserDetails::getAuthorities));
	}

	private String setupTestToken(Long userId, BasicSecurityPolicy policy) {
		service.setUsersByUsernameQuery(JdbcUserDetailsService.DEFAULT_TOKEN_USERS_BY_USERNAME_SQL);
		service.setAuthoritiesByUsernameQuery(
				JdbcUserDetailsService.DEFAULT_TOKEN_AUTHORITIES_BY_USERNAME_SQL);
		String json;
		try {
			json = objectMapper.writeValueAsString(policy);
		} catch ( JsonProcessingException e ) {
			throw new RuntimeException(e);
		}
		jdbcTemplate.update(
				"INSERT INTO solaruser.user_auth_token (auth_token,user_id,auth_secret,status,token_type,jpolicy) VALUES (?,?,?,?::solaruser.user_auth_token_status,?::solaruser.user_auth_token_type,?::json)",
				TEST_TOKEN, userId, TEST_TOKEN_PASSWORD, "Active", "User", json);
		return TEST_TOKEN;
	}

	@Test
	public void matchingToken() {
		final Long userId = 123L;
		setupTestUser(userId);
		final BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(Collections.singleton("Main")).build();
		final String token = setupTestToken(userId, policy);

		UserDetails details = service.loadUserByUsername(token);
		// @formatter:off
		then(details).isNotNull()
			.returns(token, from(UserDetails::getUsername))
			.isInstanceOfSatisfying(SecurityToken.class,t -> {
				then(t)
					.returns(token, from(SecurityToken::getToken))
					.returns(SecurityTokenType.User, from(SecurityToken::getTokenType))
					.returns(userId, from(SecurityToken::getUserId))
					.extracting(SecurityToken::getPolicy)
					.returns(policy.getSourceIds(), from(SecurityPolicy::getSourceIds))
					;
			});
		// @formatter:on
	}

	@Test
	public void matchingToken_withExpiry() {
		// GIVEN
		final Long userId = 123L;
		setupTestUser(userId);
		final BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(Collections.singleton("Main"))
				.withNotAfter(Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS))
				.build();
		final String token = setupTestToken(userId, policy);

		// WHEN
		UserDetails details = service.loadUserByUsername(token);

		// THEN
		// @formatter:off
		then(details)
			.as("Details loaded")
			.isNotNull()
			.as("Token ID returned as username")
			.returns(token, from(UserDetails::getUsername))
			.as("Type is SecurityToken")
			.asInstanceOf(type(SecurityToken.class))
			.as("Token returned as token")
			.returns(token, from(SecurityToken::getToken))
			.as("Token type is user")
			.returns(SecurityTokenType.User, from(SecurityToken::getTokenType))
			.as("User ID returned")
			.returns(userId, from(SecurityToken::getUserId))
			.satisfies(secToken -> {
				then(secToken.getPolicy())
					.as("Policy returned")
					.isNotNull()
					.as("Policy source IDs returned")
					.returns(policy.getSourceIds(), from(SecurityPolicy::getSourceIds))
					.as("Policy expiration date returned")
					.returns(policy.getNotAfter(), from(SecurityPolicy::getNotAfter))
					;
			})
			;
		// @formatter:on
	}

}
