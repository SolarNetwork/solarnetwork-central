/* ==================================================================
 * Event.java - Apr 27, 2011 7:08:09 PM
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

package net.solarnetwork.central.dras.domain;

import java.io.Serializable;

import net.solarnetwork.central.domain.BaseEntity;

import org.joda.time.DateTime;

/**
 * A DRAS event.
 * 
 * @author matt
 * @version $Revision$
 */
public class Event extends BaseEntity implements Cloneable, Serializable, Match {

	private static final long serialVersionUID = -5653502047131681962L;

	private Integer version;
	private Long creator;
	private Long programId;
	private String name;
	private String initiator;
	private DateTime notificationDate;
	private DateTime eventDate;
	private DateTime endDate;
	private Boolean enabled;
	private Boolean test;
	
	/**
	 * Default constructor.
	 */
	public Event() {
		super();
	}
	
	/**
	 * Construct with values.
	 * 
	 * @param id the ID
	 * @param programId the program ID
	 * @param name the name
	 * @param eventDate the event date
	 * @param eventPeriod the event period
	 */
	public Event(Long id, Long programId, String name, DateTime eventDate, DateTime endDate) {
		super();
		setId(id);
		setCreated(new DateTime());
		setProgramId(programId);
		setName(name);
		setEventDate(eventDate);
		setEndDate(endDate);
	}

	public Long getCreator() {
		return creator;
	}
	public void setCreator(Long creator) {
		this.creator = creator;
	}
	public Long getProgramId() {
		return programId;
	}
	public void setProgramId(Long programId) {
		this.programId = programId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getInitiator() {
		return initiator;
	}
	public void setInitiator(String initiator) {
		this.initiator = initiator;
	}
	public DateTime getNotificationDate() {
		return notificationDate;
	}
	public void setNotificationDate(DateTime notificationDate) {
		this.notificationDate = notificationDate;
	}
	public DateTime getEventDate() {
		return eventDate;
	}
	public void setEventDate(DateTime eventDate) {
		this.eventDate = eventDate;
	}
	public DateTime getEndDate() {
		return endDate;
	}
	public void setEndDate(DateTime endDate) {
		this.endDate = endDate;
	}
	public Integer getVersion() {
		return version;
	}
	public void setVersion(Integer version) {
		this.version = version;
	}
	public Boolean getEnabled() {
		return enabled;
	}
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	public Boolean getTest() {
		return test;
	}
	public void setTest(Boolean test) {
		this.test = test;
	}
	
}
