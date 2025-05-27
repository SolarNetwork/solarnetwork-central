/* ==================================================================
 * JwtScopeGrantedAuthoritiesConverter.java - 12/04/2024 9:52:37 am
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

package net.solarnetwork.central.inin.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * Convert JWT scopes to Instruction Input authorities.
 *
 * @author matt
 * @version 1.2
 */
public class JwtScopeGrantedAuthoritiesConverter
		implements Converter<Jwt, Collection<GrantedAuthority>> {

	/** The Instruction Input scope value. */
	public static final String SCOPE_INSTRUCTION_INPUT = "din/instr";

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		String[] scopes = StringUtils.delimitedListToStringArray(jwt.getClaimAsString("scope"), " ");
		if ( scopes.length < 1 ) {
			return Collections.emptyList();
		}
		List<GrantedAuthority> auths = new ArrayList<>(scopes.length);
		for ( String scope : scopes ) {
			String auth = switch (scope) {
				case SCOPE_INSTRUCTION_INPUT -> SecurityUtils.ROLE_ININ;
				default -> "SCOPE_%s".formatted(scope);
			};
			auths.add(new SimpleGrantedAuthority(auth));
		}
		return Collections.unmodifiableList(auths);
	}

}
