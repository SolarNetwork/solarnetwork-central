/* ==================================================================
 * PositiveTemporalAmountValidator.java - 22/09/2025 11:25:36 am
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

import java.time.temporal.TemporalAmount;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validate that a {@link TemporalAmount}
 * 
 * @author matt
 * @version 1.0
 */
public class PositiveTemporalAmountValidator
		implements ConstraintValidator<PositiveTemporalAmount, TemporalAmount> {

	/**
	 * Constructor.
	 */
	public PositiveTemporalAmountValidator() {
		super();
	}

	@Override
	public boolean isValid(TemporalAmount value, ConstraintValidatorContext context) {
		if ( value == null ) {
			return false;
		}
		var units = value.getUnits();
		if ( units == null || units.isEmpty() ) {
			return false;
		}
		for ( var unit : units ) {
			if ( value.get(unit) > 0 ) {
				return true;
			}
		}
		return false;
	}

}
