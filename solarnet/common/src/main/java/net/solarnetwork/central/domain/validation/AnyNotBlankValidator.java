/* ==================================================================
 * AnyNotBlankValidator.java - 28 Sept 2026 2:51:07 pm
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

import org.jspecify.annotations.Nullable;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validate a string array contains at least one non-blank element.
 *
 * @author matt
 * @version 1.0
 */
public class AnyNotBlankValidator implements ConstraintValidator<AnyNotBlank, @Nullable String[]> {

	@Override
	public void initialize(final AnyNotBlank constraintAnnotation) {
	}

	@Override
	public boolean isValid(final @Nullable String @Nullable [] value,
			final ConstraintValidatorContext context) {
		if ( value == null ) {
			return false;
		}
		for ( String s : value ) {
			if ( s != null && !s.isBlank() ) {
				return true;
			}
		}
		return false;
	}

}
