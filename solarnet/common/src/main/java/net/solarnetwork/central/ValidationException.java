/* ==================================================================
 * ValidationException.java - Dec 18, 2009 4:31:14 PM
 *
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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

package net.solarnetwork.central;

import java.io.Serial;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;

/**
 * Exception for validation errors.
 *
 * @author matt
 * @version 1.2
 */
public class ValidationException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = -40848031815846620L;

	private final @Nullable Errors errors;
	private final @Nullable MessageSource messageSource;

	/**
	 * Default constructor.
	 */
	public ValidationException() {
		this(null);
	}

	/**
	 * Constructor with Errors.
	 *
	 * @param errors
	 *        the validation errors
	 */
	public ValidationException(@Nullable Errors errors) {
		this(errors, null);
	}

	/**
	 * Constructor with Errors and a MessageSource.
	 *
	 * @param errors
	 *        the errors
	 * @param messageSource
	 *        the message source to use to resolve the Errors against
	 */
	public ValidationException(@Nullable Errors errors, @Nullable MessageSource messageSource) {
		this(errors, messageSource, null);
	}

	/**
	 * Constructor with Errors and a MessageSource.
	 *
	 * @param message
	 *        the message
	 * @param errors
	 *        the errors
	 * @param messageSource
	 *        the message source to use to resolve the Errors against
	 * @since 1.2
	 */
	public ValidationException(String message, @Nullable Errors errors,
			@Nullable MessageSource messageSource) {
		this(message, errors, messageSource, null);
	}

	/**
	 * Constructor with Errors and a MessageSource and root cause.
	 *
	 * @param errors
	 *        the errors
	 * @param messageSource
	 *        the message source to use to resolve the Errors against
	 * @param cause
	 *        the causing exception
	 */
	public ValidationException(@Nullable Errors errors, @Nullable MessageSource messageSource,
			@Nullable Throwable cause) {
		this(null, errors, messageSource, cause);
	}

	/**
	 * Constructor with Errors and a MessageSource and root cause.
	 *
	 * @param message
	 *        the message
	 * @param errors
	 *        the errors
	 * @param messageSource
	 *        the message source to use to resolve the Errors against
	 * @param cause
	 *        the causing exception
	 * @since 1.2
	 */
	public ValidationException(@Nullable String message, @Nullable Errors errors,
			@Nullable MessageSource messageSource, @Nullable Throwable cause) {
		super(message, cause);
		this.errors = errors;
		this.messageSource = messageSource;
	}

	public @Nullable Errors getErrors() {
		return errors;
	}

	public @Nullable MessageSource getMessageSource() {
		return messageSource;
	}

}
