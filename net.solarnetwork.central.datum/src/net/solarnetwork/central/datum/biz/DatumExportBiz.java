/* ==================================================================
 * DatumExportBiz.java - 5/03/2018 5:11:15 PM
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

package net.solarnetwork.central.datum.biz;

import net.solarnetwork.central.datum.domain.export.Configuration;
import net.solarnetwork.central.datum.domain.export.DatumExportStatus;

/**
 * API for a datum export service.
 * 
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public interface DatumExportBiz {

	/**
	 * Perform a datum export.
	 * 
	 * @param configuration
	 *        the full configuration for the export job
	 * @return a status
	 */
	DatumExportStatus performExport(Configuration configuration);

	/**
	 * Get the status for a running (or recently completed) export job.
	 * 
	 * <p>
	 * After requesting an export via {@link #performExport(Configuration)} the
	 * {@link DatumExportStatus#getJobId()} can be passed to this method to
	 * obtain the status of that job.
	 * </p>
	 * 
	 * @param jobId
	 * @return
	 */
	DatumExportStatus statusForJob(String jobId);

}
