/* ==================================================================
 * WebServiceGlobalControllerSupport.java - 16/11/2021 10:48:48 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.web.support.WebServiceControllerSupport.requestDescription;
import static net.solarnetwork.central.web.support.WebServiceControllerSupport.userPrincipalName;
import static net.solarnetwork.domain.Result.error;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionException;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.domain.Result;
import net.solarnetwork.util.NumberUtils;

/**
 * Global REST controller support.
 * 
 * @author matt
 * @version 1.4
 */
@RestControllerAdvice
@Order(1000)
public class WebServiceGlobalControllerSupport {

	/** A class-level logger. */
	private static final Logger log = LoggerFactory.getLogger(WebServiceGlobalControllerSupport.class);

	@Autowired
	private MessageSource messageSource;

	@Value("${spring.servlet.multipart.max-file-size:1MB}")
	private DataSize maxUploadSize = DataSize.ofMegabytes(1);

	/**
	 * Handle a {@link MaxUploadSizeExceededException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @param locale
	 *        the locale
	 * @return the response
	 * @since 1.0
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e,
			WebRequest request, Locale locale) {
		log.warn("MaxUploadSizeExceededException for {}; user [{}]", requestDescription(request),
				userPrincipalName(request));
		String msg = "Upload size exceeded";
		String maxSize = NumberUtils.humanReadableCount(
				e.getMaxUploadSize() > -1 ? e.getMaxUploadSize() : maxUploadSize.toBytes());
		if ( messageSource != null ) {
			msg = messageSource.getMessage("error.web.upload-size-exceeded", new Object[] { maxSize },
					msg, locale);
		}
		return error("WEB.00100", msg);
	}

	/**
	 * Handle data access resource failure exceptions.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @param locale
	 *        the request locale
	 * @return the response
	 * @since 1.1
	 */
	@ExceptionHandler(DataAccessResourceFailureException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.TOO_MANY_REQUESTS)
	public Result<?> handleDataAccessResourceFailureException(DataAccessResourceFailureException e,
			WebRequest request, Locale locale) {
		log.warn("DataAccessResourceFailureException in request {}; user [{}]: {}",
				requestDescription(request), userPrincipalName(request), e.toString());
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
		return error(code, msg);
	}

	/**
	 * Handle transient data access exceptions.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @param locale
	 *        the request locale
	 * @return the response
	 * @since 1.1
	 */
	@ExceptionHandler(TransientDataAccessException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.TOO_MANY_REQUESTS)
	public Result<?> handleTransientDataAccessException(TransientDataAccessException e,
			WebRequest request, Locale locale) {
		log.warn("TransientDataAccessException in request {}; user [{}]: {}",
				requestDescription(request), userPrincipalName(request), e.toString());
		String msg;
		String msgKey;
		String code;
		if ( e instanceof CannotAcquireLockException ) {
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
		return error(code, msg);
	}

	/**
	 * Handle a transaction exception.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @param locale
	 *        the request locale
	 * @return the response
	 * @since 1.1
	 */
	@ExceptionHandler(TransactionException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.TOO_MANY_REQUESTS)
	public Result<?> handleTransactionException(TransactionException e, WebRequest request,
			Locale locale) {
		log.warn("TransactionException in request {}; user [{}]: {}", requestDescription(request),
				userPrincipalName(request), e.toString());
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
		return error(code, msg);
	}

	/**
	 * Handle an {@link AuthorizationException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.1
	 */
	@ExceptionHandler(AuthorizationException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.FORBIDDEN)
	public Result<?> handleAuthorizationException(AuthorizationException e, WebRequest request) {
		log.debug("AuthorizationException in request {}: {}", requestDescription(request),
				e.getMessage());
		return error(null, e.getReason().toString());
	}

	/**
	 * Handle a {@link net.solarnetwork.central.security.SecurityException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.1
	 */
	@ExceptionHandler(net.solarnetwork.central.security.SecurityException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.FORBIDDEN)
	public Result<?> handleSecurityException(net.solarnetwork.central.security.SecurityException e,
			WebRequest request) {
		log.info("SecurityException in request {}; user [{}]: {}", requestDescription(request),
				userPrincipalName(request), e.getMessage());
		return error(null, e.getMessage());
	}

	/**
	 * Handle a {@link BadCredentialsException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.1
	 */
	@ExceptionHandler(BadCredentialsException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.FORBIDDEN)
	public Result<?> handleBadCredentialsException(BadCredentialsException e, WebRequest request) {
		log.info("BadCredentialsException in request {}: {}", requestDescription(request),
				e.getMessage());
		return error(null, e.getMessage());
	}

	/**
	 * Handle a {@link AuthenticationException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.1
	 */
	@ExceptionHandler(AuthenticationException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNAUTHORIZED)
	public Result<?> handleAuthenticationException(AuthenticationException e, WebRequest request) {
		log.info("AuthenticationException in request {}: {}", requestDescription(request),
				e.getMessage());
		return error(null, e.getMessage());
	}

	/**
	 * Handle a {@link AccessDeniedException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.1
	 */
	@ExceptionHandler(AccessDeniedException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.FORBIDDEN)
	public Result<?> handleAuthenticationException(AccessDeniedException e, WebRequest request) {
		log.info("AccessDeniedException in request {}: {}", requestDescription(request), e.getMessage());
		return error(null, e.getMessage());
	}

	/**
	 * Handle a {@link RuntimeException} not handled by other exception
	 * handlers.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.1
	 */
	@ExceptionHandler(RequestRejectedException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.BAD_REQUEST)
	public Result<?> handleRequestRejectedException(RequestRejectedException e, WebRequest request) {
		log.warn("RequestRejectedException in request {}; user [{}]: {}", requestDescription(request),
				userPrincipalName(request), e.getMessage());
		return error(null, e.getMessage());
	}

	/**
	 * Handle an {@link ExecutionException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.1
	 */
	@ExceptionHandler(ExecutionException.class)
	@ResponseBody
	@ResponseStatus
	public Result<?> handleExecutionException(ExecutionException e, WebRequest request) {
		log.debug("ExecutionException in request {}; user [{}]", requestDescription(request),
				userPrincipalName(request), e);
		Throwable cause = e;
		while ( cause.getCause() != null ) {
			cause = cause.getCause();
		}
		if ( cause instanceof IllegalArgumentException ) {
			return handleIllegalArgumentException((IllegalArgumentException) cause, request);
		}
		return error("EE.00500", cause.getMessage());
	}

	/**
	 * Handle an {@link IllegalArgumentException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.1
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Result<?> handleIllegalArgumentException(IllegalArgumentException e, WebRequest request) {
		log.debug("IllegalArgumentException in request {}", requestDescription(request), e);
		return error(null, "Illegal argument: " + e.getMessage());
	}

	/**
	 * Handle a {@link RuntimeException} not handled by other exception
	 * handlers.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.1
	 */
	@ExceptionHandler(RuntimeException.class)
	@ResponseBody
	@ResponseStatus
	public Result<?> handleRuntimeException(RuntimeException e, WebRequest request) {
		// NOTE: in Spring 4.3 the root exception will be unwrapped; support Spring 4.2 here
		Throwable cause = e;
		while ( cause.getCause() != null ) {
			cause = cause.getCause();
		}
		if ( cause instanceof IllegalArgumentException ) {
			return handleIllegalArgumentException((IllegalArgumentException) cause, request);
		}
		log.error("RuntimeException in request {}; user [{}]", requestDescription(request),
				userPrincipalName(request), e);
		return error(null, "Internal error");
	}

	/**
	 * Handle an {@link Error}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.2
	 */
	@ExceptionHandler(Error.class)
	@ResponseBody
	@ResponseStatus
	public Result<?> handleError(Error e, WebRequest request) {
		log.warn("Error in request {}", requestDescription(request), e);
		Throwable cause = e;
		while ( cause.getCause() != null ) {
			cause = cause.getCause();
		}
		return error("E.00500", cause.getMessage());
	}

	/**
	 * Handle a {@link HttpMessageConversionException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.5
	 */
	@ExceptionHandler(HttpMessageConversionException.class)
	@ResponseBody
	@ResponseStatus
	public Result<?> handleHttpMessageConversionException(HttpMessageConversionException e,
			WebRequest request) {
		Throwable cause = e;
		while ( cause.getCause() != null ) {
			cause = cause.getCause();
			if ( "org.apache.catalina.connector.ClientAbortException"
					.equals(cause.getClass().getName()) ) {
				log.debug("ClientAbortException in request {}", requestDescription(request), e);
				return error("WEB.00201", "Client abort.");
			}
		}

		log.error("HttpMessageConversionException in request {}; user [{}]", requestDescription(request),
				userPrincipalName(request), e);
		return error("WEB.00200", e.getMessage());
	}

}
