/* ==================================================================
 * UserSettingsValidator.java - 24/07/2022 8:59:24 am
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
import net.solarnetwork.central.ocpp.domain.UserSettings;

/**
 * Validator for {@link UserSettings} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class UserSettingsValidator implements Validator {

	/**
	 * The maximum length allowed for the {@code sourceIdTemplate} property.
	 */
	public static final int SOURCE_ID_TEMPLATE_MAX_LENGTH = 255;

	@Override
	public boolean supports(Class<?> clazz) {
		return UserSettings.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		UserSettings o = (UserSettings) target;
		if ( o.getUserId() == null ) {
			errors.rejectValue("userId", "ocpp.settings.error.userId.required",
					"The user ID is required.");
		}
		if ( o.getSourceIdTemplate() == null ) {
			errors.rejectValue("sourceIdTemplate", "ocpp.settings.error.sourceIdTemplate.required",
					"The source ID template is required.");
		} else if ( o.getSourceIdTemplate().trim().length() > SOURCE_ID_TEMPLATE_MAX_LENGTH ) {
			errors.rejectValue("sourceIdTemplate", "ocpp.settings.error.sourceIdTemplate.size",
					new Object[] { SOURCE_ID_TEMPLATE_MAX_LENGTH },
					"The source ID template is too long.");
		}
	}

}
