/* ===================================================================
 * DayDatumDao.java
 * 
 * Created Aug 29, 2008 7:43:23 PM
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

package net.solarnetwork.central.datum.dao;

import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.datum.domain.DayDatum;
import net.solarnetwork.central.datum.domain.DayDatumMatch;
import net.solarnetwork.central.datum.domain.LocationDatumFilter;
import org.joda.time.LocalDate;

/**
 * DAO API for {@link DayDatum} data.
 * 
 * @author matt.magoffin
 * @version 1.1
 */
public interface DayDatumDao extends DatumDao<DayDatum>,
		FilterableDao<DayDatumMatch, Long, LocationDatumFilter> {

	/**
	 * Get a datum by a node ID and specific day.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param day
	 *        the day
	 * @return the PowerDatum, or <em>null</em> if not found
	 */
	DayDatum getDatumForDate(Long nodeId, LocalDate day);

}
