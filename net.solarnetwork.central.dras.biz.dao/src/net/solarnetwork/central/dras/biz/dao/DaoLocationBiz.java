/* ==================================================================
 * DaoLocationBiz.java - Jun 11, 2011 10:18:35 AM
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

package net.solarnetwork.central.dras.biz.dao;

import java.util.ArrayList;
import java.util.List;

import net.solarnetwork.central.dao.ObjectCriteria;
import net.solarnetwork.central.dao.SortDescriptor;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.dras.biz.LocationAdminBiz;
import net.solarnetwork.central.dras.biz.LocationBiz;
import net.solarnetwork.central.dras.dao.LocationDao;
import net.solarnetwork.central.dras.dao.LocationFilter;
import net.solarnetwork.central.dras.domain.Location;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.util.ClassUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO-based implementation of {@link LocationBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
@Service
public class DaoLocationBiz implements LocationBiz, LocationAdminBiz {
	
	private LocationDao locationDao;
	
	/**
	 * Construct with values.
	 * 
	 * @param locationDao the LocationDao
	 */
	@Autowired
	public DaoLocationBiz(LocationDao locationDao) {
		this.locationDao = locationDao;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Location getLocation(Long locationId) {
		return locationDao.get(locationId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<Match> findLocations(ObjectCriteria<LocationFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		FilterResults<Match> matches =  locationDao.findFiltered(
				criteria.getSimpleFilter(), sortDescriptors, 
				criteria.getResultOffset(), criteria.getResultMax());
		List<Match> result = new ArrayList<Match>(matches.getReturnedResultCount().intValue());
		for ( Match m : matches.getResults() ) {
			result.add(m);
		}
		return result;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Location storeLocation(Location template) {
		Location entity;
		if ( template.getId() != null ) {
			entity = locationDao.get(template.getId());
		} else {
			entity = new Location();
		}
		ClassUtils.copyBeanProperties(template, entity, null);
		
		Long newLocationId = locationDao.store(entity);
		return locationDao.get(newLocationId);
	}

}
