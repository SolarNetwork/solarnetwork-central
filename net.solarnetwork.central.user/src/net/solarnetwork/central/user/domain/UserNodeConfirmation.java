/* ==================================================================
 * UserNodeConfirmation.java - Sep 7, 2011 11:06:59 AM
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

package net.solarnetwork.central.user.domain;

import net.solarnetwork.central.domain.BaseEntity;
import org.joda.time.DateTime;

/**
 * The "pending confirmation" entity for after a user generates a node
 * "invitation" to join SolarNet. The user must confirm the invitation before a
 * UserNode entity is created.
 * 
 * @author matt
 * @version 1.1
 */
public class UserNodeConfirmation extends BaseEntity {

	private static final long serialVersionUID = 405526878177177622L;

	private User user;
	private Long nodeId;
	private String confirmationKey;
	private String confirmationValue;
	private DateTime confirmationDate;
	private String securityPhrase;

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public String getConfirmationKey() {
		return confirmationKey;
	}

	public void setConfirmationKey(String confirmationKey) {
		this.confirmationKey = confirmationKey;
	}

	public String getConfirmationValue() {
		return confirmationValue;
	}

	public void setConfirmationValue(String confirmationValue) {
		this.confirmationValue = confirmationValue;
	}

	public DateTime getConfirmationDate() {
		return confirmationDate;
	}

	public void setConfirmationDate(DateTime confirmationDate) {
		this.confirmationDate = confirmationDate;
	}

	public String getSecurityPhrase() {
		return securityPhrase;
	}

	public void setSecurityPhrase(String securityPhrase) {
		this.securityPhrase = securityPhrase;
	}

}
