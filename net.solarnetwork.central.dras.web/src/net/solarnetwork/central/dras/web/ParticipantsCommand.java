/* ==================================================================
 * EventParticipantsCommand.java - May 12, 2011 3:48:08 PM
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

package net.solarnetwork.central.dras.web;

import java.util.ArrayList;
import java.util.List;

import net.solarnetwork.central.domain.Identity;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.domain.SolarNodeGroup;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.list.LazyList;

/**
 * A command object for participant set actions.
 * 
 * <p>The {@code p} and {@code g} properties use a {@link LazyList} to help
 * with Spring binding.</p>
 * 
 * <p>Note setter methods {@link #setEventId(Long)} and {@link #setProgramId(Long)}
 * are provided for convenience and consistency in client APIs, e.g. so event related
 * APIs can always pass {@code eventId}.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public class ParticipantsCommand {

	private Long id;
	
	@SuppressWarnings("unchecked")
	private List<Identity<Long>> p = 
		LazyList.decorate(new ArrayList<Identity<Long>>(), 
		FactoryUtils.instantiateFactory(SolarNode.class));

	@SuppressWarnings("unchecked")
	private List<Identity<Long>> g = 
		LazyList.decorate(new ArrayList<Identity<Long>>(), 
		FactoryUtils.instantiateFactory(SolarNodeGroup.class));

	/**
	 * @param eventId the eventId to set
	 */
	public void setEventId(Long id) {
		setId(id);
	}

	/**
	 * @param eventId the eventId to set
	 */
	public void setProgramId(Long id) {
		setId(id);
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the targets
	 */
	public List<Identity<Long>> getP() {
		return p;
	}

	/**
	 * @param targets the targets to set
	 */
	public void setP(List<Identity<Long>> p) {
		this.p = p;
	}

	/**
	 * @return the g
	 */
	public List<Identity<Long>> getG() {
		return g;
	}

	/**
	 * @param g the g to set
	 */
	public void setG(List<Identity<Long>> g) {
		this.g = g;
	}

}
