/* ==================================================================
 * CentralAuthorizationValidator.java - 24/07/2022 7:57:08 am
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
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;

/**
 * Validator for {@link CentralAuthorization} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class CentralAuthorizationValidator implements Validator {

	/** The maximum length allowed for the {@code token} property. */
	public static final int TOKEN_MAX_LENGTH = 20;

	@Override
	public boolean supports(Class<?> clazz) {
		return CentralAuthorization.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		CentralAuthorization o = (CentralAuthorization) target;
		if ( o.getUserId() == null ) {
			errors.rejectValue("userId", "ocpp.auths.error.userId.required", "The user ID is required.");
		}
		if ( o.getToken() == null ) {
			errors.rejectValue("token", "ocpp.auths.error.token.required", "The token is required.");
		} else if ( o.getToken().trim().length() > TOKEN_MAX_LENGTH ) {
			errors.rejectValue("token", "ocpp.auths.error.token.size", new Object[] { TOKEN_MAX_LENGTH },
					"The token is too long.");
		}
	}

}
