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

package net.solarnetwork.central.in.biz.impl;

import net.solarnetwork.central.in.biz.NetworkIdentityBiz;
import net.solarnetwork.domain.BasicNetworkIdentity;
import net.solarnetwork.domain.NetworkIdentity;

/**
 * Simple implementation of {@link NetworkIdentityBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
public class SimpleNetworkIdentityBiz implements NetworkIdentityBiz {

	private String networkIdentityKey;
	private String termsOfService;
	
	@Override
	public NetworkIdentity getNetworkIdentity() {
		return new BasicNetworkIdentity(networkIdentityKey, termsOfService);
	}

	public void setNetworkIdentityKey(String networkIdentityKey) {
		this.networkIdentityKey = networkIdentityKey;
	}
	public void setTermsOfService(String termsOfService) {
		this.termsOfService = termsOfService;
	}

}
