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

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import net.solarnetwork.central.datum.imp.biz.DatumImportBiz;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.domain.DatumImportRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;

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
	public DatumImportStatus submitDatumImportRequest(DatumImportRequest request,
			DatumImportResource resource) {
		return delegate.submitDatumImportRequest(request, resource);
	}

	@Override
	public DatumImportStatus datumImportJobStatusForUser(Long userId, UUID jobId) {
		return delegate.datumImportJobStatusForUser(userId, jobId);
	}

	@Override
	public Collection<DatumImportStatus> datumImportJobStatusesForUser(Long userId,
			Set<DatumImportState> states) {
		return delegate.datumImportJobStatusesForUser(userId, states);
	}

	@Override
	public DatumImportStatus updateDatumImportJobStatusForUser(Long userId, UUID jobId,
			DatumImportState desiredState, Set<DatumImportState> expectedStates) {
		return delegate.updateDatumImportJobStatusForUser(userId, jobId, desiredState, expectedStates);
	}

}
