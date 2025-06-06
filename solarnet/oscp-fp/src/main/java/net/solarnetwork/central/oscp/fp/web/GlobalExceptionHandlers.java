/* ==================================================================
 * GlobalExceptionHandlers.java - 11/08/2022 3:11:52 pm
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

package net.solarnetwork.central.oscp.fp.web;

import static net.solarnetwork.central.web.support.WebServiceControllerSupport.requestDescription;
import static net.solarnetwork.central.web.support.WebServiceControllerSupport.userPrincipalName;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.domain.Result;

/**
 * Global controller exception handlers.
 *
 * @author matt
 * @version 1.1
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandlers {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandlers.class);

	private final MessageSource messageSource;
	private final Validator validator;

	/**
	 * Constructor.
	 *
	 * @param messageSource
	 *        the message source
	 * @param validator
	 *        the validator
	 */
	public GlobalExceptionHandlers(MessageSource messageSource, Validator validator) {
		super();
		this.messageSource = requireNonNullArgument(messageSource, "messageSource");
		this.validator = requireNonNullArgument(validator, "validator");
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
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public Result<Void> handleConstraintViolationException(ConstraintViolationException e,
			WebRequest request, Locale locale) {
		log.debug("ConstraintViolationException in request {}; user [{}]: {}",
				requestDescription(request), userPrincipalName(request), e.toString());
		BindingResult errors = ExceptionUtils.toBindingResult(e, validator);
		return ExceptionUtils.generateErrorsResult(errors, "VAL.00003", locale, messageSource);
	}

	/**
	 * Handle an {@link MethodArgumentNotValidException}.
	 *
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 */
	@ExceptionHandler(BindException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public Result<Void> handleBindException(BindException e, WebRequest request, Locale locale) {
		log.debug("MethodArgumentNotValidException in request {}; user [{}]: {}",
				requestDescription(request), userPrincipalName(request), e.toString());
		return ExceptionUtils.generateErrorsResult(e, "VAL.00004", locale, messageSource);
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
	 * @since 1.1
	 */
	@ExceptionHandler(JsonParseException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Result<?> handleJsonParseException(JsonProcessingException e, WebRequest request) {
		log.warn("JsonProcessingException in request {}; user [{}]", requestDescription(request),
				userPrincipalName(request), e);
		return Result.error("VAL.00005", "Malformed JSON: " + e.getOriginalMessage());
	}

	/**
	 * Handle a {@link DateTimeParseException}, from malformed date input.
	 *
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 * @since 1.1
	 */
	@ExceptionHandler(DateTimeParseException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Result<?> handleDateTimeParseException(DateTimeParseException e, WebRequest request) {
		log.warn("DateTimeParseException in request {}; user [{}]", requestDescription(request),
				userPrincipalName(request), e);
		return Result.error("VAL.00006", "Malformed date string: " + e.getMessage());
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
	 * @since 1.1
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Result<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e,
			WebRequest request) {
		Throwable t = e.getMostSpecificCause();
		if ( t instanceof JsonProcessingException ex ) {
			return handleJsonParseException(ex, request);
		} else if ( t instanceof DateTimeParseException ex ) {
			return handleDateTimeParseException(ex, request);
		}
		log.warn("HttpMessageNotReadableException in request {}; user [{}]: {}",
				requestDescription(request), userPrincipalName(request), e.toString());
		return Result.error("VAL.00007", "Malformed request: " + e.getMessage());
	}

}
