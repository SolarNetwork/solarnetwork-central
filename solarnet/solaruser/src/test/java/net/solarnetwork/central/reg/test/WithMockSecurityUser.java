/* ==================================================================
 * WithMockSecurityUser.java - 30/05/2022 9:20:13 am
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

package net.solarnetwork.central.reg.test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * Annotation fo
 * 
 * @author matt
 * @version 1.0
 */
@Documented
@Retention(RUNTIME)
@WithSecurityContext(factory = WithMockSecurityUserContextFactory.class)
public @interface WithMockSecurityUser {

	String userId() default "1";

	String username() default "test1@localhost";

	String name() default "Tester 1";

}
