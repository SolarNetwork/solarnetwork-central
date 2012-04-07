/* ==================================================================
 * SimpleAlert.java - Jun 18, 2011 5:13:35 PM
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

/**
 * Simple implementation of {@link Alert}.
 * 
 * @author matt
 * @version $Revision$
 */
public class SimpleAlert implements Alert {
	
	private static final long serialVersionUID = -3700392541907383798L;

	private String alertType;
	private String description;
	private String messageBody;
	private Long actorId;
	private Identity<Long> regardingIdentity;
	private Set<Identity<Long>> recipients;

	@Override
	public String getAlertType() {
		return alertType;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getMessageBody() {
		return messageBody;
	}

	@Override
	public Identity<Long> getRegardingIdentity() {
		return regardingIdentity;
	}

	@Override
	public Set<Identity<Long>> getRecipients() {
		return recipients;
	}

	@Override
	public Long getActorId() {
		return actorId;
	}

	public void setActorId(Long actorId) {
		this.actorId = actorId;
	}
	public void setAlertType(String alertType) {
		this.alertType = alertType;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody;
	}
	public void setRegardingIdentity(Identity<Long> regardingIdentity) {
		this.regardingIdentity = regardingIdentity;
	}
	public void setRecipients(Set<Identity<Long>> recipients) {
		this.recipients = recipients;
	}

}
