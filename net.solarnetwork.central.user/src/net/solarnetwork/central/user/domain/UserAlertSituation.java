/* ==================================================================
 * UserAlertSituation.java - 15/05/2015 12:00:27 pm
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

package net.solarnetwork.central.user.domain;

import net.solarnetwork.central.domain.BaseEntity;
import org.joda.time.DateTime;

/**
 * A triggered alert condition.
 * 
 * @author matt
 * @version 1.0
 */
public class UserAlertSituation extends BaseEntity {

	private static final long serialVersionUID = 177812215085257818L;

	private UserAlert alert;
	private UserAlertSituationStatus status;
	private DateTime notified;

	public UserAlert getAlert() {
		return alert;
	}

	public void setAlert(UserAlert alert) {
		this.alert = alert;
	}

	public UserAlertSituationStatus getStatus() {
		return status;
	}

	public void setStatus(UserAlertSituationStatus status) {
		this.status = status;
	}

	public DateTime getNotified() {
		return notified;
	}

	public void setNotified(DateTime notified) {
		this.notified = notified;
	}

}
