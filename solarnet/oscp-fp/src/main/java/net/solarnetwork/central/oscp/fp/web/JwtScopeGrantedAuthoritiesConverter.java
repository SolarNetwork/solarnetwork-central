/* ==================================================================
 * JwtScopeGrantedAuthoritiesConverter.java - 26/08/2022 10:20:43 am
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

package net.solarnetwork.central.oscp.fp.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import net.solarnetwork.central.oscp.security.Role;

/**
 * Convert JWT scopes to Flexibility Provider authorities.
 * 
 * @author matt
 * @version 1.0
 */
public class JwtScopeGrantedAuthoritiesConverter
		implements Converter<Jwt, Collection<GrantedAuthority>> {

	/** The Capacity Provider scope value. */
	public static final String SCOPE_CAPACITYPROVIDER = "oscp-fp-cp/cp";

	/** The Capacity Optimizer scope value. */
	public static final String SCOPE_CAPACITYOPTIMIZER = "oscp-fp-co/co";

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		List<String> scopes = jwt.getClaimAsStringList("scope");
		if ( scopes == null || scopes.isEmpty() ) {
			return Collections.emptyList();
		}
		List<GrantedAuthority> auths = new ArrayList<>(scopes.size());
		for ( String scope : scopes ) {
			String auth = switch (scope) {
				case SCOPE_CAPACITYPROVIDER -> Role.ROLE_CAPACITYPROVIDER.toString();
				case SCOPE_CAPACITYOPTIMIZER -> Role.ROLE_CAPACITYOPTIMIZER.toString();
				default -> "SCOPE_%s".formatted(scope);
			};
			auths.add(new SimpleGrantedAuthority(auth));
		}
		return auths;
	}

}
