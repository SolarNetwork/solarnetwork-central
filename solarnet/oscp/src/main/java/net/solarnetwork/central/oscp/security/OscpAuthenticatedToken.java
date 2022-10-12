/* ==================================================================
 * AuthenticatedToken.java - 17/08/2022 10:28:50 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.security;

import static java.lang.String.format;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import net.solarnetwork.central.oscp.domain.AuthRoleContainer;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;

/**
 * An OSCP authenticated token.
 * 
 * @author matt
 * @version 1.0
 */
public class OscpAuthenticatedToken implements UserDetails, AuthRoleContainer {

	private static final long serialVersionUID = -2822991208596742973L;

	private final AuthRoleInfo info;
	private final Collection<? extends GrantedAuthority> authorities;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * A single {@literal ROLE_X} granted authority will be added based on the
	 * given {@code role}.
	 * </p>
	 * 
	 * @param info
	 *        the auth role info
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public OscpAuthenticatedToken(AuthRoleInfo info) {
		this(info, createAuthorityList(format("ROLE_%s", info.role().toString().toUpperCase())));
	}

	/**
	 * Constructor.
	 * 
	 * @param role
	 *        the role
	 * @param authId
	 *        the authentication ID
	 * @param authorities
	 *        the granted authorities
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public OscpAuthenticatedToken(AuthRoleInfo info,
			Collection<? extends GrantedAuthority> authorities) {
		super();
		this.info = requireNonNullArgument(info, "info");
		this.authorities = requireNonNullArgument(authorities, "authorities");
	}

	@Override
	public AuthRoleInfo getAuthRole() {
		return info;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return "N/A";
	}

	@Override
	public String getUsername() {
		return null;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
