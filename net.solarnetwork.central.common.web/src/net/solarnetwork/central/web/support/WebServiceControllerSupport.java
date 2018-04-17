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

import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fasterxml.jackson.core.JsonParseException;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.central.security.SecurityNode;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.UserAuthTokenType;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.web.domain.Response;

/**
 * A base class to support web service style controllers.
 * 
 * @author matt
 * @version 1.8
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
	 * Handle a {@link net.solarnetwork.central.security.SecurityException}.
	 * 
	 * @param e
	 *        the exception
	 * @param response
	 *        the response
	 * @return an error response object
	 * @since 1.2
	 */
	@ExceptionHandler(net.solarnetwork.central.security.SecurityException.class)
	@ResponseBody
	public Response<?> handleSecurityException(net.solarnetwork.central.security.SecurityException e,
			HttpServletResponse response) {
		log.debug("SecurityException in {} controller: {}", getClass().getSimpleName(), e.getMessage());
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		return new Response<Object>(Boolean.FALSE, null, e.getMessage(), null);
	}

	/**
	 * Handle an {@link TypeMismatchException}.
	 * 
	 * @param e
	 *        the exception
	 * @param response
	 *        the response
	 * @return an error response object
	 * @since 1.4
	 */
	@ExceptionHandler(TypeMismatchException.class)
	@ResponseBody
	public Response<?> handleTypeMismatchException(TypeMismatchException e,
			HttpServletResponse response) {
		log.error("TypeMismatchException in {} controller", getClass().getSimpleName(), e);
		return new Response<Object>(Boolean.FALSE, null, "Illegal argument: " + e.getMessage(), null);
	}

	/**
	 * Handle an {@link IllegalArgumentException}.
	 * 
	 * @param e
	 *        the exception
	 * @param response
	 *        the response
	 * @return an error response object
	 * @since 1.3
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseBody
	public Response<?> handleIllegalArgumentException(IllegalArgumentException e,
			HttpServletResponse response) {
		log.error("IllegalArgumentException in {} controller", getClass().getSimpleName(), e);
		return new Response<Object>(Boolean.FALSE, null, "Illegal argument: " + e.getMessage(), null);
	}

	/**
	 * Handle a {@link JsonParseException}, presuming from malformed JSON input.
	 * 
	 * @param e
	 *        the exception
	 * @param response
	 *        the response
	 * @return an error response object
	 * @since 1.6
	 */
	@ExceptionHandler(JsonParseException.class)
	@ResponseBody
	public Response<?> handleJsonParseException(JsonParseException e, HttpServletResponse response) {
		log.error("JsonParseException in {} controller", getClass().getSimpleName(), e);
		return new Response<Object>(Boolean.FALSE, null, "Malformed JSON: " + e.getMessage(), null);
	}

	/**
	 * Handle a {@link HttpMessageNotReadableException}, from malformed JSON
	 * input.
	 * 
	 * @param e
	 *        the exception
	 * @param response
	 *        the response
	 * @return an error response object
	 * @since 1.6
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseBody
	public Response<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e,
			HttpServletResponse response) {
		Throwable t = e.getMostSpecificCause();
		if ( t instanceof JsonParseException ) {
			return handleJsonParseException((JsonParseException) t, response);
		}
		log.error("HttpMessageNotReadableException in {} controller", getClass().getSimpleName(), e);
		return new Response<Object>(Boolean.FALSE, null, "Malformed JSON: " + e.getMessage(), null);
	}

	/**
	 * Handle a {@link DataIntegrityViolationException}.
	 * 
	 * @param e
	 *        the exception
	 * @param response
	 *        the response
	 * @return an error response object
	 * @since 1.8
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	@ResponseBody
	public Response<?> handleDataIntegrityViolationException(DataIntegrityViolationException e,
			HttpServletResponse response, Locale locale) {
		log.warn("DataIntegrityViolationException in {} controller", getClass().getSimpleName(), e);
		String msg;
		String msgKey;
		String code;
		if ( e instanceof DuplicateKeyException ) {
			msg = "Duplicate key";
			msgKey = "error.dao.duplicateKey";
			code = "DAO.00101";
		} else {
			msg = "Data integrity violation";
			msgKey = "error.dao.dataIntegrityViolation";
			code = "DAO.00100";
		}
		if ( messageSource != null ) {
			msg = messageSource.getMessage(msgKey,
					new Object[] { e.getMostSpecificCause().getMessage() }, msg, locale);
		}
		return new Response<Object>(Boolean.FALSE, code, msg, null);
	}

	/**
	 * Handle a {@link RuntimeException} not handled by other exception
	 * handlers.
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
		// NOTE: in Spring 4.3 the root exception will be unwrapped; support Spring 4.2 here
		Throwable cause = e;
		while ( cause.getCause() != null ) {
			cause = cause.getCause();
		}
		if ( cause instanceof IllegalArgumentException ) {
			return handleIllegalArgumentException((IllegalArgumentException) cause, response);
		}
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
	public Response<?> handleBindException(BindException e, HttpServletResponse response,
			Locale locale) {
		log.debug("BindException in {} controller", getClass().getSimpleName(), e);
		response.setStatus(422);
		String msg = generateErrorsMessage(e, locale, messageSource);
		return new Response<Object>(Boolean.FALSE, null, msg, null);
	}

	private String generateErrorsMessage(Errors e, Locale locale, MessageSource msgSrc) {
		String msg = (msgSrc == null ? "Validation error"
				: msgSrc.getMessage("error.validation", null, "Validation error", locale));
		if ( msgSrc != null && e.hasErrors() ) {
			StringBuilder buf = new StringBuilder();
			for ( ObjectError error : e.getAllErrors() ) {
				if ( buf.length() > 0 ) {
					buf.append(" ");
				}
				buf.append(msgSrc.getMessage(error, locale));
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
		log.debug("ValidationException in {} controller", getClass().getSimpleName(), e);
		response.setStatus(422);
		String msg = generateErrorsMessage(e.getErrors(), locale,
				e.getMessageSource() != null ? e.getMessageSource() : messageSource);
		return new Response<Object>(Boolean.FALSE, null, msg, null);
	}

	/**
	 * Get all node IDs the current actor is authorized to access.
	 * 
	 * @param userBiz
	 *        The UserBiz to use to fill in all available nodes for user-based
	 *        actors, or {@code null} to to fill in nodes.
	 * @return The allowed node IDs.
	 * @throws AuthorizationException
	 *         if no node IDs are allowed or there is no actor
	 * @since 1.5
	 */
	protected Long[] authorizedNodeIdsForCurrentActor(UserBiz userBiz) {
		final SecurityActor actor;
		try {
			actor = SecurityUtils.getCurrentActor();
		} catch ( SecurityException e ) {
			log.warn("Access DENIED to node {} for non-authenticated user");
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}

		if ( actor instanceof SecurityNode ) {
			SecurityNode node = (SecurityNode) actor;
			return new Long[] { node.getNodeId() };
		} else if ( actor instanceof SecurityUser ) {
			SecurityUser user = (SecurityUser) actor;
			// default to all nodes for actor
			List<UserNode> nodes = userBiz.getUserNodes(user.getUserId());
			if ( nodes != null && !nodes.isEmpty() ) {
				Long[] result = new Long[nodes.size()];
				for ( ListIterator<UserNode> itr = nodes.listIterator(); itr.hasNext(); ) {
					result[itr.nextIndex()] = itr.next().getId();
				}
				return result;
			}
		} else if ( actor instanceof SecurityToken ) {
			SecurityToken token = (SecurityToken) actor;
			Long[] result = null;
			if ( UserAuthTokenType.User.toString().equals(token.getTokenType()) ) {
				// default to all nodes for actor
				List<UserNode> nodes = userBiz.getUserNodes(token.getUserId());
				if ( nodes != null && !nodes.isEmpty() ) {
					result = new Long[nodes.size()];
					for ( ListIterator<UserNode> itr = nodes.listIterator(); itr.hasNext(); ) {
						result[itr.nextIndex()] = itr.next().getId();
					}
				}
			} else if ( UserAuthTokenType.ReadNodeData.toString().equals(token.getTokenType()) ) {
				// all node IDs in token
				if ( token.getPolicy() != null && token.getPolicy().getNodeIds() != null ) {
					Set<Long> nodeIds = token.getPolicy().getNodeIds();
					result = nodeIds.toArray(new Long[nodeIds.size()]);
				}
			}
			if ( result != null && result.length > 0 ) {
				return result;
			}
		}
		throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
	}

	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
