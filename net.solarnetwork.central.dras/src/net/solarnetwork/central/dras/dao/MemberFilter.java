/* ==================================================================
 * MemberFilter.java - Jun 15, 2011 2:30:09 PM
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

package net.solarnetwork.central.dras.dao;

import net.solarnetwork.central.domain.Filter;

/**
 * API for a filter on membership data points, e.g. Program or Event members.
 * 
 * @author matt
 * @version $Revision$
 */
public interface MemberFilter extends Filter {

	/**
	 * Find members for a specific event.
	 * 
	 * @return the event ID
	 */
	Long getEventId();

	/**
	 * Find members for a specific program.
	 * 
	 * @return the program ID
	 */
	Long getProgramId();
	
	/**
	 * Find members for a specific group.
	 * 
	 * @return the group ID
	 */
	Long getGroupId();

}
