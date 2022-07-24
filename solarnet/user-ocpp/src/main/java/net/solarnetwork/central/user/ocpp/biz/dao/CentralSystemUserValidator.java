/* ==================================================================
 * CentralSystemUserValidator.java - 24/07/2022 8:59:24 am
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

package net.solarnetwork.central.user.ocpp.biz.dao;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;

/**
 * Validator for {@link CentralSystemUser} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class CentralSystemUserValidator implements Validator {

	/**
	 * The maximum length allowed for the {@code username} property.
	 */
	public static final int USERNAME_MAX_LENGTH = 64;

	@Override
	public boolean supports(Class<?> clazz) {
		return CentralSystemUser.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		CentralSystemUser o = (CentralSystemUser) target;
		if ( o.getUserId() == null ) {
			errors.rejectValue("userId", "ocpp.credentials.error.userId.required",
					"The user ID is required.");
		}
		if ( o.getUsername() == null ) {
			errors.rejectValue("username", "ocpp.credentials.error.username.required",
					"The username is required.");
		} else if ( o.getUsername().trim().length() > USERNAME_MAX_LENGTH ) {
			errors.rejectValue("username", "ocpp.credentials.error.username.size",
					new Object[] { USERNAME_MAX_LENGTH }, "The username is too long.");
		}
		if ( o.getPassword() == null ) {
			errors.rejectValue("password", "ocpp.credentials.error.password.required",
					"The password is required.");
		}
	}

}
