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

import java.util.Locale;
import javax.servlet.http.HttpServletResponse;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.web.domain.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * A base class to support web service style controllers.
 * 
 * @author matt
 * @version 1.1
 */
public abstract class WebServiceControllerSupport {

	/** The default value for the {@code requestDateFormat} property. */
	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

	/** The default value for the {@code requestDateFormat} property. */
	public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm";

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private MessageSource messageSource;

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
	 * Handle an {@link BindException}.
	 * 
	 * @param e
	 *        the exception
	 * @param response
	 *        the response
	 * @return an error response object
	 */
	@ExceptionHandler(BindException.class)
	@ResponseBody
	public Response<?> handleBindException(BindException e, HttpServletResponse response, Locale locale) {
		log.error("BindException in {} controller", getClass().getSimpleName(), e);
		response.setStatus(422);
		String msg = generateErrorsMessage(e, locale);
		return new Response<Object>(Boolean.FALSE, null, msg, null);
	}

	private String generateErrorsMessage(Errors e, Locale locale) {
		String msg = (messageSource == null ? "Validation error" : messageSource.getMessage(
				"error.validation", null, "Validation error", locale));
		if ( messageSource != null && e.hasErrors() ) {
			StringBuilder buf = new StringBuilder();
			for ( ObjectError error : e.getAllErrors() ) {
				if ( buf.length() > 0 ) {
					buf.append(" ");
				}
				buf.append(messageSource.getMessage(error, locale));
			}
			msg = buf.toString();
		}
		return msg;
	}

	/**
	 * Handle an {@link ValidationException}.
	 * 
	 * @param e
	 *        the exception
	 * @param response
	 *        the response
	 * @return an error response object
	 */
	@ExceptionHandler(ValidationException.class)
	@ResponseBody
	public Response<?> handleValidationException(ValidationException e, HttpServletResponse response,
			Locale locale) {
		log.error("ValidationException in {} controller", getClass().getSimpleName(), e);
		response.setStatus(422);
		String msg = generateErrorsMessage(e.getErrors(), locale);
		return new Response<Object>(Boolean.FALSE, null, msg, null);
	}

	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
