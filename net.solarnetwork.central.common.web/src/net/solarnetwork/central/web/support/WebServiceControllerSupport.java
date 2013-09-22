/* ==================================================================
 * WebServiceControllerSupport.java - Dec 18, 2012 7:29:54 AM
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

package net.solarnetwork.central.web.support;

import javax.servlet.http.HttpServletResponse;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.web.domain.Response;
import net.solarnetwork.util.JodaDateFormatEditor;
import net.solarnetwork.util.JodaDateFormatEditor.ParseMode;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * A base class to support web service style controllers.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class WebServiceControllerSupport {

	/** The default value for the {@code requestDateFormat} property. */
	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

	/** The default value for the {@code requestDateFormat} property. */
	public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Handle an {@link AuthorizationException}.
	 * 
	 * @param e
	 *        the exception
	 * @param response
	 *        the response
	 * @return an error response object
	 */
	@ExceptionHandler(AuthorizationException.class)
	@ResponseBody
	public Response<?> handleAuthorizationException(AuthorizationException e,
			HttpServletResponse response) {
		log.debug("AuthorizationException in {} controller: {}", getClass().getSimpleName(),
				e.getMessage());
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		return new Response<Object>(Boolean.FALSE, null, e.getReason().toString(), null);
	}

	/**
	 * Handle an {@link RuntimeException}.
	 * 
	 * @param e
	 *        the exception
	 * @param response
	 *        the response
	 * @return an error response object
	 */
	@ExceptionHandler(RuntimeException.class)
	@ResponseBody
	public Response<?> handleRuntimeException(RuntimeException e, HttpServletResponse response) {
		log.error("RuntimeException in {} controller", getClass().getSimpleName(), e);
		return new Response<Object>(Boolean.FALSE, null, "Internal error", null);
	}

	/**
	 * Web binder initialization.
	 * 
	 * <p>
	 * Registers a {@link LocalDate} property editor using the
	 * {@link #DEFAULT_DATE_FORMAT} pattern.
	 * </p>
	 * 
	 * @param binder
	 *        the binder to initialize
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(LocalDate.class, new JodaDateFormatEditor(DEFAULT_DATE_FORMAT,
				ParseMode.LocalDate));
		binder.registerCustomEditor(DateTime.class, new JodaDateFormatEditor(DEFAULT_DATE_TIME_FORMAT,
				ParseMode.DateTime));
	}
}
