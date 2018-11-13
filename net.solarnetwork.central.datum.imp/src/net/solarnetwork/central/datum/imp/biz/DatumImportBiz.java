/* ==================================================================
 * DatumImportBiz.java - 6/11/2018 4:05:56 PM
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

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Future;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumComponents;
import net.solarnetwork.central.datum.imp.domain.Configuration;
import net.solarnetwork.central.datum.imp.domain.DatumImportPreviewRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportReceipt;
import net.solarnetwork.central.datum.imp.domain.DatumImportRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.central.domain.FilterResults;

/**
 * API for a datum import service.
 * 
 * @author matt
 * @version 1.0
 */
public interface DatumImportBiz {

	/**
	 * Get a list of all available input format services.
	 * 
	 * @return the available services, never {@literal null}
	 */
	Iterable<DatumImportInputFormatService> availableInputFormatServices();

	/**
	 * Submit an import datum request.
	 * 
	 * <p>
	 * The import process is not expected to start after calling this method.
	 * Rather it should enter the {@link DatumImportState#Queued} state. To
	 * initiate the import process, the {@link #performImport(Long, String)}
	 * must be called, passing in the same user ID and the returned
	 * {@link DatumImportStatus#getJobId()}.
	 * </p>
	 * 
	 * @param request
	 *        the request
	 * @param resource
	 *        the resource
	 * @return the receipt
	 * @throws IOException
	 *         if any IO error occurs
	 */
	DatumImportReceipt submitDatumImportRequest(DatumImportRequest request, DatumImportResource resource)
			throws IOException;

	/**
	 * Preview a staged import request.
	 * 
	 * <p>
	 * This method can only be called after a job ID has been returned from a
	 * previous call to
	 * {@link #submitDatumImportRequest(DatumImportRequest, DatumImportResource)},
	 * and only if the request's {@link Configuration#isStage()} was
	 * {@literal true}.
	 * </p>
	 * 
	 * @param request
	 *        the request details
	 * @return a sample of datum extracted from the import request data, never
	 *         {@literal null}
	 */
	Future<FilterResults<GeneralNodeDatumComponents>> previewStagedImportRequest(
			DatumImportPreviewRequest request);

	/**
	 * Get the status of a specific job.
	 * 
	 * @param userId
	 *        the user ID that owns the job
	 * @param jobId
	 *        the ID of the job to get
	 * @return the job status, or {@literal null} if not available
	 */
	DatumImportStatus datumImportJobStatusForUser(Long userId, String jobId);

	/**
	 * Find all available job statuses for a specific user.
	 * 
	 * @param userId
	 *        the ID of the user to find the job statuses for
	 * @param states
	 *        the specific states to limit the results to, or {@literal null}
	 *        for all states
	 * @return the job statuses, never {@literal null}
	 */
	Collection<DatumImportStatus> datumImportJobStatusesForUser(Long userId,
			Set<DatumImportState> states);

	/**
	 * Update the state of a specific job.
	 * 
	 * @param userId
	 *        the user ID that owns the job
	 * @param jobId
	 *        the ID of the job to update
	 * @param desiredState
	 *        the state to change the job to
	 * @param expectedStates
	 *        a set of states that must include the job's current state in order
	 *        to change it to {@code desiredState}, or {@literal null} if the
	 *        current state of the job does not matter
	 * @return the job status, or {@literal null} if not available
	 */
	DatumImportStatus updateDatumImportJobStateForUser(Long userId, String jobId,
			DatumImportState desiredState, Set<DatumImportState> expectedStates);

}
