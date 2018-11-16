/* ==================================================================
 * DelegatingDatumImportBiz.java - 9/11/2018 4:43:03 PM
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

package net.solarnetwork.central.datum.imp.support;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Future;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumComponents;
import net.solarnetwork.central.datum.imp.biz.DatumImportBiz;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.domain.Configuration;
import net.solarnetwork.central.datum.imp.domain.DatumImportPreviewRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportReceipt;
import net.solarnetwork.central.datum.imp.domain.DatumImportRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.central.domain.FilterResults;

/**
 * Delegating implementation of {@link DatumImportBiz}, mostly to help with AOP.
 * 
 * @author matt
 * @version 1.0
 */
public class DelegatingDatumImportBiz implements DatumImportBiz {

	private final DatumImportBiz delegate;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingDatumImportBiz(DatumImportBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public Iterable<DatumImportInputFormatService> availableInputFormatServices() {
		return delegate.availableInputFormatServices();
	}

	@Override
	public DatumImportReceipt submitDatumImportRequest(DatumImportRequest request,
			DatumImportResource resource) throws IOException {
		return delegate.submitDatumImportRequest(request, resource);
	}

	@Override
	public Future<FilterResults<GeneralNodeDatumComponents>> previewStagedImportRequest(
			DatumImportPreviewRequest request) {
		return delegate.previewStagedImportRequest(request);
	}

	@Override
	public DatumImportStatus datumImportJobStatusForUser(Long userId, String jobId) {
		return delegate.datumImportJobStatusForUser(userId, jobId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.solarnetwork.central.datum.imp.biz.DatumImportBiz#
	 * updateDatumImportJobConfigurationForUser(java.lang.Long,
	 * java.lang.String,
	 * net.solarnetwork.central.datum.imp.domain.Configuration)
	 */
	@Override
	public DatumImportStatus updateDatumImportJobConfigurationForUser(Long userId, String jobId,
			Configuration configuration) {
		return delegate.updateDatumImportJobConfigurationForUser(userId, jobId, configuration);
	}

	@Override
	public Collection<DatumImportStatus> datumImportJobStatusesForUser(Long userId,
			Set<DatumImportState> states) {
		return delegate.datumImportJobStatusesForUser(userId, states);
	}

	@Override
	public DatumImportStatus updateDatumImportJobStateForUser(Long userId, String jobId,
			DatumImportState desiredState, Set<DatumImportState> expectedStates) {
		return delegate.updateDatumImportJobStateForUser(userId, jobId, desiredState, expectedStates);
	}

}
