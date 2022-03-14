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

import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;

/**
 * Exception for validation errors.
 * 
 * @author matt
 * @version 1.1
 */
public class ValidationException extends RuntimeException {

	private static final long serialVersionUID = -40848031815846620L;

	private final Errors errors;
	private final MessageSource messageSource;

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
	public ValidationException(Errors errors) {
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
	public ValidationException(Errors errors, MessageSource messageSource) {
		this(errors, messageSource, null);
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
	public ValidationException(Errors errors, MessageSource messageSource, Throwable cause) {
		super(cause);
		this.errors = errors;
		this.messageSource = messageSource;
	}

	public Errors getErrors() {
		return errors;
	}

	public MessageSource getMessageSource() {
		return messageSource;
	}

}
