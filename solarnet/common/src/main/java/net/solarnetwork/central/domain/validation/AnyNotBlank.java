/* ==================================================================
 * AnyNotBlank.java - 28 Sept 2026 2:48:31 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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
 * At least one element of a string array must be non-blank.
 *
 * <p>
 * Like {@link jakarta.validation.constraints.NotEmpty}, and unlike the other
 * constraints in this package, a {@code null} value is <b>not</b> valid: an
 * absent array has no non-blank element. Blank elements are allowed as long as
 * some element is non-blank, so this suits an array of optional lines where at
 * least one line is required, such as a street address.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD, ANNOTATION_TYPE })
@Constraint(validatedBy = AnyNotBlankValidator.class)
@Documented
public @interface AnyNotBlank {

	/**
	 * The associated error message.
	 *
	 * @return the message
	 */
	String message() default "At least one non-blank value is required";

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
