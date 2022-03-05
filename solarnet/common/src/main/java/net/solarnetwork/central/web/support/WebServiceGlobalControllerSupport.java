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
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionException;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.web.domain.Response;

/**
 * Global REST controller support.
 * 
 * @author matt
 * @version 1.1
 */
@RestControllerAdvice
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
	public Response<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e,
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
		return new Response<Object>(Boolean.FALSE, "WEB.00100", msg, null);
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
	public Response<?> handleDataAccessResourceFailureException(DataAccessResourceFailureException e,
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
		return new Response<Object>(Boolean.FALSE, code, msg, null);
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
	public Response<?> handleTransientDataAccessException(TransientDataAccessException e,
			WebRequest request, Locale locale) {
		log.warn("TransientDataAccessException in request {}; user [{}]: {}",
				requestDescription(request), userPrincipalName(request), e.toString());
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
	public Response<?> handleTransactionException(TransactionException e, WebRequest request,
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
		return new Response<Object>(Boolean.FALSE, code, msg, null);
	}

}
