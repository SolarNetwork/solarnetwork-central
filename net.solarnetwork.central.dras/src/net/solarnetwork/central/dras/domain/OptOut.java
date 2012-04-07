/* ==================================================================
 * OptOut.java - Jun 3, 2011 3:23:34 PM
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
import java.util.Set;

import org.joda.time.Interval;

import net.solarnetwork.central.domain.BaseEntity;

/**
 * An out-out schedule.
 * 
 * @author matt
 * @version $Revision$
 */
public class OptOut extends BaseEntity implements Cloneable, Serializable {

	private static final long serialVersionUID = -6949396885338262730L;

	private Set<Interval> schedule;

	public Set<Interval> getSchedule() {
		return schedule;
	}
	public void setSchedule(Set<Interval> schedule) {
		this.schedule = schedule;
	}
	
}
