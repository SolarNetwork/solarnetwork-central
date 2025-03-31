/* ==================================================================
 * SmaPeriod.java - 31/03/2025 9:22:02 am
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

package net.solarnetwork.central.c2c.biz.impl;

/**
 * Enumeration of SMA query periods.
 *
 * @author matt
 * @version 1.0
 */
public enum SmaPeriod {

	/**
	 * The most recent values for the requested measurement set that has been
	 * transmitted from the device(s) to the backend.
	 */
	Recent,

	/**
	 * The highest-resolution values available for the requested day and
	 * measurement set.
	 */
	Day,

	/**
	 * The highest-resolution values available for the requested week and
	 * measurement set.
	 */
	Week,

	/** The day-values for the requested month and measurement set. */
	Month,

	/** The month-values for the requested year and measurement set. */
	Year,

	/** The year-values for the requested month and measurement set. */
	Total,

	;

	/**
	 * Get the key.
	 *
	 * @return the key, never {@literal null}
	 */
	public final String getKey() {
		return name();
	}

}
