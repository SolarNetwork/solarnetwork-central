/* ==================================================================
 * TestRegistrationValidator.java - Jan 7, 2010 11:19:28 AM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.user.test;

import net.solarnetwork.central.user.domain.User;

import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Validator for user registration.
 * 
 * @author matt
 * @version $Id$
 */
public class TestRegistrationValidator implements Validator {

	/**
	 * Validate a new registration.
	 * 
	 * @param reg the registration to validate
	 * @param context the message context
	 */
	public void validateStart(User reg, Errors errors) {
		if ( !StringUtils.hasText(reg.getEmail()) ) {
			errors.rejectValue("email", "registration.email.required", "Email is required.");
		} else if ( reg.getEmail().length() > 240 ) {
			errors.rejectValue("email", "registration.email.toolong", "Email value is too long.");
		}
		if ( !StringUtils.hasText(reg.getName()) ) {
			errors.rejectValue("name", "registration.name.required", "Name is required.");
		} else if ( reg.getName().length() > 128 ) {
			errors.rejectValue("name", "registration.name.toolong", "Name value is too long.");
		}
		if ( !StringUtils.hasText(reg.getPassword()) ) {
			errors.rejectValue("password", "registration.password.required", "Password is required.");
		}
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return User.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		User reg = (User)target;
		validateStart(reg, errors);
	}
	
}
