/* ==================================================================
 * StartStopClaimableJobStateValidator.java - 12/10/2024 7:38:10 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.solarnetwork.central.domain.BasicClaimableJobState;

/**
 * Validate the value is one of the user-settable start (Queued) and stop
 * (Completed) states.
 * 
 * @author matt
 * @version 1.0
 */
public class StartStopClaimableJobStateValidator
		implements ConstraintValidator<StartStopClaimableJobState, BasicClaimableJobState> {

	/**
	 * Constructor.
	 */
	public StartStopClaimableJobStateValidator() {
		super();
	}

	@Override
	public boolean isValid(BasicClaimableJobState value, ConstraintValidatorContext context) {
		if ( value == null ) {
			return true;
		}
		return switch (value) {
			case Queued, Completed -> true;
			default -> false;
		};
	}

}
