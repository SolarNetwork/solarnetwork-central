/* ==================================================================
 * SimpleNetworkIdentityBiz.java - Sep 13, 2011 7:48:17 PM
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

package net.solarnetwork.central.in.biz.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.biz.NetworkIdentificationBiz;
import net.solarnetwork.central.dao.NetworkAssociationDao;
import net.solarnetwork.central.in.biz.NetworkIdentityBiz;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkIdentity;

/**
 * Simple implementation of {@link NetworkIdentityBiz}.
 * 
 * @author matt
 * @version 2.0
 */
public class SimpleNetworkIdentityBiz implements NetworkIdentityBiz {

	private final NetworkIdentificationBiz networkIdentificationBiz;
	private final NetworkAssociationDao networkAssociationDao;

	/**
	 * Constructor.
	 * 
	 * @param networkIdentificationBiz
	 *        the identification biz
	 * @param networkAssociationDao
	 *        the association DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleNetworkIdentityBiz(NetworkIdentificationBiz networkIdentificationBiz,
			NetworkAssociationDao networkAssociationDao) {
		super();
		this.networkIdentificationBiz = requireNonNullArgument(networkIdentificationBiz,
				"networkIdentificationBiz");
		this.networkAssociationDao = requireNonNullArgument(networkAssociationDao,
				"networkAssociationDao");
	}

	@Override
	public NetworkIdentity getNetworkIdentity() {
		return networkIdentificationBiz.getNetworkIdentity();
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.SUPPORTS)
	public NetworkAssociation getNetworkAssociation(String username, String confirmationKey) {
		return networkAssociationDao.getNetworkAssociationForConfirmationKey(username, confirmationKey);
	}

}
