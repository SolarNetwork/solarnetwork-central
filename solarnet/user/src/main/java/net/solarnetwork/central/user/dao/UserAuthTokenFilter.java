/* ==================================================================
 * UserAuthTokenFilter.java - 2/04/2025 8:40:27 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.solarnetwork.central.common.dao.ActiveCriteria;
import net.solarnetwork.central.common.dao.IdentifierCriteria;
import net.solarnetwork.central.common.dao.UserCriteria;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Filter API for user auth token entities.
 * 
 * @author matt
 * @version 1.1
 */
public interface UserAuthTokenFilter
		extends UserCriteria, ActiveCriteria, IdentifierCriteria, PaginationCriteria {

	/**
	 * Test if any token type criteria exists.
	 * 
	 * @return {@code true} if a token type criteria exists
	 */
	default boolean hasTokenTypeCriteria() {
		return !tokenTypesSet().isEmpty();
	}

	/**
	 * Get an array of token types.
	 * 
	 * @return array of topics (may be {@code null})
	 */
	String[] getTokenTypes();

	/**
	 * Get the first token type.
	 * 
	 * <p>
	 * This returns the first available token type from the
	 * {@link #getTokenTypes()} array, or {@code null} if not available.
	 * </p>
	 * 
	 * @return the token type, or {@code null} if not available
	 */
	default String getTokenType() {
		String[] a = getTokenTypes();
		return (a != null && a.length > 0 ? a[0] : null);
	}

	/**
	 * Get the token types as a set of enumeration values.
	 * 
	 * @return the valid token types as a set
	 */
	default Set<SecurityTokenType> tokenTypesSet() {
		String[] a = getTokenTypes();
		if ( a == null || a.length < 1 ) {
			return Collections.emptySet();
		}
		Set<SecurityTokenType> result = new LinkedHashSet<>(2);
		for ( String t : a ) {
			try {
				result.add(SecurityTokenType.valueOf(t));
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return Collections.unmodifiableSet(result);
	}

	/**
	 * Get the token types as an array of enumeration values.
	 * 
	 * @return array of token types, or {@code null}
	 */
	default SecurityTokenType[] getTokenTypeEnums() {
		Set<SecurityTokenType> set = tokenTypesSet();
		if ( set.isEmpty() ) {
			return null;
		}
		return set.toArray(SecurityTokenType[]::new);

	}

}
