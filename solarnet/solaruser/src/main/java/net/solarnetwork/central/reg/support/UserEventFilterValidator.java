/* ==================================================================
 * UserEventFilterValidator.java - 10/10/2022 7:16:32 am
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

package net.solarnetwork.central.reg.support;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import net.solarnetwork.central.common.dao.UserEventFilter;

/**
 * Validator for {@link UserEventFilter} input.
 * 
 * @author matt
 * @version 1.0
 */
public class UserEventFilterValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return UserEventFilter.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final UserEventFilter filter = (UserEventFilter) target;
		// some date range is required
		if ( !filter.hasDateRange() ) {
			errors.reject("error.filter.dateRange.required", "A date range is required.");
		}
	}

}
