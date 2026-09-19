/* ==================================================================
 * PositiveTemporalAmount.java - 22/09/2025 11:24:00 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validate that a {@code TemporalAmount} has a positive value.
 * 
 * @author matt
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD, ANNOTATION_TYPE })
@Constraint(validatedBy = PositiveTemporalAmountValidator.class)
@Documented
public @interface PositiveTemporalAmount {

	/**
	 * The associated error message.
	 * 
	 * @return the message
	 */
	String message() default "Temporal amount must be positive";

	/**
	 * The validation groups.
	 * 
	 * @return the groups
	 */
	Class<?>[] groups() default {};

	/**
	 * The optional payloads.
	 * 
	 * @return the payloads
	 */
	Class<? extends Payload>[] payload() default {};

}
