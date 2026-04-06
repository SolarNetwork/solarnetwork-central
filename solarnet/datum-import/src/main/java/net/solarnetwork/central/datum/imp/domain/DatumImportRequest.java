/* ==================================================================
 * DatumImportRequest.java - 7/11/2018 11:07:39 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.imp.domain;

import java.time.Instant;

/**
 * API for an import request.
 * 
 * @author matt
 * @version 2.0
 */
public interface DatumImportRequest {

	/**
	 * Get the configuration of the import.
	 * 
	 * @return the configuration
	 */
	Configuration getConfiguration();

	/**
	 * Get the user ID that initiated the import request.
	 * 
	 * @return the user ID
	 */
	Long getUserId();

	/**
	 * Get the data import starting date.
	 * 
	 * @return the import date
	 */
	Instant getImportDate();

}
