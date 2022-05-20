/* ==================================================================
 * LocationRequestInfoValidator.java - 20/05/2022 1:23:50 pm
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

package net.solarnetwork.central.reg.web.domain;

import static org.springframework.validation.ValidationUtils.rejectIfEmptyOrWhitespace;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import net.solarnetwork.central.domain.LocationRequestInfo;
import net.solarnetwork.domain.Location;

/**
 * Validation for {@link LocationRequestInfo} input.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
@Component
@Qualifier(LocationRequestInfoValidator.LOCATION_REQUEST_INFO)
public class LocationRequestInfoValidator implements SmartValidator {

	/** The qualifier for this validator. */
	public static final String LOCATION_REQUEST_INFO = "locationRequestInfo";

	@Override
	public boolean supports(Class<?> clazz) {
		return LocationRequestInfo.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		validate(target, errors, (Object[]) null);
	}

	@Override
	public void validate(Object target, Errors errors, Object... validationHints) {
		LocationRequestInfo info = (LocationRequestInfo) target;
		rejectIfEmptyOrWhitespace(errors, "sourceId", "validation.property.required",
				new Object[] { "sourceId" });

		rejectIfEmptyOrWhitespace(errors, "features", "validation.property.required",
				new Object[] { "features" });

		Location loc = info.getLocation();
		if ( loc == null ) {
			errors.rejectValue("location", "validation.property.required", new Object[] { "location" },
					null);
			return;
		}
		rejectIfEmptyOrWhitespace(errors, "location.name", "validation.property.required",
				new Object[] { "location.name" });
		rejectIfEmptyOrWhitespace(errors, "location.country", "validation.property.required",
				new Object[] { "location.country" });
		rejectIfEmptyOrWhitespace(errors, "location.timeZoneId", "validation.property.required",
				new Object[] { "location.zone" });
		rejectIfEmptyOrWhitespace(errors, "location.stateOrProvince", "validation.property.required",
				new Object[] { "location.stateOrProvince" });
		rejectIfEmptyOrWhitespace(errors, "location.locality", "validation.property.required",
				new Object[] { "location.locality" });
	}

}
