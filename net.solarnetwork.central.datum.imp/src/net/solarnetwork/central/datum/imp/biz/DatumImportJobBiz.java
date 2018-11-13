/* ==================================================================
 * DatumImportJobBiz.java - 13/11/2018 2:19:38 PM
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

package net.solarnetwork.central.datum.imp.biz;

import net.solarnetwork.central.datum.imp.domain.DatumImportRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;

/**
 * Service API for operations related to datum import jobs.
 * 
 * <p>
 * This API is meant more for internal use by import scheduling jobs.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface DatumImportJobBiz {

	/**
	 * Perform a datum import.
	 * 
	 * <p>
	 * This method can only be called after a job ID has been returned from a
	 * previous call to
	 * {@link DatumImportBiz#submitDatumImportRequest(DatumImportRequest, DatumImportResource)}.
	 * </p>
	 * 
	 * @param userId
	 *        the user ID that owns the job
	 * @param jobId
	 *        the ID of the job to get
	 * @return the job status, or {@literal null} if the job is not available
	 */
	DatumImportStatus performImport(Long userId, String jobId);

}
