/* ==================================================================
 * UserOcppSettingsDao.java - 27/02/2020 4:11:21 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.dao;

import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.dao.GenericDao;

/**
 * DAO API for {@link ChargePointSettings} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargePointSettingsDao extends GenericDao<ChargePointSettings, Long> {

	/**
	 * Get settings resolved using {@link UserSettings} defaults if a charge
	 * point setting does not exist.
	 * 
	 * @param id
	 *        the charge point ID to resolve settings for
	 * @return the settings, or {@literal null} if no charge point or user
	 *         settings exist
	 */
	ChargePointSettings resolveSettings(Long id);

}
