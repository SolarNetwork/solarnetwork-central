/* ===================================================================
 * SolarNodeDao.java
 * 
 * Created Aug 18, 2008 3:11:00 PM
 * 
 * Copyright (c) 2008 Solarnetwork.net Dev Team.
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
 * ===================================================================
 */

package net.solarnetwork.central.dao;

import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.domain.SolarNodeFilter;
import net.solarnetwork.central.domain.SolarNodeFilterMatch;

/**
 * DAO API for SolarNode data.
 * 
 * @author matt
 * @version 1.1
 */
public interface SolarNodeDao
		extends GenericDao<SolarNode, Long>, FilterableDao<SolarNodeFilterMatch, Long, SolarNodeFilter> {

	/**
	 * Get an unused node ID value.
	 * 
	 * <p>
	 * Once an ID has been returned by this method, that ID will never be
	 * returned again.
	 * </p>
	 * 
	 * @return an unused node ID
	 */
	Long getUnusedNodeId();

}
