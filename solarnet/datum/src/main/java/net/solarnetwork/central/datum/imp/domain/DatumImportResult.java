/* ==================================================================
 * DatumImportResult.java - 7/11/2018 7:22:06 AM
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
 * API for the result of a datum import job.
 * 
 * @author matt
 * @version 2.0
 */
public interface DatumImportResult {

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
	 * Get the date the import job was completed.
	 * 
	 * @return the completion date
	 */
	Instant getCompletionDate();

	/**
	 * Get the number of datum successfully loaded.
	 * 
	 * <p>
	 * Note that even if {@link #isSuccess()} is {@literal false} this method
	 * can return a value greater than {@literal 0}, if partial results are
	 * supported by the transaction mode of the import process.
	 * </p>
	 * 
	 * @return the number of successfully loaded datum
	 */
	long getLoadedCount();

}
