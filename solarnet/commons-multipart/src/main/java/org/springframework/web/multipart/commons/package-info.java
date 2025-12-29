/* ==================================================================
 * package-info.java - 27/07/2024 6:26:07 am
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

/**
 * Spring web multipart implementation using Apache Tomcat Multipart.
 *
 * <p>
 * This package was removed in Spring 6, however
 * {@code net.solarnetwork.central.security.web.SecurityTokenAuthenticationFilter}
 * does not work with the
 * {@code org.springframework.web.multipart.support.StandardServletMultipartResolver}
 * as the request body digest cannot be computed after the servlet container
 * parses the multipart content. The code has been adapted to use Apache
 * Tomcat's internal version of Commons Fileupload.
 * </p>
 *
 * @author matt
 * @version 1.0
 */

@NullMarked
package org.springframework.web.multipart.commons;

import org.jspecify.annotations.NullMarked;
