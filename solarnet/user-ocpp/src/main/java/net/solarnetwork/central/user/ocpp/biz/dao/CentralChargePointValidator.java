/* ==================================================================
 * CentralChargePointValidator.java - 24/07/2022 8:19:01 am
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
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointInfo;

/**
 * Validator for {@link CentralChargePoint} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class CentralChargePointValidator implements Validator {

	/**
	 * The maximum length allowed for the {@code info.id} property.
	 */
	public static final int IDENTIFIER_MAX_LENGTH = 48;

	/**
	 * The maximum length allowed for the {@code info.chargePointVendor}
	 * property.
	 */
	public static final int CHARGE_POINT_VENDOR_MAX_LENGTH = 20;

	/**
	 * The maximum length allowed for the {@code info.chargePointModel}
	 * property.
	 */
	public static final int CHARGE_POINT_MODEL_MAX_LENGTH = 20;

	/**
	 * The maximum length allowed for the {@code info.chargePointSerialNumber}
	 * property.
	 */
	public static final int CHARGE_POINT_SERIAL_NUM_MAX_LENGTH = 25;

	/**
	 * The maximum length allowed for the {@code info.chargeBoxSerialNumber}
	 * property.
	 */
	public static final int CHARGE_BOX_SERIAL_NUM_MAX_LENGTH = 25;

	/**
	 * The maximum length allowed for the {@code info.firmwareVersion} property.
	 */
	public static final int FIRMWARE_VERSION_MAX_LENGTH = 50;

	/**
	 * The maximum length allowed for the {@code info.iccid} property.
	 */
	public static final int ICCID_MAX_LENGTH = 20;

	/**
	 * The maximum length allowed for the {@code info.imsi} property.
	 */
	public static final int IMSI_MAX_LENGTH = 20;

	/**
	 * The maximum length allowed for the {@code info.meterType} property.
	 */
	public static final int METER_TYPE_MAX_LENGTH = 25;

	/**
	 * The maximum length allowed for the {@code info.meterSerialNumber}
	 * property.
	 */
	public static final int METER_SERIAL_NUM_MAX_LENGTH = 25;

	@Override
	public boolean supports(Class<?> clazz) {
		return CentralChargePoint.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		CentralChargePoint o = (CentralChargePoint) target;
		if ( o.getUserId() == null ) {
			errors.rejectValue("userId", "ocpp.chargers.error.userId.required",
					"The user ID is required.");
		}
		if ( o.getNodeId() == null ) {
			errors.rejectValue("nodeId", "ocpp.chargers.error.nodeId.required",
					"The node ID is required.");
		}
		final ChargePointInfo info = o.getInfo();
		if ( info == null ) {
			errors.rejectValue("info", "ocpp.chargers.error.info.required", "The info is required.");
		} else {
			if ( info.getId() == null ) {
				errors.rejectValue("info.id", "ocpp.chargers.error.info.id.required",
						"The info ID is required.");
			} else if ( info.getId().trim().length() > IDENTIFIER_MAX_LENGTH ) {
				errors.rejectValue("info.id", "ocpp.chargers.error.info.id.size",
						new Object[] { IDENTIFIER_MAX_LENGTH }, "The ID is too long.");
			}
			if ( info.getChargePointVendor() == null ) {
				errors.rejectValue("info.chargePointVendor",
						"ocpp.chargers.error.info.chargePointVendor.required",
						"The info charge point vendor is required.");
			} else if ( info.getChargePointVendor().trim().length() > CHARGE_POINT_VENDOR_MAX_LENGTH ) {
				errors.rejectValue("info.chargePointVendor",
						"ocpp.chargers.error.info.chargePointVendor.size",
						new Object[] { CHARGE_POINT_VENDOR_MAX_LENGTH },
						"The charge point vendor is too long.");
			}
			if ( info.getChargePointModel() == null ) {
				errors.rejectValue("info.chargePointModel",
						"ocpp.chargers.error.info.chargePointModel.required",
						"The info charge point model is required.");
			} else if ( info.getChargePointModel().trim().length() > CHARGE_POINT_MODEL_MAX_LENGTH ) {
				errors.rejectValue("info.chargePointModel",
						"ocpp.chargers.error.info.chargePointModel.size",
						new Object[] { CHARGE_POINT_MODEL_MAX_LENGTH },
						"The charge point model is too long.");
			}
			if ( info.getChargePointSerialNumber() != null && info.getChargePointSerialNumber().trim()
					.length() > CHARGE_POINT_SERIAL_NUM_MAX_LENGTH ) {
				errors.rejectValue("info.chargePointSerialNumber",
						"ocpp.chargers.error.info.chargePointSerialNumber.size",
						new Object[] { CHARGE_POINT_SERIAL_NUM_MAX_LENGTH },
						"The charge point serial number is too long.");
			}
			if ( info.getChargeBoxSerialNumber() != null && info.getChargeBoxSerialNumber().trim()
					.length() > CHARGE_BOX_SERIAL_NUM_MAX_LENGTH ) {
				errors.rejectValue("info.chargeBoxSerialNumber",
						"ocpp.chargers.error.info.chargeBoxSerialNumber.size",
						new Object[] { CHARGE_BOX_SERIAL_NUM_MAX_LENGTH },
						"The charge box serial number is too long.");
			}
			if ( info.getFirmwareVersion() != null
					&& info.getFirmwareVersion().trim().length() > FIRMWARE_VERSION_MAX_LENGTH ) {
				errors.rejectValue("info.firmwareVersion",
						"ocpp.chargers.error.info.firmwareVersion.size",
						new Object[] { FIRMWARE_VERSION_MAX_LENGTH },
						"The firmware version is too long.");
			}
			if ( info.getIccid() != null && info.getIccid().trim().length() > ICCID_MAX_LENGTH ) {
				errors.rejectValue("info.iccid", "ocpp.chargers.error.info.iccid.size",
						new Object[] { ICCID_MAX_LENGTH }, "The ICCID is too long.");
			}
			if ( info.getImsi() != null && info.getImsi().trim().length() > IMSI_MAX_LENGTH ) {
				errors.rejectValue("info.imsi", "ocpp.chargers.error.info.imsi.size",
						new Object[] { IMSI_MAX_LENGTH }, "The IMSI is too long.");
			}
			if ( info.getMeterType() != null
					&& info.getMeterType().trim().length() > METER_TYPE_MAX_LENGTH ) {
				errors.rejectValue("info.meterType", "ocpp.chargers.error.info.meterType.size",
						new Object[] { METER_TYPE_MAX_LENGTH }, "The meter type is too long.");
			}
			if ( info.getMeterSerialNumber() != null
					&& info.getMeterSerialNumber().trim().length() > METER_SERIAL_NUM_MAX_LENGTH ) {
				errors.rejectValue("info.meterSerialNumber",
						"ocpp.chargers.error.info.meterSerialNumber.size",
						new Object[] { METER_SERIAL_NUM_MAX_LENGTH },
						"The meter serial number is too long.");
			}
		}
	}

}
