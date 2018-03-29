/* ==================================================================
 * DatumExportStatus.java - 29/03/2018 5:56:23 PM
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

package net.solarnetwork.central.datum.domain.export;

import java.util.concurrent.Future;

/**
 * The status of a datum export job.
 * 
 * <p>
 * This API is also a {@link Future} so you can get the results of the export
 * when it finishes.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface DatumExportStatus extends Future<DatumExportResult> {

	/**
	 * Get a unique ID for this export job.
	 * 
	 * @return the unique ID of this export job
	 */
	String getJobId();

	/**
	 * Get the state of the export job.
	 * 
	 * @return the state, never {@literal null}
	 */
	DatumExportState getJobState();

	/**
	 * Get a percentage complete for the job overall.
	 * 
	 * @return a percentage complete, or {@literal -1} if not known
	 */
	double getPercentComplete();

}
