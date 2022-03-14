/* ==================================================================
 * DatumExportResult.java - 29/03/2018 5:55:32 PM
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

package net.solarnetwork.central.datum.export.domain;

import java.time.Instant;

/**
 * API for the result of a datum export job.
 * 
 * @author matt
 * @version 2.0
 * @since 1.23
 */
public interface DatumExportResult {

	/**
	 * Get a success flag.
	 * 
	 * @return the success flag
	 */
	boolean isSuccess();

	/**
	 * Get a message about the result.
	 * 
	 * <p>
	 * If {@link #isSuccess()} returns {@literal false}, this method will return
	 * a message about the error.
	 * </p>
	 * 
	 * @return a message
	 */
	String getMessage();

	/**
	 * Get the date the export task was completed.
	 * 
	 * @return the completion date
	 */
	Instant getCompletionDate();

}
