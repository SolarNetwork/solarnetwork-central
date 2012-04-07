/* ==================================================================
 * SimpleAlertProcessingResult.java - Jun 19, 2011 2:51:50 PM
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

package net.solarnetwork.central.dras.support;

import java.util.Set;

import net.solarnetwork.central.domain.Identity;
import net.solarnetwork.central.dras.biz.AlertBiz.Alert;
import net.solarnetwork.central.dras.biz.AlertBiz.AlertProcessingResult;

/**
 * Simple implementation of {@link AlertProcessingResult}.
 * 
 * @author matt
 * @version $Revision$
 */
public class SimpleAlertProcessingResult implements AlertProcessingResult {

	private static final long serialVersionUID = -7430590866070895342L;

	private final Alert alert;
	private Set<Identity<Long>> alertedUsers;
	
	public SimpleAlertProcessingResult(Alert alert) {
		this.alert = alert;
	}
	
	@Override
	public Alert getAlert() {
		return alert;
	}

	@Override
	public Set<Identity<Long>> getAlertedUsers() {
		return alertedUsers;
	}

	public void setAlertedUsers(Set<Identity<Long>> alertedUsers) {
		this.alertedUsers = alertedUsers;
	}

}
