/* ==================================================================
 * DelegatingNodeOwnershipBiz.java - Apr 20, 2015 8:18:18 PM
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.biz.NodeOwnershipBiz;

/**
 * Delegating implementation of {@link NodeOwnershipBiz}, mostly to help with
 * AOP.
 * 
 * @author matt
 * @version 1.0
 */
public class DelegatingNodeOwnershipBiz implements NodeOwnershipBiz {

	private final NodeOwnershipBiz delegate;

	/**
	 * Construct with a delegate.
	 * 
	 * @param delegate
	 *        The delegate.
	 */
	public DelegatingNodeOwnershipBiz(NodeOwnershipBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public String requestNodeOwnershipTransfer(Long userId, Long nodeId, String newOwnerEmail)
			throws AuthorizationException {
		return delegate.requestNodeOwnershipTransfer(userId, nodeId, newOwnerEmail);
	}

	@Override
	public void cancelNodeOwnershipTransfer(Long userId, Long nodeId) throws AuthorizationException {
		delegate.cancelNodeOwnershipTransfer(userId, nodeId);
	}

	@Override
	public void confirmNodeOwnershipTransfer(String confirmationCode, boolean accept)
			throws AuthorizationException {
		delegate.confirmNodeOwnershipTransfer(confirmationCode, accept);
	}

}
