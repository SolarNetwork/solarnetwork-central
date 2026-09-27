/* ==================================================================
 * SnFeatureEntitlement.java - 26 Sept 2026 7:06:30 pm
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.account.domain;

/**
 * An enumeration of SolarNetwork optional features that are enabled with a
 * granted entitlement (such as a security role).
 * 
 * <p>
 * These are typically granted once a user creates an account and subscribes to
 * SolarNetwork invoicing.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public enum SnFeatureEntitlement {

	/** Access to Cloud Integrations. */
	CLOUD_INTEGRATIONS("ROLE_CLOUD_INTEGRATIONS"),

	/** Access to SolarDIN dynamic datum input. */
	DATUM_INPUT_ENDPOINTS("ROLE_DATUM_INPUT"),

	/** Access to SolarDNP3. */
	DNP3("ROLE_DNP3"),

	/*- Access to event hooks. */
	// TODO: these might be removed EVENT_HOOKS("ROLE_EVENT"),

	/** Access to datum export. */
	DATUM_EXPORT("ROLE_EXPORT"),

	/** Access to datum import. */
	DATUM_IMPORT("ROLE_IMPORT"),

	/** Access to SolarDIN dynamic instruction input. */
	INSTRUCTION_INPUT_ENDPOINTS("ROLE_INSTRUCTION_INPUT"),

	/** Access to SolarOCPP. */
	OCPP("ROLE_OCPP"),

	/** Access to SolarOSCP. */
	OSCP("ROLE_OSCP"),

	;

	private final String roleName;

	private SnFeatureEntitlement(String roleName) {
		this.roleName = roleName;
	}

	/**
	 * Get the role name associated with this entitlement.
	 * 
	 * @return the role name
	 */
	public final String getRoleName() {
		return roleName;
	}

}
