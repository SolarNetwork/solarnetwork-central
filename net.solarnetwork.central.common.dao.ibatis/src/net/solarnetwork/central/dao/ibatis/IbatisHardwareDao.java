/* ==================================================================
 * IbatisHardwareDao.java - Sep 29, 2011 12:40:39 PM
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

package net.solarnetwork.central.dao.ibatis;

import java.util.Map;

import net.solarnetwork.central.dao.HardwareDao;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.Hardware;
import net.solarnetwork.central.domain.HardwareFilter;

/**
 * Ibatis implementation of {@link HardwareDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisHardwareDao
extends IbatisFilterableDaoSupport<Hardware, EntityMatch, HardwareFilter>
implements HardwareDao {

	/**
	 * Default constructor.
	 */
	public IbatisHardwareDao() {
		super(Hardware.class, EntityMatch.class);
	}
	
	@Override
	protected void postProcessFilterProperties(HardwareFilter filter,
			Map<String, Object> sqlProps) {
		// add flags to the query processor for dynamic logic
		StringBuilder fts = new StringBuilder();
		spaceAppend(filter.getName(), fts);
		if ( fts.length() > 0 ) {
			sqlProps.put("fts", fts.toString());
		}
	}

}
