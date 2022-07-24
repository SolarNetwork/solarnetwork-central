/* ==================================================================
 * CentralChargePointConnectorValidator.java - 24/07/2022 8:45:26 am
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
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.ocpp.domain.StatusNotification;

/**
 * Validator for {@link CentralChargePointConnector} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class CentralChargePointConnectorValidator implements Validator {

	/**
	 * The maximum length allowed for the {@code info.vendorId} property.
	 */
	public static final int VENDOR_ID_MAX_LENGTH = 255;

	/**
	 * The maximum length allowed for the {@code info.vendorErrorCode} property.
	 */
	public static final int VENDOR_ERROR_CODE_MAX_LENGTH = 50;

	@Override
	public boolean supports(Class<?> clazz) {
		return CentralChargePointConnector.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		CentralChargePointConnector o = (CentralChargePointConnector) target;
		if ( o.getUserId() == null ) {
			errors.rejectValue("userId", "ocpp.connectors.error.userId.required",
					"The user ID is required.");
		}
		if ( o.getChargePointId() == null ) {
			errors.rejectValue("chargePointId", "ocpp.connectors.error.chargePointId.required",
					"The charge point ID is required.");
		}
		if ( o.getConnectorId() == null ) {
			errors.rejectValue("connectorId", "ocpp.connectors.error.connectorId.required",
					"The ID is required.");
		}
		StatusNotification info = o.getInfo();
		if ( info == null ) {
			errors.rejectValue("info", "ocpp.connectors.error.info.required", "The info is required.");
		} else {
			if ( info.getVendorId() != null
					&& info.getVendorId().trim().length() > VENDOR_ID_MAX_LENGTH ) {
				errors.rejectValue("info.vendorId", "ocpp.connectors.error.info.vendorId.size",
						new Object[] { VENDOR_ID_MAX_LENGTH }, "The vendor ID is too long.");
			}
			if ( info.getVendorErrorCode() != null
					&& info.getVendorErrorCode().trim().length() > VENDOR_ERROR_CODE_MAX_LENGTH ) {
				errors.rejectValue("info.vendorErrorCode",
						"ocpp.connectors.error.info.vendorErrorCode.size",
						new Object[] { VENDOR_ERROR_CODE_MAX_LENGTH },
						"The vendor error code is too long.");
			}
		}
	}

}
