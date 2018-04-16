/* ==================================================================
 * UserDatumExportConfigurationDao.java - 21/03/2018 11:12:06 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.export.dao;

import net.solarnetwork.central.user.dao.UserRelatedGenericDao;
import net.solarnetwork.central.user.export.domain.UserDataConfiguration;

/**
 * DAO API for {@link UserDataConfiguration} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserDataConfigurationDao extends UserRelatedGenericDao<UserDataConfiguration, Long>,
		UserConfigurationDao<UserDataConfiguration, Long> {

}
