/* ==================================================================
 * HandlerExceptionResolverRequestRejectedHandler.java - 9/03/2022 10:28:19 AM
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

package net.solarnetwork.central.security.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import net.solarnetwork.util.ObjectUtils;

/**
 * {@link RequestRejectedHandler} that delegates the response to a
 * {@link HandlerExceptionResolver}.
 * 
 * @author matt
 * @version 1.0
 */
public class HandlerExceptionResolverRequestRejectedHandler implements RequestRejectedHandler {

	private static final Logger log = LoggerFactory
			.getLogger(HandlerExceptionResolverRequestRejectedHandler.class);

	private final HandlerExceptionResolver handlerExceptionResolver;

	/**
	 * Constructor.
	 * 
	 * @param handlerExceptionResolver
	 *        the resolve
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public HandlerExceptionResolverRequestRejectedHandler(
			HandlerExceptionResolver handlerExceptionResolver) {
		super();
		this.handlerExceptionResolver = ObjectUtils.requireNonNullArgument(handlerExceptionResolver,
				"handlerExceptionResolver");
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			RequestRejectedException e) throws IOException, ServletException {
		if ( handleWithResolver(request, response, e) ) {
			return;
		}
		log.info("RequestRejectedException in request {}: {}", request.getRequestURL(), e.getMessage());
		response.sendError(HttpStatus.BAD_REQUEST.value());

	}

	private boolean handleWithResolver(HttpServletRequest request, HttpServletResponse response,
			Exception exception) throws ServletException {
		if ( handlerExceptionResolver != null ) {
			try {
				if ( handlerExceptionResolver.resolveException(request, response, null,
						exception) != null ) {
					return true;
				}
			} catch ( RuntimeException e ) {
				throw e;
			} catch ( Exception e ) {
				throw new ServletException(e);
			}
		}
		return false;
	}

}
