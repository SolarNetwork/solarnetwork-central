/* ==================================================================
 * ControllerSupport.java - Dec 4, 2012 9:46:09 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web;

import javax.servlet.http.HttpServletResponse;
import net.solarnetwork.central.user.biz.AuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Supporting base class for other controllers.
 * 
 * @author matt
 * @version 1.0
 */
public class ControllerSupport {

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * AuthorizationException handler.
	 * 
	 * <p>
	 * Logs a WARN log and returns HTTP 403 (Forbidden).
	 * </p>
	 * 
	 * @param e
	 *        the exception
	 * @param res
	 *        the servlet response
	 */
	@ExceptionHandler(AuthorizationException.class)
	public void handleSecurityException(AuthorizationException e, HttpServletResponse res) {
		if ( log.isWarnEnabled() ) {
			log.warn("Authorization exception: " + e.getMessage());
		}
		res.setStatus(HttpServletResponse.SC_FORBIDDEN);
	}

}
