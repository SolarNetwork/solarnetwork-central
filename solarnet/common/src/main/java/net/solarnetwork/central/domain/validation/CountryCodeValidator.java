/* ==================================================================
 * CountryCodeValidator.java - 27 Sept 2026 4:36:29 pm
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

import java.util.Locale;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validate a country code.
 * 
 * @author matt
 * @version 1.0
 */
public class CountryCodeValidator implements ConstraintValidator<ValidCountryCode, String> {

	@Override
	public void initialize(final ValidCountryCode constraintAnnotation) {
	}

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		if ( value == null ) {
			return true;
		}
		for ( String c : Locale.getISOCountries() ) {
			if ( c.equals(value) ) {
				return true;
			}
		}
		return false;
	}

}
