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

import java.security.Principal;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.Result;
import net.solarnetwork.security.AbstractAuthorizationBuilder;
import net.solarnetwork.util.StringUtils;
import net.solarnetwork.web.domain.Response;

/**
 * A base class to support web service style controllers.
 * 
 * @author matt
 * @version 2.1
 */
@RestControllerAdvice(annotations = GlobalExceptionRestController.class)
@Order(100)
public final class WebServiceControllerSupport {

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

	/**
	 * A value to use for anonymous users in log messages.
	 * 
	 * @since 2.1
	 */
	public static final String ANONYMOUS_USER_PRINCIPAL = "anonymous";

	/** A class-level logger. */
	private static final Logger log = LoggerFactory.getLogger(WebServiceControllerSupport.class);

	@Autowired
	private MessageSource messageSource;

	@Autowired(required = false)
	private Validator validator;

	/**
	 * Get a standardized string description of a request.
	 * 
	 * @param request
	 *        the request
	 * @return the description
	 */
	public static String requestDescription(WebRequest request) {
		StringBuilder buf = new StringBuilder(request.getDescription(false));
		Map<String, String[]> params = request.getParameterMap();
		if ( params != null ) {
			buf.append("?");
			boolean next = false;
			for ( Entry<String, String[]> e : params.entrySet() ) {
				if ( next ) {
					buf.append('&');
				} else {
					next = true;
				}
				buf.append(e.getKey()).append("=");
				String[] vals = e.getValue();
				if ( vals == null || vals.length < 1 ) {
					continue;
				} else if ( vals.length == 1 ) {
					buf.append(vals[0]);
				} else {
					for ( int i = 0, len = vals.length; i < len; i++ ) {
						if ( i > 0 ) {
							buf.append(",");
						}
						buf.append(vals[i]);
					}
				}

			}
		}
		return buf.toString();
	}

	/**
	 * Get the user principal name of a given request.
	 * 
	 * @param request
	 *        the request
	 * @return the name, or {@link #ANONYMOUS_USER_PRINCIPAL}
	 */
	public static String userPrincipalName(WebRequest request) {
		Principal userPrincipal = request.getUserPrincipal();
		if ( userPrincipal != null ) {
			return userPrincipal.getName();
		}
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if ( authHeader != null ) {
			int idx = authHeader.indexOf(' ');
			if ( idx > 0 && idx < authHeader.length() ) {
				String data = authHeader.substring(idx + 1);
				Map<String, String> dataMap = StringUtils.commaDelimitedStringToMap(data);
				String name = dataMap
						.get(AbstractAuthorizationBuilder.AUTHORIZATION_COMPONENT_CREDENTIAL);
				if ( name != null ) {
					return name;
				}
			}
			return authHeader;
		}
		return ANONYMOUS_USER_PRINCIPAL;
	}

	/**
	 * Handle an {@link BeanInstantiationException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.15
	 */
	@ExceptionHandler(BeanInstantiationException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleBeanInstantiationException(BeanInstantiationException e,
			WebRequest request) {
		log.debug("BeanInstantiationException in request {}: {}", requestDescription(request),
				e.getMessage(), e);
		return new Response<Object>(Boolean.FALSE, "422", "Malformed request data.", null);
	}

	/**
	 * Handle an {@link TypeMismatchException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @param response
	 *        the response
	 * @return an error response object
	 * @since 1.4
	 */
	@ExceptionHandler(TypeMismatchException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleTypeMismatchException(TypeMismatchException e, WebRequest request,
			HttpServletResponse response) {
		log.debug("TypeMismatchException in request {}", requestDescription(request), e);
		return new Response<Object>(Boolean.FALSE, null, "Illegal argument: " + e.getMessage(), null);
	}

	/**
	 * Handle an {@link UnsupportedOperationException} as a {@literal 404} error
	 * status.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.15
	 */
	@ExceptionHandler(UnsupportedOperationException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.NOT_FOUND)
	public Response<?> handleUnsupportedOperationException(UnsupportedOperationException e,
			WebRequest request) {
		log.debug("UnsupportedOperationException in request {}", requestDescription(request), e);
		return new Response<Object>(Boolean.FALSE, "404", e.getMessage(), null);
	}

	/**
	 * Handle a {@link JsonProcessingException}, presuming from malformed JSON
	 * input.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.6
	 */
	@ExceptionHandler(JsonParseException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleJsonParseException(JsonProcessingException e, WebRequest request) {
		log.debug("JsonProcessingException in request {}", requestDescription(request), e);
		return new Response<Object>(Boolean.FALSE, null, "Malformed JSON: " + e.getOriginalMessage(),
				null);
	}

	/**
	 * Handle a {@link DateTimeParseException}, from malformed date input.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.15
	 */
	@ExceptionHandler(DateTimeParseException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleDateTimeParseException(DateTimeParseException e, WebRequest request) {
		log.debug("DateTimeParseException in request {}", requestDescription(request), e);
		return new Response<Object>(Boolean.FALSE, null, "Malformed date string: " + e.getMessage(),
				null);
	}

	/**
	 * Handle a general {@Link DateTimeException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return the error response object
	 * @since 2.0
	 */
	@ExceptionHandler(DateTimeException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleDateTimeException(DateTimeException e, WebRequest request) {
		log.debug("DateTimeException in request {}", requestDescription(request), e);
		return new Response<Object>(Boolean.FALSE, null, "Date exception: " + e.getMessage(), null);
	}

	/**
	 * Handle a {@link HttpMessageNotReadableException}, from malformed JSON
	 * input.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.6
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e,
			WebRequest request) {
		Throwable t = e.getMostSpecificCause();
		if ( t instanceof JsonProcessingException ) {
			return handleJsonParseException((JsonProcessingException) t, request);
		} else if ( t instanceof DateTimeParseException ) {
			return handleDateTimeParseException((DateTimeParseException) t, request);
		}
		log.warn("HttpMessageNotReadableException in request {}: {}", requestDescription(request),
				e.toString());
		return new Response<Object>(Boolean.FALSE, null, "Malformed request: " + e.getMessage(), null);
	}

	/**
	 * Handle a {@link DataIntegrityViolationException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @param locale
	 *        the locale
	 * @return an error response object
	 * @since 1.8
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleDataIntegrityViolationException(DataIntegrityViolationException e,
			WebRequest request, Locale locale) {
		log.error("DataIntegrityViolationException in request {}: {}", requestDescription(request),
				e.toString());
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
			String sqlState = null;
			while ( t.getCause() != null ) {
				t = t.getCause();
				if ( t instanceof SQLException ) {
					sqlEx = (SQLException) t;
					break;
				}
			}
			if ( sqlEx != null ) {
				log.warn("Root SQLException from {}: {}", e.getMessage(), sqlEx.getMessage(), sqlEx);
				sqlState = sqlEx.getSQLState();
			}
			if ( sqlState != null && sqlState.startsWith("22") ) {
				// Class 22 — Data Exception
				msg = "Invalid query parameter";
				msgKey = "error.dao.sqlState.class.22";
				code = "DAO.00103";
			} else if ( sqlState != null && sqlState.startsWith("23") ) {
				msg = "Integrity constraint violation";
				if ( sqlState.equals("23503") ) {
					msgKey = "error.dao.sqlState.class.23503";
					code = "DAO.00105";
				} else {
					msgKey = "error.dao.sqlState.class.23";
					code = "DAO.00104";
				}
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
	 * @param request
	 *        the request
	 * @param locale
	 *        the locale
	 * @return an error response object
	 * @since 1.15
	 */
	@ExceptionHandler(DataRetrievalFailureException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.NOT_FOUND)
	public Response<?> handleDataRetrievalFailureException(DataRetrievalFailureException e,
			WebRequest request, Locale locale) {
		log.debug("DataRetrievalFailureException in request {}, user [{}]", requestDescription(request),
				userPrincipalName(request), e);
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
	 * Handle a {@link InvalidDataAccessResourceUsageException} .
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @param locale
	 *        the desired locale
	 * @return an error response object
	 * @since 1.18
	 */
	@ExceptionHandler(InvalidDataAccessResourceUsageException.class)
	@ResponseBody
	@ResponseStatus
	public Response<?> handleInvalidDataAccessResourceUsageException(
			InvalidDataAccessResourceUsageException e, WebRequest request, Locale locale) {
		log.error("InvalidDataAccessResourceUsageException in request {}", requestDescription(request),
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
	 * Handle an {@link ConstraintViolationException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public Result<Void> handleConstraintViolationException(ConstraintViolationException e,
			WebRequest request, Locale locale) {
		log.debug("ConstraintViolationException in request {}: {}", requestDescription(request),
				e.toString());
		BindingResult errors = ExceptionUtils.toBindingResult(e, validator);
		return ExceptionUtils.generateErrorsResult(errors, "VAL.00003", locale, messageSource);
	}

	/**
	 * Handle an {@link BindException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @param locale
	 *        the locale
	 * @return an error response object
	 */
	@ExceptionHandler(BindException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public Result<?> handleBindException(BindException e, WebRequest request, Locale locale) {
		log.debug("BindException in request {}: {}", requestDescription(request), e.toString());
		return ExceptionUtils.generateErrorsResult(e, "VAL.00004", locale, messageSource);
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
	 * @param request
	 *        the request
	 * @param locale
	 *        the locale
	 * @return an error response object
	 */
	@ExceptionHandler(ValidationException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleValidationException(ValidationException e, WebRequest request,
			Locale locale) {
		log.debug("ValidationException in request {}: {}", requestDescription(request), e.toString());
		String msg = generateErrorsMessage(e.getErrors(), locale,
				e.getMessageSource() != null ? e.getMessageSource() : messageSource);
		return new Response<Object>(Boolean.FALSE, null, msg, null);
	}

	/**
	 * Handle a {@link MultipartException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.20
	 */
	@ExceptionHandler(MultipartException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleMultipartException(MultipartException e, WebRequest request) {
		log.info("MultipartException in request {}; user [{}]: {}", requestDescription(request),
				userPrincipalName(request), e.toString());
		StringBuilder buf = new StringBuilder();
		buf.append("Error parsing multipart HTTP request");
		String msg = e.getMostSpecificCause().getMessage();
		if ( msg != null && !msg.isEmpty() ) {
			buf.append(": ").append(msg);
		}
		return new Response<Object>(Boolean.FALSE, "422", buf.toString(), null);
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
		Collection<String> vary = response.getHeaders(HttpHeaders.VARY);
		if ( vary == null || !vary.contains(HttpHeaders.ACCEPT) ) {
			response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT);
		}
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
	@Autowired
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
