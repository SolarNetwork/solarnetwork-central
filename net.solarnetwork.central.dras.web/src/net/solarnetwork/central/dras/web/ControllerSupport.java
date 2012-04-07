/* ==================================================================
 * ControllerSupport.java - Jun 10, 2011 5:07:29 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dras.web;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import net.solarnetwork.central.security.SecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Base class for Controllers.
 * 
 * @author matt
 * @version $Revision$
 */
public class ControllerSupport {

	/** The model key for the primary result object. */
	public static final String MODEL_KEY_RESULT = "result";
	
	/** The model key for the primary error object. */
	public static final String MODEL_KEY_ERROR = "error";

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	private String viewName;
	
	/**
	 * SecurityException handler.
	 * 
	 * <p>Logs a WARN log and returns HTTP 403 (Forbidden).</p>
	 * 
	 * @param e the security exception
	 * @param res the servlet response
	 */
	@ExceptionHandler(SecurityException.class)
	public void handleSecurityException(SecurityException e, HttpServletResponse res) {
		if ( log.isWarnEnabled() ) {
			log.warn("Security exception: " +e.getMessage());
		}
		res.setStatus(HttpServletResponse.SC_FORBIDDEN);
	}

	/**
	 * DataIntegrityViolationException handler.
	 * 
	 * <p>Logs a WARN log and returns HTTP 404 (Forbidden).</p>
	 * 
	 * @param e the security exception
	 * @param res the servlet response
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public void handleSecurityException(DataIntegrityViolationException e, HttpServletResponse res) {
		log.error("DataIntegrityViolationException", e);
		try {
			res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid input.");
		} catch ( IOException ioe ) {
			log.debug("IOException sending DataIntegrityViolationException response: {}",
					ioe.getMessage());
		}
	}

	public String getViewName() {
		return viewName;
	}
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

}
