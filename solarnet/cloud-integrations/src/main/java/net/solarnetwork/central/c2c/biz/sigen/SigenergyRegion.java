/* ==================================================================
 * SigenergyRegion.java - 5/12/2025 3:10:08 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.sigen;

/**
 * Enumeration of Sigenergy service regions.
 *
 * @author matt
 * @version 1.0
 */
public enum SigenergyRegion {

	/** Australia and New Zealand. */
	AustraliaNewZealand("aus"),

	/** Europe. */
	Europe("eu"),

	/** Asia Pacific and Middle Asia. */
	AsiaPacificMiddleAsia("apac"),

	/** China. */
	China("cn"),

	/** Latin America. */
	LatinAmerica("us"),

	/** Middle East and Africa. */
	MiddleEastAfrica("eu"),

	/** North America. */
	NorthAmerica("us"),

	/** Japan. */
	Japan("jp"),

	;

	private final String key;

	private SigenergyRegion(String key) {
		this.key = key;
	}

	/**
	 * Get the key.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

}
