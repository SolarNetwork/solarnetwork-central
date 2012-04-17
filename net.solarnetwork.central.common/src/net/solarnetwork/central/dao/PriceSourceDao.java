/* ==================================================================
 * PriceSourceDao.java - Apr 16, 2012 1:20:20 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao;

import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.PriceSource;
import net.solarnetwork.central.domain.SourceLocation;

/**
 * DAO API for PriceSource.
 * 
 * @author matt
 * @version $Revision$
 */
public interface PriceSourceDao extends GenericDao<PriceSource, Long>,
FilterableDao<EntityMatch, Long, SourceLocation> {

	/**
	 * Find a unique PriceSource for a given name.
	 * 
	 * @param name the PriceSource name
	 * @return the PriceSource, or <em>null</em> if not found
	 */
	PriceSource getPriceSourceForName(String sourceName);

}
