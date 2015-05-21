/* ==================================================================
 * CentralTestConstants.java - 18/05/2015 9:35:21 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.test;

/**
 * Base class for SolarNet unit tests.
 * 
 * @author matt
 * @version 1.0
 */
public interface CentralTestConstants {

	/** A test Node ID. */
	Long TEST_NODE_ID = -1L;

	/** A test Weather Source ID. */
	Long TEST_WEATHER_SOURCE_ID = -1L;

	/** A test Weather Source name. */
	String TEST_WEATHER_SOURCE_NAME = "Test weather source";

	/** A test Price Source ID. */
	Long TEST_PRICE_SOURCE_ID = -1L;

	/** A test Price Source name. */
	String TEST_PRICE_SOURCE_NAME = "Test price source";

	/** A test Location ID. */
	Long TEST_LOC_ID = -1L;

	/** A test location country. */
	String TEST_LOC_COUNTRY = "NZ";

	/** A test location name */
	String TEST_LOC_NAME = "Test Location";

	/** A test location region */
	String TEST_LOC_REGION = "Wellington";

	/** A test location postal code */
	String TEST_LOC_POSTAL_CODE = "6011";

	/** A test weather Location ID. */
	Long TEST_WEATHER_LOC_ID = -1L;

	/** A test hardware ID. */
	Long TEST_HARDWARE_ID = -1L;

	/** A test hardware manufacturer. */
	String TEST_HARDWARE_MANUFACTURER = "Test Manufacturer";

	/** A test hardware model. */
	String TEST_HARDWARE_MODEL = "Test Model";

	/** A test hardware control ID. */
	Long TEST_HARDWARE_CONTROL_ID = -1L;

	/** A test TimeZone ID. */
	String TEST_TZ = "Pacific/Auckland";

	/** A test currency. */
	String TEST_CURRENCY = "NZD";

}
