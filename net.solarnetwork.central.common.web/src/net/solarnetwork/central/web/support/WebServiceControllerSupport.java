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

import java.sql.SQLException;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionException;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
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
 * @version 1.20
 */
public abstract class WebServiceControllerSupport {

	/** The default format pattern for a date property. */
	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

	/** The default format pattern for a date and time property. */
	public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm";

	/**
	 * The default format pattern for adate and time property with an explicit
	 * {@literal Z} time zone.
	 * 
	 * @since 1.12
	 */
	public static final String DEFAULT_DATE_TIME_FORMAT_Z = "yyyy-MM-dd'T'HH:mm'Z'";

	/**
	 * An alternate format pattern for a date and time property using a space
	 * delimiter between the date and time.
	 * 
	 * @since 1.12
	 */
	public static final String ALT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";

	/**
	 * An alternate format pattern for a date and time property with an explicit
	 * {@literal Z} time zone using a space delimiter between the date and time.
	 * 
	 * @since 1.12
	 */
	public static final String ALT_DATE_TIME_FORMAT_Z = "yyyy-MM-dd HH:mm'Z'";

	/**
	 * The default format pattern for a millisecond-precise date and time
	 * property.
	 * 
	 * @since 1.12
	 */
	public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

	/**
	 * The default format pattern for a millisecond-precise date and time
	 * property with an explicit {@literal Z} time zone.
	 * 
	 * @since 1.12
	 */
	public static final String DEFAULT_TIMESTAMP_FORMAT_Z = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	/**
	 * An alternate format pattern for a millisecond-precise date and time
	 * property using a space delimiter between the date and time.
	 * 
	 * @since 1.12
	 */
	public static final String ALT_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

	/**
	 * An alternate format pattern for a millisecond-precise date and time
	 * property with an explicit {@literal Z} time zone using a space delimiter
	 * between the date and time.
	 * 
	 * @since 1.12
	 */
	public static final String ALT_TIMESTAMP_FORMAT_Z = "yyyy-MM-dd HH:mm:ss.SSS'Z'";

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private MessageSource messageSource;

	/**
	 * Handle an {@link AuthorizationException}.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 */
	@ExceptionHandler(AuthorizationException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.FORBIDDEN)
	public Response<?> handleAuthorizationException(AuthorizationException e) {
		log.debug("AuthorizationException in {} controller: {}", getClass().getSimpleName(),
				e.getMessage());
		return new Response<Object>(Boolean.FALSE, null, e.getReason().toString(), null);
	}

	/**
	 * Handle an {@link BeanInstantiationException}.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 * @since 1.15
	 */
	@ExceptionHandler(BeanInstantiationException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleBeanInstantiationException(BeanInstantiationException e) {
		log.debug("BeanInstantiationException in {} controller: {}", getClass().getSimpleName(),
				e.getMessage(), e);
		return new Response<Object>(Boolean.FALSE, "422", "Malformed request data.", null);
	}

	/**
	 * Handle a {@link net.solarnetwork.central.security.SecurityException}.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 * @since 1.2
	 */
	@ExceptionHandler(net.solarnetwork.central.security.SecurityException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.FORBIDDEN)
	public Response<?> handleSecurityException(net.solarnetwork.central.security.SecurityException e) {
		log.debug("SecurityException in {} controller: {}", getClass().getSimpleName(), e.getMessage());
		return new Response<Object>(Boolean.FALSE, null, e.getMessage(), null);
	}

	/**
	 * Handle a {@link AuthenticationException}.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 * @since 1.13
	 */
	@ExceptionHandler(AuthenticationException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNAUTHORIZED)
	public Response<?> handleAuthenticationException(AuthenticationException e) {
		log.debug("AuthenticationException in {} controller: {}", getClass().getSimpleName(),
				e.getMessage());
		return new Response<Object>(Boolean.FALSE, null, e.getMessage(), null);
	}

	/**
	 * Handle a {@link AccessDeniedException}.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 * @since 1.13
	 */
	@ExceptionHandler(AccessDeniedException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.FORBIDDEN)
	public Response<?> handleAuthenticationException(AccessDeniedException e) {
		log.debug("AccessDeniedException in {} controller: {}", getClass().getSimpleName(),
				e.getMessage());
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
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleTypeMismatchException(TypeMismatchException e,
			HttpServletResponse response) {
		log.debug("TypeMismatchException in {} controller", getClass().getSimpleName(), e);
		return new Response<Object>(Boolean.FALSE, null, "Illegal argument: " + e.getMessage(), null);
	}

	/**
	 * Handle an {@link IllegalArgumentException}.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 * @since 1.3
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleIllegalArgumentException(IllegalArgumentException e) {
		log.debug("IllegalArgumentException in {} controller", getClass().getSimpleName(), e);
		return new Response<Object>(Boolean.FALSE, null, "Illegal argument: " + e.getMessage(), null);
	}

	/**
	 * Handle an {@link UnsupportedOperationException} as a {@literal 404} error
	 * status.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 * @since 1.15
	 */
	@ExceptionHandler(UnsupportedOperationException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.NOT_FOUND)
	public Response<?> handleUnsupportedOperationException(UnsupportedOperationException e) {
		log.debug("UnsupportedOperationException in {} controller", getClass().getSimpleName(), e);
		return new Response<Object>(Boolean.FALSE, "404", e.getMessage(), null);
	}

	/**
	 * Handle a {@link JsonProcessingException}, presuming from malformed JSON
	 * input.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 * @since 1.6
	 */
	@ExceptionHandler(JsonParseException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleJsonParseException(JsonProcessingException e) {
		log.debug("JsonProcessingException in {} controller", getClass().getSimpleName(), e);
		return new Response<Object>(Boolean.FALSE, null, "Malformed JSON: " + e.getOriginalMessage(),
				null);
	}

	/**
	 * Handle a {@link HttpMessageNotReadableException}, from malformed JSON
	 * input.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 * @since 1.15
	 */
	@ExceptionHandler(DateTimeParseException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleDateTimeParseException(DateTimeParseException e) {
		log.debug("DateTimeParseException in {} controller", getClass().getSimpleName(), e);
		return new Response<Object>(Boolean.FALSE, null, "Malformed date string: " + e.getMessage(),
				null);
	}

	/**
	 * Handle a {@link HttpMessageNotReadableException}, from malformed JSON
	 * input.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 * @since 1.6
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
		Throwable t = e.getMostSpecificCause();
		if ( t instanceof JsonProcessingException ) {
			return handleJsonParseException((JsonProcessingException) t);
		} else if ( t instanceof DateTimeParseException ) {
			return handleDateTimeParseException((DateTimeParseException) t);
		}
		log.warn("HttpMessageNotReadableException in {} controller", getClass().getSimpleName(), e);
		return new Response<Object>(Boolean.FALSE, null, "Malformed request: " + e.getMessage(), null);
	}

	/**
	 * Handle a {@link DataIntegrityViolationException}.
	 * 
	 * @param e
	 *        the exception
	 * @param locale
	 *        the locale
	 * @return an error response object
	 * @since 1.8
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleDataIntegrityViolationException(DataIntegrityViolationException e,
			Locale locale) {
		log.warn("DataIntegrityViolationException in {} controller", getClass().getSimpleName(), e);
		String msg;
		String msgKey;
		String code;
		if ( e instanceof DuplicateKeyException ) {
			msg = "Duplicate key";
			msgKey = "error.dao.duplicateKey";
			code = "DAO.00101";
		} else {
			SQLException sqlEx = null;
			Throwable t = e;
			while ( t.getCause() != null ) {
				t = t.getCause();
				if ( t instanceof SQLException ) {
					sqlEx = (SQLException) t;
					break;
				}
			}
			if ( sqlEx != null ) {
				log.warn("Root SQLException from {}: {}", e.getMessage(), sqlEx.getMessage(), sqlEx);
			}
			if ( sqlEx != null && sqlEx.getSQLState() != null && sqlEx.getSQLState().startsWith("22") ) {
				// Class 22 — Data Exception
				msg = "Invalid query parameter";
				msgKey = "error.dao.sqlState.class.22";
				code = "DAO.00103";
			} else {
				msg = "Data integrity violation";
				msgKey = "error.dao.dataIntegrityViolation";
				code = "DAO.00100";
			}
		}
		if ( messageSource != null ) {
			msg = messageSource.getMessage(msgKey,
					new Object[] { e.getMostSpecificCause().getMessage() }, msg, locale);
		}
		return new Response<Object>(Boolean.FALSE, code, msg, null);
	}

	/**
	 * Handle a {@link DataRetrievalFailureException}.
	 * 
	 * @param e
	 *        the exception
	 * @param locale
	 *        the locale
	 * @return an error response object
	 * @since 1.15
	 */
	@ExceptionHandler(DataRetrievalFailureException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.NOT_FOUND)
	public Response<?> handleDataRetrievalFailureException(DataRetrievalFailureException e,
			Locale locale) {
		log.debug("DataRetrievalFailureException in {} controller", getClass().getSimpleName(), e);
		String msg;
		String msgKey;
		String code;
		msg = "Key not found";
		msgKey = "error.dao.keyNotFound";
		code = "DAO.00102";
		if ( messageSource != null ) {
			msg = messageSource.getMessage(msgKey,
					new Object[] { e.getMostSpecificCause().getMessage() }, msg, locale);
		}
		return new Response<Object>(Boolean.FALSE, code, msg, null);
	}

	/**
	 * Handle transient data access exceptions.
	 * 
	 * @param e
	 *        the exception
	 * @param locale
	 *        the request locale
	 * @return the repsonse
	 * @since 1.14
	 */
	@ExceptionHandler(TransientDataAccessException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.TOO_MANY_REQUESTS)
	public Response<?> handleTransientDataAccessException(TransientDataAccessException e,
			Locale locale) {
		log.warn("TransientDataAccessException in {} controller", getClass().getSimpleName(), e);
		String msg;
		String msgKey;
		String code;
		if ( e instanceof DeadlockLoserDataAccessException ) {
			msg = "Deadlock loser";
			msgKey = "error.dao.deadlockLoser";
			code = "DAO.00204";
		} else if ( e instanceof TransientDataAccessResourceException ) {
			msg = "Temporary connection failure";
			msgKey = "error.dao.transientDataAccessResource";
			code = "DAO.00203";
		} else if ( e instanceof QueryTimeoutException ) {
			msg = "Query timeout";
			msgKey = "error.dao.queryTimeout";
			code = "DAO.00202";
		} else if ( e instanceof PessimisticLockingFailureException ) {
			msg = "Lock failure";
			msgKey = "error.dao.pessimisticLockingFailure";
			code = "DAO.00201";
		} else if ( e instanceof ConcurrencyFailureException ) {
			msg = "Concurrency failure";
			msgKey = "error.dao.concurrencyFailure";
			code = "DAO.00205";
		} else {
			msg = "Data integrity violation";
			msgKey = "error.dao.transientDataAccess";
			code = "DAO.00200";
		}
		if ( messageSource != null ) {
			msg = messageSource.getMessage(msgKey,
					new Object[] { e.getMostSpecificCause().getMessage() }, msg, locale);
		}
		return new Response<Object>(Boolean.FALSE, code, msg, null);
	}

	/**
	 * Handle a {@link InvalidDataAccessResourceUsageException} .
	 * 
	 * @param e
	 *        the exception
	 * @param locale
	 *        the desired locale
	 * @return an error response object
	 * @since 1.18
	 */
	@ExceptionHandler(InvalidDataAccessResourceUsageException.class)
	@ResponseBody
	@ResponseStatus
	public Response<?> handleInvalidDataAccessResourceUsageException(
			InvalidDataAccessResourceUsageException e, Locale locale) {
		log.error("InvalidDataAccessResourceUsageException in {} controller", getClass().getSimpleName(),
				e.getMostSpecificCause());
		String msg = "Internal error";
		String msgKey = "error.dao.invalidResourceUsage";
		String code = "DAO.00500";
		if ( messageSource != null ) {
			msg = messageSource.getMessage(msgKey,
					new Object[] { e.getMostSpecificCause().getMessage() }, msg, locale);
		}
		return new Response<Object>(Boolean.FALSE, code, msg, null);
	}

	/**
	 * Handle data access resource failure exceptions.
	 * 
	 * @param e
	 *        the exception
	 * @param locale
	 *        the request locale
	 * @return the repsonse
	 * @since 1.16
	 */
	@ExceptionHandler(DataAccessResourceFailureException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.TOO_MANY_REQUESTS)
	public Response<?> handleDataAccessResourceFailureException(DataAccessResourceFailureException e,
			Locale locale) {
		log.warn("DataAccessResourceFailureException in {} controller", getClass().getSimpleName(), e);
		String msg;
		String msgKey;
		String code;
		msg = "Temporary connection failure";
		msgKey = "error.dao.transientDataAccessResource";
		code = "DAO.00206";
		if ( messageSource != null ) {
			msg = messageSource.getMessage(msgKey,
					new Object[] { e.getMostSpecificCause().getMessage() }, msg, locale);
		}
		return new Response<Object>(Boolean.FALSE, code, msg, null);
	}

	/**
	 * Handle a transaction exception.
	 * 
	 * @param e
	 *        the exception
	 * @param locale
	 *        the request locale
	 * @return the response
	 * @since 1.14
	 */
	@ExceptionHandler(TransactionException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.TOO_MANY_REQUESTS)
	public Response<?> handleTransactionException(TransactionException e, Locale locale) {
		log.warn("TransactionException in {} controller", getClass().getSimpleName(), e);
		String msg;
		String msgKey;
		String code;
		if ( e instanceof CannotCreateTransactionException ) {
			Throwable t = e.getRootCause();
			// look for Tomcat JDBC pool exhaustion without direct dependency on class
			if ( t != null && "org.apache.tomcat.jdbc.pool.PoolExhaustedException"
					.equals(t.getClass().getName()) ) {
				msg = "Connection pool exhausted";
				msgKey = "error.dao.poolExhausted";
				code = "DAO.00302";
			} else {
				msg = "Cannot create transaction";
				msgKey = "error.dao.cannotCreateTransaction";
				code = "DAO.00301";
			}
		} else {
			msg = "Transaction error";
			msgKey = "error.dao.transaction";
			code = "DAO.00300";
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
	 * @return an error response object
	 */
	@ExceptionHandler(RuntimeException.class)
	@ResponseBody
	@ResponseStatus
	public Response<?> handleRuntimeException(RuntimeException e) {
		// NOTE: in Spring 4.3 the root exception will be unwrapped; support Spring 4.2 here
		Throwable cause = e;
		while ( cause.getCause() != null ) {
			cause = cause.getCause();
		}
		if ( cause instanceof IllegalArgumentException ) {
			return handleIllegalArgumentException((IllegalArgumentException) cause);
		}
		log.error("RuntimeException in {} controller", getClass().getSimpleName(), e);
		return new Response<Object>(Boolean.FALSE, null, "Internal error", null);
	}

	/**
	 * Handle an {@link BindException}.
	 * 
	 * @param e
	 *        the exception
	 * @param locale
	 *        the locale
	 * @return an error response object
	 */
	@ExceptionHandler(BindException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleBindException(BindException e, Locale locale) {
		log.debug("BindException in {} controller", getClass().getSimpleName(), e);
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
	 * @param locale
	 *        the locale
	 * @return an error response object
	 */
	@ExceptionHandler(ValidationException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleValidationException(ValidationException e, Locale locale) {
		log.debug("ValidationException in {} controller", getClass().getSimpleName(), e);
		String msg = generateErrorsMessage(e.getErrors(), locale,
				e.getMessageSource() != null ? e.getMessageSource() : messageSource);
		return new Response<Object>(Boolean.FALSE, null, msg, null);
	}

	/**
	 * Handle an {@link ExecutionException}.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 * @since 1.10
	 */
	@ExceptionHandler(ExecutionException.class)
	@ResponseBody
	@ResponseStatus
	public Response<?> handleExecutionException(ExecutionException e) {
		log.debug("ExecutionException in {} controller", getClass().getSimpleName(), e);
		Throwable cause = e;
		while ( cause.getCause() != null ) {
			cause = cause.getCause();
		}
		if ( cause instanceof IllegalArgumentException ) {
			return handleIllegalArgumentException((IllegalArgumentException) cause);
		}
		return new Response<Object>(Boolean.FALSE, "EE.00500", cause.getMessage(), null);
	}

	/**
	 * Handle an {@link Error}.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 * @since 1.17
	 */
	@ExceptionHandler(Error.class)
	@ResponseBody
	@ResponseStatus
	public Response<?> handleError(Error e) {
		log.debug("Error in {} controller", getClass().getSimpleName(), e);
		Throwable cause = e;
		while ( cause.getCause() != null ) {
			cause = cause.getCause();
		}
		return new Response<Object>(Boolean.FALSE, "E.00500", cause.getMessage(), null);
	}

	/**
	 * Handle a {@link MultipartException}.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 * @since 1.20
	 */
	@ExceptionHandler(MultipartException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleMultipartException(MultipartException e) {
		StringBuilder buf = new StringBuilder();
		buf.append("Error parsing multipart HTTP request");
		String msg = e.getMostSpecificCause().getMessage();
		if ( msg != null && !msg.isEmpty() ) {
			buf.append(": ").append(msg);
		}
		return new Response<Object>(Boolean.FALSE, "422", buf.toString(), null);
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

	/**
	 * Add a {@literal Vary} HTTP response header.
	 * 
	 * <p>
	 * This is so the responses work well with caching proxies.
	 * </p>
	 * 
	 * @param response
	 *        the response to add the header to
	 * @since 1.11
	 */
	@ModelAttribute
	public void addVaryResponseHeader(HttpServletResponse response) {
		response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT);
	}

	/**
	 * Get the message source.
	 * 
	 * @return the message source
	 */
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Set a message source to use for resolving exception messages.
	 * 
	 * @param messageSource
	 *        the message source
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
