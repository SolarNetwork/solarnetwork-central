/* ==================================================================
 * DelegatingUserMetadataBiz.java - 11/11/2016 5:05:42 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.support;

import java.util.List;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.user.biz.UserMetadataBiz;
import net.solarnetwork.central.user.domain.UserMetadataFilter;
import net.solarnetwork.central.user.domain.UserMetadataFilterMatch;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Delegating implementation of {@link UserMetadataBiz}, mostly to help with
 * AOP.
 * 
 * @author matt
 * @version 2.0
 * @since 1.23
 */
public class DelegatingUserMetadataBiz implements UserMetadataBiz {

	private final UserMetadataBiz delegate;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        The delegate to use.
	 */
	public DelegatingUserMetadataBiz(UserMetadataBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public void addUserMetadata(Long userId, GeneralDatumMetadata meta) {
		delegate.addUserMetadata(userId, meta);
	}

	@Override
	public void storeUserMetadata(Long userId, GeneralDatumMetadata meta) {
		delegate.storeUserMetadata(userId, meta);
	}

	@Override
	public void removeUserMetadata(Long userId) {
		delegate.removeUserMetadata(userId);
	}

	@Override
	public FilterResults<UserMetadataFilterMatch> findUserMetadata(UserMetadataFilter criteria,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		return delegate.findUserMetadata(criteria, sortDescriptors, offset, max);
	}

}
