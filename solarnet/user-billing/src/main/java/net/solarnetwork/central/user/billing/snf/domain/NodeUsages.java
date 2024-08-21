/* ==================================================================
 * NodeUsages.java - 2/06/2021 7:06:26 AM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.domain;

/**
 * Node usage constants.
 *
 * @author matt
 * @version 1.5
 */
public interface NodeUsages {

	/** A key to use for datum properties added usage. */
	String DATUM_PROPS_IN_KEY = "datum-props-in";

	/** A key to use for datum queried usage. */
	String DATUM_OUT_KEY = "datum-out";

	/** A key to use for datum days stored usage. */
	String DATUM_DAYS_STORED_KEY = "datum-days-stored";

	/** A key to use for instructions issued usage. */
	String INSTRUCTIONS_ISSUED_KEY = "instr-issued";

	/** A key to use for SolarFlux data in usage. */
	String FLUX_DATA_IN_KEY = "flux-data-in";

	/** A key to use for SolarFlux data out usage. */
	String FLUX_DATA_OUT_KEY = "flux-data-out";

	/** A key to use for OCPP Charger usage. */
	String OCPP_CHARGERS_KEY = "ocpp-chargers";

	/** A key to use for OSCP Capacity Group usage. */
	String OSCP_CAPACITY_GROUPS_KEY = "oscp-cap-groups";

	/** A key to use for OSCP capacity usage. */
	String OSCP_CAPACITY_KEY = "oscp-cap";

	/** A key to use for DNP3 data points usage. */
	String DNP3_DATA_POINTS_KEY = "dnp3-data-points";

	/** A key to use for OAuth client credentials usage. */
	String OAUTH_CLIENT_CREDENTIALS_KEY = "oauth-client-creds";

}
