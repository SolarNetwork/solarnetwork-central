/* ==================================================================
 * DelegatingUserOcppBiz.java - 29/02/2020 8:27:01 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.ocpp.support;

import java.util.Collection;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.central.user.ocpp.biz.UserOcppBiz;

/**
 * Delegating implementation of {@link UserOcppBiz}, mostly to help with AOP.
 * 
 * @author matt
 * @version 1.0
 */
public class DelegatingUserOcppBiz implements UserOcppBiz {

	private final UserOcppBiz delegate;

	/**
	 * Construct with a delegate;
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingUserOcppBiz(UserOcppBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public Collection<CentralSystemUser> systemUsersForUser(Long userId) {
		return delegate.systemUsersForUser(userId);
	}

	@Override
	public CentralSystemUser saveSystemUser(CentralSystemUser systemUser) {
		return delegate.saveSystemUser(systemUser);
	}

	@Override
	public Collection<CentralChargePoint> chargePointsForUser(Long userId) {
		return delegate.chargePointsForUser(userId);
	}

	@Override
	public CentralChargePoint saveChargePoint(CentralChargePoint chargePoint) {
		return delegate.saveChargePoint(chargePoint);
	}

}
