/* ==================================================================
 * IbatisLocationDao.java - Jun 6, 2011 11:23:08 AM
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

package net.solarnetwork.central.dras.dao.ibatis;

import net.solarnetwork.central.dras.dao.LocationDao;
import net.solarnetwork.central.dras.dao.LocationFilter;
import net.solarnetwork.central.dras.domain.Location;
import net.solarnetwork.central.dras.domain.Match;

/**
 * Ibatis implementation of {@link LocationDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisLocationDao extends DrasIbatisFilterableDaoSupport<Location, Match, LocationFilter>
implements LocationDao {
	
	/**
	 * Default constructor.
	 */
	public IbatisLocationDao() {
		super(Location.class, Match.class);
	}

}
