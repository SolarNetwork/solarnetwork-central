/* ==================================================================
 * NodeUsageType.java - 2/06/2021 6:59:49 AM
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
 * Enumeration of node usage types.
 *
 * @author matt
 * @version 1.4
 * @since 1.3
 */
public enum NodeUsageType implements NodeUsages {

	/** Datum properties added usage. */
	DatumPropsIn(1, DATUM_PROPS_IN_KEY),

	/** Datum queried/exported usage. */
	DatumOut(2, DATUM_OUT_KEY),

	/** Datum days stored usage. */
	DatumDaysStored(3, DATUM_DAYS_STORED_KEY),

	/** Instructions issued usage. */
	InstructionsIssued(4, INSTRUCTIONS_ISSUED_KEY),

	/** SolarFlux data in usage. */
	FluxDataIn(5, FLUX_DATA_IN_KEY),

	/** SolarFlux data out usage. */
	FluxDataOut(6, FLUX_DATA_OUT_KEY),

	/** OCPP Charger usage. */
	OcppChargers(7, OCPP_CHARGERS_KEY),

	/** OSCP Capacity Group usage. */
	OscpCapacityGroups(8, OSCP_CAPACITY_GROUPS_KEY),

	/** OSCP Capacity usage. */
	OscpCapacity(9, OSCP_CAPACITY_KEY),

	/** DNP3 Data Points usage. */
	Dnp3DataPoints(10, DNP3_DATA_POINTS_KEY),

	/** OAuth client credentials usage. */
	OAuthClientCredentials(11, OAUTH_CLIENT_CREDENTIALS_KEY),

	/** Cloud Integrations data usage. */
	CloudIntegrationsData(12, CLOUD_INTEGRATIONS_DATA_KEY),

	;

	private final int order;
	private final String key;

	NodeUsageType(int order, String key) {
		this.order = order;
		this.key = key;
	}

	/**
	 * Get the order.
	 *
	 * @return the order
	 */
	public int getOrder() {
		return order;
	}

	/**
	 * Get the key.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get an enumeration for a key value.
	 *
	 * @param key
	 *        the enumeration key
	 * @return the enumeration
	 * @throws IllegalArgumentException
	 *         if {@code key} is not valid
	 */
	public static NodeUsageType forKey(String key) {
		return switch (key) {
			case DATUM_PROPS_IN_KEY -> DatumPropsIn;
			case DATUM_OUT_KEY -> DatumOut;
			case DATUM_DAYS_STORED_KEY -> DatumDaysStored;
			case INSTRUCTIONS_ISSUED_KEY -> InstructionsIssued;
			case OCPP_CHARGERS_KEY -> OcppChargers;
			case OSCP_CAPACITY_GROUPS_KEY -> OscpCapacityGroups;
			case OSCP_CAPACITY_KEY -> OscpCapacity;
			case DNP3_DATA_POINTS_KEY -> Dnp3DataPoints;
			case FLUX_DATA_IN_KEY -> FluxDataIn;
			case FLUX_DATA_OUT_KEY -> FluxDataOut;
			case OAUTH_CLIENT_CREDENTIALS_KEY -> OAuthClientCredentials;
			case CLOUD_INTEGRATIONS_DATA_KEY -> CloudIntegrationsData;
			default -> throw new IllegalArgumentException("Unknown NodeUsageType key value: " + key);
		};
	}
}
