/* ==================================================================
 * JdbcUserDetailsService.java - Feb 2, 2010 3:52:04 PM
 *
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.codec.JsonUtils;

/**
 * Extension of {@link JdbcDaoImpl} that returns {@link AuthenticatedUser}
 * objects.
 *
 * <p>
 * The SQL required by this implementation adds the following additional column
 * requirements to the base {@link JdbcDaoImpl} requirements:
 * </p>
 *
 * <ol>
 * <li>User ID (Long) - the User primary key</li>
 * <li>Display Name (String) - the User's display name</li>
 * <li>Token (Boolean) - <em>true</em> if this represents a token</li>
 * <li>Token type (String) - if Token is <em>true</em>, this should be the token
 * type</li>
 * <li>Token IDs (String) - if Token is <em>true</em>, this is an optional set
 * of IDs</li>
 * </ol>
 *
 * @author matt
 * @version 2.1
 */
public class JdbcUserDetailsService extends JdbcDaoImpl implements UserDetailsService {

	public static final String DEFAULT_USERS_BY_USERNAME_SQL = "SELECT username, password, enabled, user_id, display_name, FALSE AS is_token"
			+ " FROM solaruser.user_login WHERE username = ?";

	public static final String DEFAULT_AUTHORITIES_BY_USERNAME_SQL = "SELECT username, authority FROM solaruser.user_login_role WHERE username = ?";

	public static final String DEFAULT_TOKEN_USERS_BY_USERNAME_SQL = "SELECT username, password, enabled, user_id, display_name, TRUE AS is_token, token_type, jpolicy"
			+ " FROM solaruser.user_auth_token_login WHERE username = ?";

	public static final String DEFAULT_TOKEN_AUTHORITIES_BY_USERNAME_SQL = "SELECT username, authority FROM solaruser.user_auth_token_role WHERE username = ?";

	private List<GrantedAuthority> staticAuthorities;
	private final ObjectMapper objectMapper;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 */
	public JdbcUserDetailsService() {
		this(JsonUtils.newObjectMapper());
	}

	/**
	 * Constructor.
	 *
	 * @param objectMapper
	 *        the mapper to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcUserDetailsService(ObjectMapper objectMapper) {
		super();
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		setUsersByUsernameQuery(DEFAULT_USERS_BY_USERNAME_SQL);
		setAuthoritiesByUsernameQuery(DEFAULT_AUTHORITIES_BY_USERNAME_SQL);
	}

	@Override
	protected UserDetails createUserDetails(String username, UserDetails userFromUserQuery,
			List<GrantedAuthority> combinedAuthorities) {
		User user = (User) super.createUserDetails(username, userFromUserQuery, combinedAuthorities);
		if ( userFromUserQuery instanceof AuthenticatedToken token ) {
			return new AuthenticatedToken(user, token.getTokenType(), token.getUserId(),
					token.getPolicy());
		}
		AuthenticatedUser authUser = (AuthenticatedUser) userFromUserQuery;
		return new AuthenticatedUser(user, authUser.getUserId(), authUser.getName(),
				authUser.isAuthenticatedWithToken());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	protected List<UserDetails> loadUsersByUsername(String username) {
		assert getJdbcTemplate() != null;
		return getJdbcTemplate().query(getUsersByUsernameQuery(), (rs, rowNum) -> {
			String username1 = rs.getString(1);
			String password = rs.getString(2);
			boolean enabled = rs.getBoolean(3);
			Long id = rs.getLong(4);
			String name = rs.getString(5);
			boolean authWithToken = rs.getBoolean(6);
			if ( authWithToken ) {
				String tokenTypeString = rs.getString(7);
				SecurityTokenType tokenType = null;
				if ( tokenTypeString != null ) {
					try {
						tokenType = SecurityTokenType.valueOf(tokenTypeString);
					} catch ( IllegalArgumentException e ) {
						log.warn("Unknown token type will be ignored: {}", tokenTypeString);
					}
				}
				String policyJson = rs.getString(8);
				SecurityPolicy policy = null;
				if ( policyJson != null && !"{}".equals(policyJson) ) {
					try {
						policy = objectMapper.readValue(policyJson, BasicSecurityPolicy.class);
					} catch ( IOException e ) {
						log.error("Error deserializing [{}] SecurityPolicy from [{}]: {}", username1,
								policyJson, e.getMessage());
					}
				}
				return new AuthenticatedToken(new User(username1, password, enabled, true, true, true,
						AuthorityUtils.NO_AUTHORITIES), tokenType, id, policy);
			}
			return new AuthenticatedUser(new User(username1, password, enabled, true, true, true,
					AuthorityUtils.NO_AUTHORITIES), id, name, false);
		}, username);
	}

	@Override
	protected void addCustomAuthorities(String username, List<GrantedAuthority> authorities) {
		if ( staticAuthorities != null ) {
			authorities.addAll(staticAuthorities);
		}
	}

	/**
	 * Set a list of statically assigned authorities based on a list of role
	 * names.
	 *
	 * <p>
	 * All users will be automatically granted these authorities.
	 * </p>
	 *
	 * @param roles
	 *        the role names to grant
	 */
	public void setStaticRoles(List<String> roles) {
		List<GrantedAuthority> auths = new ArrayList<>(roles == null ? 0 : roles.size());
		if ( roles != null ) {
			for ( String role : roles ) {
				auths.add(new SimpleGrantedAuthority(role));
			}
		}
		staticAuthorities = auths;
	}

}
