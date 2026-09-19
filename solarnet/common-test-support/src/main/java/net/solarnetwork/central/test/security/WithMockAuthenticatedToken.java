/* ==================================================================
 * WithMockAuthenticatedToken.java - 23/03/2026 11:11:17 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.test.security;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import org.springframework.security.test.context.support.WithSecurityContext;
import net.solarnetwork.central.security.SecurityTokenType;

/**
 * Mock authenticated token support.
 *
 * @author matt
 * @version 1.0
 */
@Documented
@Retention(RUNTIME)
@WithSecurityContext(factory = WithMockAuthenticatedTokenContextFactory.class)
public @interface WithMockAuthenticatedToken {

	/**
	 * The token owner user ID.
	 *
	 * @return the user ID
	 */
	long userId() default 1L;

	/**
	 * The token ID.
	 *
	 * @return the token ID
	 */
	String token() default "abc123";

	/**
	 * The token type.
	 *
	 * @return the token type
	 */
	SecurityTokenType type() default SecurityTokenType.ReadNodeData;

}
