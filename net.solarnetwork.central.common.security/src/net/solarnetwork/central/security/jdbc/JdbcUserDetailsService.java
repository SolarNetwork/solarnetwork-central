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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.security.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.central.security.AuthenticatedUser;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
 * </ol>
 * 
 * @author matt
 * @version $Id$
 */
public class JdbcUserDetailsService extends JdbcDaoImpl implements UserDetailsService {

	private List<GrantedAuthority> staticAuthorities;

	@Override
	protected UserDetails createUserDetails(String username, UserDetails userFromUserQuery,
			List<GrantedAuthority> combinedAuthorities) {
		User user = (User) super.createUserDetails(username, userFromUserQuery, combinedAuthorities);
		AuthenticatedUser authUser = (AuthenticatedUser) userFromUserQuery;
		return new AuthenticatedUser(user, authUser.getUserId(), authUser.getName());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	protected List<UserDetails> loadUsersByUsername(String username) {
		return getJdbcTemplate().query(getUsersByUsernameQuery(), new String[] { username },
				new RowMapper<UserDetails>() {

					@Override
					public UserDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
						String username = rs.getString(1);
						String password = rs.getString(2);
						boolean enabled = rs.getBoolean(3);
						Long id = rs.getLong(4);
						String name = rs.getString(5);
						return new AuthenticatedUser(new User(username, password, enabled, true, true,
								true, AuthorityUtils.NO_AUTHORITIES), id, name);
					}
				});
	}

	@Override
	protected void addCustomAuthorities(String username, List<GrantedAuthority> authorities) {
		if ( staticAuthorities != null ) {
			authorities.addAll(staticAuthorities);
		}
	}

	/**
	 * Set a list of statically assigned authorites based on a list of role
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
		List<GrantedAuthority> auths = new ArrayList<GrantedAuthority>(roles == null ? 0 : roles.size());
		for ( String role : roles ) {
			auths.add(new SimpleGrantedAuthority(role));
		}
		staticAuthorities = auths;
	}

}
