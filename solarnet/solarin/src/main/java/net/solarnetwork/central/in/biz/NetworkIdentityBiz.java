/* ==================================================================
 * NetworkIdentityBiz.java - Sep 13, 2011 7:08:59 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.in.biz;

import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkIdentity;

/**
 * API for identifying the SolarIn service to nodes.
 * 
 * @author matt
 * @version $Revision$
 */
public interface NetworkIdentityBiz {

	/**
	 * Get the public-facing network identity for this service.
	 * 
	 * <p>
	 * This is the information that should be publicly available for users to
	 * view, so they can validate this against the same info presented during
	 * node association.
	 * </p>
	 * 
	 * @return identity key
	 */
	NetworkIdentity getNetworkIdentity();

	/**
	 * Get a network association for a given username and confirmation key.
	 * 
	 * @param username
	 *        the username
	 * @param confirmationKey
	 *        the confirmation key
	 * @return the association, or <em>null</em> if not available
	 */
	NetworkAssociation getNetworkAssociation(String username, String confirmationKey);

}
