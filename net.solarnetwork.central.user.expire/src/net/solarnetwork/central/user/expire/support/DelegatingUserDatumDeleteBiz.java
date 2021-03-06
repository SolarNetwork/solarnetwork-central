/* ==================================================================
 * DelegatingUserDatumDeleteBiz.java - 24/11/2018 8:45:34 PM
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

package net.solarnetwork.central.user.expire.support;

import java.util.Collection;
import java.util.Set;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.user.expire.biz.UserDatumDeleteBiz;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobInfo;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobState;

/**
 * Delegating implementation of {@link UserDatumDeleteBiz}, mostly to help with
 * AOP.
 * 
 * @author matt
 * @version 1.0
 */
public class DelegatingUserDatumDeleteBiz implements UserDatumDeleteBiz {

	private final UserDatumDeleteBiz delegate;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingUserDatumDeleteBiz(UserDatumDeleteBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public DatumRecordCounts countDatumRecords(GeneralNodeDatumFilter filter) {
		return delegate.countDatumRecords(filter);
	}

	@Override
	public DatumDeleteJobInfo submitDatumDeleteRequest(GeneralNodeDatumFilter request) {
		return delegate.submitDatumDeleteRequest(request);
	}

	@Override
	public DatumDeleteJobInfo datumDeleteJobForUser(Long userId, String jobId) {
		return delegate.datumDeleteJobForUser(userId, jobId);
	}

	@Override
	public Collection<DatumDeleteJobInfo> datumDeleteJobsForUser(Long userId,
			Set<DatumDeleteJobState> states) {
		return delegate.datumDeleteJobsForUser(userId, states);
	}

}
