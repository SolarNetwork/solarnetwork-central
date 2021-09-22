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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.common.dao.jdbc.test.AbstractJdbcDaoTestSupport;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicySerializer;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.jdbc.JdbcUserDetailsService;
import net.solarnetwork.codec.ObjectMapperFactoryBean;

/**
 * Test cases for the {@link JdbcUserDetailsService} class.
 * 
 * @author matt
 * @version 2.0
 */
public class JdbcUserDetailsServiceTests extends AbstractJdbcDaoTestSupport {

	private static final String TEST_PASSWORD = "password";
	private static final String TEST_ROLE = "ROLE_USER";
	private static final String TEST_TOKEN = "01234567890123456789";
	private static final String TEST_TOKEN_PASSWORD = "token-password";

	private PasswordEncoder passwordEncoder;
	private JdbcUserDetailsService service;
	private ObjectMapper objectMapper;

	@Before
	public void setup() throws Exception {
		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		List<JsonSerializer<?>> list = new ArrayList<JsonSerializer<?>>(1);
		list.add(new SecurityPolicySerializer());
		factory.setSerializers(list);
		objectMapper = factory.getObject();

		passwordEncoder = new BCryptPasswordEncoder(12, new java.security.SecureRandom());

		service = new JdbcUserDetailsService();
		service.setDataSource(jdbcTemplate.getDataSource());
	}

	@Test(expected = UsernameNotFoundException.class)
	public void noMatchingUser() {
		service.loadUserByUsername("foobar");
	}

	private String setupTestUser(Long id) {
		String username = id + "@localhost";
		jdbcTemplate.update(
				"INSERT INTO solaruser.user_user (id,disp_name,email,password) VALUES (?,?,?,?)", id,
				"Test User " + id, username, passwordEncoder.encode(TEST_PASSWORD));
		jdbcTemplate.update("INSERT INTO solaruser.user_role (user_id,role_name) VALUES (?,?)", id,
				TEST_ROLE);
		return username;
	}

	@Test
	public void matchingUser() {
		final Long userId = 123L;
		final String username = setupTestUser(userId);
		UserDetails details = service.loadUserByUsername(username);
		Assert.assertNotNull(details);
		Assert.assertEquals("Username", username, details.getUsername());

		Set<GrantedAuthority> auths = new HashSet<GrantedAuthority>();
		auths.add(new SimpleGrantedAuthority(TEST_ROLE));

		Assert.assertEquals("Roles", auths, details.getAuthorities());
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
		Assert.assertNotNull(details);
		Assert.assertEquals("Username", token, details.getUsername());
		Assert.assertTrue("Is AuthenticatedToken", details instanceof SecurityToken);
		SecurityToken authToken = (SecurityToken) details;
		Assert.assertEquals("Token", token, authToken.getToken());
		Assert.assertEquals("Token type", "User", authToken.getTokenType());
		Assert.assertEquals("Token user ID", userId, authToken.getUserId());
		Assert.assertNotNull("Token policy", authToken.getPolicy());
		Assert.assertEquals("Token source IDs", policy.getSourceIds(),
				authToken.getPolicy().getSourceIds());
	}

}
