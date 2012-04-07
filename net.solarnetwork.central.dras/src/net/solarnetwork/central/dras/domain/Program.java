/* ==================================================================
 * Program.java - Apr 29, 2011 2:15:39 PM
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

import org.joda.time.DateTime;

import net.solarnetwork.central.domain.BaseEntity;

/**
 * A Program represents an individual demand-response (DR) strategy that clients can 
 * elect to participate in.
 * 
 * <p>Programs are created by the DRAS operator, and clients become participants
 * within the program by opting-in to receive DR events from that program. The
 * DRAS operator creates DR events for a specific program, and then participants
 * within that system, possibly filtered to a subset of participants, are issued
 * the DR events.</p>
 * 
 * <p>In SolarNetwork, a participant is a SolarNode.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public class Program extends BaseEntity implements Match, Cloneable, Serializable {

	private static final long serialVersionUID = 2465787708197385100L;

	private String name;
	private Long creator;
	private Integer priority;
	private Boolean enabled;

	/**
	 * Default constructor.
	 */
	public Program() {
		super();
	}
	
	/**
	 * Construct with values.
	 * 
	 * @param id the ID
	 * @param name the name
	 * @param priority the priority
	 */
	public Program(Long id, String name, Integer priority) {
		super();
		setId(id);
		setName(name);
		setPriority(priority);
		setCreated(new DateTime());
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getPriority() {
		return priority;
	}
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	public Long getCreator() {
		return creator;
	}
	public void setCreator(Long creator) {
		this.creator = creator;
	}
	public Boolean getEnabled() {
		return enabled;
	}
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	
}
