/* ==================================================================
 * MeasurementDao.java - 2/09/2022 7:19:49 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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
 */

package net.solarnetwork.central.oscp.dao;

import java.util.Collection;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.Measurement;
import net.solarnetwork.dao.DateRangeCriteria;

/**
 * API for accessing measurement data for assets.
 * 
 * @author matt
 * @version 1.0
 */
public interface MeasurementDao {

	/**
	 * Get the measurements for a given asset.
	 * 
	 * @param asset
	 *        the asset to get the measurement for
	 * @param criteria
	 *        the criteria
	 * @return the measurements
	 */
	Collection<Measurement> getMeasurements(AssetConfiguration asset, DateRangeCriteria criteria);

}
